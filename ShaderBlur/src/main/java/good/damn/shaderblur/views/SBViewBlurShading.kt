package good.damn.shaderblur.views

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.opengl.GLSurfaceView
import android.view.View
import android.view.ViewTreeObserver
import good.damn.shaderblur.builders.SBBlur
import good.damn.shaderblur.renderer.forward.SBRendererBlur

class SBViewBlurShading(
    context: Context,
    private val sourceView: View,
    blurRadius: Int,
    blur: SBBlur,
    shadeColor: FloatArray? = null
): GLSurfaceView(
    context
), ViewTreeObserver.OnDrawListener {

    private val mBlurRenderer = SBRendererBlur(
        blurRadius,
        blur,
        shadeColor
    )

    private var mIsLaidOut = false

    private val mCanvas = Canvas()
    private var mInputBitmap: Bitmap? = null


    init {
        setEGLContextClientVersion(3)
        setRenderer(
            mBlurRenderer
        )

        renderMode = RENDERMODE_WHEN_DIRTY
    }


    override fun onLayout(
        changed: Boolean,
        left: Int, top: Int,
        right: Int, bottom: Int
    ) {
        super.onLayout(
            changed,
            left, top,
            right, bottom
        )

        recycleBitmap()

        mInputBitmap = Bitmap.createBitmap(
            width,
            height,
            Bitmap.Config.ARGB_8888
        ).apply {
            mBlurRenderer.requestRender(
                this
            )
        }

        mIsLaidOut = true
    }

    override fun onDraw() {
        if (!mBlurRenderer.isFrameDrawn || !mIsLaidOut) {
            return
        }
        val inputBitmap = mInputBitmap
            ?: return

        mCanvas.setBitmap(
            inputBitmap
        )
        mCanvas.translate(
            -sourceView.scrollX.toFloat(),
            -sourceView.scrollY.toFloat()
        )
        sourceView.draw(
            mCanvas
        )
        mBlurRenderer.requestRender(
            inputBitmap
        )
        mBlurRenderer.isFrameDrawn = false
        requestRender()
    }

    fun startRenderLoop() {
        sourceView.viewTreeObserver.removeOnDrawListener(
            this
        )
        sourceView.viewTreeObserver.addOnDrawListener(
            this
        )
    }

    fun stopRenderLoop() {
        sourceView.viewTreeObserver.removeOnDrawListener(
            this
        )
    }

    fun clean() {
        recycleBitmap()
        mBlurRenderer.clean()
    }

    private inline fun recycleBitmap() {
        mInputBitmap?.apply {
            if (!isRecycled) {
                recycle()
            }
        }
    }
}