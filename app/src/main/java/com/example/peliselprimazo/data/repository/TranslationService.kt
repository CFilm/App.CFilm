package com.example.peliselprimazo.data.repository

import com.example.peliselprimazo.data.remote.TranslationApi
import com.google.gson.JsonArray
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TranslationService @Inject constructor(
    private val translationApi: TranslationApi
) {
    suspend fun translateToSpanish(text: String?): String {
        if (text.isNullOrBlank()) return ""
        return try {
            val response: JsonArray = translationApi.translateText(text = text)
            val translatedText = parseGoogleTranslateResponse(response)
            if (translatedText.isNotBlank()) translatedText else text
        } catch (e: Exception) {
            e.printStackTrace()
            text
        }
    }

    private fun parseGoogleTranslateResponse(jsonArray: JsonArray): String {
        return try {
            val sentencesArray = jsonArray.asJsonArray[0].asJsonArray
            val translatedBuilder = StringBuilder()
            for (i in 0 until sentencesArray.size()) {
                val sentenceItem = sentencesArray[i].asJsonArray
                if (sentenceItem.size() > 0 && !sentenceItem[0].isJsonNull) {
                    translatedBuilder.append(sentenceItem[0].asString)
                }
            }
            translatedBuilder.toString()
        } catch (e: Exception) {
            ""
        }
    }
}
