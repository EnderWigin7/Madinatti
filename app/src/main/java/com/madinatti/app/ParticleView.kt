package com.madinatti.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.math.sin

class ParticleView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val particles = ArrayList<FloatArray>(PARTICLE_COUNT)
    private var time = 0f

    companion object {
        const val PARTICLE_COUNT = 1800
        const val STRATA = 12
    }

    // Each particle: floatArray[x, y, z, side, stratum, flow, oscillation, velocity, brightness]
    private fun init(w: Float, h: Float) {
        particles.clear()
        val cx = w / 2f
        repeat(PARTICLE_COUNT) {
            val stratum = (Math.random() * STRATA).toInt()
            val depth = stratum * 25f
            val y = (Math.random() * h).toFloat()
            val phase = y * 0.01f + stratum * 0.3f
            val undulation = sin(phase) * 35f +
                    sin(phase * 2 + stratum * 0.8f) * 18f +
                    sin(phase * 4 + stratum * 1.5f) * 8f
            val side = if (Math.random() < 0.5) -1f else 1f
            val thickness = 40f + stratum * 3f
            val x = cx + side * (60f + undulation + depth) +
                    ((Math.random() - 0.5) * thickness).toFloat()
            particles.add(floatArrayOf(
                x, y,
                (stratum - STRATA / 2f) * 30f + ((Math.random() - 0.5) * 20).toFloat(),
                side,
                stratum.toFloat(),
                (Math.random() * Math.PI * 2).toFloat(),
                (Math.random() * Math.PI * 2).toFloat(),
                0.4f + stratum * 0.08f,
                0.6f + Math.random().toFloat() * 0.4f
            ))
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w == 0f || h == 0f) return
        if (particles.isEmpty()) init(w, h)

        time += 0.016f
        val cx = w / 2f

        for (p in particles) {
            val stratum = p[4].toInt()
            val phase = p[1] * 0.01f + stratum * 0.3f + time * 0.03f
            val undulation = sin(phase) * 35f +
                    sin(phase * 2 + stratum * 0.8f) * 18f +
                    sin(phase * 4 + stratum * 1.5f) * 8f
            val depth = stratum * 25f
            val thickness = 40f + stratum * 3f
            val targetX = cx + p[3] * (60f + undulation + depth)
            val drift = sin(p[5] + time * 0.6f + stratum * 0.4f) * thickness * 0.7f

            p[0] = p[0] * 0.94f + (targetX + drift) * 0.06f
            p[1] += p[7]
            p[1] += sin(p[6] + time * 0.8f) * 0.3f
            p[2] += sin(time * 0.3f + p[5] + stratum * 0.6f) * 0.25f

            if (p[1] > h + 40f) {
                p[1] = -40f
                p[5] = (Math.random() * Math.PI * 2).toFloat()
            }

            val depthFactor = ((p[2] + STRATA * 15f) / (STRATA * 30f)).coerceIn(0f, 1f)
            val opacity = (0.12f + depthFactor * 0.15f).coerceIn(0f, 1f)
            val size = (0.3f + depthFactor * 0.9f).coerceIn(0.1f, 2f)
            val b = (45 + stratum * 3 + (p[8] * 15).toInt()).coerceIn(0, 255)

            if (stratum % 3 == 0) {
                paint.color = android.graphics.Color.argb((opacity * 15).toInt(), b, b, b)
                canvas.drawCircle(p[0], p[1], size * 2.2f, paint)
            }
            paint.color = android.graphics.Color.argb((opacity * 255).toInt(), b, b, b)
            canvas.drawCircle(p[0], p[1], size, paint)
        }
        postInvalidateOnAnimation()
    }
}