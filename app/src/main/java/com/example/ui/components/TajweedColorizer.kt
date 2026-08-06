package com.example.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight

object TajweedColorizer {

    // Distinct Tajweed colors
    val ColorMadd = Color(0xFFD32F2F)      // Red for Madd (elongation)
    val ColorGhunna = Color(0xFF2E7D32)    // Green for Ghunna & Ikhfa
    val ColorQalqalah = Color(0xFF1565C0)  // Royal Blue for Qalqalah (ق, ط, ب, ج, د with Sukun)
    val ColorIdgham = Color(0xFFEF6C00)    // Amber/Orange for Idgham & Iqlab

    private val qalqalahLetters = setOf('ق', 'ط', 'ب', 'ج', 'د')
    private val sukunChar = 'ْ'
    private val maddSymbols = setOf('ٓ', 'ٰ', 'آ', 'أ')

    fun colorizeTajweed(text: String, isTajweedActive: Boolean = true): AnnotatedString {
        if (!isTajweedActive || text.isBlank()) {
            return buildAnnotatedString { append(text) }
        }

        return buildAnnotatedString {
            var i = 0
            val len = text.length

            while (i < len) {
                val char = text[i]

                // Check for Madd
                if (char in maddSymbols || (i + 1 < len && text[i + 1] == 'ٓ')) {
                    pushStyle(SpanStyle(color = ColorMadd, fontWeight = FontWeight.Bold))
                    append(char)
                    pop()
                }
                // Check for Qalqalah (Letter followed by Sukun)
                else if (char in qalqalahLetters && i + 1 < len && text[i + 1] == sukunChar) {
                    pushStyle(SpanStyle(color = ColorQalqalah, fontWeight = FontWeight.Bold))
                    append(char)
                    append(text[i + 1])
                    pop()
                    i++
                }
                // Check for Ghunna (Meem / Nun with Shadda)
                else if ((char == 'ن' || char == 'م') && i + 1 < len && text[i + 1] == 'ّ') {
                    pushStyle(SpanStyle(color = ColorGhunna, fontWeight = FontWeight.Bold))
                    append(char)
                    append(text[i + 1])
                    pop()
                    i++
                }
                // Check for Tanween (Idgham / Iqlab hint)
                else if (char == 'ً' || char == 'ٍ' || char == 'ٌ') {
                    pushStyle(SpanStyle(color = ColorIdgham, fontWeight = FontWeight.Bold))
                    append(char)
                    pop()
                }
                else {
                    append(char)
                }

                i++
            }
        }
    }
}
