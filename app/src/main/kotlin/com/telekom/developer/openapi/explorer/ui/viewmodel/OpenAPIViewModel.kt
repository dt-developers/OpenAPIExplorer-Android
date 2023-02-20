package com.telekom.developer.openapi.explorer.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.charleskorn.kaml.MissingRequiredPropertyException
import com.telekom.developer.openapi.OpenApiParser
import com.telekom.developer.openapi.exception.CouldNotResolveReferenceException
import com.telekom.developer.openapi.explorer.model.ApiCall
import com.telekom.developer.openapi.explorer.ui.model.UserDialog
import com.telekom.developer.openapi.explorer.ui.model.UserDialog.UserInputDialog
import com.telekom.developer.openapi.explorer.ui.model.UserInput
import com.telekom.developer.openapi.model.ApiSpecification
import com.telekom.developer.openapi.model.Operation
import com.telekom.developer.openapi.model.Schema
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okio.Buffer
import java.io.File
import java.util.concurrent.TimeUnit

private const val PREFERENCES_LAST_API_ASSET = "lastAPAsset"
private const val PREFERENCES_LAST_API_WAS_FROM_ASSETS = "lastAPIFromAssets"
private const val PRIVATE_LAST_API_CACHE_FILE = "last_loaded_api.yml"

class OpenAPIViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val sharedPreferences =
        getApplication<Application>()
            .getSharedPreferences("defaults", Application.MODE_PRIVATE)

    val dialog = mutableStateOf<UserDialog?>(null)

    val api = mutableStateOf<ApiSpecification?>(null)
    val apiCalls = mutableStateOf(listOf<ApiCall>())
    val error = mutableStateOf("")

    val toggled = mutableStateOf(listOf<Int>())

    private val client = OkHttpClient.Builder()
        .callTimeout(10, TimeUnit.SECONDS)
        .build()

    fun listAPIsInAssets() {
        val apis = getApplication<Application>()
            .assets
            .list("")
            ?.filter {
                it.endsWith("yml")
            }.orEmpty()

        dialog.value = UserDialog.SelectAPIsDialog(
            apis
        )
    }

    fun clearAPI() {
        api.value = null
        apiCalls.value = listOf()
    }


    fun loadLastAPI() {
        val path = sharedPreferences.getString(PREFERENCES_LAST_API_ASSET, null)

        if (path != null) {
            val fromAssets =
                sharedPreferences.getBoolean(PREFERENCES_LAST_API_WAS_FROM_ASSETS, false)

            if (fromAssets) {
                loadAPIFromAssets(path)
            } else {
                loadAPIFromUri(
                    Uri.parse(
                        "file://${getApplication<Application>().filesDir}" +
                                "/${PRIVATE_LAST_API_CACHE_FILE}"
                    )
                )
            }
        }
    }

    fun loadAPIFromUri(uri: Uri) {
        val app = getApplication<Application>()
        val stream = app.contentResolver.openInputStream(uri)
        if (stream != null) {
            val body = String(bytes = stream.readBytes())
            stream.close()

            val file = File(app.filesDir, PRIVATE_LAST_API_CACHE_FILE)
            file.writeText(body)

            if (parseApiBody(body)) {
                sharedPreferences.edit()
                    .putBoolean(PREFERENCES_LAST_API_WAS_FROM_ASSETS, false)
                    .apply()

                apiCalls.value = listOf()
            } // no error checking here, it is done in the parser method
        } else {
            error.value = "Couldn't open stream."
        }
    }

    fun loadAPIFromAssets(asset: String) {
        error.value = ""
        apiCalls.value = emptyList()

        val apiBody = getApplication<Application>().assets
            .open(asset)
            .reader()
            .readText()

        if (parseApiBody(apiBody)) {
            sharedPreferences.edit()
                .putBoolean(PREFERENCES_LAST_API_WAS_FROM_ASSETS, true)
                .putString(PREFERENCES_LAST_API_ASSET, asset)
                .apply()

            apiCalls.value = listOf()
        }
    }

    private fun parseApiBody(apiBody: String): Boolean {
        return try {
            api.value = OpenApiParser.parse(apiBody)
            true
        } catch (noResolution: CouldNotResolveReferenceException) {
            error.value = noResolution.reference
            false
        } catch (missing: MissingRequiredPropertyException) {
            error.value =
                "${missing.message}\n\n" +
                        "line: ${missing.line}:${missing.column}\n" +
                        missing.path.toHumanReadableString()
            false
        } catch (throwable: Throwable) {
            error.value = throwable.localizedMessage ?: throwable.message ?: throwable.toString()
            false
        }
    }

    fun apiClicked(url: String, method: String, operation: Operation) {
        val parametersRequested = prepareUserInput(operation)
        if (parametersRequested.isEmpty()) {
            // no user input needed, no variables set in yml
            launchApi(url, method, operation, mapOf())
        } else {
            // ask user for parameters
            dialog.value = UserInputDialog(
                inputsNeeded = parametersRequested,
                url = url,
                method = method,
                operation = operation
            )
        }
    }

    private fun prepareUserInput(operation: Operation): Map<String, UserInput> {
        val result = mutableMapOf<String, UserInput>()

        for ((index, server) in api.value?.servers.orEmpty().withIndex()) {
            for (variableName in server.variables?.keys.orEmpty()) {
                val variable = server.variables?.get(variableName)!!
                val key = "baseUrl.$index.$variableName"
                result[key] = UserInput(
                    name = variableName,
                    required = true,
                    details = variable.description,
                    previous = previousUserInput(key, variable.default)
                )
            }

            if (server.variables.isNullOrEmpty()) {
                val regex = "\\{(.+)\\}".toRegex()
                val match = regex.find(server.url)
                if (match != null) {
                    val variableName = match.groupValues[1]
                    val key = "baseUrl.$index.$variableName"
                    result[key] = UserInput(
                        name = variableName,
                        required = true,
                        previous = previousUserInput(key, "")
                    )
                }
            }
        }

        for (parameter in operation.parameters ?: emptyList()) {
            val key = parameter.name
            result[key] =
                UserInput(
                    parameter.name,
                    parameter.required,
                    parameter.schema.description,
                    previousUserInput(key, parameter.default ?: ""),
                )
        }

        for ((name, content) in operation.requestBody?.content ?: emptyMap()) {
            val schema = content.schema
            if (schema != null) {
                val fromBody = parseSchemaForParameters(name, schema)
                result.putAll(fromBody)
            }
        }

        return result
    }

    private fun previousUserInput(key: String, defaultValue: String): String {
        return sharedPreferences.getString(key, defaultValue) ?: defaultValue
    }

    private fun parseSchemaForParameters(
        name: String,
        schema: Schema,
    ): Map<String, UserInput> {
        return if (schema.properties != null && schema.properties!!.isNotEmpty()) {
            val result = mutableMapOf<String, UserInput>()
            for ((childName, childSchema) in schema.properties ?: emptyMap()) {
                val childParameters =
                    parseSchemaForParameters("$name.$childName", childSchema)
                childParameters.forEach { result[it.key] = it.value }
            }
            result
        } else {
            val key = "$name${if (schema.title.isNullOrEmpty()) "" else ".${schema.title}"}"
            mapOf(
                key to UserInput(
                    name,
                    schema.required?.contains(name) == true,
                    schema.description,
                    previousUserInput(key, schema.default ?: "")
                )
            )
        }
    }

    fun launchApi(
        uri: String,
        method: String,
        operation: Operation,
        userParameters: Map<String, String>
    ) {
        viewModelScope.launch {
            userParameters.save()

            val url = "${getBaseUrl(userParameters)}$uri"
                .resolveUrlParameters(operation, userParameters)
            var builder = Request.Builder().url(url)

            try {
                builder = when (method) {
                    Operation.GET -> builder.get()
                    Operation.POST -> builder.post(operation.toRequestBody(userParameters))
                    Operation.DELETE -> builder.delete()
                    else -> builder
                }

                val request = builder.build()

                withContext(Dispatchers.IO) {
                    val call = client.newCall(request)
                    try {
                        apiCalls.value = apiCalls.value + call.execute().toApiCall()
                    } catch (th: Throwable) {
                        apiCalls.value = apiCalls.value + ApiCall(
                            method,
                            getBaseUrl(userParameters),
                            call.request().body()?.string() ?: "",
                            503,
                            th.message ?: "<NETWORK NOT AVAILABLE>"
                        )
                    }
                }
            } catch (throwable: Throwable) {
                error.value = throwable.localizedMessage ?: throwable.message ?: "Undefined error"
            }
        }
    }

    private fun getBaseUrl(userParameters: Map<String, String>): String {
        return api.value?.serverUrl(userParameters) ?: "https://developer.telekom.com"
    }

    private fun ApiSpecification.serverUrl(userParameters: Map<String, String>): String? =
        if (servers.isNullOrEmpty()) {
            null
        } else {
            val userInput = userParameters.keys.filter { it.startsWith("baseUrl.0") }.toList()
            var url = servers!![0].url

            for (input in userInput) {
                val normalized = input.replace("baseUrl.0.", "")
                url = url.replace("{$normalized}", userParameters[input] ?: "")
            }

            if (!url.startsWith("http")) {
                url = "http://$url"
            }

            url.replace("http://", "https://")
        }

    private fun Operation.toRequestBody(
        userParameters: Map<String, String>
    ): okhttp3.RequestBody {
        return if (requestBody != null) {
            val contentMap = requestBody!!.content

            if (contentMap != null) {
                val jsonContent = contentMap["application/json"]
                if (jsonContent?.schema != null) {
                    okhttp3.RequestBody.create(
                        MediaType.parse("application/json"),
                        jsonContent.schema!!.toBodyWithParameters(
                            userParameters,
                            "application/json"
                        )
                    )
                } else {
                    val formContent = contentMap["application/x-www-form-urlencoded"]
                    if (formContent?.schema != null) {
                        formContent.schema!!.toFormBodyWithParameters(
                            userParameters
                        )
                    } else {
                        throw ClassNotFoundException("Could not find content body type. No json, no form, ignoring.")
                    }
                }
            } else {
                throw ClassNotFoundException("Could not find json content type.")
            }
        } else {
            throw IllegalStateException("No request body given, but expected.")
        }
    }

    private fun String.resolveUrlParameters(
        operation: Operation,
        userParameters: Map<String, String>
    ): String {
        return if (operation.parameters != null && !operation.parameters.isNullOrEmpty()) {
            var result = this
            val pathParameters = operation.parameters!!.filter { it.location == "path" }
            for (parameter in pathParameters) {
                val key = parameter.name
                val value = userParameters[key] ?: ""
                result = result.replace("{${parameter.name}}", value)
            }
            result
        } else {
            this
        }
    }

    private fun Map<String, String>.save() {
        val editor = sharedPreferences.edit()
        for ((k, v) in this) {
            editor.putString(k, v)
        }
        editor.apply()
    }
}

