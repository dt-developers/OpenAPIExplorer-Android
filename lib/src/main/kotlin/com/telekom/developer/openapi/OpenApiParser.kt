package com.telekom.developer.openapi

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import com.charleskorn.kaml.YamlList
import com.charleskorn.kaml.YamlMap
import com.charleskorn.kaml.YamlNode
import com.charleskorn.kaml.YamlScalar
import com.charleskorn.kaml.yamlMap
import com.charleskorn.kaml.yamlScalar
import com.telekom.developer.openapi.exception.CouldNotResolveReferenceException
import com.telekom.developer.openapi.exception.NotYetSupportedException
import com.telekom.developer.openapi.model.ApiSpecification
import com.telekom.developer.openapi.model.Content
import com.telekom.developer.openapi.model.Operation
import com.telekom.developer.openapi.model.Path
import com.telekom.developer.openapi.model.RequestBody
import com.telekom.developer.openapi.model.Schema
import kotlinx.serialization.decodeFromString

private const val REFERENCE_KEY = "\$ref"

/*
 * 3.0.3
 *
 * https://swagger.io/specification/
 */
class OpenApiParser {
    companion object {

        @Throws(NotYetSupportedException::class, CouldNotResolveReferenceException::class)
        fun parse(ymlDescription: String): ApiSpecification {
            val yaml = Yaml(
                configuration = YamlConfiguration(
                    strictMode = false,
                ),
            )

            val raw = yaml.parseToYamlNode(ymlDescription)
            val specification = yaml.decodeFromString<ApiSpecification>(ymlDescription)

            return specification.resolveReferences(raw.yamlMap)
        }
    }
}

private fun ApiSpecification.resolveReferences(rawMap: YamlMap): ApiSpecification {
    return copy(
        paths = paths.map { (url, path) ->
            url to resolvePathReferences(path, rawMap)
        }.toMap()
    )
}

private fun resolvePathReferences(path: Path, rawMap: YamlMap): Path {
    return path.copy(
        get = path.get?.resolveReferences(rawMap),
        put = path.put?.resolveReferences(rawMap),
        post = path.post?.resolveReferences(rawMap),
        delete = path.delete?.resolveReferences(rawMap),
    )
}

private fun Operation.resolveReferences(rawMap: YamlMap): Operation {
    return copy(
        parameters = parameters?.map {
            if (it.schema.reference != null) {
                it.copy(
                    schema = rawMap.resolveReferences(it.schema.reference).toSchema(rawMap)
                )
            } else {
                it
            }
        },
        requestBody = requestBody?.resolveReferences(rawMap),
    )
}

private fun YamlMap.resolveReferences(reference: String?): YamlNode {
    if (reference != null) {
        if (!reference.startsWith("#")) {
            throw NotYetSupportedException("Non local references are not supported yet. Reference was '$reference'.")
        }
        val referenceComponents = reference.split("/").drop(1)

        var node: YamlMap? = this.yamlMap
        for (section in referenceComponents) {
            node = node?.get(section)
        }

        if (node != null) {
            return node
        } else {
            throw CouldNotResolveReferenceException(reference)
        }
    } else {
        throw CouldNotResolveReferenceException("null in ${path.toHumanReadableString()}")
    }
}

private fun YamlNode.toSchema(rawMap: YamlMap): Schema {
    val reference = yamlMap.get<YamlScalar>(REFERENCE_KEY)?.content
    return if (reference != null) {
        val resolved = rawMap.resolveReferences(reference)
        return resolved.toSchema(rawMap)
    } else {
        Schema(
            title = yamlMap.get<YamlScalar>("title")?.content,
            description = yamlMap.get<YamlScalar>("description")?.content,
            type = yamlMap.get<YamlScalar>("type")?.content,
            format = yamlMap.get<YamlScalar>("format")?.content,
            properties = yamlMap.get<YamlMap>("properties")?.yamlMap?.entries?.map { (key: YamlScalar, node: YamlNode) ->
                key.yamlScalar.content to node.toSchema(rawMap)
            }?.toMap(),
            default = yamlMap.get<YamlScalar>("default")?.content,
            required = yamlMap.get<YamlList>("required")?.toKotlinList(),
            reference = null,
        )
    }
}

private fun YamlNode.toRequestBody(rawMap: YamlMap): RequestBody {
    return RequestBody(
        reference = null,
        description = yamlMap.get<YamlScalar>("description")?.content,
        content = yamlMap.get<YamlMap>("content")?.yamlMap?.entries?.map { (key: YamlScalar, node: YamlNode) ->
            val rawSchema = node.yamlMap.get<YamlMap>("schema")
            if (rawSchema != null) {
                key.yamlScalar.content to Content(
                    schema = rawSchema.toSchema(rawMap) ?: Schema()
                )
            } else {
                throw ClassNotFoundException("'schema' not found. in ${key.yamlScalar.content}")
            }
        }?.toMap(),
        required = yamlMap.get<YamlScalar>("required")?.toBoolean() ?: false,
    )
}

private fun RequestBody.resolveReferences(rawMap: YamlMap): RequestBody {
    return if (reference != null) {
        rawMap.resolveReferences(reference).toRequestBody(rawMap)
    } else {
        copy(
            content = content?.map { (type, content) ->
                if (content.schema?.reference != null) {
                    type to content.copy(
                        schema = rawMap.resolveReferences(content.schema.reference).toSchema(rawMap)
                    )
                } else {
                    type to content
                }
            }?.toMap()
        )
    }
}

private fun YamlList.toKotlinList(): List<String> {
    val result = mutableListOf<String>()

    for (node in items) {
        result.add(node.yamlScalar.content)
    }

    return result
}

