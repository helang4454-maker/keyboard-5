package com.contoh.modkeyboard

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.inputmethodservice.InputMethodService
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import android.widget.TextView

class ModKeyboardService : InputMethodService() {

    private var shift = false
    private var caps = false
    private var ctrl = false
    private var alt = false
    private var meta = false
    private var panel = 0          // 0=Aa 1=F 2=# 3=numpad
    private var light = false
    private var hIdx = 1
    private val heights = intArrayOf(80, 100, 120)

    private val handler = Handler(Looper.getMainLooper())
    private var repeat: Runnable? = null
    private var container: LinearLayout? = null

    private val prefs by lazy { getSharedPreferences("mk", Context.MODE_PRIVATE) }

    override fun onCreate() {
        super.onCreate()
        light = prefs.getBoolean("light", false)
        hIdx = prefs.getInt("hIdx", 1).coerceIn(0, 2)
    }

    override fun onEvaluateFullscreenMode() = false

    override fun onCreateInputView(): View {
        val c = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        container = c
        render()
        return c
    }

    // ---------- tema ----------
    private fun bg() = if (light) 0xFFD0D3DB.toInt() else 0xFF1E2026.toInt()
    private fun keyL() = if (light) Color.WHITE else 0xFF5B5E66.toInt()
    private fun keyD() = if (light) 0xFFAEB3BD.toInt() else 0xFF3D4048.toInt()
    private fun txt() = if (light) Color.BLACK else Color.WHITE
    private fun pillBg() = if (light) 0xB3FFFFFF.toInt() else 0x29FFFFFF

    private fun dp(v: Float) = (v * resources.displayMetrics.density).toInt()
    private fun keyH() = dp(46f * heights[hIdx] / 100f)

    private fun shape(fill: Int, radius: Float = 5f) = GradientDrawable().apply {
        cornerRadius = dp(radius).toFloat()
        setColor(fill)
    }

    // ---------- status ----------
    private fun anyCmd() = ctrl || alt || meta
    private fun upper() = shift || caps

    private fun stopRepeat() {
        repeat?.let { handler.removeCallbacks(it) }
        repeat = null
    }

    private fun startRepeat(tap: () -> Unit) {
        stopRepeat()
        val r = object : Runnable {
            override fun run() {
                tap()
                handler.postDelayed(this, 60)
            }
        }
        repeat = r
        handler.postDelayed(r, 400)
    }

    // ---------- hantar input ----------
    private fun sendKey(code: Int) {
        val ic = currentInputConnection ?: return
        var m = 0
        if (ctrl) m = m or KeyEvent.META_CTRL_ON
        if (alt) m = m or KeyEvent.META_ALT_ON
        if (meta) m = m or KeyEvent.META_META_ON
        if (upper()) m = m or KeyEvent.META_SHIFT_ON
        val t = System.currentTimeMillis()
        ic.sendKeyEvent(KeyEvent(t, t, KeyEvent.ACTION_DOWN, code, 0, m))
        ic.sendKeyEvent(KeyEvent(t, t, KeyEvent.ACTION_UP, code, 0, m))
    }

    private fun afterKey() {
        var changed = false
        if (shift && !caps) { shift = false; changed = true }
        if (anyCmd()) { ctrl = false; alt = false; meta = false; changed = true }
        if (changed) render()
    }

    private fun typeChar(c: Char) {
        val ic = currentInputConnection ?: return
        if (anyCmd()) {
            val code = KeyEvent.keyCodeFromString("KEYCODE_" + c.uppercaseChar())
            if (code != KeyEvent.KEYCODE_UNKNOWN) sendKey(code)
            else ic.commitText(c.toString(), 1)
        } else {
            val out = if (upper() && panel == 0) c.uppercaseChar() else c
            ic.commitText(out.toString(), 1)
        }
        afterKey()
    }

    private fun doSpace() {
        if (anyCmd()) sendKey(KeyEvent.KEYCODE_SPACE)
        else currentInputConnection?.commitText(" ", 1)
        afterKey()
    }

