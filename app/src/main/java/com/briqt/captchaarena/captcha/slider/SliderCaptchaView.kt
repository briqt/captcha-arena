package com.briqt.captchaarena.captcha.slider

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.briqt.captchaarena.model.TrajectoryPoint
import com.briqt.captchaarena.model.TrajectoryData
import kotlin.math.abs
import kotlin.random.Random

/**
 * Custom slider puzzle CAPTCHA view.
 * Features:
 * - Random gap position generation
 * - Puzzle piece with jigsaw shape
 * - Trajectory recording and analysis
 * - Configurable tolerance and anti-bot detection
 */
class SliderCaptchaView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Captcha configuration
    var tolerance: Float = 5f // pixels tolerance for success
    var pieceSize: Int = 120  // puzzle piece size in dp
    var antiBot: Boolean = true // enable trajectory analysis

    // Callbacks
    var onCaptchaResult: ((success: Boolean, trajectory: TrajectoryData) -> Unit)? = null

    // Internal state
    private var backgroundBitmap: Bitmap? = null
    private var gapX: Float = 0f
    private var gapY: Float = 0f
    private var sliderX: Float = 0f
    private var sliderStartX: Float = 0f
    private var isDragging = false
    private val trajectory = mutableListOf<TrajectoryPoint>()
    private var dragStartTime: Long = 0L

    // Paints
    private val gapPaint = Paint().apply {
        color = Color.argb(120, 0, 0, 0)
        style = Paint.Style.FILL
    }
    private val piecePaint = Paint().apply {
        isFilterBitmap = true
    }
    private val sliderTrackPaint = Paint().apply {
        color = Color.argb(40, 0, 0, 0)
        style = Paint.Style.FILL
    }
    private val sliderThumbPaint = Paint().apply {
        color = Color.rgb(76, 175, 80)
        style = Paint.Style.FILL
        setShadowLayer(4f, 0f, 2f, Color.argb(80, 0, 0, 0))
    }
    private val arrowPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    private val borderPaint = Paint().apply {
        color = Color.rgb(76, 175, 80)
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }
    private val textPaint = Paint().apply {
        color = Color.rgb(150, 150, 150)
        textSize = 36f
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }

    // Dimensions
    private val pieceSizePx get() = (pieceSize * resources.displayMetrics.density).toInt()
    private val sliderHeight = (48 * resources.displayMetrics.density).toInt()
    private val sliderThumbSize = (40 * resources.displayMetrics.density).toInt()
    private val imageAreaHeight get() = height - sliderHeight - (16 * resources.displayMetrics.density).toInt()

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    fun reset() {
        generateCaptcha()
        sliderX = 0f
        isDragging = false
        trajectory.clear()
        invalidate()
    }

    private fun generateCaptcha() {
        if (width <= 0 || height <= 0) return

        val imgWidth = width
        val imgHeight = imageAreaHeight

        // Generate a colorful background
        backgroundBitmap = generateBackground(imgWidth, imgHeight)

        // Random gap position (ensure piece fits and not too close to edges)
        val margin = pieceSizePx + 20
        gapX = Random.nextFloat() * (imgWidth - margin * 2) + margin
        gapY = Random.nextFloat() * (imgHeight - pieceSizePx - 20) + 10f
    }

    private fun generateBackground(w: Int, h: Int): Bitmap {
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)

        // Gradient background
        val gradient = LinearGradient(
            0f, 0f, w.toFloat(), h.toFloat(),
            intArrayOf(
                Color.rgb(Random.nextInt(100, 200), Random.nextInt(100, 200), Random.nextInt(100, 200)),
                Color.rgb(Random.nextInt(50, 150), Random.nextInt(50, 150), Random.nextInt(50, 150)),
                Color.rgb(Random.nextInt(80, 180), Random.nextInt(80, 180), Random.nextInt(80, 180))
            ),
            null, Shader.TileMode.CLAMP
        )
        val bgPaint = Paint().apply { shader = gradient }
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), bgPaint)

        // Add random shapes for visual complexity
        val shapePaint = Paint().apply { isAntiAlias = true }
        repeat(15) {
            shapePaint.color = Color.argb(
                Random.nextInt(30, 80),
                Random.nextInt(256), Random.nextInt(256), Random.nextInt(256)
            )
            val cx = Random.nextFloat() * w
            val cy = Random.nextFloat() * h
            val r = Random.nextFloat() * 80 + 20
            canvas.drawCircle(cx, cy, r, shapePaint)
        }
        repeat(8) {
            shapePaint.color = Color.argb(
                Random.nextInt(20, 60),
                Random.nextInt(256), Random.nextInt(256), Random.nextInt(256)
            )
            val rect = RectF(
                Random.nextFloat() * w, Random.nextFloat() * h,
                Random.nextFloat() * w, Random.nextFloat() * h
            )
            canvas.drawRect(rect, shapePaint)
        }

        return bmp
    }

    private fun createPuzzlePath(x: Float, y: Float, size: Int): Path {
        val path = Path()
        val s = size.toFloat()
        val knob = s * 0.25f

        // Start at top-left
        path.moveTo(x, y)
        // Top edge with knob
        path.lineTo(x + s * 0.35f, y)
        path.arcTo(
            RectF(x + s * 0.35f - knob / 2, y - knob / 2, x + s * 0.35f + knob / 2, y + knob / 2),
            180f, -180f, false
        )
        path.lineTo(x + s, y)
        // Right edge
        path.lineTo(x + s, y + s * 0.35f)
        path.arcTo(
            RectF(x + s - knob / 2, y + s * 0.35f - knob / 2, x + s + knob / 2, y + s * 0.35f + knob / 2),
            270f, -180f, false
        )
        path.lineTo(x + s, y + s)
        // Bottom edge
        path.lineTo(x, y + s)
        // Left edge
        path.lineTo(x, y)
        path.close()

        return path
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        generateCaptcha()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw background image
        backgroundBitmap?.let { bmp ->
            canvas.drawBitmap(bmp, 0f, 0f, null)
        }

        // Draw gap (shadow)
        val gapPath = createPuzzlePath(gapX, gapY, pieceSizePx)
        canvas.drawPath(gapPath, gapPaint)
        canvas.drawPath(gapPath, borderPaint.apply { color = Color.argb(100, 255, 255, 255) })

        // Draw sliding piece at current slider position
        val pieceX = sliderX
        val piecePath = createPuzzlePath(pieceX, gapY, pieceSizePx)

        // Clip and draw piece from background
        canvas.save()
        canvas.clipPath(piecePath)
        backgroundBitmap?.let { bmp ->
            // Draw the piece content (from gap location)
            canvas.drawBitmap(bmp, pieceX - gapX, 0f, piecePaint)
        }
        canvas.restore()

        // Piece border
        canvas.drawPath(piecePath, borderPaint.apply { color = Color.WHITE; strokeWidth = 3f })

        // Draw slider track
        val trackTop = imageAreaHeight + 8 * resources.displayMetrics.density
        val trackRect = RectF(0f, trackTop, width.toFloat(), trackTop + sliderHeight)
        canvas.drawRoundRect(trackRect, 8f, 8f, sliderTrackPaint)

        // Draw hint text
        canvas.drawText("← 拖动滑块完成拼图 →", width / 2f, trackTop + sliderHeight / 2f + 12f, textPaint)

        // Draw slider thumb
        val thumbX = sliderX + sliderThumbSize / 2f
        val thumbY = trackTop + sliderHeight / 2f
        canvas.drawCircle(thumbX, thumbY, sliderThumbSize / 2f, sliderThumbPaint)

        // Draw arrow on thumb
        val arrowPath = Path().apply {
            moveTo(thumbX - 8, thumbY)
            lineTo(thumbX + 4, thumbY - 8)
            lineTo(thumbX + 4, thumbY + 8)
            close()
        }
        canvas.drawPath(arrowPath, arrowPaint)
        val arrowPath2 = Path().apply {
            moveTo(thumbX + 8, thumbY)
            lineTo(thumbX - 4, thumbY - 8)
            lineTo(thumbX - 4, thumbY + 8)
            close()
        }
        canvas.drawPath(arrowPath2, arrowPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val trackTop = imageAreaHeight + 8 * resources.displayMetrics.density

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                // Check if touch is on the slider thumb area
                if (event.y >= trackTop && event.y <= trackTop + sliderHeight) {
                    isDragging = true
                    sliderStartX = event.x - sliderX
                    dragStartTime = System.currentTimeMillis()
                    trajectory.clear()
                    recordPoint(event)
                    return true
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (isDragging) {
                    sliderX = (event.x - sliderStartX).coerceIn(0f, width.toFloat() - pieceSizePx)
                    recordPoint(event)
                    invalidate()
                    return true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isDragging) {
                    isDragging = false
                    recordPoint(event)
                    checkResult()
                    return true
                }
            }
        }
        return super.onTouchEvent(event)
    }

    private fun recordPoint(event: MotionEvent) {
        trajectory.add(
            TrajectoryPoint(
                x = event.x,
                y = event.y,
                timestamp = System.currentTimeMillis(),
                pressure = event.pressure,
                size = event.size
            )
        )
    }

    private fun checkResult() {
        val endTime = System.currentTimeMillis()
        val positionCorrect = abs(sliderX - gapX) <= tolerance

        val trajectoryData = TrajectoryData(
            points = trajectory.toList(),
            startTime = dragStartTime,
            endTime = endTime,
            success = positionCorrect
        )

        // Anti-bot analysis
        val botDetected = if (antiBot) detectBot(trajectoryData) else false

        val finalSuccess = positionCorrect && !botDetected
        onCaptchaResult?.invoke(finalSuccess, trajectoryData.copy(success = finalSuccess))

        if (!finalSuccess) {
            // Reset on failure
            postDelayed({ reset() }, 500)
        }
    }

    /**
     * Simple bot detection heuristics:
     * - Too few trajectory points (machine swipe)
     * - Perfectly straight line (no Y deviation)
     * - Constant speed (no acceleration/deceleration)
     * - Too fast completion
     */
    private fun detectBot(data: TrajectoryData): Boolean {
        if (data.points.size < 5) return true
        if (data.duration < 100) return true // too fast

        // Check Y deviation - humans always have some
        if (data.maxYDeviation < 0.5f) return true

        // Check speed variance - humans have variable speed
        val speeds = data.points.zipWithNext().map { (a, b) ->
            val dt = (b.timestamp - a.timestamp).coerceAtLeast(1)
            abs(b.x - a.x) / dt.toFloat()
        }
        if (speeds.size > 3) {
            val avgSpeed = speeds.average()
            val speedVariance = speeds.map { (it - avgSpeed) * (it - avgSpeed) }.average()
            // Very low variance = constant speed = bot
            if (speedVariance < 0.001 && avgSpeed > 0) return true
        }

        return false
    }
}
