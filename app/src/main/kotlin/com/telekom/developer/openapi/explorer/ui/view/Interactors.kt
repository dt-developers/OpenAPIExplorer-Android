package com.telekom.developer.openapi.explorer.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.telekom.developer.openapi.explorer.R

@Composable
fun Interactors(
    toggled: Boolean,
    toggle: () -> Unit,
    execute: (() -> Unit)?,
    executeTitle: String = "Execute",
) {
    Row {
        Spacer(modifier = Modifier.weight(1f))
        Button(
            modifier = Modifier.padding(4.dp),
            onClick = toggle
        ) {
            Image(
                painter = if (toggled)
                    painterResource(id = R.drawable.ic_show_less)
                else
                    painterResource(id = R.drawable.ic_show_more),
                contentDescription = null
            )
        }
        if (execute != null) {
            Button(
                modifier = Modifier.padding(4.dp),
                onClick = execute
            ) {
                Text(executeTitle)
            }
        }
    }
}