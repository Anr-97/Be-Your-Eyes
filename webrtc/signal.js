'use strict';
const log4js = require('log4js');
const https = require('https');
const fs = require('fs');
const crypto = require('crypto');
const { Server } = require('socket.io');
const express = require('express');

function generateUniqueId() {
  return crypto.randomUUID();
}

const privateKeyPath = process.env.WEBRTC_SIGNAL_SERVER_PRIVATE_KEY_PATH || '/etc/nginx/ssl/www.lanshive.xyz.key';
const certificatePath = process.env.WEBRTC_SIGNAL_SERVER_CERTIFICATE_PATH || '/etc/nginx/ssl/www.lanshive.xyz.pem';

const users = new Map();
const volunteers = new Map();
const pendingRequests = new Map();
const rooms = new Map();

log4js.configure({
  appenders: {
    file: {
      type: 'file',
      filename: 'webrtc-signal.log',
      maxLogSize: 10485760,
      backups: 3,
      layout: {
        type: 'pattern',
        pattern: '%d{yyyy-MM-dd hh:mm:ss.SSS} [%p] %c - %m'
      }
    },
    console: { type: 'console' }
  },
  categories: { default: { appenders: ['file', 'console'], level: 'debug' } }
});
const logger = log4js.getLogger('webrtc');

const app = express();
let server;

try {
  server = https.createServer({
    key: fs.readFileSync(privateKeyPath),
    cert: fs.readFileSync(certificatePath)
  }, app);
} catch (err) {
  logger.error('SSL配置错误:', err);
  process.exit(1);
}

const MAX_ROOM_SIZE = parseInt(process.env.WEBRTC_SIGNAL_SERVER_MAX_ROOM_SIZE) || 2;

const io = new Server(server, {
  cors: {
    origin: ['https://www.lanshive.xyz', 'https://lanshive.xyz'],
    methods: ['GET', 'POST'],
    allowedHeaders: ['Content-Type', 'Authorization'],
    credentials: true
  },
  pingInterval: 25000,
  pingTimeout: 60000,
  connectionStateRecovery: { maxDisconnectionDuration: 30000 }
});

// 添加一个用于详细记录SDP内容的函数
function logSdpInfo(sdp, direction, from, to) {
  try {
    if (!sdp || !sdp.type || !sdp.sdp) {
      logger.warn(`[SDP ${direction.toUpperCase()}] 收到无效的SDP格式 ${from} -> ${to || 'broadcast'}`);
      return;
    }
    
    const sdpType = sdp.type;
    const mediaLines = sdp.sdp.split('\n').filter(line => 
      line.startsWith('m=') || 
      line.startsWith('a=rtpmap:') || 
      line.startsWith('a=rtcp-fb:') ||
      line.startsWith('a=fmtp:')
    );
    
    logger.debug(`[SDP ${direction.toUpperCase()} 详情] 类型: ${sdpType}, 从 ${from} 到 ${to || 'broadcast'}`);
    logger.debug(`[SDP ${direction.toUpperCase()} 媒体] ${mediaLines.join(' | ').substring(0, 300)}...`);
    
    // 检查关键内容
    const hasAudio = sdp.sdp.includes('m=audio');
    const hasVideo = sdp.sdp.includes('m=video');
    const hasDTLS = sdp.sdp.includes('a=fingerprint');
    const hasICE = sdp.sdp.includes('a=ice-ufrag');
    
    logger.debug(`[SDP ${direction.toUpperCase()} 检查] 音频: ${hasAudio}, 视频: ${hasVideo}, DTLS: ${hasDTLS}, ICE: ${hasICE}`);
  } catch (error) {
    logger.error(`[SDP ${direction.toUpperCase()}] 分析SDP时出错:`, error);
  }
}