fun Schema.toBodyWithParameters(
    userParameters: Map<String, String>,
    titleHint: String = ""
): String {
    return buildString {
        if (!properties?.keys.isNullOrEmpty()) {
            // object
            if (!title.isNullOrEmpty()) {
                append("\"$title\": { ")
            } else {
                append("{")
            }

            var separator = ""
            for (key in properties!!.keys) {
                append("$separator\"$key\":")
                separator = ", "
                append(
                    properties!![key]!!.toBodyWithParameters(
                        userParameters,
                        titleHint = "$titleHint.$key"
                    )
                )
            }

            append(" } ")
        } else {
            // scalar
            val value = if (userParameters.containsKey(title)) {
                userParameters[title]
            } else {
                userParameters[titleHint]
            }

            userParameters[userParameters[title].toString()]
            if (value.isNullOrEmpty()) {
                append("null")
            } else {
                if (type == "string") {
                    append("\"${value}\"")
                } else {
                    append(value)
                }
            }
        }
    }
}

fun Schema.toFormBodyWithParameters(
    userParameters: Map<String, String>
): FormBody {
    val builder = FormBody.Builder()
    for (name in properties?.keys.orEmpty()) {
        builder.add(name, userParameters[name] ?: "")
    }
    return builder.build()
}

private fun Response.toApiCall(): ApiCall = ApiCall(
    api = request().url().toString(),
    method = request().method(),
    requestBody = request().body()?.string() ?: "",
    responseCode = code(),
    responseBody = body()?.string() ?: "<empty>",
)

private fun okhttp3.RequestBody.string(): String {
    val sink = Buffer()
    writeTo(sink)

    return sink.readUtf8()
}