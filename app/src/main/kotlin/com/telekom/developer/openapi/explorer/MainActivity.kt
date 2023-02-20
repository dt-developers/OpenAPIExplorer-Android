@file:OptIn(ExperimentalTextApi::class)

package com.telekom.developer.openapi.explorer

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.telekom.developer.openapi.explorer.model.ApiCall
import com.telekom.developer.openapi.explorer.ui.OpenAPIView
import com.telekom.developer.openapi.explorer.ui.UserApiSelectionDialog
import com.telekom.developer.openapi.explorer.ui.UserParameterInput
import com.telekom.developer.openapi.explorer.ui.model.UserDialog
import com.telekom.developer.openapi.explorer.ui.model.UserDialog.SelectAPIsDialog
import com.telekom.developer.openapi.explorer.ui.model.UserDialog.UserInputDialog
import com.telekom.developer.openapi.explorer.ui.theme.OpenAPIExplorerTheme
import com.telekom.developer.openapi.explorer.ui.viewmodel.OpenAPIViewModel
import com.telekom.developer.openapi.model.ApiSpecification


class MainActivity : ComponentActivity() {
    val viewmodel: OpenAPIViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewmodel.loadLastAPI()

        setContent {
            val api by remember { viewmodel.api }
            val apiCalls by remember { viewmodel.apiCalls }
            val toggled by remember { viewmodel.toggled }

            val dialog by remember { viewmodel.dialog }
            val error by remember { viewmodel.error }

            OpenApiAppView(api, apiCalls, toggled, dialog, error)
        }
    }

    @Composable
    @OptIn(ExperimentalMaterial3Api::class)
    private fun OpenApiAppView(
        api: ApiSpecification?,
        apiCalls: List<ApiCall>,
        toggled: List<Int>,
        dialog: UserDialog?,
        error: String
    ) {
        OpenAPIExplorerTheme {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text(stringResource(id = R.string.app_name)) },
                        actions = {
                            IconButton(onClick = ::loadClicked) {
                                Icon(Icons.Filled.Add, null)
                            }
                        }
                    )
                },
                content = { paddingValues ->
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        color = MaterialTheme.colorScheme.background,
                    ) {
                        OpenAPIView(
                            api = api,
                            apiCalls = apiCalls,
                            toggled = toggled,
                            apiClicked = { url, method, operation ->
                                viewmodel.apiClicked(url, method, operation)
                            },
                            toggleClicked = ::toggled,
                            copyText = ::copyTextToClipboard,
                            loadClicked = ::loadClicked
                        )

                        if (dialog != null) {
                            UserDialog(dialog)
                        }

                        if (error.isNotEmpty()) {
                            ErrorMessage(error)
                        }
                    }
                },
            )
        }
    }

    private fun toggled(index: Int) {
        viewmodel.toggled.value = buildList {
            addAll(viewmodel.toggled.value)
            if (index in viewmodel.toggled.value) {
                remove(index)
            } else {
                add(index)
            }
        }
    }

    @Composable
    private fun ErrorMessage(error: String) {
        val toast = Toast.makeText(applicationContext, error, Toast.LENGTH_LONG)
        toast.addCallback(object : Toast.Callback() {
            override fun onToastHidden() {
                viewmodel.error.value = ""
            }
        })
        toast.show()
    }

    @Composable
    private fun UserDialog(dialog: UserDialog) {
        when (dialog) {
            is UserInputDialog -> {
                UserParameterInput(
                    dialog = dialog,
                    okay = { parameters ->
                        viewmodel.dialog.value = null
                        viewmodel.launchApi(
                            dialog.url,
                            dialog.method,
                            dialog.operation,
                            parameters
                        )
                    },
                    dismissed = { viewmodel.dialog.value = null }
                )
            }

            is SelectAPIsDialog -> UserApiSelectionDialog(
                dialog = dialog,
                dismissed = { viewmodel.dialog.value = null },
                selected = { file ->
                    viewmodel.dialog.value = null
                    viewmodel.loadAPIFromAssets(file)
                }
            )
        }
    }

    private fun copyTextToClipboard(text: String) {
        getSystemService(ClipboardManager::class.java)
            .setPrimaryClip(
                ClipData.newPlainText(
                    getString(R.string.copy_to_clipboard_title),
                    text
                )
            )

        Toast.makeText(
            this,
            getString(R.string.copy_to_clipboard_toast_title),
            Toast.LENGTH_SHORT
        )
            .show()
    }

    private fun loadClicked() {
        viewmodel.listAPIsInAssets()
    }
}