// 添加一个用于记录ICE候选信息的函数
function logIceCandidate(candidate, direction, from, to) {
  try {
    if (!candidate) {
      logger.warn(`[ICE ${direction.toUpperCase()}] 收到空ICE候选 ${from} -> ${to || 'broadcast'}`);
      return;
    }
    
    const { sdpMid, sdpMLineIndex, candidate: candStr } = candidate;
    
    logger.debug(`[ICE ${direction.toUpperCase()}] 从 ${from} 到 ${to || 'broadcast'}, sdpMid: ${sdpMid}, sdpMLineIndex: ${sdpMLineIndex}`);
    
    // 提取候选类型和地址
    if (candStr) {
      const typeMatch = candStr.match(/typ\s+(\w+)/);
      const addressMatch = candStr.match(/(\d+\.\d+\.\d+\.\d+)/);
      const type = typeMatch ? typeMatch[1] : 'unknown';
      const address = addressMatch ? addressMatch[1] : 'unknown';
      
      logger.debug(`[ICE ${direction.toUpperCase()} 详情] 类型: ${type}, 地址: ${address}, 原始: ${candStr.substring(0, 100)}...`);
    }
  } catch (error) {
    logger.error(`[ICE ${direction.toUpperCase()}] 分析ICE候选时出错:`, error);
  }
}

