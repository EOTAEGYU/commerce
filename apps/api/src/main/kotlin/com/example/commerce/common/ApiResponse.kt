package com.example.commerce.common

data class ApiResponse<T>(
    val success: Boolean,
    val data: T?,
    val error: ErrorResponse?,
) {
    companion object {
        fun <T> success(data: T): ApiResponse<T> =
            ApiResponse(success = true, data = data, error = null)

        fun <T> error(errorCode: ErrorCode): ApiResponse<T> =
            ApiResponse(success = false, data = null, error = ErrorResponse.of(errorCode))
    }
}

data class ErrorResponse(
    val code: String,
    val message: String,
) {
    companion object {
        fun of(errorCode: ErrorCode): ErrorResponse =
            ErrorResponse(code = errorCode.name, message = errorCode.message)
    }
}
