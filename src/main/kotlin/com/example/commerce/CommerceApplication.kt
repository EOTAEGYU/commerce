package com.example.commerce

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaAuditing

@EnableJpaAuditing
@SpringBootApplication
class CommerceApplication

fun main(args: Array<String>) {
    runApplication<CommerceApplication>(*args)
}
