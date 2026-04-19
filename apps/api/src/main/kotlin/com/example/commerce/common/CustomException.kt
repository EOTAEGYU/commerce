package com.example.commerce.common

class CustomException(val errorCode: ErrorCode) : RuntimeException(errorCode.message)