// 用户上线
io.on('connection', (socket) => {
  const clientId = socket.id;
  logger.info(`Client connected: ${clientId}`);

  // 用户上线
  socket.on('user_online', ({ userId, userType }) => {
    if (userType === 'volunteer') {
      volunteers.set(userId, { status: 'available', socketId: socket.id });
      io.emit('volunteer_status_update', { volunteerId: userId, status: 'available' });
      logger.debug(`[ONLINE] ${userId} (${userType}) connected. Socket: ${socket.id}`);
    } else {
      users.set(userId, { status: 'online', socketId: socket.id });
      logger.debug(`[ONLINE] ${userId} (user) connected. Socket: ${socket.id}`);
    }
  });

  // 创建通话请求
  socket.on('call_request', ({ userId, requirements, offer }) => {
    logger.debug(`收到 call_request，来自用户: ${userId}`);
    const requestId = generateUniqueId();
    const availableVolunteers = [...volunteers.entries()].filter(([_, v]) => v.status === 'available');
    
    if (availableVolunteers.length === 0) {
      socket.emit('call_queued', { requestId });
      logger.debug(`[CALL QUEUED] ${userId} - 无可用志愿者`);
      return;
    }
    
    const [volunteerId, volunteerInfo] = availableVolunteers[0];
    volunteers.set(volunteerId, { ...volunteerInfo, status: 'requested' });
    pendingRequests.set(requestId, {
      userId, volunteerId, requirements, timestamp: Date.now(), status: 'pending'
    });

    // 发送包含 offer 的来电信息
    io.to(volunteerInfo.socketId).emit('incoming_call', { 
      requestId, 
      userId, 
      requirements, 
      offer 
    });

    logger.debug(`[CALL REQUEST] 来自 ${userId}，匹配志愿者: ${volunteerId}`);

    // 设置超时处理
    setTimeout(() => {
      const request = pendingRequests.get(requestId);
      if (request?.status === 'pending') {
        pendingRequests.set(requestId, { ...request, status: 'timeout' });
        
        const volunteer = volunteers.get(volunteerId);
        if (volunteer) {
          volunteers.set(volunteerId, { ...volunteer, status: 'available' });
        }
        
        const user = users.get(userId);
        if (user) {
          io.to(user.socketId).emit('call_timeout', { requestId });
          logger.debug(`[CALL TIMEOUT] 请求 ${requestId} 已超时`);
        }
      }
    }, 30000);
  });

  // 接受通话请求
  socket.on('call_response', ({ requestId, accepted, answer, reason }) => {
    const request = pendingRequests.get(requestId);
    if (!request) {
      logger.error(`找不到请求ID: ${requestId}`);
      socket.emit('error', { message: 'Request not found', code: 'REQUEST_NOT_FOUND' });
      return;
    }

    if (accepted) {
      const roomId = `call_${requestId}`;
      pendingRequests.set(requestId, { ...request, status: 'accepted', roomId });
      
      const volunteer = volunteers.get(request.volunteerId);
      if (volunteer) {
        volunteers.set(request.volunteerId, { ...volunteer, status: 'busy' });
      }

      const user = users.get(request.userId);
      if (user) {
        logger.info(`[CALL ACCEPTED] 请求 ${requestId} 已被接受，发送answer到用户 ${request.userId}`);
        // 记录answer SDP详情
        if (answer && typeof answer === 'object') {
          logSdpInfo(answer, 'answer', socket.id, user.socketId);
        } else {
          logger.warn(`[CALL ACCEPTED] 请求 ${requestId} 接受但answer无效或为空`);
        }
        
        io.to(user.socketId).emit('call_accepted', { 
          requestId, 
          roomId,
          answer
        });
        logger.debug(`[CALL ACCEPTED] 请求 ${requestId} 已被接受，创建房间 ${roomId}`);
      } else {
        logger.error(`[CALL ACCEPTED] 请求 ${requestId} 接受后找不到用户 ${request.userId}`);
      }

      // 创建房间
      if (!rooms.has(roomId)) {
        rooms.set(roomId, {
          clients: new Set([user?.socketId, volunteer?.socketId].filter(Boolean)),
          offers: new Map(),
          answers: new Map(),
          createdAt: Date.now()
        });
        
        // 将两个客户端加入同一房间
        if (user?.socketId) {
          const userSocket = io.sockets.sockets.get(user.socketId);
          if (userSocket) {
              userSocket.join(roomId);
              logger.info(`[JOIN ROOM] 用户 ${user.socketId} 加入房间 ${roomId}`);
          } else {
              logger.warn(`[WARN] 用户 socketId ${user.socketId} 无法找到 socket 实例`);
          }
        }

        if (volunteer?.socketId) {
          const volunteerSocket = io.sockets.sockets.get(volunteer.socketId);
          if (volunteerSocket) {
              volunteerSocket.join(roomId);
              logger.info(`[JOIN ROOM] 志愿者 ${volunteer.socketId} 加入房间 ${roomId}`);
          } else {
              logger.warn(`[WARN] 志愿者 socketId ${volunteer.socketId} 无法找到 socket 实例`);
          }
        }
        
        // 记录房间信息
        const roomClients = rooms.get(roomId).clients;
        logger.info(`已创建房间 ${roomId} 包含客户端: ${Array.from(roomClients).join(', ')}`);
        
        // 检查房间是否包含正确的两个客户端
        if (roomClients.size !== 2) {
          logger.warn(`[ROOM WARN] 房间 ${roomId} 客户端数量异常: ${roomClients.size}`);
        }
      } else {
        logger.warn(`[ROOM WARN] 尝试创建已存在的房间 ${roomId}`);
      }
    } else {
      pendingRequests.delete(requestId);
      
      const volunteer = volunteers.get(request.volunteerId);
      if (volunteer) {
        volunteers.set(request.volunteerId, { ...volunteer, status: 'available' });
      }

      const user = users.get(request.userId);
      if (user) {
        io.to(user.socketId).emit('call_rejected', { requestId, reason });
        logger.debug(`[CALL REJECTED] 请求 ${requestId} 被拒绝: ${reason || '无原因'}`);
      }
    }
  });

  // 处理 Offer
  socket.on('offer', ({ roomId, to, sdp }) => {
    if (!sdp || typeof sdp !== 'object') {
      logger.error(`收到无效的SDP offer，来自 ${clientId} 在房间 ${roomId}`);
      socket.emit('error', { message: 'Invalid SDP offer', code: 'INVALID_SDP' });
      return;
    }
    
    logger.info(`收到offer，来自 ${clientId} 在房间 ${roomId}`);
    // 记录offer SDP详情
    logSdpInfo(sdp, 'offer', clientId, to);
    
    if (rooms.has(roomId)) {
      const room = rooms.get(roomId);
      room.offers.set(clientId, sdp);
      
      if (to) {
        // 检查目标socket是否存在和连接
        const targetSocket = io.sockets.sockets.get(to);
        if (!targetSocket) {
          logger.warn(`[OFFER 传递失败] 目标客户端 ${to} 不存在或未连接`);
          socket.emit('error', { message: 'Target client not found', code: 'TARGET_NOT_FOUND' });
          return;
        }
        
        // 直接发送给指定客户端
        io.to(to).emit('offer', { from: clientId, sdp });
        logger.info(`转发offer到指定客户端 ${to} 成功`);
      } else {
        // 检查房间内其他客户端
        const otherClients = Array.from(room.clients).filter(c => c !== clientId);
        if (otherClients.length === 0) {
          logger.warn(`[OFFER 广播] 房间 ${roomId} 内没有其他客户端`);
        }
        
        // 广播给房间内其他客户端
        socket.to(roomId).emit('offer', { from: clientId, sdp });
        logger.info(`广播offer到房间 ${roomId} 的其他客户端: ${otherClients.join(', ')}`);
      }
    } else {
      logger.error(`找不到房间 ${roomId}，来自 ${clientId} 的offer`);
      socket.emit('error', { message: 'Room not found', code: 'ROOM_NOT_FOUND' });
    }
  });
  
  // 处理 Answer
  socket.on('answer', ({ roomId, to, sdp }) => {
    if (!sdp || typeof sdp !== 'object') {
      logger.error(`收到无效的SDP answer，来自 ${clientId} 在房间 ${roomId}`);
      socket.emit('error', { message: 'Invalid SDP answer', code: 'INVALID_SDP' });
      return;
    }
    
    logger.info(`收到answer，来自 ${clientId} 在房间 ${roomId}`);
    // 记录answer SDP详情
    logSdpInfo(sdp, 'answer', clientId, to);
    
    const room = rooms.get(roomId);
    if (room) {
      room.answers.set(clientId, sdp);
      
      if (to) {
        // 检查目标socket是否存在和连接
        const targetSocket = io.sockets.sockets.get(to);
        if (!targetSocket) {
          logger.warn(`[ANSWER 传递失败] 目标客户端 ${to} 不存在或未连接`);
          socket.emit('error', { message: 'Target client not found', code: 'TARGET_NOT_FOUND' });
          return;
        }
        
        // 直接发送给指定客户端
        io.to(to).emit('answer', { from: clientId, sdp });
        logger.info(`转发answer到客户端 ${to} 成功`);
      } else {
        // 检查房间内其他客户端
        const otherClients = Array.from(room.clients).filter(c => c !== clientId);
        if (otherClients.length === 0) {
          logger.warn(`[ANSWER 广播] 房间 ${roomId} 内没有其他客户端`);
        }
        
        // 广播给房间内其他客户端
        socket.to(roomId).emit('answer', { from: clientId, sdp });
        logger.info(`广播answer到房间 ${roomId} 的其他客户端: ${otherClients.join(', ')}`);
      }
      
      // 记录房间状态变更
      logger.debug(`房间 ${roomId} 状态更新：offer数量=${room.offers.size}, answer数量=${room.answers.size}`);
    } else {
      logger.error(`找不到房间 ${roomId}，来自 ${clientId} 的answer`);
      socket.emit('error', { message: 'Room not found', code: 'ROOM_NOT_FOUND' });
    }
  });
  
  // 处理 ICE 候选
  socket.on('ice-candidate', ({ roomId, to, candidate }) => {
    if (!candidate || typeof candidate !== 'object') {
      logger.error(`收到无效的ICE候选，来自 ${clientId} 在房间 ${roomId}`);
      return;
    }
    
    logger.info(`收到ICE候选，来自 ${clientId} 在房间 ${roomId}`);
    // 记录ICE候选详情
    logIceCandidate(candidate, 'received', clientId, to);
    
    const room = rooms.get(roomId);
    if (room) {
      if (to) {
        // 检查目标socket是否存在和连接
        const targetSocket = io.sockets.sockets.get(to);
        if (!targetSocket) {
          logger.warn(`[ICE 传递失败] 目标客户端 ${to} 不存在或未连接`);
          return;
        }
        
        // 直接发送给指定客户端
        io.to(to).emit('ice-candidate', { from: clientId, roomId, candidate });
        logger.info(`转发ICE候选到客户端 ${to} 成功`);
      } else {
        // 检查房间内其他客户端
        const otherClients = Array.from(room.clients).filter(c => c !== clientId);
        if (otherClients.length === 0) {
          logger.warn(`[ICE 广播] 房间 ${roomId} 内没有其他客户端`);
        }
        
        // 广播给房间内其他客户端
        socket.to(roomId).emit('ice-candidate', { from: clientId, roomId, candidate });
        logger.info(`广播ICE候选到房间 ${roomId} 的其他客户端: ${otherClients.join(', ')}`);
      }
    } else {
      logger.error(`找不到房间 ${roomId}，来自 ${clientId} 的ICE候选`);
    }
  });

  // 添加对新命名的ICE候选事件的支持
  socket.on('ice_candidate', ({ roomId, to, candidate }) => {
    if (!candidate || typeof candidate !== 'object') {
      logger.error(`收到无效的ICE候选者，来自 ${clientId} 在房间 ${roomId}`);
      return;
    }
    
    logger.info(`收到ICE候选者(ice_candidate)，来自 ${clientId} 在房间 ${roomId}`);
    // 记录ICE候选详情
    logIceCandidate(candidate, 'received', clientId, to);
    
    const room = rooms.get(roomId);
    if (room) {
      if (to) {
        // 检查目标socket是否存在和连接
        const targetSocket = io.sockets.sockets.get(to);
        if (!targetSocket) {
          logger.warn(`[ICE 传递失败] 目标客户端 ${to} 不存在或未连接`);
          return;
        }
        
        // 直接发送给指定客户端
        io.to(to).emit('ice_candidate', { from: clientId, roomId, candidate });
        logger.info(`转发ICE候选者到客户端 ${to} 成功`);
      } else {
        // 检查房间内其他客户端
        const otherClients = Array.from(room.clients).filter(c => c !== clientId);
        if (otherClients.length === 0) {
          logger.warn(`[ICE 广播] 房间 ${roomId} 内没有其他客户端`);
        }
        
        // 广播给房间内其他客户端
        socket.to(roomId).emit('ice_candidate', { from: clientId, roomId, candidate });
        logger.info(`广播ICE候选者到房间 ${roomId} 的其他客户端: ${otherClients.join(', ')}`);
      }
    } else {
      logger.error(`找不到房间 ${roomId}，来自 ${clientId} 的ICE候选者`);
    }
  });

  // 用户加入房间
  socket.on('join', (roomId, callback) => {
    try {
      roomId = roomId.toString();
      if (!rooms.has(roomId)) {
        rooms.set(roomId, {
          clients: new Set(),
          offers: new Map(),
          answers: new Map()
        });
      }
      
      const room = rooms.get(roomId);
      if (room.clients.size >= MAX_ROOM_SIZE) {
        throw new Error('ROOM_FULL');
      }
      
      room.clients.add(clientId);
      socket.join(roomId);
      
      callback?.({ 
        status: 'success', 
        roomId, 
        clientCount: room.clients.size,
        clients: Array.from(room.clients)
      });
      logger.info(`客户端 ${clientId} 加入了房间 ${roomId}`);

      if (room.clients.size > 1) {
        socket.to(roomId).emit('peer-joined', {
          newClient: clientId,
          roomSize: room.clients.size
        });
      }
    } catch (err) {
      logger.error(`加入房间错误: ${err.message}`, { clientId, roomId });
      callback?.({ 
        status: 'error', 
        code: err.message === 'ROOM_FULL' ? 429 : 500, 
        message: err.message 
      });
    }
  });

  // 结束通话
  socket.on('end_call', ({ roomId }) => {
    logger.debug(`客户端 ${clientId} 请求结束通话，房间: ${roomId}`);
    
    if (rooms.has(roomId)) {
      socket.to(roomId).emit('call_ended', { by: clientId });
      
      // 清理房间
      const room = rooms.get(roomId);
      room.clients.forEach(client => {
        const socketObj = io.sockets.sockets.get(client);
        if (socketObj) {
          socketObj.leave(roomId);
        }
      });
      
      rooms.delete(roomId);
      logger.debug(`已清理房间 ${roomId}`);
      
      // 重置志愿者状态
      for (const [volunteerId, info] of volunteers.entries()) {
        if (room.clients.has(info.socketId)) {
          volunteers.set(volunteerId, { ...info, status: 'available' });
          io.emit('volunteer_status_update', { volunteerId, status: 'available' });
          logger.debug(`重置志愿者 ${volunteerId} 状态为可用`);
          break;
        }
      }
    }
  });

  // 用户断开连接
  socket.on('disconnect', () => {
    logger.info(`客户端断开连接: ${clientId}`);

    // 清理房间数据
    rooms.forEach((room, roomId) => {
      if (room.clients.has(clientId)) {
        room.clients.delete(clientId);
        room.offers.delete(clientId);
        room.answers.delete(clientId);
        
        socket.to(roomId).emit('peer-left', {
          leftClient: clientId,
          remaining: room.clients.size
        });
        
        if (room.clients.size === 0) {
          rooms.delete(roomId);
          logger.debug(`删除空房间 ${roomId}`);
        }
      }
    });

    // 清理用户数据
    for (const [userId, info] of users.entries()) {
      if (info.socketId === clientId) {
        users.delete(userId);
        logger.debug(`用户 ${userId} 已离线`);
        break;
      }
    }

    // 清理志愿者数据
    for (const [volunteerId, info] of volunteers.entries()) {
      if (info.socketId === clientId) {
        volunteers.delete(volunteerId);
        io.emit('volunteer_status_update', { volunteerId, status: 'offline' });
        logger.debug(`志愿者 ${volunteerId} 已离线`);
        break;
      }
    }

    // 清理待处理的请求
    for (const [requestId, request] of pendingRequests.entries()) {
      const volunteerInfo = volunteers.get(request.volunteerId);
      const userInfo = users.get(request.userId);
      
      if (
        (volunteerInfo && volunteerInfo.socketId === clientId) || 
        (userInfo && userInfo.socketId === clientId)
      ) {
        // 如果是志愿者断开，通知用户
        if (volunteerInfo && volunteerInfo.socketId === clientId && userInfo) {
          io.to(userInfo.socketId).emit('call_rejected', { 
            requestId, 
            reason: '志愿者已断开连接' 
          });
        }
        
        // 如果是用户断开，通知志愿者
        if (userInfo && userInfo.socketId === clientId && volunteerInfo) {
          io.to(volunteerInfo.socketId).emit('call_cancelled', { 
            requestId, 
            reason: '用户已断开连接' 
          });
          
          // 重置志愿者状态
          volunteers.set(request.volunteerId, { ...volunteerInfo, status: 'available' });
          io.emit('volunteer_status_update', { 
            volunteerId: request.volunteerId, 
            status: 'available' 
          });
        }
        
        pendingRequests.delete(requestId);
        logger.debug(`已删除请求 ${requestId} (断开连接)`);
      }
    }
  });
});

