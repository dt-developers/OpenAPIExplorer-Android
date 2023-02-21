package com.telekom.developer.openapi.explorer.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.telekom.developer.openapi.explorer.R
import com.telekom.developer.openapi.explorer.model.ApiCall
import com.telekom.developer.openapi.explorer.ui.theme.TMagenta
import com.telekom.developer.openapi.model.ApiSpecification
import com.telekom.developer.openapi.model.Operation
import kotlinx.coroutines.launch

@Composable
fun OpenAPIView(
    api: ApiSpecification?,
    apiCalls: List<ApiCall> = emptyList(),
    toggled: List<Int>,
    apiClicked: (url: String, method: String, operation: Operation) -> Unit,
    toggleClicked: (index: Int) -> Unit,
    copyText: (text: String) -> Unit,
    loadClicked: () -> Unit,
    deleteCallsClicked: () -> Unit,
) {
    val listState = rememberLazyListState()

    if (api == null) {
        Button(
            modifier = Modifier.wrapContentSize(),
            onClick = loadClicked
        ) {
            Text(stringResource(R.string.select_specification_button))
        }
    } else {
        var descriptionOffset = 0
        var apiOffset = 0

        LazyColumn(
            state = listState,
            modifier = Modifier.padding(8.dp),
        ) {
            headerItem(api.info.title)
            descriptionOffset++

            if (api.info.description != null) {
                item {
                    Text(
                        text = api.info.description.orEmpty()
                    )
                    descriptionOffset++
                }
                itemSpacer()
                descriptionOffset++
            }

            headerItem(titleId = R.string.main_section_apis_title)
            apiOffset++
            pathItems(api.paths, apiClicked, toggled, toggleClicked)
            apiOffset += api.paths.size

            if (apiCalls.isNotEmpty()) {
                itemSpacer()
                headerItem(
                    titleId = R.string.main_section_calls_title,
                    delete = deleteCallsClicked
                )
                callItems(
                    apiCalls,
                    api.paths.size * 1000,
                    toggled,
                    toggleClicked,
                    copyText
                )
            }
        }

        CreateScrollers(listState, descriptionOffset, apiOffset, api, apiCalls)
    }
}

@Composable
private fun CreateScrollers(
    listState: LazyListState,
    descriptionOffset: Int,
    apiOffset: Int,
    api: ApiSpecification,
    apiCalls: List<ApiCall>
) {
    val coroutineScope = rememberCoroutineScope()

    fun scrollTo(index: Int) {
        coroutineScope.launch {
            when (index) {
                0 -> listState.animateScrollToItem(0)
                1 -> listState.animateScrollToItem(descriptionOffset)
                else -> listState.animateScrollToItem(descriptionOffset + apiOffset)
            }
        }
    }

    val toDescription = if (api.info.description.isNullOrEmpty()) {
        null
    } else {
        { scrollTo(0) }
    }

    val toAPIs = if (api.paths.isNotEmpty()) {
        { scrollTo(1) }
    } else {
        null
    }

    val toCalls = if (apiCalls.isNotEmpty()) {
        { scrollTo(2) }
    } else {
        null
    }

    Scrollers(
        toDescription = toDescription,
        toAPIs = toAPIs,
        toCalls = toCalls,
    )
}

private fun LazyListScope.headerItem(
    title: String? = null,
    @StringRes titleId: Int? = null,
    delete: (() -> Unit)? = null
) {
    item {
        Row {
            Text(
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 36.sp,
                text = if (titleId != null) {
                    stringResource(id = titleId)
                } else {
                    title.orEmpty()
                }
            )
            if (delete != null) {
                Spacer(modifier = Modifier.weight(1f))
                IconButton(
                    modifier = Modifier.padding(4.dp),
                    onClick = delete
                ) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = null
                    )
                }
            }
        }
    }
}

private fun LazyListScope.itemSpacer() {
    item {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.Center,
        )
        {
            Spacer(
                modifier = Modifier
                    .background(TMagenta)
                    .size(width = 200.dp, height = 2.dp)
            )
        }
    }
}

@Composable
private fun Scrollers(
    toDescription: (() -> Unit)?,
    toAPIs: (() -> Unit)?,
    toCalls: (() -> Unit)?,
) {
    Column(modifier = Modifier.wrapContentSize(align = Alignment.BottomEnd)) {
        if (toDescription != null) {
            IconButton(onClick = toDescription) {
                Icon(Icons.Filled.Search, null)
            }
        }

        if (toAPIs != null) {
            IconButton(onClick = toAPIs) {
                Icon(Icons.Filled.List, null)
            }
        }

        if (toCalls != null) {
            IconButton(onClick = toCalls) {
                Icon(Icons.Filled.Done, null)
            }
        }
    }
}

