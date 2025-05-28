package com.maizeyield.config

import com.maizeyield.model.MaizeVariety
import com.maizeyield.repository.MaizeVarietyRepository
import mu.KotlinLogging
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component
import java.math.BigDecimal

private val logger = KotlinLogging.logger {}

@Component
class DataSeeder(
    private val maizeVarietyRepository: MaizeVarietyRepository
) : ApplicationRunner {

    override fun run(args: ApplicationArguments?) {
        seedMaizeVarieties()
    }

    private fun seedMaizeVarieties() {
        if (maizeVarietyRepository.count() == 0L) {
            logger.info { "Seeding maize varieties..." }

            val varieties = listOf(
                MaizeVariety(
                    name = "ZM 523",
                    maturityDays = 120,
                    optimalTemperatureMin = BigDecimal("18.0"),
                    optimalTemperatureMax = BigDecimal("30.0"),
                    droughtResistance = true,
                    diseaseResistance = "Gray Leaf Spot, Northern Corn Leaf Blight",
                    description = "High-yielding drought-tolerant variety suitable for medium to low rainfall areas"
                ),
                MaizeVariety(
                    name = "ZM 625",
                    maturityDays = 125,
                    optimalTemperatureMin = BigDecimal("20.0"),
                    optimalTemperatureMax = BigDecimal("32.0"),
                    droughtResistance = false,
                    diseaseResistance = "Maize Streak Virus, Common Rust",
                    description = "High-yielding variety for high potential areas with good rainfall"
                ),
                MaizeVariety(
                    name = "ZM 309",
                    maturityDays = 90,
                    optimalTemperatureMin = BigDecimal("18.0"),
                    optimalTemperatureMax = BigDecimal("28.0"),
                    droughtResistance = true,
                    diseaseResistance = "Gray Leaf Spot",
                    description = "Early maturing drought-tolerant variety for short season areas"
                ),
                MaizeVariety(
                    name = "ZM 421",
                    maturityDays = 105,
                    optimalTemperatureMin = BigDecimal("19.0"),
                    optimalTemperatureMax = BigDecimal("29.0"),
                    droughtResistance = false,
                    diseaseResistance = "Northern Corn Leaf Blight, Common Rust",
                    description = "Medium-season variety with good adaptability to various conditions"
                ),
                MaizeVariety(
                    name = "ZM 701",
                    maturityDays = 140,
                    optimalTemperatureMin = BigDecimal("20.0"),
                    optimalTemperatureMax = BigDecimal("33.0"),
                    droughtResistance = false,
                    diseaseResistance = "Maize Streak Virus, Gray Leaf Spot",
                    description = "Late maturing high-yielding variety for irrigated conditions"
                ),
                MaizeVariety(
                    name = "PAN 4M-19",
                    maturityDays = 115,
                    optimalTemperatureMin = BigDecimal("18.0"),
                    optimalTemperatureMax = BigDecimal("30.0"),
                    droughtResistance = true,
                    diseaseResistance = "Gray Leaf Spot, Turcicum Leaf Blight",
                    description = "Commercial hybrid with excellent drought tolerance and disease resistance"
                ),
                MaizeVariety(
                    name = "SC Duma 43",
                    maturityDays = 130,
                    optimalTemperatureMin = BigDecimal("19.0"),
                    optimalTemperatureMax = BigDecimal("31.0"),
                    droughtResistance = false,
                    diseaseResistance = "Northern Corn Leaf Blight, Common Rust",
                    description = "High-yielding hybrid suitable for commercial production"
                ),
                MaizeVariety(
                    name = "ZM 607",
                    maturityDays = 100,
                    optimalTemperatureMin = BigDecimal("17.0"),
                    optimalTemperatureMax = BigDecimal("28.0"),
                    droughtResistance = true,
                    diseaseResistance = "Gray Leaf Spot, Maize Streak Virus",
                    description = "Early to medium maturing drought-tolerant variety"
                ),
                MaizeVariety(
                    name = "Pioneer 30G19",
                    maturityDays = 118,
                    optimalTemperatureMin = BigDecimal("20.0"),
                    optimalTemperatureMax = BigDecimal("32.0"),
                    droughtResistance = true,
                    diseaseResistance = "Gray Leaf Spot, Northern Corn Leaf Blight, Common Rust",
                    description = "Premium hybrid with excellent stress tolerance and yield potential"
                ),
                MaizeVariety(
                    name = "Dekalb DKC 80-40",
                    maturityDays = 135,
                    optimalTemperatureMin = BigDecimal("21.0"),
                    optimalTemperatureMax = BigDecimal("34.0"),
                    droughtResistance = false,
                    diseaseResistance = "Northern Corn Leaf Blight, Southern Rust",
                    description = "Late season hybrid for high-yield potential under optimal conditions"
                )
            )

            maizeVarietyRepository.saveAll(varieties)
            logger.info { "Successfully seeded ${varieties.size} maize varieties" }
        } else {
            logger.info { "Maize varieties already exist, skipping seeding" }
        }
    }
}