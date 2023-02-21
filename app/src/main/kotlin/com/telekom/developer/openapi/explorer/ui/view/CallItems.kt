package com.telekom.developer.openapi.explorer.ui.view

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.telekom.developer.openapi.explorer.model.ApiCall
import com.telekom.developer.openapi.explorer.ui.theme.TMagenta

fun LazyListScope.callItems(
    apiCalls: List<ApiCall>,
    indexOffset: Int,
    toggled: List<Int>,
    toggleClicked: (index: Int) -> Unit,
    copyText: (text: String) -> Unit,
) {
    itemsIndexed(apiCalls.reversed()) { index, call ->
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
                    text = "${call.method} ${call.api}"
                )
                Text(text = call.responseCode.toString())

                if (index + indexOffset in toggled) {
                    Spacer(modifier = Modifier.size(8.dp))

                    Text(
                        color = TMagenta,
                        text = "Request"
                    )
                    Text(
                        fontWeight = FontWeight.ExtraLight,
                        text = call.requestBody,
                        maxLines = 10,
                    )

                    Spacer(modifier = Modifier.size(8.dp))

                    Text(
                        color = TMagenta,
                        text = "Response"
                    )
                    Text(
                        fontWeight = FontWeight.ExtraLight,
                        text = call.responseBody,
                        maxLines = 10,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Interactors(
                    index + indexOffset in toggled,
                    { toggleClicked(index + indexOffset) },
                    {
                        copyText(
                            """
                            ${call.method} ${call.api}
                            ${call.responseCode}
                            
                            Request
                            ${call.requestBody}
                            
                            Response
                            ${call.responseBody}
                            """.trimIndent()
                        )
                    },
                    "Copy"

                )
            }
        }
    }
}