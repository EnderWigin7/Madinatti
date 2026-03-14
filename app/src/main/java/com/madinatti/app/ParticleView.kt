package com.madinatti.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.*

class ParticleView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val path = Path()
    private val lines = ArrayList<FlowLine>(LINE_COUNT)
    private var time = 0f
    private var w = 0f
    private var h = 0f
    private var touchX = -1f
    private var touchY = -1f

    companion object {
        const val LINE_COUNT = 120
        const val MAX_POINTS = 120
        const val LINE_ALPHA = 0.35f
        const val SPEED = 0.6f
    }

    inner class FlowLine {
        val points = ArrayList<Float>(MAX_POINTS * 2)
        var x = 0f
        var y = 0f
        var age = 0
        var lifespan = 0
        var opacity = 0f
        var strokeWidth = 0f

        init { reset() }

        fun reset() {
            points.clear()
            val angle = Math.random() * Math.PI * 2
            val dist = 60f + Math.random().toFloat() * (minOf(w, h) * 0.45f)
            x = w / 2f + cos(angle).toFloat() * dist
            y = h / 2f + sin(angle).toFloat() * dist
            age = 0
            lifespan = (500 + Math.random() * 700).toInt()
            opacity = 0f
            strokeWidth = 0.3f + Math.random().toFloat() * 0.9f
        }

        private fun noise(nx: Float, ny: Float, t: Float) =
            sin(nx * 7f + t * 3f) * 0.5f + sin(ny * 8f + t * 4f) * 0.5f

        fun update(t: Float): Boolean {
            age++
            if (age >= lifespan) return false

            val progress = age.toFloat() / lifespan
            opacity = when {
                progress < 0.1f -> (progress / 0.1f) * LINE_ALPHA
                progress > 0.9f -> ((1f - progress) / 0.1f) * LINE_ALPHA
                else -> LINE_ALPHA
            }

            val nx = (x - w / 2f) * 0.008f
            val ny = (y - h / 2f) * 0.008f
            val n = noise(nx, ny, t * 0.00012f)
            val cx = x - w / 2f
            val cy = y - h / 2f
            val r = sqrt(cx * cx + cy * cy)
            val mask = (1f - r / (minOf(w, h) * 0.5f)).coerceAtLeast(0f)
            val angle = n * PI.toFloat() * 4f + atan2(cy, cx)
            val vx = cos(angle) * mask
            val vy = sin(angle) * mask

            // Touch attraction
            if (touchX > 0) {
                val dx = touchX - x
                val dy = touchY - y
                val dist = sqrt(dx * dx + dy * dy)
                if (dist < 250f && dist > 0f) {
                    x += dx / dist * 2f
                    y += dy / dist * 2f
                }
            }

            points.add(x)
            points.add(y)
            if (points.size > MAX_POINTS * 2) {
                points.removeAt(0)
                points.removeAt(0)
            }

            x += vx * SPEED
            y += vy * SPEED

            val mag = sqrt(vx * vx + vy * vy)
            if (x < 0 || x > w || y < 0 || y > h || mag < 0.01f) return false
            return true
        }

        fun draw(canvas: Canvas) {
            if (points.size < 4) return
            paint.strokeWidth = strokeWidth
            paint.color = android.graphics.Color.argb(
                (opacity * 255f).toInt().coerceIn(0, 255), 46, 204, 113)
            path.reset()
            path.moveTo(points[0], points[1])
            var i = 2
            while (i < points.size - 2) {
                val mx = (points[i] + points[i + 2]) / 2f
                val my = (points[i + 1] + points[i + 3]) / 2f
                path.quadTo(points[i], points[i + 1], mx, my)
                i += 2
            }
            canvas.drawPath(path, paint)
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        this.w = w.toFloat()
        this.h = h.toFloat()
        lines.clear()
        repeat(LINE_COUNT) { lines.add(FlowLine()) }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        touchX = event.x
        touchY = event.y
        if (event.action == MotionEvent.ACTION_UP) {
            touchX = -1f
            touchY = -1f
        }
        performClick()
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (w == 0f || h == 0f) return
        time += 1f
        val dead = ArrayList<Int>()
        lines.forEachIndexed { idx, line ->
            if (!line.update(time)) dead.add(idx)
            else line.draw(canvas)
        }
        dead.reversed().forEach { lines[it].reset() }
        postInvalidateOnAnimation()
    }
}