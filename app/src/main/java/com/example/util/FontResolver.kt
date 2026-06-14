package com.example.util

import android.graphics.Typeface
import java.io.File

object FontResolver {
    fun resolveTypeface(fontFamily: String?): Typeface {
        if (fontFamily.isNullOrEmpty()) {
            return Typeface.SANS_SERIF
        }
        try {
            // Check if the fontFamily string points to an existing custom font file
            val file = File(fontFamily)
            if (file.exists() && (fontFamily.lowercase().endsWith(".ttf") || fontFamily.lowercase().endsWith(".otf"))) {
                return Typeface.createFromFile(file)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return when (fontFamily) {
            "Serif" -> Typeface.SERIF
            "Monospace" -> Typeface.MONOSPACE
            "Cursive" -> Typeface.create("sans-serif-condensed", Typeface.ITALIC)
            "Elegant" -> Typeface.create("serif", Typeface.BOLD_ITALIC)
            "Brutalist" -> Typeface.create("sans-serif-black", Typeface.BOLD)
            "Rounded" -> Typeface.create("sans-serif-medium", Typeface.NORMAL)
            "Tech" -> Typeface.create("monospace", Typeface.BOLD)
            "Futuristic" -> Typeface.create("sans-serif-thin", Typeface.NORMAL)
            else -> Typeface.SANS_SERIF
        }
    }
}
