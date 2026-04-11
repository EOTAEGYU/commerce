package com.example.commerce.common.config

import com.example.commerce.category.dto.CategoryCreateRequest
import com.example.commerce.category.service.CategoryService
import com.example.commerce.product.dto.ProductCreateRequest
import com.example.commerce.product.dto.ProductOptionRequest
import com.example.commerce.product.repository.ProductRepository
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
    private val categoryService: CategoryService,
    private val productRepository: ProductRepository,
    private val productService: com.example.commerce.product.service.ProductService,
) : ApplicationRunner {
    override fun run(args: ApplicationArguments) {
        initAdmin()
        if (productRepository.count() == 0L) {
            initSampleData()
        }
    }

    private fun initAdmin() {
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

    private fun initSampleData() {
        // 대분류 카테고리
        val top = categoryService.create(CategoryCreateRequest("상의", displayOrder = 1))
        val bottom = categoryService.create(CategoryCreateRequest("하의", displayOrder = 2))
        val shoes = categoryService.create(CategoryCreateRequest("신발", displayOrder = 3))

        // 소분류 카테고리
        val tshirt = categoryService.create(CategoryCreateRequest("반팔티셔츠", parentId = top.id, displayOrder = 1))
        val sweatshirt = categoryService.create(CategoryCreateRequest("맨투맨", parentId = top.id, displayOrder = 2))
        val jeans = categoryService.create(CategoryCreateRequest("청바지", parentId = bottom.id, displayOrder = 1))
        val slacks = categoryService.create(CategoryCreateRequest("슬랙스", parentId = bottom.id, displayOrder = 2))
        val sneakers = categoryService.create(CategoryCreateRequest("스니커즈", parentId = shoes.id, displayOrder = 1))

        // 상품 (반팔티셔츠)
        repeat(3) { i ->
            productService.create(
                ProductCreateRequest(
                    name = listOf("베이직 반팔 티셔츠", "스트라이프 반팔 티셔츠", "포켓 반팔 티셔츠")[i],
                    description = "편안하고 깔끔한 반팔 티셔츠입니다.",
                    price = listOf(19900L, 24900L, 22900L)[i],
                    categoryId = tshirt.id!!,
                    options = listOf(
                        ProductOptionRequest("S", "화이트", 30),
                        ProductOptionRequest("M", "화이트", 40),
                        ProductOptionRequest("L", "화이트", 20),
                        ProductOptionRequest("S", "블랙", 25),
                        ProductOptionRequest("M", "블랙", 35),
                        ProductOptionRequest("L", "블랙", 0),
                    ),
                ),
            )
        }

        // 상품 (맨투맨)
        repeat(3) { i ->
            productService.create(
                ProductCreateRequest(
                    name = listOf("크루넥 맨투맨", "후드 맨투맨", "오버핏 맨투맨")[i],
                    description = "가을/겨울 시즌 베스트 맨투맨입니다.",
                    price = listOf(39900L, 49900L, 44900L)[i],
                    categoryId = sweatshirt.id!!,
                    options = listOf(
                        ProductOptionRequest("S", "그레이", 20),
                        ProductOptionRequest("M", "그레이", 30),
                        ProductOptionRequest("L", "그레이", 15),
                        ProductOptionRequest("S", "네이비", 20),
                        ProductOptionRequest("M", "네이비", 25),
                    ),
                ),
            )
        }

        // 상품 (청바지)
        repeat(3) { i ->
            productService.create(
                ProductCreateRequest(
                    name = listOf("슬림 스트레이트 청바지", "와이드 청바지", "스키니 청바지")[i],
                    description = "데일리로 입기 좋은 청바지입니다.",
                    price = listOf(59900L, 64900L, 54900L)[i],
                    categoryId = jeans.id!!,
                    options = listOf(
                        ProductOptionRequest("28", "인디고", 15),
                        ProductOptionRequest("30", "인디고", 20),
                        ProductOptionRequest("32", "인디고", 10),
                        ProductOptionRequest("28", "블랙", 10),
                        ProductOptionRequest("30", "블랙", 15),
                    ),
                ),
            )
        }

        // 상품 (슬랙스)
        repeat(2) { i ->
            productService.create(
                ProductCreateRequest(
                    name = listOf("테이퍼드 슬랙스", "와이드 슬랙스")[i],
                    description = "오피스 룩에 어울리는 슬랙스입니다.",
                    price = listOf(49900L, 54900L)[i],
                    categoryId = slacks.id!!,
                    options = listOf(
                        ProductOptionRequest("28", "차콜", 15),
                        ProductOptionRequest("30", "차콜", 20),
                        ProductOptionRequest("32", "차콜", 10),
                        ProductOptionRequest("28", "베이지", 10),
                        ProductOptionRequest("30", "베이지", 12),
                    ),
                ),
            )
        }

        // 상품 (스니커즈)
        repeat(3) { i ->
            productService.create(
                ProductCreateRequest(
                    name = listOf("캔버스 스니커즈", "러닝 스니커즈", "하이탑 스니커즈")[i],
                    description = "가볍고 편안한 스니커즈입니다.",
                    price = listOf(59900L, 89900L, 79900L)[i],
                    categoryId = sneakers.id!!,
                    options = listOf(
                        ProductOptionRequest("250", "화이트", 10),
                        ProductOptionRequest("260", "화이트", 15),
                        ProductOptionRequest("270", "화이트", 8),
                        ProductOptionRequest("250", "블랙", 10),
                        ProductOptionRequest("260", "블랙", 12),
                        ProductOptionRequest("270", "블랙", 0),
                    ),
                ),
            )
        }
    }
}
