package com.likho.keyboard

import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.URL
import javax.net.ssl.HttpsURLConnection

/** Calls the Anthropic Messages API with the user's own API key. */
object Claude {

    private fun prompt(text: String, lang: Option, tone: Option): String {
        val target = if (lang.id == "same") {
            "Write in the SAME language and script as the input."
        } else {
            "Write the result in ${lang.id}." +
                if (lang.id == "Roman Urdu") " Roman Urdu means Urdu written in the Latin alphabet, the way people text in Pakistan." else ""
        }
        val toneLine = if (tone.id == "none") "unchanged" else tone.id
        return """
You are a writing assistant inside a smart keyboard app. Rewrite the user's text.

$target
Tone: $toneLine. ${tone.rule}

Rules:
- The input may be in Roman Urdu, Urdu, Hinglish, English or any mix. Understand it fully.
- Keep the original meaning, names, numbers and facts. Do not add new information.
- Keep it as one message ready to send. Keep line breaks if the input has them.
- Reply with ONLY the rewritten text. No quotes, no labels, no explanation, no alternatives.

Text:
""${'"'}
$text
""${'"'}
""".trim()
    }

    fun rewrite(apiKey: String, model: String, text: String, lang: Option, tone: Option): String {
        val body = JSONObject()
            .put("model", model)
            .put("max_tokens", 1500)
            .put(
                "messages", JSONArray().put(
                    JSONObject().put("role", "user").put("content", prompt(text, lang, tone))
                )
            )

        val conn: HttpsURLConnection
        val code: Int
        val response: String
        try {
            conn = URL("https://api.anthropic.com/v1/messages").openConnection() as HttpsURLConnection
            conn.requestMethod = "POST"
            conn.connectTimeout = 15000
            conn.readTimeout = 60000
            conn.doOutput = true
            conn.setRequestProperty("content-type", "application/json")
            conn.setRequestProperty("x-api-key", apiKey)
            conn.setRequestProperty("anthropic-version", "2023-06-01")
            conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            response = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""
        } catch (e: IOException) {
            throw Exception("Internet check karein, connect nahi ho saka")
        }

        if (code !in 200..299) {
            val apiMsg = try {
                JSONObject(response).getJSONObject("error").optString("message")
            } catch (e: Exception) { "" }
            throw Exception(
                when (code) {
                    401 -> "API key ghalat hai. Likho app mein theek karein."
                    403 -> "Is API key ko ijazat nahi."
                    404 -> "Model nahi mila. App mein model ka naam check karein."
                    429 -> "Limit poori ho gayi. Thori der baad try karein."
                    529, 503 -> "Server busy hai. Dobara try karein."
                    else -> "Error $code: ${apiMsg.take(120)}"
                }
            )
        }

        val content = JSONObject(response).getJSONArray("content")
        val sb = StringBuilder()
        for (i in 0 until content.length()) {
            val part = content.getJSONObject(i)
            if (part.optString("type") == "text") sb.append(part.optString("text"))
        }
        val out = sb.toString().trim()
        if (out.isEmpty()) throw Exception("Koi jawab nahi aaya. Dobara try karein.")
        return out
    }
}
