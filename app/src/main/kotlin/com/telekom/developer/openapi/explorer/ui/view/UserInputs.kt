package com.telekom.developer.openapi.explorer.ui.view

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.telekom.developer.openapi.explorer.R
import com.telekom.developer.openapi.explorer.ui.model.UserDialog
import com.telekom.developer.openapi.explorer.ui.model.UserDialog.UserInputDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserParameterInput(
    dialog: UserInputDialog,
    dismissed: () -> Unit,
    okay: (parameters: Map<String, String>) -> Unit,
) {
    var parameters by remember { mutableStateOf<Map<String, String>>(mapOf()) }

    AlertDialog(
        confirmButton = {
            Button(onClick = {
                okay(parameters)
            }) {
                Text(stringResource(id = android.R.string.ok))
            }
        },
        dismissButton = {
            Button(onClick = dismissed) {
                Text(stringResource(id = android.R.string.cancel))
            }
        },
        onDismissRequest = dismissed,
        title = { Text(stringResource(id = R.string.user_input_requested)) },
        text = {
            LazyColumn {
                val entries = dialog.inputsNeeded.entries.toList()

                items(entries) {
                    val key = it.key
                    val input = it.value
                    val required = input.required

                    var text by remember { mutableStateOf(input.previous) }
                    parameters = parameters.mute(key, text)

                    Column {
                        Text("$key${if (required) " *" else ""}")
                        TextField(
                            value = text,
                            maxLines = 1,
                            onValueChange = { changedValue ->
                                text = changedValue
                                parameters = parameters.mute(key, changedValue)
                            })
                    }
                }
            }
        }
    )
}

@Composable
fun UserApiSelectionDialog(
    dialog: UserDialog.SelectAPIsDialog,
    dismissed: () -> Unit,
    selected: (selected: String) -> Unit,
    loadFromFile: () -> Unit,
) {
    AlertDialog(
        confirmButton = {
            Button(onClick = dismissed) {
                Text(stringResource(id = android.R.string.cancel))
            }
        },
        onDismissRequest = dismissed,
        title = { Text(stringResource(R.string.operation_selection_title)) },
        dismissButton = {
            Button(onClick = loadFromFile) {
                Text(stringResource(R.string.load_from_file))
            }
        },
        text = {
            LazyColumn {
                items(dialog.availableAPIs) { api ->
                    Button({ selected(api) }) {
                        Text(api)
                    }
                }
            }
        }
    )
}

private fun Map<String, String>.mute(
    key: String,
    value: String
): Map<String, String> {
    val mutable = toMutableMap()
    mutable[key] = value
    return mutable.toMap()
}
