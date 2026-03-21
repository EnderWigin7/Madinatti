package com.madinatti.app

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class QiblaCompassView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var azimuth = 0f
    private var qiblaBearing = 0f
    private val smoothFactor = 0.12f

    // ── Paints ──
    private val outerRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.parseColor("#442ECC71")
        strokeWidth = 2.5f
    }

    private val innerFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#0A2ECC71")
    }

    private val innerRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.parseColor("#222ECC71")
        strokeWidth = 1.5f
    }

    private val tickMajorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.parseColor("#7FA68A")
        strokeWidth = 2f
        strokeCap = Paint.Cap.ROUND
    }

    private val tickMinorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.parseColor("#337FA68A")
        strokeWidth = 1f
        strokeCap = Paint.Cap.ROUND
    }

    private val cardinalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        color = Color.parseColor("#CCFFFFFF")
        typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
    }

    private val northLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        color = Color.parseColor("#2ECC71")
        typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
    }

    private val degreeLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        color = Color.parseColor("#447FA68A")
        typeface = Typeface.DEFAULT
    }

    private val qiblaArrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#2ECC71")
    }

    private val qiblaGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.parseColor("#332ECC71")
        strokeCap = Paint.Cap.ROUND
    }

    private val kaabaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
    }

    private val centerDotOuter = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#2ECC71")
    }

    private val centerDotInner = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#0D1F17")
    }

    private val pointerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.WHITE
    }


    private var targetAzimuth = 0f

    fun updateAzimuth(newAzimuth: Float) {
        targetAzimuth = newAzimuth
        var diff = targetAzimuth - azimuth
        if (diff > 180) diff -= 360
        if (diff < -180) diff += 360
        azimuth += diff * smoothFactor
        azimuth = ((azimuth % 360) + 360) % 360
        invalidate()
    }

    fun setQiblaBearing(bearing: Float) {
        qiblaBearing = bearing
        invalidate()
    }


    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val cx = width / 2f
        val cy = height / 2f
        val radius = minOf(cx, cy) - dp(16f)
        val d = resources.displayMetrics.density

        // Text sizes
        cardinalPaint.textSize = 15f * d
        northLabelPaint.textSize = 15f * d
        degreeLabelPaint.textSize = 9f * d
        kaabaPaint.textSize = 20f * d
        qiblaGlowPaint.strokeWidth = dp(22f)

        // ── Fixed pointer at top (device direction) ──
        val ptrPath = Path().apply {
            moveTo(cx, cy - radius - dp(6f))
            lineTo(cx - dp(5f), cy - radius + dp(2f))
            lineTo(cx + dp(5f), cy - radius + dp(2f))
            close()
        }
        canvas.drawPath(ptrPath, pointerPaint)

        // ── Rotate compass face ──
        canvas.save()
        canvas.rotate(-azimuth, cx, cy)

        // Inner fill
        canvas.drawCircle(cx, cy, radius - dp(22f), innerFillPaint)

        // Rings
        canvas.drawCircle(cx, cy, radius, outerRingPaint)
        canvas.drawCircle(cx, cy, radius - dp(22f), innerRingPaint)

        // Tick marks
        for (i in 0 until 360 step 5) {
            canvas.save()
            canvas.rotate(i.toFloat(), cx, cy)
            val isCardinal = i % 90 == 0
            val isMajor = i % 30 == 0
            val startY = cy - radius + dp(3f)
            val endY = startY + when {
                isCardinal -> dp(16f)
                isMajor -> dp(12f)
                i % 10 == 0 -> dp(8f)
                else -> dp(5f)
            }
            val paint = if (isMajor) tickMajorPaint else tickMinorPaint
            canvas.drawLine(cx, startY, cx, endY, paint)
            canvas.restore()
        }

        // Cardinal directions
        val cardinalR = radius - dp(40f)
        drawRotatedText(canvas, "N", 0f, cx, cy, cardinalR, northLabelPaint)
        drawRotatedText(canvas, "E", 90f, cx, cy, cardinalR, cardinalPaint)
        drawRotatedText(canvas, "S", 180f, cx, cy, cardinalR, cardinalPaint)
        drawRotatedText(canvas, "W", 270f, cx, cy, cardinalR, cardinalPaint)

        // Degree labels
        for (deg in listOf(30, 60, 120, 150, 210, 240, 300, 330)) {
            drawRotatedText(
                canvas, "$deg°", deg.toFloat(),
                cx, cy, cardinalR, degreeLabelPaint
            )
        }

        // ── Qibla glow arc ──
        val glowRect = RectF(
            cx - radius + dp(11f), cy - radius + dp(11f),
            cx + radius - dp(11f), cy + radius - dp(11f)
        )
        canvas.drawArc(
            glowRect,
            qiblaBearing - 90f - 12f,
            24f, false, qiblaGlowPaint
        )

        // ── Qibla arrow ──
        canvas.save()
        canvas.rotate(qiblaBearing, cx, cy)

        val arrowPath = Path().apply {
            moveTo(cx, cy - radius + dp(24f))
            lineTo(cx - dp(7f), cy - radius + dp(36f))
            lineTo(cx + dp(7f), cy - radius + dp(36f))
            close()
        }
        canvas.drawPath(arrowPath, qiblaArrowPaint)

        // Kaaba emoji
        canvas.drawText(
            "🕋", cx,
            cy - radius + dp(56f), kaabaPaint
        )
        canvas.restore()

        canvas.drawCircle(cx, cy, dp(5f), centerDotOuter)
        canvas.drawCircle(cx, cy, dp(2.5f), centerDotInner)

        canvas.restore()
    }

    private fun drawRotatedText(
        canvas: Canvas, text: String, angle: Float,
        cx: Float, cy: Float, radius: Float, paint: Paint
    ) {
        canvas.save()
        canvas.rotate(angle, cx, cy)
        val textY = cy - radius + paint.textSize * 0.35f
        canvas.drawText(text, cx, textY, paint)
        canvas.restore()
    }

    private fun dp(v: Float): Float =
        v * resources.displayMetrics.density
}