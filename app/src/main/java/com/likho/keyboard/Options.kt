package com.likho.keyboard

import android.content.Context
import android.content.SharedPreferences

data class Option(val id: String, val label: String, val short: String = label, val rule: String = "")

object Options {
    val langs = listOf(
        Option("same", "Same language", "Same"),
        Option("English", "English"),
        Option("Urdu", "اردو Urdu", "Urdu"),
        Option("Roman Urdu", "Roman Urdu"),
        Option("Hindi", "हिन्दी Hindi", "Hindi"),
        Option("Punjabi", "پنجابی Punjabi", "Punjabi"),
        Option("Arabic", "العربية Arabic", "Arabic"),
        Option("Persian", "فارسی Persian", "Persian"),
        Option("Pashto", "پښتو Pashto", "Pashto"),
        Option("Turkish", "Türkçe", "Turkish"),
        Option("Spanish", "Español", "Spanish"),
        Option("French", "Français", "French"),
        Option("German", "Deutsch", "German"),
        Option("Chinese (Simplified)", "中文 Chinese", "Chinese"),
        Option("Japanese", "日本語 Japanese", "Japanese"),
        Option("Korean", "한국어 Korean", "Korean"),
        Option("Russian", "Русский", "Russian"),
        Option("Portuguese", "Português", "Portuguese"),
        Option("Bengali", "বাংলা Bengali", "Bengali"),
    )

    val tones = listOf(
        Option("none", "Sirf translate", "Translate", "Keep the original tone and wording style; only translate, or if the language is unchanged, fix spelling and grammar."),
        Option("Professional", "💼 Professional", "Professional", "Polished, clear and workplace-appropriate."),
        Option("Formal", "🎩 Formal", "Formal", "Formal and respectful, suitable for official letters or emails."),
        Option("Friendly", "😊 Friendly", "Friendly", "Warm, kind and approachable."),
        Option("Casual", "😎 Casual", "Casual", "Relaxed and conversational, like texting a friend."),
        Option("Polite", "🙏 Polite", "Polite", "Extra courteous and considerate."),
        Option("Confident", "💪 Confident", "Confident", "Assertive and self-assured, without being rude."),
        Option("Persuasive", "🎯 Persuasive", "Persuasive", "Convincing, gives the reader a reason to act."),
        Option("Empathetic", "🤍 Empathetic", "Empathetic", "Caring and understanding of the reader's feelings."),
        Option("Funny", "😂 Funny", "Funny", "Light-hearted and witty while keeping the message."),
        Option("Romantic", "💌 Romantic", "Romantic", "Sweet and affectionate."),
        Option("Concise", "✂️ Short", "Short", "As short as possible while keeping every key point."),
        Option("Detailed", "📝 Detailed", "Detailed", "A little more clarity and detail, without inventing facts."),
        Option("Grammar", "✅ Grammar fix", "Grammar", "Only correct grammar, spelling and punctuation; change nothing else."),
        Option("Emoji", "✨ Emojis", "Emojis", "Same message with fitting emojis added."),
    )

    fun lang(id: String?) = langs.firstOrNull { it.id == id } ?: langs[1]
    fun tone(id: String?) = tones.firstOrNull { it.id == id } ?: tones[1]
}

object Prefs {
    const val DEFAULT_MODEL = "claude-haiku-5-5"
    fun get(ctx: Context): SharedPreferences = ctx.getSharedPreferences("likho", Context.MODE_PRIVATE)
}
