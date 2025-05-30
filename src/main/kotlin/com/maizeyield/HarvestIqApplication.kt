package com.maizeyield

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.retry.annotation.EnableRetry

@SpringBootApplication
@EnableRetry
class HarvestIqApplication

fun main(args: Array<String>) {
    runApplication<HarvestIqApplication>(*args)
}
