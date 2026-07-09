package ru.kuzdikenov.ragupdater

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(properties = ["rag-sync.worker.enabled=false"])
class RagUpdaterApplicationTests {

    @Test
    fun contextLoads() {
    }

}
