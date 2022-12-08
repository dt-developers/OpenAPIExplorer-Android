package com.telekom.developer.openapi.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import java.io.Serial

@Serializable
data class Info(
    val title: String,
    val version: String,
    val description: String? = null,
    val termsOfService: String? = null,
    val contact: Contact? = null,
    val license: License? = null,
)

@Serializable
data class Contact(
    val email: String
)

@Serializable
data class License(
    val name: String,
    val url: String,
)

@Serializable
data class Variable(
    val default: String,
    val description: String? = null,
    val enum: List<String>? = null,
)

@Serializable
data class Server(
    val url: String,
    val description: String? = null,
    val variables: Map<String, Variable>? = null,
)

@Serializable
data class Operation(
    val responses: Map<String, Response>,
    val summary: String? = null,
    val description: String? = null,
    val parameters: List<Parameter>? = null,
    val requestBody: RequestBody? = null,
) {
    companion object {
        const val GET: String = "GET"
        const val POST: String = "POST"
        const val DELETE: String = "DELETE"
    }
}

@Serializable
data class RequestBody(
    @SerialName("\$ref")
    val reference: String? = null,
    val description: String? = null,
    val content: Map<String, Content>? = null,
    val required: Boolean = false,
)

@Serializable
data class Content(
    val schema: Schema? = null,
)

@Serializable
data class Response(
    val description: String? = null,
    val schema: Schema? = null,
    val content: Map<String, Content>? = null,
)

@Serializable
data class Schema(
    @SerialName("\$ref")
    val reference: String? = null,
    val title: String? = null,
    val description: String? = null,
    val type: String? = null,
    val format: String? = null,
    val properties: Map<String, Schema>? = null,
    val default: String? = null,
    val required: List<String>? = null,
)

@Serializable
data class Parameter(
    val name: String,
    @SerialName("in")
    val location: String,
    val default: String? = null,
    val description: String? = null,
    val required: Boolean = false,
    val schema: Schema,
)

@Serializable
data class Path(
    val summary: String? = null,
    val description: String? = null,
    val get: Operation? = null,
    val put: Operation? = null,
    val post: Operation? = null,
    val delete: Operation? = null,
)

@Serializable
data class ApiSpecification(
    val openapi: String,
    val info: Info,
    val paths: Map<String, Path>,
    val servers: List<Server>? = null,
)
