package com.telekom.developer.openapi.explorer.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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

@Composable
fun OpenAPIView(
    api: ApiSpecification?,
    apiCalls: List<ApiCall> = emptyList(),
    toggled: List<Int>,
    apiClicked: (url: String, method: String, operation: Operation) -> Unit,
    toggleClicked: (index: Int) -> Unit,
    copyText: (text: String) -> Unit,
    loadClicked: () -> Unit,
) {
    if (api == null) {
        Button(
            modifier = Modifier.wrapContentSize(),
            onClick = loadClicked
        ) {
            Text(stringResource(R.string.select_specification_button))
        }
    } else {
        LazyColumn(
            modifier = Modifier.padding(8.dp),
        ) {
            headerItem(api.info.title)

            if (api.info.description != null) {
                item {
                    Text(
                        text = api.info.description.orEmpty()
                    )
                }
                itemSpacer()
            }

            headerItem(titleId = R.string.main_section_apis_title)
            pathItems(api.paths, apiClicked, toggled, toggleClicked)

            if (apiCalls.isNotEmpty()) {
                itemSpacer()
                headerItem(titleId = R.string.main_section_calls_title)
                callItems(
                    apiCalls,
                    api.paths.size * 1000,
                    toggled,
                    toggleClicked,
                    copyText
                )
            }
        }
    }

}private fun LazyListScope.headerItem(title: String? = null, @StringRes titleId:Int? = null) {
    item {
        Text(
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 36.sp,
            text = if(titleId != null) {
                stringResource(id = titleId)
            } else {
                title.orEmpty()
            }
        )
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
