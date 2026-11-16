package com.likho.keyboard

import android.content.res.Configuration
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.inputmethodservice.InputMethodService
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.text.TextUtils
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.ExtractedTextRequest
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class LikhoIME : InputMethodService() {

    private val main = Handler(Looper.getMainLooper())

    private lateinit var area: FrameLayout
    private lateinit var langBtn: TextView
    private lateinit var toneBtn: TextView
    private lateinit var goBtn: TextView
    private lateinit var undoBtn: TextView
    private lateinit var statusTxt: TextView

    private var shift = false
    private var capsLock = false
    private var lastShiftTap = 0L
    private var symbols = false
    private var busy = false
    private var undoText: String? = null

    // Colors
    private var cBg = 0
    private var cKey = 0
    private var cKeyPressed = 0
    private var cSpecial = 0
    private var cText = 0
    private var cMuted = 0
    private var cAccent = 0
    private var cAccentText = 0

    private val prefs by lazy { Prefs.get(this) }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun setColors() {
        val dark = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
        if (dark) {
            cBg = 0xFF0F1513.toInt(); cKey = 0xFF26312C.toInt(); cKeyPressed = 0xFF3A4842.toInt()
            cSpecial = 0xFF1B2420.toInt(); cText = 0xFFE7EEEA.toInt(); cMuted = 0xFF93A39B.toInt()
            cAccent = 0xFF3DBB92.toInt(); cAccentText = 0xFF06140F.toInt()
        } else {
            cBg = 0xFFE3E8E5.toInt(); cKey = 0xFFFFFFFF.toInt(); cKeyPressed = 0xFFD0D8D3.toInt()
            cSpecial = 0xFFC9D2CD.toInt(); cText = 0xFF15201B.toInt(); cMuted = 0xFF5D6B64.toInt()
            cAccent = 0xFF0E6B52.toInt(); cAccentText = 0xFFFFFFFF.toInt()
        }
    }

    override fun onEvaluateFullscreenMode(): Boolean = false

    override fun onCreateInputView(): View {
        setColors()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(cBg)
            setPadding(dp(4), dp(6), dp(4), dp(6))
        }
        root.addView(buildBar())
        statusTxt = TextView(this).apply {
            setTextColor(cMuted)
            textSize = 13f
            gravity = Gravity.CENTER
            setPadding(dp(8), 0, dp(8), dp(6))
            visibility = View.GONE
        }
        root.addView(statusTxt)
        area = FrameLayout(this)
        root.addView(area, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(224)))
        updateBar()
        showKeys()
        return root
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        if (!::area.isInitialized) return
        if (!restarting) {
            symbols = false
            undoText = null
            undoBtn.visibility = View.GONE
        }
        updateBar()
        showKeys()
    }

    // ---------- Top AI bar ----------

    private fun rounded(color: Int, radius: Int = dp(8)) = GradientDrawable().apply {
        setColor(color); cornerRadius = radius.toFloat()
    }

    private fun pressable(normal: Int, pressed: Int) = StateListDrawable().apply {
        addState(intArrayOf(android.R.attr.state_pressed), rounded(pressed))
        addState(intArrayOf(), rounded(normal))
    }

    private fun pill(text: String, accent: Boolean = false, onClick: () -> Unit): TextView =
        TextView(this).apply {
            this.text = text
            gravity = Gravity.CENTER
            textSize = 13.5f
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
            setPadding(dp(8), 0, dp(8), 0)
            if (accent) {
                setTextColor(cAccentText); setTypeface(typeface, Typeface.BOLD)
                background = pressable(cAccent, cKeyPressed)
            } else {
                setTextColor(cText)
                background = pressable(cKey, cKeyPressed)
            }
            setOnClickListener { it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); onClick() }
        }

    private fun lp(w: Int, h: Int, weight: Float = 0f, m: Int = dp(3)) =
        LinearLayout.LayoutParams(w, h, weight).apply { setMargins(m, m, m, m) }

    private fun buildBar(): View {
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, dp(4))
        }
        langBtn = pill("") { showPicker(true) }
        toneBtn = pill("") { showPicker(false) }
        undoBtn = pill("↶ Undo") { undo() }.apply { visibility = View.GONE }
        goBtn = pill("✨ Convert", accent = true) { convert() }
        bar.addView(langBtn, lp(0, dp(40), 1f))
        bar.addView(toneBtn, lp(0, dp(40), 1f))
        bar.addView(undoBtn, lp(ViewGroup.LayoutParams.WRAP_CONTENT, dp(40)))
        bar.addView(goBtn, lp(0, dp(40), 1.1f))
        return bar
    }

    private fun updateBar() {
        langBtn.text = "🌐 " + Options.lang(prefs.getString("lang", "English")).short
        toneBtn.text = "🎭 " + Options.tone(prefs.getString("tone", "Professional")).short
    }

    private fun status(msg: String?) {
        main.removeCallbacksAndMessages("status")
        if (msg == null) { statusTxt.visibility = View.GONE; return }
        statusTxt.text = msg
        statusTxt.visibility = View.VISIBLE
        main.postAtTime({ statusTxt.visibility = View.GONE }, "status", SystemClock.uptimeMillis() + 5000)
    }

    // ---------- Keys ----------

    private fun key(label: String, color: Int, big: Boolean = true, onClick: (() -> Unit)?): TextView =
        TextView(this).apply {
            text = label
            gravity = Gravity.CENTER
            setTextColor(cText)
            textSize = if (big && label.length == 1) 21f else 15f
            maxLines = 1
            background = pressable(color, cKeyPressed)
            if (onClick != null) {
                setOnClickListener { it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); onClick() }
            }
        }

    private fun row(keys: List<Pair<View, Float>>, sidePad: Int = 0): LinearLayout {
        val r = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(sidePad, 0, sidePad, 0)
        }
        for ((v, w) in keys) r.addView(v, lp(0, ViewGroup.LayoutParams.MATCH_PARENT, w, dp(2)))
        return r
    }

    private fun upper() = shift || capsLock

    private fun charKey(ch: String, weight: Float = 1f, label: String? = null): Pair<View, Float> {
        val shown = label ?: if (upper()) ch.uppercase() else ch
        return key(shown, cKey) { type(ch) } to weight
    }

    private fun type(ch: String) {
        val ic = currentInputConnection ?: return
        ic.commitText(if (upper()) ch.uppercase() else ch, 1)
        if (shift && !capsLock) { shift = false; showKeys() }
    }

    private fun showKeys() {
        area.removeAllViews()
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val rowLp = { LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f) }

        val r1 = if (symbols) "1234567890" else "qwertyuiop"
        val r2 = if (symbols) "@#₨&_-+()/" else "asdfghjkl"
        val r3 = if (symbols) "*\"':;!?" else "zxcvbnm"

        box.addView(row(r1.map { charKey(it.toString()) }), rowLp())
        box.addView(row(r2.map { charKey(it.toString()) }, if (symbols) 0 else dp(16)), rowLp())

        val third = mutableListOf<Pair<View, Float>>()
        third.add(if (symbols) charKey("%", 1.5f) else shiftKey())
        r3.forEach { third.add(charKey(it.toString())) }
        third.add(backspaceKey())
        box.addView(row(third), rowLp())

        val fourth = listOf(
            key(if (symbols) "ABC" else "?123", cSpecial, false) { symbols = !symbols; showKeys() } to 1.5f,
            key("🌐", cSpecial, false) { switchKeyboard() } to 1f,
            charKey(",", 1f),
            key("Likho", cKey, false) { currentInputConnection?.commitText(" ", 1) }
                .apply { setTextColor(cMuted) } to 4.5f,
            charKey(".", 1f),
            enterKey(),
        )
        box.addView(row(fourth), rowLp())

        area.addView(box, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
    }

    private fun shiftKey(): Pair<View, Float> {
        val label = if (capsLock) "⇪" else "⇧"
        val v = key(label, if (shift || capsLock) cAccent else cSpecial) {
            val now = SystemClock.uptimeMillis()
            when {
                now - lastShiftTap < 350 && shift -> { capsLock = true; shift = true }
                capsLock -> { capsLock = false; shift = false }
                else -> shift = !shift
            }
            lastShiftTap = now
            showKeys()
        }
        if (shift || capsLock) v.setTextColor(cAccentText)
        return v to 1.5f
    }

    private val repeatDelete = object : Runnable {
        override fun run() { deleteOne(); main.postDelayed(this, 55) }
    }

    private fun backspaceKey(): Pair<View, Float> {
        val v = key("⌫", cSpecial, onClick = null)
        v.setOnTouchListener { view, ev ->
            when (ev.action) {
                MotionEvent.ACTION_DOWN -> {
                    view.isPressed = true
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    deleteOne()
                    main.postDelayed(repeatDelete, 420)
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    view.isPressed = false
                    main.removeCallbacks(repeatDelete)
                }
            }
            true
        }
        return v to 1.5f
    }

    private fun deleteOne() {
        val ic = currentInputConnection ?: return
        val sel = ic.getSelectedText(0)
        if (!sel.isNullOrEmpty()) ic.commitText("", 1)
        else sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL)
    }

    private fun enterKey(): Pair<View, Float> {
        val v = key("⏎", cAccent) {
            val ic = currentInputConnection ?: return@key
            val opts = currentInputEditorInfo?.imeOptions ?: 0
            val action = opts and EditorInfo.IME_MASK_ACTION
            val noEnterAction = (opts and EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0
            if (!noEnterAction && action != EditorInfo.IME_ACTION_NONE && action != EditorInfo.IME_ACTION_UNSPECIFIED) {
                ic.performEditorAction(action)
            } else {
                sendKeyChar('\n')
            }
        }
        v.setTextColor(cAccentText)
        return v to 1.5f
    }

    private fun switchKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showInputMethodPicker()
    }

    // ---------- Language / tone picker ----------

    private fun showPicker(isLang: Boolean) {
        area.removeAllViews()
        val list = if (isLang) Options.langs else Options.tones
        val prefKey = if (isLang) "lang" else "tone"
        val current = if (isLang) Options.lang(prefs.getString("lang", "English")).id
                      else Options.tone(prefs.getString("tone", "Professional")).id

        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        val head = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        head.addView(TextView(this).apply {
            text = if (isLang) "Kis language mein?" else "Kaunsa tone?"
            setTextColor(cText); textSize = 15f; setTypeface(typeface, Typeface.BOLD)
            setPadding(dp(8), 0, 0, 0)
        }, lp(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        head.addView(pill("✕ Keyboard") { showKeys() }, lp(ViewGroup.LayoutParams.WRAP_CONTENT, dp(36)))
        col.addView(head)

        list.chunked(3).forEach { chunk ->
            val r = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            chunk.forEach { opt ->
                val selected = opt.id == current
                val b = key(opt.label, if (selected) cAccent else cKey, false) {
                    prefs.edit().putString(prefKey, opt.id).apply()
                    updateBar()
                    showKeys()
                }.apply {
                    textSize = 13.5f
                    ellipsize = TextUtils.TruncateAt.END
                    setPadding(dp(4), 0, dp(4), 0)
                    if (selected) { setTextColor(cAccentText); setTypeface(typeface, Typeface.BOLD) }
                }
                r.addView(b, lp(0, dp(44), 1f))
            }
            repeat(3 - chunk.size) { r.addView(View(this), lp(0, dp(44), 1f)) }
            col.addView(r)
        }

        val scroll = ScrollView(this)
        scroll.addView(col)
        area.addView(scroll, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
    }

    // ---------- AI convert ----------

    private fun fullText(ic: InputConnection): String {
        val et = ic.getExtractedText(ExtractedTextRequest().apply { hintMaxChars = 100000 }, 0)
        et?.text?.let { return it.toString() }
        val before = ic.getTextBeforeCursor(10000, 0)?.toString() ?: ""
        val after = ic.getTextAfterCursor(10000, 0)?.toString() ?: ""
        return before + after
    }

    private fun replaceAll(ic: InputConnection, newText: String) {
        val current = fullText(ic)
        ic.beginBatchEdit()
        val et = ic.getExtractedText(ExtractedTextRequest(), 0)
        if (et?.text != null) {
            ic.setSelection(0, et.text.length)
        } else if (!ic.performContextMenuAction(android.R.id.selectAll)) {
            val before = ic.getTextBeforeCursor(current.length, 0)?.length ?: 0
            val after = ic.getTextAfterCursor(current.length, 0)?.length ?: 0
            ic.deleteSurroundingText(before, after)
        }
        ic.commitText(newText, 1)
        ic.endBatchEdit()
    }

    private fun convert() {
        if (busy) return
        val apiKey = prefs.getString("api_key", "")?.trim().orEmpty()
        if (apiKey.isEmpty()) {
            status("Pehle Likho Keyboard app khol kar API key daalein")
            return
        }
        val ic = currentInputConnection ?: return
        val selected = ic.getSelectedText(0)?.toString()
        val useSelection = !selected.isNullOrBlank()
        val text = if (useSelection) selected!! else fullText(ic)
        if (text.isBlank()) { status("Pehle kuch likhein, phir Convert dabayein"); return }

        val lang = Options.lang(prefs.getString("lang", "English"))
        val tone = Options.tone(prefs.getString("tone", "Professional"))
        val model = prefs.getString("model", Prefs.DEFAULT_MODEL)?.trim().takeUnless { it.isNullOrEmpty() }
            ?: Prefs.DEFAULT_MODEL

        busy = true
        goBtn.text = "⏳ Likh raha..."
        status(null)

        Thread {
            try {
                val result = Claude.rewrite(apiKey, model, text, lang, tone)
                main.post {
                    busy = false
                    goBtn.text = "✨ Convert"
                    val conn = currentInputConnection
                    if (conn == null) { status("Text field band ho gaya"); return@post }
                    if (useSelection) conn.commitText(result, 1) else replaceAll(conn, result)
                    undoText = text
                    undoBtn.visibility = View.VISIBLE
                }
            } catch (e: Exception) {
                main.post {
                    busy = false
                    goBtn.text = "✨ Convert"
                    status(e.message ?: "Kuch masla hua. Dobara try karein.")
                }
            }
        }.start()
    }

    private fun undo() {
        val original = undoText ?: return
        val ic = currentInputConnection ?: return
        replaceAll(ic, original)
        undoText = null
        undoBtn.visibility = View.GONE
    }
}
