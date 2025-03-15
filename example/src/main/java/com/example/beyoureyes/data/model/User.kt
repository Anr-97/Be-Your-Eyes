package com.example.beyoureyes.data.model

data class User(
    val id: String,
    val email: String,
    val name: String? = null,
    val role: UserRole = UserRole.USER
)

enum class UserRole {
    USER, VOLUNTEER, ADMIN
}

data class AuthResponse(
    val token: String,
    val user: User
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class RegisterRequest(
    val email: String,
    val password: String,
    val name: String? = null,
    val role: UserRole = UserRole.USER
)

data class ResetPasswordRequest(
    val email: String
) 