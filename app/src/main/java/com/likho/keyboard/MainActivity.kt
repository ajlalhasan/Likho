package com.likho.keyboard

import android.app.Activity
import android.content.Intent
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.view.Gravity
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

/** Setup screen: turn the keyboard on, pick it, and save the API key. */
class MainActivity : Activity() {

    private lateinit var statusTxt: TextView
    private val accent = 0xFF0E6B52.toInt()

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = Prefs.get(this)

        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(28), dp(20), dp(40))
        }

        col.addView(TextView(this).apply {
            text = "Likho Keyboard"
            textSize = 28f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(accent)
        })
        col.addView(TextView(this).apply {
            text = "Kisi bhi app mein likho. ✨ Convert dabao, text dusri language ya naye tone mein badal jayega."
            textSize = 15f
            setPadding(0, dp(6), 0, dp(14))
        })

        statusTxt = TextView(this).apply {
            textSize = 14f
            setPadding(dp(12), dp(10), dp(12), dp(10))
            background = GradientDrawable().apply { setColor(0xFFDCEFE7.toInt()); cornerRadius = dp(10).toFloat() }
            setTextColor(0xFF15201B.toInt())
        }
        col.addView(statusTxt)

        // Step 1
        col.addView(heading("1. Keyboard on karein"))
        col.addView(note("Settings mein \"Likho Keyboard\" ko on karein. Android ek warning dikhayega, wo har keyboard ke liye aati hai. Ye app aapka text sirf tab bhejti hai jab aap ✨ Convert dabate hain."))
        col.addView(button("Keyboard settings kholo") {
            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
        })

        // Step 2
        col.addView(heading("2. Likho ko select karein"))
        col.addView(button("Keyboard choose karo") {
            (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker()
        })

        // Step 3
        col.addView(heading("3. Anthropic API key daalein"))
        col.addView(note("console.anthropic.com par account bana kar API key banayein (sk-ant- se shuru hoti hai). Key sirf is phone par save hoti hai."))
        val keyField = EditText(this).apply {
            hint = "sk-ant-..."
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            setText(prefs.getString("api_key", ""))
            isSingleLine = true
        }
        col.addView(keyField)
        col.addView(note("Model (zaroorat ho to hi badlein):"))
        val modelField = EditText(this).apply {
            setText(prefs.getString("model", Prefs.DEFAULT_MODEL))
            isSingleLine = true
        }
        col.addView(modelField)
        col.addView(button("Save karo") {
            prefs.edit()
                .putString("api_key", keyField.text.toString().trim())
                .putString("model", modelField.text.toString().trim().ifEmpty { Prefs.DEFAULT_MODEL })
                .apply()
            Toast.makeText(this, "Save ho gaya", Toast.LENGTH_SHORT).show()
            updateStatus()
        })

        // Step 4
        col.addView(heading("4. Yahan test karein"))
        col.addView(note("Neeche Roman Urdu mein kuch likhein, upar se language aur tone chunein, phir ✨ Convert dabayein. Sirf kuch hissa select karein to sirf wahi badlega."))
        col.addView(EditText(this).apply {
            hint = "jaise: bhai kal meeting mein late ho jaunga"
            minLines = 3
            gravity = Gravity.TOP or Gravity.START
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        })

        setContentView(ScrollView(this).apply {
            setBackgroundColor(0xFFF3F5F2.toInt())
            addView(col)
        })
        updateStatus()
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) updateStatus()
    }

    private fun updateStatus() {
        if (!::statusTxt.isInitialized) return
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        val enabled = imm.enabledInputMethodList.any { it.packageName == packageName }
        val current = Settings.Secure.getString(contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD) ?: ""
        val selected = current.startsWith("$packageName/")
        val hasKey = !Prefs.get(this).getString("api_key", "").isNullOrBlank()
        statusTxt.text = listOf(
            (if (enabled) "✅" else "⬜") + " Keyboard on hai",
            (if (selected) "✅" else "⬜") + " Likho select hai",
            (if (hasKey) "✅" else "⬜") + " API key saved hai",
        ).joinToString("\n")
    }

    private fun heading(t: String) = TextView(this).apply {
        text = t
        textSize = 18f
        setTypeface(typeface, Typeface.BOLD)
        setTextColor(0xFF15201B.toInt())
        setPadding(0, dp(22), 0, dp(4))
    }

    private fun note(t: String) = TextView(this).apply {
        text = t
        textSize = 14f
        setTextColor(0xFF5D6B64.toInt())
        setTextIsSelectable(true)
        setPadding(0, 0, 0, dp(6))
    }

    private fun button(t: String, onClick: () -> Unit) = Button(this).apply {
        text = t
        isAllCaps = false
        setTextColor(0xFFFFFFFF.toInt())
        background = GradientDrawable().apply { setColor(accent); cornerRadius = dp(10).toFloat() }
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48)).apply {
            topMargin = dp(6)
        }
        setOnClickListener { onClick() }
    }
}