    private fun doEnter() {
        if (anyCmd()) {
            sendKey(KeyEvent.KEYCODE_ENTER)
        } else {
            val ei = currentInputEditorInfo
            val opts = ei?.imeOptions ?: 0
            val action = opts and EditorInfo.IME_MASK_ACTION
            val noEnter = (opts and EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0
            if (!noEnter && action != EditorInfo.IME_ACTION_NONE &&
                action != EditorInfo.IME_ACTION_UNSPECIFIED
            ) {
                currentInputConnection?.performEditorAction(action)
            } else {
                sendKey(KeyEvent.KEYCODE_ENTER)
            }
        }
        afterKey()
    }

    private fun nav(code: Int) { sendKey(code); afterKey() }

    // ---------- elemen UI ----------
    private fun key(
        label: String,
        w: Float = 1f,
        dark: Boolean = false,
        on: Boolean = false,
        blue: Boolean = false,
        flat: Boolean = false,
        size: Float = 20f,
        rep: Boolean = false,
        tap: () -> Unit
    ): View {
        val tv = TextView(this)
        tv.text = label
        tv.gravity = Gravity.CENTER
        tv.textSize = if (label.length > 2) minOf(size, 14f) else size
        val fill = when {
            flat -> Color.TRANSPARENT
            blue -> 0xFF0A7AFF.toInt()
            on -> Color.WHITE
            dark -> keyD()
            else -> keyL()
        }
        tv.setTextColor(
            when {
                blue -> Color.WHITE
                on -> Color.BLACK
                else -> txt()
            }
        )
        tv.background = shape(fill)
        tv.setOnTouchListener { v, e ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    v.alpha = 0.7f
                    if (rep) startRepeat(tap)   // sebelum tap(); render() akan batalkan jika perlu
                    tap()
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.alpha = 1f
                    stopRepeat()
                }
            }
            true
        }
        val h = if (flat) (keyH() * 0.8f).toInt() else keyH()
        tv.layoutParams = LinearLayout.LayoutParams(0, h, w).apply {
            setMargins(dp(3f), 0, dp(3f), 0)
        }
        return tv
    }

    private fun spacer(w: Float): View = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(0, 1, w)
    }

    private fun row(items: List<View>, bottom: Float = 10f): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(bottom) }
            items.forEach { addView(it) }
        }

    private fun chars(s: String): List<View> = s.map { ch ->
        val label = if (upper() && panel == 0) ch.uppercaseChar().toString() else ch.toString()
        key(label) { typeChar(ch) }
    }

    private fun bsKey(w: Float) = key("⌫", w = w, dark = true, rep = true) {
        sendKey(KeyEvent.KEYCODE_DEL)
        afterKey()
    }

    private fun pill(label: String, on: Boolean, onTap: () -> Unit): View {
        val tv = TextView(this)
        tv.text = label
        tv.textSize = 13f
        tv.gravity = Gravity.CENTER
        tv.setPadding(dp(12f), 0, dp(12f), 0)
        tv.setTextColor(if (on) Color.BLACK else txt())
        tv.background = shape(if (on) Color.WHITE else pillBg(), 15f)
        tv.setOnClickListener { onTap() }
        tv.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, dp(30f)
        ).apply { setMargins(dp(3f), 0, dp(3f), 0) }
        return tv
    }

    // ---------- bina papan kekunci ----------
    private fun render() {
        val c = container ?: return
        stopRepeat()
        c.removeAllViews()
        c.setBackgroundColor(bg())
        c.setPadding(dp(3f), dp(8f), dp(3f), dp(14f))

        // bar atas: Ctrl Alt Cmd + tab panel
        val top = mutableListOf<View>()
        top += pill("Ctrl", ctrl) { ctrl = !ctrl; render() }
        top += pill("Alt", alt) { alt = !alt; render() }
        top += pill("Cmd", meta) { meta = !meta; render() }
        top += spacer(1f)
        listOf("Aa", "F", "#", "▦").forEachIndexed { i, t ->
            top += pill(t, panel == i) { panel = i; render() }
        }
        c.addView(row(top, 8f))

        when (panel) {
            0 -> {
                c.addView(row(chars("qwertyuiop")))
                c.addView(row(listOf(spacer(.5f)) + chars("asdfghjkl") + listOf(spacer(.5f))))
                val shiftLabel = if (caps) "⇪" else "⇧"
                c.addView(
                    row(
                        listOf(
                            key(shiftLabel, w = 1.4f, dark = true, on = shift && !caps) {
                                when {
                                    caps -> { caps = false; shift = false }
                                    shift -> { shift = false; caps = true }
                                    else -> shift = true
                                }
                                render()
                            },
                            spacer(.3f)
                        ) + chars("zxcvbnm") + listOf(spacer(.3f), bsKey(1.4f))
                    )
                )
            }
            1 -> {
                fun fk(a: Int, b: Int) = (a..b).map { n ->
                    key("F$n") { nav(KeyEvent.KEYCODE_F1 + n - 1) }
                }
                c.addView(row(fk(1, 6)))
                c.addView(row(fk(7, 12)))
                c.addView(
                    row(
                        listOf(
                            key("Esc") { nav(KeyEvent.KEYCODE_ESCAPE) },
                            key("Tab") { nav(KeyEvent.KEYCODE_TAB) },
                            key("←", rep = true) { nav(KeyEvent.KEYCODE_DPAD_LEFT) },
                            key("↑", rep = true) { nav(KeyEvent.KEYCODE_DPAD_UP) },
                            key("↓", rep = true) { nav(KeyEvent.KEYCODE_DPAD_DOWN) },
                            key("→", rep = true) { nav(KeyEvent.KEYCODE_DPAD_RIGHT) }
                        )
                    )
                )
                c.addView(
                    row(
                        listOf(
                            key("Home") { nav(KeyEvent.KEYCODE_MOVE_HOME) },
                            key("End") { nav(KeyEvent.KEYCODE_MOVE_END) },
                            key("PgUp") { nav(KeyEvent.KEYCODE_PAGE_UP) },
                            key("PgDn") { nav(KeyEvent.KEYCODE_PAGE_DOWN) },
                            key("Del", rep = true) { nav(KeyEvent.KEYCODE_FORWARD_DEL) },
                            bsKey(1f)
                        )
                    )
                )
            }
            2 -> {
                c.addView(row(chars("1234567890")))
                c.addView(row(chars("-/:;()$&@\"")))
                c.addView(row(chars("[]{}#%^*+=")))
                c.addView(row(chars("_\\|~<>.,") + listOf(bsKey(2f))))
            }
            else -> {
                val extra = listOf(
                    bsKey(1f),
                    key("(") { typeChar('(') },
                    key(")") { typeChar(')') },
                    key(",") { typeChar(',') }
                )
                listOf("789/", "456*", "123-", "0.=+").forEachIndexed { i, s ->
                    c.addView(row(chars(s) + listOf(extra[i])))
                }
            }
        }

        // baris bawah
        c.addView(
            row(
                listOf(
                    if (panel == 2) key("ABC", w = 1.3f, dark = true) { panel = 0; render() }
                    else key("123", w = 1.3f, dark = true) { panel = 2; render() },
                    key(",", dark = true) { typeChar(',') },
                    key("space", w = 4.6f) { doSpace() },
                    key(".", dark = true) { typeChar('.') },
                    key("return", w = 2f, blue = true) { doEnter() }
                ),
                2f
            )
        )

        // baris paling bawah: glob, tema, tinggi, sembunyi
        c.addView(
            row(
                listOf(
                    key("🌐", flat = true) {
                        (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                            .showInputMethodPicker()
                    },
                    spacer(3f),
                    key(if (light) "🌙" else "☀", flat = true) {
                        light = !light
                        prefs.edit().putBoolean("light", light).apply()
                        render()
                    },
                    key("↕", flat = true) {
                        hIdx = (hIdx + 1) % heights.size
                        prefs.edit().putInt("hIdx", hIdx).apply()
                        render()
                    },
                    key("▼", flat = true) { requestHideSelf(0) }
                ),
                0f
            )
        )
    }
}
