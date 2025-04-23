package xyz.lanshive.beyoureyes.adapter;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import xyz.lanshive.beyoureyes.R;
import xyz.lanshive.beyoureyes.model.HelpRecord;

public class HelpRecordsAdapter extends RecyclerView.Adapter<HelpRecordsAdapter.ViewHolder> {
    private List<HelpRecord> helpRecords;

    public HelpRecordsAdapter(List<HelpRecord> helpRecords) {
        // 进行空检查
        if (helpRecords != null) {
            this.helpRecords = helpRecords;
        } else {
            throw new IllegalArgumentException("helpRecords cannot be null");
        }
    }

    // 添加更新数据的方法
    public void updateData(List<HelpRecord> newHelpRecords) {
        if (newHelpRecords != null) {
            this.helpRecords = newHelpRecords;
            notifyDataSetChanged();
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_help_record, parent, false);
        return new ViewHolder(view);
    }

    @SuppressLint("DefaultLocale")
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (helpRecords != null && position < helpRecords.size()) {
            HelpRecord record = helpRecords.get(position);
            holder.userNameText.setText(String.format("帮助用户：%s", record.getUserName()));
            holder.statusText.setText(String.format("%s · %d分钟",
                record.getFormattedStatus(), record.getDuration()));
            holder.timeText.setText(record.getFormattedTime());
        }
    }

    @Override
    public int getItemCount() {
        return helpRecords != null ? helpRecords.size() : 0;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView userNameText;
        TextView statusText;
        TextView timeText;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            userNameText = itemView.findViewById(R.id.userNameText);
            statusText = itemView.findViewById(R.id.statusText);
            timeText = itemView.findViewById(R.id.timeText);
        }
    }
}