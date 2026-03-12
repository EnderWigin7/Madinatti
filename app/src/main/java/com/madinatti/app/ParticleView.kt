package com.madinatti.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.math.sin
import kotlin.random.Random

class ParticleView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val particles = mutableListOf<Particle>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var time = 0f
    private val random = Random(42)

    data class Particle(
        var x: Float,
        var y: Float,
        val speed: Float,
        val size: Float,
        val alpha: Int,
        val phase: Float
    )

    init {
        repeat(80) {
            particles.add(
                Particle(
                    x = random.nextFloat(),
                    y = random.nextFloat(),
                    speed = 0.0008f + random.nextFloat() * 0.001f,
                    size = 2f + random.nextFloat() * 4f,
                    alpha = 30 + random.nextInt(80),
                    phase = random.nextFloat() * 6.28f
                )
            )
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        time += 0.016f

        particles.forEach { p ->
            val x = (p.x + sin(time + p.phase) * 0.02f) * width
            val y = (p.y - time * p.speed) % 1f * height

            paint.color = 0xFF2ECC71.toInt()
            paint.alpha = p.alpha

            canvas.drawCircle(x, y.let { if (it < 0) it + height else it }, p.size, paint)
        }

        postInvalidateOnAnimation()
    }
}