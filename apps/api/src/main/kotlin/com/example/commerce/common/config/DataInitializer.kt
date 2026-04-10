package com.example.commerce.common.config

import com.example.commerce.user.entity.User
import com.example.commerce.user.entity.UserRole
import com.example.commerce.user.repository.UserRepository
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Profile
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component

@Component
@Profile("!test")
class DataInitializer(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
) : ApplicationRunner {
    override fun run(args: ApplicationArguments) {
        if (userRepository.findByEmail("admin@commerce.com") == null) {
            userRepository.save(
                User(
                    email = "admin@commerce.com",
                    password = passwordEncoder.encode("admin1234")!!,
                    name = "관리자",
                    role = UserRole.ADMIN,
                ),
            )
        }
    }
}
