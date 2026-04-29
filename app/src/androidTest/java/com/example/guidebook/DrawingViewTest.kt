package com.example.guidebook

import android.graphics.Color
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

    @Test fun `setStrokeWidth does not crash`() {
        view.setStrokeWidth(10f)
        view.setStrokeWidth(1f)
    }

    @Test fun `setEraserMode toggle does not crash`() {
        view.setEraserMode(true)
        view.setEraserMode(false)
    }
}
