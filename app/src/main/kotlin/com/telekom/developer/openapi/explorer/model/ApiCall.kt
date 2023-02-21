package com.telekom.developer.openapi.explorer.model

import kotlinx.serialization.Serializable

@Serializable
data class ApiCall(
    val method: String,
    val api: String,
    val requestHeaders: Map<String, List<String>>,
    val requestBody: String,
    val responseCode: Int,
    val responseBody: String,
)