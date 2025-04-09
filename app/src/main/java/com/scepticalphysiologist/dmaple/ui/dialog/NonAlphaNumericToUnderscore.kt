package com.scepticalphysiologist.dmaple.ui.dialog

import androidx.compose.ui.text.AnnotatedString
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
