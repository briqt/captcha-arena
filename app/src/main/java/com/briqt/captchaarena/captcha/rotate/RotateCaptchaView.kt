package com.briqt.captchaarena.captcha.rotate

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.briqt.captchaarena.model.TrajectoryData
import com.briqt.captchaarena.model.TrajectoryPoint
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.random.Random

/**
 * Rotation CAPTCHA view.
 * User must rotate the inner circle to match the correct orientation.
 */
class RotateCaptchaView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var tolerance: Float = 8f // degrees tolerance
    var onCaptchaResult: ((success: Boolean, trajectory: TrajectoryData) -> Unit)? = null

    // State
    private var targetAngle: Float = 0f    // angle the image should be at (always 0 = correct)
    private var currentAngle: Float = 0f   // current rotation offset
    private var initialAngle: Float = 0f   // the scrambled angle user needs to correct
    private var isDragging = false
    private var lastTouchAngle: Float = 0f

    // Images
    private var outerBitmap: Bitmap? = null
    private var innerBitmap: Bitmap? = null

    // Trajectory
    private val trajectory = mutableListOf<TrajectoryPoint>()
    private var startTime = 0L

    // Paints
    private val imagePaint = Paint().apply { isFilterBitmap = true; isAntiAlias = true }
    private val outerRingPaint = Paint().apply {
        color = Color.rgb(200, 200, 200)
        style = Paint.Style.STROKE
        strokeWidth = 4f
        isAntiAlias = true
    }
    private val innerRingPaint = Paint().apply {
        color = Color.rgb(76, 175, 80)
        style = Paint.Style.STROKE
        strokeWidth = 6f
        isAntiAlias = true
    }
    private val indicatorPaint = Paint().apply {
        color = Color.rgb(76, 175, 80)
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    private val bgPaint = Paint().apply {
        color = Color.rgb(245, 245, 245)
        style = Paint.Style.FILL
    }
    private val hintPaint = Paint().apply {
        color = Color.rgb(120, 120, 120)
        textSize = 36f
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }

    fun reset() {
        if (width <= 0 || height <= 0) return
        generateChallenge()
        trajectory.clear()
        startTime = System.currentTimeMillis()
        invalidate()
    }

    private fun generateChallenge() {
        val size = (minOf(width, height) * 0.7f).toInt()

        // Generate a recognizable image that has a clear "up" direction
        outerBitmap = generateOuterImage(size)
        innerBitmap = generateInnerImage((size * 0.7f).toInt())

        // Random rotation offset (what user needs to correct)
        initialAngle = Random.nextFloat() * 300 + 30 // 30-330 degrees (not too close to 0)
        currentAngle = initialAngle
    }

    private fun generateOuterImage(size: Int): Bitmap {
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val cx = size / 2f
        val cy = size / 2f
        val r = size / 2f - 4f

        // Draw ring background
        canvas.drawCircle(cx, cy, r, Paint().apply {
            color = Color.rgb(230, 230, 235)
            style = Paint.Style.FILL
        })

        // Draw reference marks (N/S/E/W)
        val markPaint = Paint().apply {
            color = Color.rgb(180, 180, 180)
            textSize = 24f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText("▲", cx, 30f, markPaint)

        return bmp
    }

    private fun generateInnerImage(size: Int): Bitmap {
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val cx = size / 2f
        val cy = size / 2f
        val r = size / 2f - 2f

        // Gradient circle with recognizable pattern
        val gradient = RadialGradient(
            cx, cy, r,
            intArrayOf(
                Color.rgb(Random.nextInt(100, 200), Random.nextInt(150, 255), Random.nextInt(100, 200)),
                Color.rgb(Random.nextInt(50, 150), Random.nextInt(100, 200), Random.nextInt(50, 150))
            ),
            null, Shader.TileMode.CLAMP
        )
        canvas.drawCircle(cx, cy, r, Paint().apply { shader = gradient; isAntiAlias = true })

        // Draw an arrow/triangle pointing up (this is what user needs to align)
        val arrowPaint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        val path = Path().apply {
            moveTo(cx, cy - r * 0.6f)  // top point
            lineTo(cx - r * 0.25f, cy - r * 0.1f) // bottom left
            lineTo(cx + r * 0.25f, cy - r * 0.1f) // bottom right
            close()
        }
        canvas.drawPath(path, arrowPaint)

        // Add some decorative elements
        val decoPaint = Paint().apply {
            color = Color.argb(60, 255, 255, 255)
            style = Paint.Style.STROKE
            strokeWidth = 2f
            isAntiAlias = true
        }
        canvas.drawCircle(cx, cy, r * 0.5f, decoPaint)
        canvas.drawCircle(cx, cy, r * 0.3f, decoPaint)

        return bmp
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        generateChallenge()
        startTime = System.currentTimeMillis()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val cx = width / 2f
        val imgAreaHeight = height * 0.8f
        val cy = imgAreaHeight / 2f

        // Background
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Draw outer ring (static)
        outerBitmap?.let { bmp ->
            val left = cx - bmp.width / 2f
            val top = cy - bmp.height / 2f
            canvas.drawBitmap(bmp, left, top, imagePaint)
        }
        val outerR = (minOf(width, height) * 0.35f)
        canvas.drawCircle(cx, cy, outerR, outerRingPaint)

        // Draw reference indicator at top
        canvas.drawCircle(cx, cy - outerR - 12, 8f, indicatorPaint)

        // Draw inner circle (rotated)
        innerBitmap?.let { bmp ->
            canvas.save()
            canvas.rotate(currentAngle, cx, cy)
            val left = cx - bmp.width / 2f
            val top = cy - bmp.height / 2f
            canvas.drawBitmap(bmp, left, top, imagePaint)
            canvas.restore()
        }
        val innerR = (minOf(width, height) * 0.25f)
        canvas.drawCircle(cx, cy, innerR, innerRingPaint)

        // Hint text
        canvas.drawText("旋转内圈使箭头朝上 ↑", cx, imgAreaHeight + (height - imgAreaHeight) / 2f + 12f, hintPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val cx = width / 2f
        val cy = height * 0.8f / 2f

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                isDragging = true
                lastTouchAngle = getAngle(event.x, event.y, cx, cy)
                trajectory.add(TrajectoryPoint(event.x, event.y, System.currentTimeMillis(), event.pressure, event.size))
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (isDragging) {
                    val angle = getAngle(event.x, event.y, cx, cy)
                    val delta = angle - lastTouchAngle
                    currentAngle += delta
                    // Normalize to 0-360
                    currentAngle = ((currentAngle % 360) + 360) % 360
                    lastTouchAngle = angle
                    trajectory.add(TrajectoryPoint(event.x, event.y, System.currentTimeMillis(), event.pressure, event.size))
                    invalidate()
                    return true
                }
            }
            MotionEvent.ACTION_UP -> {
                if (isDragging) {
                    isDragging = false
                    trajectory.add(TrajectoryPoint(event.x, event.y, System.currentTimeMillis(), event.pressure, event.size))
                    checkResult()
                    return true
                }
            }
        }
        return super.onTouchEvent(event)
    }

    private fun getAngle(x: Float, y: Float, cx: Float, cy: Float): Float {
        return Math.toDegrees(atan2((y - cy).toDouble(), (x - cx).toDouble())).toFloat()
    }

    private fun checkResult() {
        val endTime = System.currentTimeMillis()
        // Success if current angle is close to 0 (or 360)
        val normalizedAngle = ((currentAngle % 360) + 360) % 360
        val success = normalizedAngle < tolerance || normalizedAngle > (360 - tolerance)

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
