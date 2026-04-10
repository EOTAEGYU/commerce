package com.example.commerce.user.repository

import com.example.commerce.user.entity.User
import com.example.commerce.user.entity.UserRole
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest

@DataJpaTest(properties = ["spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"])
class UserRepositoryTest {

    @Autowired
    lateinit var userRepository: UserRepository

    @BeforeEach
    fun setUp() {
        userRepository.deleteAll()
    }

    private fun saveUser(email: String = "test@test.com") =
        userRepository.save(
            User(email = email, password = "encoded_password", name = "홍길동", role = UserRole.USER)
        )

    @Nested
    inner class FindByEmail {

        @Test
        fun `저장된 이메일로 조회 시 User 반환`() {
            saveUser("test@test.com")

            val result = userRepository.findByEmail("test@test.com")

            assertNotNull(result)
            assertEquals("test@test.com", result!!.email)
        }

        @Test
        fun `존재하지 않는 이메일 조회 시 null 반환`() {
            val result = userRepository.findByEmail("none@test.com")

            assertNull(result)
        }
    }

    @Nested
    inner class ExistsByEmail {

        @Test
        fun `저장된 이메일은 true 반환`() {
            saveUser("test@test.com")

            assertTrue(userRepository.existsByEmail("test@test.com"))
        }

        @Test
        fun `존재하지 않는 이메일은 false 반환`() {
            assertFalse(userRepository.existsByEmail("none@test.com"))
        }
    }

    @Nested
    inner class Save {

        @Test
        fun `회원 저장 시 id 자동 생성 및 createdAt 설정`() {
            val user = saveUser()

            assertTrue(user.id > 0)
            assertNotNull(user.createdAt)
            assertNotNull(user.updatedAt)
        }

        @Test
        fun `동일 이메일 저장 시 예외 발생`() {
            saveUser("test@test.com")

            assertThrows(Exception::class.java) {
                saveUser("test@test.com")
                userRepository.flush()
            }
        }
    }
}
