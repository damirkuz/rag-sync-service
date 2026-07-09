package ru.kuzdikenov.ragupdater

import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
@ConfigurationPropertiesScan
class RagUpdaterApplication

fun main(args: Array<String>) {
    runApplication<RagUpdaterApplication>(*args)
}
