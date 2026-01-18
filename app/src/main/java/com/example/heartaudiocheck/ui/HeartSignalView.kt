package com.example.heartaudiocheck.ui

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import kotlin.math.max

class HeartSignalView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var values: FloatArray? = null
    private var durationSec: Float = 30f
    private var sampleRateHz: Float = 100f

    private var textColor: Int = 0xFFFFFFFF.toInt()
    private var secondaryColor: Int = 0xCCFFFFFF.toInt()

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(2f)
    }

    private val axisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(1f)
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = sp(12f)
    }

    private val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = dp(1f)
    }

    private var pxPerSec = dp(120f)
    private val pad = dp(12f)
    private val axisH = dp(22f)

    init {
        resolveThemeColors()
        applyPaintColors()
    }

    private fun resolveThemeColors() {
        // textColorPrimary
        val tv = TypedValue()
        if (context.theme.resolveAttribute(android.R.attr.textColorPrimary, tv, true)) {
            val csl: ColorStateList? = try {
                context.getColorStateList(tv.resourceId)
            } catch (_: Exception) {
                null
            }
            textColor = csl?.defaultColor ?: tv.data
        }

        // textColorSecondary as fallback for axes
        val tv2 = TypedValue()
        if (context.theme.resolveAttribute(android.R.attr.textColorSecondary, tv2, true)) {
            val csl2: ColorStateList? = try {
                context.getColorStateList(tv2.resourceId)
            } catch (_: Exception) {
                null
            }
            secondaryColor = csl2?.defaultColor ?: tv2.data
        } else {
            secondaryColor = textColor
        }
    }

    private fun applyPaintColors() {
        linePaint.color = textColor
        axisPaint.color = secondaryColor
        axisPaint.alpha = 120

        tickPaint.color = secondaryColor
        tickPaint.alpha = 160

        textPaint.color = secondaryColor
        textPaint.alpha = 200
    }

    fun setSeries(values: FloatArray, durationSec: Float, sampleRateHz: Float) {
        this.values = values
        this.durationSec = durationSec
        this.sampleRateHz = sampleRateHz
        requestLayout()
        invalidate()
    }

    fun clearSeries() {
        values = null
        requestLayout()
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val h = MeasureSpec.getSize(heightMeasureSpec)
        val wWanted = max(suggestedMinimumWidth, (durationSec * pxPerSec + pad * 2).toInt())
        val w = resolveSize(wWanted, widthMeasureSpec)
        val hh = resolveSize(max(h, dp(140f).toInt()), heightMeasureSpec)
        setMeasuredDimension(w, hh)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // in case theme changes at runtime / locale recreate
        resolveThemeColors()
        applyPaintColors()

        val w = width.toFloat()
        val h = height.toFloat()

        val plotTop = pad
        val plotBottom = h - pad - axisH
        val plotH = max(1f, plotBottom - plotTop)

        // baseline
        canvas.drawLine(pad, plotBottom, w - pad, plotBottom, axisPaint)

        // time ticks
        drawTimeAxis(canvas, plotBottom + dp(6f), w)

        val v = values ?: return
        if (v.isEmpty()) return

        val n = v.size
        var prevX = pad
        var prevY = plotBottom - (v[0].coerceIn(0f, 1f) * plotH)

        for (i in 1 until n) {
            val t = i / sampleRateHz
            val x = pad + t * pxPerSec
            val y = plotBottom - (v[i].coerceIn(0f, 1f) * plotH)
            canvas.drawLine(prevX, prevY, x, y, linePaint)
            prevX = x
            prevY = y
            if (x > w - pad) break
        }
    }

    private fun drawTimeAxis(canvas: Canvas, y: Float, w: Float) {
        val tickEverySec = 5
        val maxSec = durationSec.toInt()

        for (s in 0..maxSec step tickEverySec) {
            val x = pad + s * pxPerSec
            if (x > w - pad) break

            canvas.drawLine(x, y, x, y + dp(8f), tickPaint)

            val label = "${s}s"
            canvas.drawText(label, x + dp(2f), y + dp(20f), textPaint)
        }
    }

    private fun dp(v: Float): Float = v * resources.displayMetrics.density
    private fun sp(v: Float): Float = v * resources.displayMetrics.scaledDensity
}
