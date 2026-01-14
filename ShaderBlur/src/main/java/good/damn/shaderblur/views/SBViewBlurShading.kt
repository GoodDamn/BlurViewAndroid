package good.damn.shaderblur.views

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.opengl.GLSurfaceView
import android.view.View
import android.view.ViewTreeObserver
import good.damn.shaderblur.renderer.forward.SBRendererBlur

class SBViewBlurShading(
    context: Context,
    private val mSourceView: View,
    blurRadius: Int,
    scaleFactor: Float,
    shadeColor: FloatArray? = null
): GLSurfaceView(
    context
), ViewTreeObserver.OnDrawListener {

    companion object {
        private const val TAG = "BlurShaderView"
    }

    private val mBlurRenderer = SBRendererBlur(
        blurRadius,
        scaleFactor,
        shadeColor
    )

    private var mIsLaidOut = false

    private val mCanvas = Canvas()
    private lateinit var mInputBitmap: Bitmap


    init {
        setEGLContextClientVersion(2)
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

        mInputBitmap = Bitmap.createBitmap(
            width,
            height,
            Bitmap.Config.ARGB_8888
        )
        mBlurRenderer.requestRender(
            mInputBitmap
        )
        mIsLaidOut = true
    }

    override fun onDraw() {
        if (!mBlurRenderer.isFrameDrawn || !mIsLaidOut) {
            return
        }
        mCanvas.setBitmap(
            mInputBitmap
        )
        mCanvas.translate(
            -mSourceView.scrollX.toFloat(),
            -mSourceView.scrollY.toFloat()
        )
        mSourceView.draw(
            mCanvas
        )
        mBlurRenderer.requestRender(
            mInputBitmap
        )
        mBlurRenderer.isFrameDrawn = false
        requestRender()
    }

    fun startRenderLoop() {
        mSourceView.viewTreeObserver.removeOnDrawListener(
            this
        )
        mSourceView.viewTreeObserver.addOnDrawListener(
            this
        )
    }

    fun stopRenderLoop() {
        mSourceView.viewTreeObserver.removeOnDrawListener(
            this
        )
    }

    fun clean() {
        mBlurRenderer.clean()
    }
}