// 启动服务器
server.listen(8080, '0.0.0.0', () => {
  logger.info(`WebRTC信令服务器已启动，监听端口: 8080`);
});

server.on('error', (err) => logger.error('服务器错误:', err));
io.on('error', (err) => logger.error('Socket.IO错误:', err));

// 对状态检查接口增加房间详情
app.get('/status', (req, res) => {
  res.json({
    users: [...users.entries()].map(([id, data]) => ({ id, status: data.status })),
    volunteers: [...volunteers.entries()].map(([id, data]) => ({ id, status: data.status })),
    rooms: [...rooms.entries()].map(([roomId, roomData]) => ({ 
      id: roomId, 
      clients: Array.from(roomData.clients),
      hasOffers: roomData.offers.size > 0,
      hasAnswers: roomData.answers.size > 0,
      createdAt: roomData.createdAt || 'unknown'
    })),
    pendingRequests: [...pendingRequests.entries()].map(([id, data]) => ({ 
      id, 
      userId: data.userId,
      volunteerId: data.volunteerId, 
      status: data.status,
      timestamp: data.timestamp,
      roomId: data.roomId
    }))
  });
});

// 添加WebRTC诊断端点
app.get('/diagnose/:roomId', (req, res) => {
  const roomId = req.params.roomId;
  const room = rooms.get(roomId);
  
  if (!room) {
    return res.json({ 
      status: 'error', 
      message: `房间 ${roomId} 不存在` 
    });
  }
  
  res.json({
    roomId,
    clients: Array.from(room.clients),
    clientCount: room.clients.size,
    offers: {
      count: room.offers.size,
      from: Array.from(room.offers.keys())
    },
    answers: {
      count: room.answers.size,
      from: Array.from(room.answers.keys())
    },
    createdAt: room.createdAt || 'unknown',
    age: room.createdAt ? `${Math.round((Date.now() - room.createdAt) / 1000)}秒` : 'unknown'
  });
});

// 健康检查端点
app.get('/health', (req, res) => {
  res.status(200).send('OK');
});