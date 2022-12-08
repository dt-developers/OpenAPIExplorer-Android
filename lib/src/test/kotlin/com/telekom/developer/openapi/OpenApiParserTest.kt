package com.telekom.developer.openapi

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class OpenApiParserTest {

    private fun loadFile(path: String) =
        OpenApiParser::class.java.getResource("/$path")?.readText() ?: ""


    @Test
    fun `can convert simple GET operation`() {
        val simpleFileContent = loadFile("get.yml")

        val specification = OpenApiParser.parse(simpleFileContent)

        assertEquals("Sample API", specification.info.title)
        assertEquals("0.1.9", specification.info.version)

        assertNotNull(specification.servers)
        assertEquals(2, specification.servers?.count())

        assertEquals("/users", specification.paths.keys.first())
        assertNotNull(specification.paths["/users"]?.get)
        assertNotNull(specification.paths["/users"]?.get?.responses)
        assertEquals("200", specification.paths["/users"]?.get?.responses?.keys?.first())

        assertEquals(
            "A JSON array of user names",
            specification.paths["/users"]?.get?.responses?.get("200")?.description
        )

        assertEquals(
            "array",
            specification.paths["/users"]?.get?.responses?.get("200")?.content?.values?.first()?.schema?.type
        )
    }

    @Test
    fun `can convert GET operation _with_ references`() {
        val simpleFileContent = loadFile("get_with_references.yml")

        val specification = OpenApiParser.parse(simpleFileContent)

        assertEquals(
            "integer",
            specification.paths["/users/{id}"]?.get?.parameters?.first()?.schema?.type
        )
    }

    @Test
    fun `can convert POST operation _with_ references`() {
        val simpleFileContent = loadFile("post_with_references.yml")

        val specification = OpenApiParser.parse(simpleFileContent)

        assertEquals(
            "integer",
            specification.paths["/users/"]?.post?.requestBody?.content?.values?.first()?.schema?.type
        )
    }
}