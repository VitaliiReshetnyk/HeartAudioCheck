package com.example.heartaudiocheck.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

class HeartSignalView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var series: FloatArray = FloatArray(0)
    private var durationSec: Float = 0f
    private var sampleRateHz: Int = 100

    private var pxPerSecond: Float = dp(20f) // 20dp per second -> 60s ~ 1200dp (scrollable)
    private var padLeft = dp(12f)
    private var padRight = dp(12f)
    private var padTop = dp(10f)
    private var padBottom = dp(20f)

    private val paintAxis = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(1.2f)
    }

    private val paintLine = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(1.8f)
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val paintText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = sp(12f)
    }

    fun setSeries(values: FloatArray, durationSec: Float, sampleRateHz: Int) {
        this.series = values
        this.durationSec = durationSec
        this.sampleRateHz = if (sampleRateHz > 0f) sampleRateHz else 100

        requestLayout()
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val h = MeasureSpec.getSize(heightMeasureSpec)
        val contentW = (durationSec * pxPerSecond + padLeft + padRight).toInt().coerceAtLeast(suggestedMinimumWidth)
        val w = resolveSize(contentW, widthMeasureSpec)
        setMeasuredDimension(w, h)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Use system-like colors (same family as text)
        val baseTextColor = try { (rootView.findViewById<View>(android.R.id.content) as? View)?.solidColor ?: 0 } catch (_: Throwable) { 0 }
        val textColor = if (paintText.color != 0) paintText.color else 0xFFFFFFFF.toInt()

        // Better: take current theme text color from this view's context by inheriting from default text appearance
        // But since this is a custom view, we keep it readable:
        paintAxis.color = 0x66FFFFFF.toInt()
        paintLine.color = 0xCCFFFFFF.toInt()
        paintText.color = 0xCCFFFFFF.toInt()

        val w = width.toFloat()
        val h = height.toFloat()
        val left = padLeft
        val top = padTop
        val right = w - padRight
        val bottom = h - padBottom

        if (series.isEmpty() || durationSec <= 0f) {
            // Draw only axes + 0s label
            canvas.drawLine(left, bottom, right, bottom, paintAxis)
            canvas.drawLine(left, top, left, bottom, paintAxis)
            canvas.drawText("0s", left, h - dp(4f), paintText)
            return
        }

        // Axes
        canvas.drawLine(left, bottom, right, bottom, paintAxis)
        canvas.drawLine(left, top, left, bottom, paintAxis)

        // Time ticks every 5 seconds, labels every 10 seconds
        val tickStepSec = 5
        val labelStepSec = 10
        val total = ceil(durationSec).toInt()

        for (t in 0..total step tickStepSec) {
            val x = left + t * pxPerSecond
            if (x > right) break
            val tickH = if (t % labelStepSec == 0) dp(10f) else dp(6f)
            canvas.drawLine(x, bottom, x, bottom + tickH, paintAxis)
            if (t % labelStepSec == 0) {
                canvas.drawText("${t}s", x - dp(6f), h - dp(4f), paintText)
            }
        }

        // Robust min/max (avoid flatline)
        var minV = Float.POSITIVE_INFINITY
        var maxV = Float.NEGATIVE_INFINITY
        for (v in series) {
            if (v < minV) minV = v
            if (v > maxV) maxV = v
        }

        // If almost constant -> show a centered line (still visible)
        val range = max(1e-6f, maxV - minV)
        val usableH = max(1f, bottom - top)

        val n = series.size
        val dt = 1f / sampleRateHz
        val maxX = left + durationSec * pxPerSecond

        // Draw polyline
        var prevX = left
        var prevY = bottom - ((series[0] - minV) / range) * usableH

        for (i in 1 until n) {
            val t = i * dt
            if (t > durationSec) break
            val x = left + t * pxPerSecond
            if (x > maxX) break
            val y = bottom - ((series[i] - minV) / range) * usableH
            canvas.drawLine(prevX, prevY, x, y, paintLine)
            prevX = x
            prevY = y
        }
    }

    private fun dp(v: Float): Float = v * resources.displayMetrics.density
    private fun sp(v: Float): Float = v * resources.displayMetrics.scaledDensity
}
