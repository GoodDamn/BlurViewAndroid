package good.damn.shaderblur.views

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.opengl.GLSurfaceView
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import good.damn.shaderblur.opengl.BlurRenderer
import kotlinx.coroutines.Runnable

class BlurShaderView(
    context: Context,
    private val mSourceView: View,
    blurRadius: Int
): GLSurfaceView(
    context
), java.lang.Runnable {

    companion object {
        private const val TAG = "BlurShaderView"
    }

    private val mBlurRenderer = BlurRenderer(
        blurRadius
    )

    private val mCanvas = Canvas()
    private lateinit var mInputBitmap: Bitmap

    private val mHandlerDelay = Handler(
        Looper.getMainLooper()
    )


    init {
        setEGLContextClientVersion(2)
        setRenderer(
            mBlurRenderer
        )

        renderMode = RENDERMODE_WHEN_DIRTY

        post {
            Log.d(TAG, "onCreate: surfaceBlurView: onGlobalLayoutListener");
            mHandlerDelay.postDelayed({
                mSourceView.viewTreeObserver.addOnDrawListener {
                    post(this)
                }
            },2);
        }
    }

    override fun run() {
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
        requestRender()
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        mInputBitmap = Bitmap.createBitmap(
            width,
            height,
            Bitmap.Config.ARGB_8888
        )

        mBlurRenderer.requestRender(
            mInputBitmap
        )
    }

    fun clean() {
        mBlurRenderer.clean()
    }
}