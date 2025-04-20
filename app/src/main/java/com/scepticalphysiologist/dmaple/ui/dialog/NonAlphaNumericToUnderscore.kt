// Copyright (c) 2025-2025. Dr Sean Paul Parsons. All rights reserved.

package com.scepticalphysiologist.dmaple.ui.dialog

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation


/** Text field transformer to replace all non-alphanumeric characters with an underscore. */
class NonAlphaNumericToUnderscore: VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val trans = text.text.map{char ->  if(char.isLetterOrDigit()) char else '_'}.joinToString("")
        return TransformedText(
            text = AnnotatedString(trans),
            offsetMapping = OffsetMapping.Identity
        )
    }
}

/** A text editor that replaces non-alphanumeric input with underscores. */
@Composable
fun AlphaNumericOnlyTextEdit(text: String, onValueChange: (String) -> Unit) {
    val transform = NonAlphaNumericToUnderscore()
    TextField(
        value = text,
        readOnly = false,
        keyboardOptions = KeyboardOptions.Default.copy(
            keyboardType = KeyboardType.Ascii,
            autoCorrectEnabled = false,
        ),
        maxLines = 1,
        onValueChange = { onValueChange(transform.filter(AnnotatedString(it)).text.text) },
        visualTransformation = transform,
        modifier = Modifier.fillMaxWidth()
    )
}
