package com.telekom.developer.openapi.explorer.ui.view

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.telekom.developer.openapi.explorer.ui.theme.TCyan
import com.telekom.developer.openapi.explorer.ui.theme.TMagenta
import com.telekom.developer.openapi.model.Operation
import com.telekom.developer.openapi.model.Parameter
import com.telekom.developer.openapi.model.Path
import com.telekom.developer.openapi.model.RequestBody
import com.telekom.developer.openapi.model.Schema

fun LazyListScope.pathItems(
    paths: Map<String, Path>,
    execute: (url: String, method: String, operation: Operation) -> Unit,
    toggled: List<Int>,
    toggle: (index: Int) -> Unit,
) {
    itemsIndexed(paths.keys.toList()) { index, url ->
        val path = paths[url]
        if (path?.get != null) {
            PathItem(
                Operation.GET,
                url,
                path.get!!,
                (index * 100) in toggled,
                { toggle(100 * index) }) {
                execute(url, Operation.GET, path.get!!)
            }
        }

        if (path?.post != null) {
            PathItem(
                Operation.POST,
                url,
                path.post!!,
                (index * 100 + 1) in toggled,
                { toggle(100 * index + 1) }) {
                execute(url, Operation.POST, path.post!!)
            }
        }

        if (path?.delete != null) {
            PathItem(
                Operation.DELETE,
                url,
                path.delete!!,
                (index * 100 + 2) in toggled,
                { toggle(100 * index + 2) }) {
                execute(url, Operation.DELETE, path.delete!!)
            }
        }
    }
}

@Composable
private fun PathItem(
    method: String,
    url: String,
    operation: Operation,
    toggled: Boolean,
    toggle: () -> Unit,
    execute: () -> Unit
) {
    Card(
        modifier = Modifier
            .padding(4.dp)
            .fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                color = TMagenta,
                fontWeight = FontWeight.Bold,
                text = "$method $url"
            )
            if (!operation.summary.isNullOrEmpty()) {
                Text(text = operation.summary.orEmpty())
            }

            if (toggled) {
                if (operation.parameters != null) {
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(text = "parameter${if (operation.parameters!!.size > 1) "s" else ""}")
                    for (parameter in operation.parameters!!) {
                        ParameterDescription(parameter)
                    }
                }

                if (operation.requestBody != null) {
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(text = "body")
                    Column {
                        BodyDescription(operation.requestBody!!)
                    }
                }
            }

            Interactors(toggled, toggle, execute)
        }
    }
}

@Composable
private fun ParameterDescription(parameter: Parameter) {
    SchemaDescription(
        name = parameter.name,
        schema = parameter.schema,
        required = if (parameter.required) listOf(parameter.name) else parameter.schema.required
    )
}

@Composable
private fun SchemaDescription(
    schema: Schema,
    name: String? = null,
    indentation: Int = 0,
    required: List<String>?
) {
    Row {
        for (i in 0..indentation) {
            Spacer(modifier = Modifier.width((indentation * 8).dp))
        }
        Text(
            text = buildAnnotatedString {
                val type = schema.type
                val format = schema.format

                if (name != null) {
                    if (required != null && required.contains(name)) {
                        withStyle(
                            SpanStyle(
                                fontWeight = FontWeight.ExtraBold,
                                color = TMagenta
                            )
                        ) {
                            append(name)
                            append('*')
                        }
                    } else {
                        append(name)
                    }
                    append(" : ")
                }

                if (type != null) {
                    withStyle(SpanStyle(color = TCyan, fontWeight = FontWeight.Light)) {
                        append(type)
                        if (format != null) {
                            append(" as ")
                            append(format)
                        }
                    }
                }
            }
        )
    }

    if (schema.properties != null) {
        schema.properties!!.entries.sortedBy { (name, _) -> name }
            .forEach { (name, subschema) ->
                SchemaDescription(
                    name = name,
                    schema = subschema,
                    required = schema.required,
                    indentation = indentation + 1
                )
            }
    }
}

@Composable
private fun BodyDescription(body: RequestBody) {
    val content = body.content
    if (content != null) {
        for (type in content.keys.toList()) {
            Text(
                fontStyle = FontStyle.Italic,
                text = type
            )
            if (content[type]!!.schema != null) {
                Spacer(modifier = Modifier.size(8.dp))
                SchemaDescription(
                    schema = content[type]!!.schema!!,
                    required = content[type]!!.schema!!.required
                )
            }
        }
    }
}

