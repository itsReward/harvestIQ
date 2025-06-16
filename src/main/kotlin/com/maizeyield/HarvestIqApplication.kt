package com.maizeyield

import com.maizeyield.config.RecommendationProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.retry.annotation.EnableRetry

@SpringBootApplication
@EnableConfigurationProperties(RecommendationProperties::class)
@EnableRetry
class HarvestIqApplication

fun main(args: Array<String>) {
    runApplication<HarvestIqApplication>(*args)
}
