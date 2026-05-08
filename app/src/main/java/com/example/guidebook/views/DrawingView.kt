package com.example.guidebook.views

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class DrawingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val paths = mutableListOf<Pair<Path, Paint>>()
    private lateinit var currentPath: Path
    private lateinit var currentPaint: Paint

    private var strokeColor = Color.BLACK
    private var strokeWidth = 4f
    private var isEraser = false

    private var canvasBitmap: Bitmap? = null
    private var drawCanvas: Canvas? = null

    init {
        setupPaint()
    }

    private fun setupPaint() {
        currentPath = Path()
        currentPaint = buildPaint(strokeColor, strokeWidth)
    }

    private fun buildPaint(color: Int, width: Float): Paint = Paint().apply {
        isAntiAlias = true
        this.color = color
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
        this.strokeWidth = width
        style = Paint.Style.STROKE
    }

    override fun onSizeChanged(w: Int, h: Int, oldW: Int, oldH: Int) {
        super.onSizeChanged(w, h, oldW, oldH)
        canvasBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        drawCanvas = Canvas(canvasBitmap!!)
    }

    override fun onDraw(canvas: Canvas) {
        canvasBitmap?.let { canvas.drawBitmap(it, 0f, 0f, null) }
        canvas.drawPath(currentPath, currentPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // Support stylus pressure
        val pressure = if (event.device?.sources?.and(android.view.InputDevice.SOURCE_STYLUS) != 0)
            event.pressure.coerceIn(0.1f, 1f)
        else 1f

        val x = event.x
        val y = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                currentPath = Path()
                currentPaint = if (isEraser) {
                    buildPaint(Color.WHITE, strokeWidth * 4)
                } else {
                    buildPaint(strokeColor, strokeWidth * pressure * 3)
                }
                currentPath.moveTo(x, y)
            }
            MotionEvent.ACTION_MOVE -> {
                currentPath.lineTo(x, y)
                invalidate()
            }
            MotionEvent.ACTION_UP -> {
                drawCanvas?.drawPath(currentPath, currentPaint)
                paths.add(Pair(currentPath, currentPaint))
                currentPath = Path()
                invalidate()
            }
        }
        return true
    }

    fun setColor(color: Int) {
        strokeColor = color
        isEraser = false
    }

    fun setStrokeWidth(width: Float) {
        strokeWidth = width
    }

    fun setEraserMode(enabled: Boolean) {
        isEraser = enabled
    }

    fun undo() {
        if (paths.isNotEmpty()) {
            paths.removeAt(paths.size - 1)
            redrawAll()
        }
    }

    fun clear() {
        paths.clear()
        canvasBitmap?.eraseColor(Color.TRANSPARENT)
        invalidate()
    }

    private fun redrawAll() {
        canvasBitmap?.eraseColor(Color.TRANSPARENT)
        paths.forEach { (path, paint) -> drawCanvas?.drawPath(path, paint) }
        invalidate()
    }

    fun getBitmap(): Bitmap {
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(Color.WHITE)
        canvasBitmap?.let { c.drawBitmap(it, 0f, 0f, null) }
        return bmp
    }

    fun loadBitmap(bitmap: Bitmap) {
        drawCanvas?.drawBitmap(bitmap, 0f, 0f, null)
        invalidate()
    }
}
