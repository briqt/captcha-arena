package com.briqt.captchaarena.captcha.clickword

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.briqt.captchaarena.model.TrajectoryData
import com.briqt.captchaarena.model.TrajectoryPoint
import kotlin.math.hypot
import kotlin.random.Random

/**
 * Click-word (文字点选) CAPTCHA view.
 * Displays Chinese characters scattered on a background image.
 * User must click them in the correct order shown in the prompt.
 */
class ClickWordCaptchaView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var tolerance: Float = 40f // click tolerance radius
    var onCaptchaResult: ((success: Boolean, trajectory: TrajectoryData) -> Unit)? = null

    // Characters pool
    private val charPool = "天地人和春夏秋冬风雨雷电山水花鸟日月星辰龙虎豹鹤松竹梅兰菊荷"

    // Current challenge
    private var targetChars: List<Char> = emptyList()     // chars to click in order
    private var allChars: List<CharInfo> = emptyList()     // all displayed chars with positions
    private var clickedIndices: MutableList<Int> = mutableListOf()
    private var currentTargetIndex = 0
    private var backgroundBitmap: Bitmap? = null

    // Trajectory tracking
    private val trajectory = mutableListOf<TrajectoryPoint>()
    private var startTime = 0L

    data class CharInfo(
        val char: Char,
        val x: Float,
        val y: Float,
        val rotation: Float,
        val size: Float,
        val color: Int,
        val isTarget: Boolean
    )

    // Paints
    private val charPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
        typeface = Typeface.DEFAULT_BOLD
    }
    private val promptPaint = Paint().apply {
        isAntiAlias = true
        color = Color.rgb(33, 33, 33)
        textSize = 42f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }
    private val promptBgPaint = Paint().apply {
        color = Color.argb(220, 255, 255, 255)
        style = Paint.Style.FILL
    }
    private val markerPaint = Paint().apply {
        color = Color.rgb(76, 175, 80)
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    private val markerTextPaint = Paint().apply {
        color = Color.WHITE
        textSize = 28f
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
        typeface = Typeface.DEFAULT_BOLD
    }

    fun reset() {
        if (width <= 0 || height <= 0) return
        generateChallenge()
        clickedIndices.clear()
        currentTargetIndex = 0
        trajectory.clear()
        startTime = System.currentTimeMillis()
        invalidate()
    }

    private fun generateChallenge() {
        val imgHeight = (height * 0.75f).toInt()

        // Generate background
        backgroundBitmap = generateBackground(width, imgHeight)

        // Pick 3 target characters
        val shuffled = charPool.toList().shuffled()
        targetChars = shuffled.take(3)

        // Place 6-8 total characters (including 3 targets)
        val extraCount = Random.nextInt(3, 5)
        val extraChars = shuffled.drop(3).take(extraCount)
        val allCharsList = (targetChars + extraChars).shuffled()

        // Generate positions (avoid overlaps)
        val positions = mutableListOf<CharInfo>()
        val margin = 80f
        for (char in allCharsList) {
            var attempts = 0
            var placed = false
            while (attempts < 50 && !placed) {
                val x = Random.nextFloat() * (width - margin * 2) + margin
                val y = Random.nextFloat() * (imgHeight - margin * 2) + margin
                // Check overlap
                val overlaps = positions.any { hypot(it.x - x, it.y - y) < 80f }
                if (!overlaps) {
                    positions.add(
                        CharInfo(
                            char = char,
                            x = x,
                            y = y,
                            rotation = Random.nextFloat() * 60 - 30,
                            size = Random.nextFloat() * 16 + 44,
                            color = Color.rgb(
                                Random.nextInt(50, 220),
                                Random.nextInt(50, 220),
                                Random.nextInt(50, 220)
                            ),
                            isTarget = char in targetChars
                        )
                    )
                    placed = true
                }
                attempts++
            }
        }
        allChars = positions
    }

    private fun generateBackground(w: Int, h: Int): Bitmap {
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val gradient = LinearGradient(
            0f, 0f, w.toFloat(), h.toFloat(),
            intArrayOf(
                Color.rgb(240, 240, 245),
                Color.rgb(220, 225, 235),
                Color.rgb(235, 230, 240)
            ),
            null, Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), Paint().apply { shader = gradient })

        // Add noise lines
        val linePaint = Paint().apply {
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
            isAntiAlias = true
        }
        repeat(20) {
            linePaint.color = Color.argb(30, Random.nextInt(256), Random.nextInt(256), Random.nextInt(256))
            canvas.drawLine(
                Random.nextFloat() * w, Random.nextFloat() * h,
                Random.nextFloat() * w, Random.nextFloat() * h,
                linePaint
            )
        }
        return bmp
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        generateChallenge()
        startTime = System.currentTimeMillis()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val imgHeight = (height * 0.75f).toInt()

        // Background
        backgroundBitmap?.let { canvas.drawBitmap(it, 0f, 0f, null) }

        // Characters
        for (info in allChars) {
            canvas.save()
            canvas.rotate(info.rotation, info.x, info.y)
            charPaint.color = info.color
            charPaint.textSize = info.size
            charPaint.textAlign = Paint.Align.CENTER
            // Add shadow for readability
            charPaint.setShadowLayer(3f, 1f, 1f, Color.argb(80, 0, 0, 0))
            canvas.drawText(info.char.toString(), info.x, info.y + info.size / 3, charPaint)
            canvas.restore()
        }

        // Draw click markers
        for ((idx, charIdx) in clickedIndices.withIndex()) {
            val info = allChars[charIdx]
            canvas.drawCircle(info.x, info.y, 18f, markerPaint)
            canvas.drawText((idx + 1).toString(), info.x, info.y + 10f, markerTextPaint)
        }

        // Draw prompt area at bottom
        val promptTop = imgHeight.toFloat()
        canvas.drawRect(0f, promptTop, width.toFloat(), height.toFloat(), promptBgPaint)
        val promptText = "请依次点击: ${targetChars.joinToString(" ")}"
        canvas.drawText(promptText, width / 2f, promptTop + (height - imgHeight) / 2f + 14f, promptPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val imgHeight = (height * 0.75f)
            if (event.y > imgHeight) return false // clicked on prompt area

            trajectory.add(
                TrajectoryPoint(event.x, event.y, System.currentTimeMillis(), event.pressure, event.size)
            )

            // Find closest character to click point
            val clickedIdx = allChars.indexOfFirst { info ->
                hypot(info.x - event.x, info.y - event.y) < tolerance
            }

            if (clickedIdx >= 0 && clickedIdx !in clickedIndices) {
                clickedIndices.add(clickedIdx)
                invalidate()

                // Check if all targets clicked
                if (clickedIndices.size == targetChars.size) {
                    checkResult()
                }
            }
            return true
        }
        return super.onTouchEvent(event)
    }

    private fun checkResult() {
        val endTime = System.currentTimeMillis()

        // Verify order: each clicked char should match the target order
        val success = clickedIndices.size == targetChars.size &&
            clickedIndices.mapIndexed { idx, charIdx ->
                allChars[charIdx].char == targetChars[idx]
            }.all { it }

        val data = TrajectoryData(
            points = trajectory.toList(),
            startTime = startTime,
            endTime = endTime,
            success = success
        )
        onCaptchaResult?.invoke(success, data)

        if (!success) {
            postDelayed({ reset() }, 800)
        }
    }
}
