package com.telekom.developer.openapi.explorer.ui.model

import com.telekom.developer.openapi.model.Operation

sealed class UserDialog {
    data class UserInputDialog(
        val inputsNeeded: Map<String, UserInput>,
        val url: String,
        val method: String,
        val operation: Operation,
    ) : UserDialog()

    data class SelectAPIsDialog(
        val availableAPIs: List<String>
    ) : UserDialog()
}


data class UserInput(
    val name: String,
    val required: Boolean,
    val details: String? = null,
    val previous: String = ""
)