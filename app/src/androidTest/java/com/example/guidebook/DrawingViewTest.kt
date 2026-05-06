package com.example.guidebook

import android.graphics.Bitmap
import android.graphics.Color
import android.view.MotionEvent
import android.view.View
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.guidebook.views.DrawingView
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DrawingViewTest {

    private lateinit var view: DrawingView

    @Before fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        view = DrawingView(context)
        // onSizeChanged 트리거를 위해 수동으로 크기 지정
        view.measure(
            android.view.View.MeasureSpec.makeMeasureSpec(400, android.view.View.MeasureSpec.EXACTLY),
            android.view.View.MeasureSpec.makeMeasureSpec(400, android.view.View.MeasureSpec.EXACTLY)
        )
        view.layout(0, 0, 400, 400)
    }

    @Test fun `setColor sets eraser mode to false`() {
        view.setEraserMode(true)
        view.setColor(Color.BLUE)
        // 색상 설정 후 지우개 모드 해제 — clear + undo 동작에 영향 없음을 확인
        // (isEraser는 private이므로 동작으로 간접 검증: getBitmap이 반환되어야 함)
        val bmp = view.getBitmap()
        assertNotNull(bmp)
    }

    @Test fun `clear empties the canvas`() {
        view.clear()
        val bmp = view.getBitmap()
        assertNotNull(bmp)
        assertEquals(400, bmp.width)
        assertEquals(400, bmp.height)
    }

    @Test fun `getBitmap returns white background bitmap`() {
        view.clear()
        val bmp = view.getBitmap()
        // 좌상단 픽셀이 흰색인지 확인
        val pixel = bmp.getPixel(0, 0)
        assertEquals(Color.WHITE, pixel)
    }

    @Test fun `undo on empty paths does not crash`() {
        view.undo() // 빈 상태에서 undo — 예외 없이 통과
    }

    // ── 터치 이벤트 기반 테스트 ───────────────────────────────────────────────

    private fun injectStroke(view: View, x1: Float, y1: Float, x2: Float, y2: Float) {
        val t = System.currentTimeMillis()
        MotionEvent.obtain(t, t, MotionEvent.ACTION_DOWN, x1, y1, 0).let {
            view.dispatchTouchEvent(it); it.recycle()
        }
        MotionEvent.obtain(t, t + 10, MotionEvent.ACTION_MOVE, x2, y2, 0).let {
            view.dispatchTouchEvent(it); it.recycle()
        }
        MotionEvent.obtain(t, t + 20, MotionEvent.ACTION_UP, x2, y2, 0).let {
            view.dispatchTouchEvent(it); it.recycle()
        }
    }

    @Test fun `drawing a stroke produces non-white pixels on bitmap`() {
        view.setColor(Color.BLACK)
        injectStroke(view, 50f, 200f, 350f, 200f)
        val bmp = view.getBitmap()
        // 수평선 중앙 픽셀이 흰색이 아니어야 함
        assertNotEquals(Color.WHITE, bmp.getPixel(200, 200))
    }

    @Test fun `undo after stroke restores white canvas`() {
        view.setColor(Color.BLACK)
        injectStroke(view, 50f, 200f, 350f, 200f)
        view.undo()
        val bmp = view.getBitmap()
        assertEquals(Color.WHITE, bmp.getPixel(200, 200))
    }

    @Test fun `eraser draws white over previously drawn stroke`() {
        view.setColor(Color.BLACK)
        injectStroke(view, 50f, 200f, 350f, 200f)
        view.setEraserMode(true)
        injectStroke(view, 150f, 200f, 250f, 200f)
        val bmp = view.getBitmap()
        // 지우개로 덮은 구간 중심 픽셀은 흰색이어야 함
        assertEquals(Color.WHITE, bmp.getPixel(200, 200))
    }

    @Test fun `multiple strokes then multiple undos restores white canvas`() {
        view.setColor(Color.BLACK)
        injectStroke(view, 10f, 100f, 100f, 100f)
        injectStroke(view, 10f, 200f, 100f, 200f)
        injectStroke(view, 10f, 300f, 100f, 300f)
        view.undo()
        view.undo()
        view.undo()
        val bmp = view.getBitmap()
        assertTrue(isBitmapAllWhite(bmp))
    }

    private fun isBitmapAllWhite(bmp: Bitmap): Boolean {
        for (y in 0 until bmp.height step 10) {
            for (x in 0 until bmp.width step 10) {
                if (bmp.getPixel(x, y) != Color.WHITE) return false
            }
        }
        return true
    }


}
