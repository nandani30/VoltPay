package com.voltpay.backend

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class VoltPayBackendApplication

fun main(args: Array<String>) {
    runApplication<VoltPayBackendApplication>(*args)
}
