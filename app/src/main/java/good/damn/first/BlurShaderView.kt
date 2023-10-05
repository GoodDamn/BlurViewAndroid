package good.damn.first

import android.content.Context
import android.opengl.GLSurfaceView
import android.os.Handler
import android.util.Log
import android.view.View
import kotlinx.coroutines.Runnable
import java.io.File
import java.io.FileOutputStream

class BlurShaderView(context: Context): GLSurfaceView(context) {

    private val TAG = "BlurShaderView";
    private var mBlurRenderer = OpenGLRendererBlur()

    private lateinit var mSourceView: View

    private val mHandlerDelay = Handler()

    private val mRunRequestRender = Runnable {
        requestRender()
    }

    init {
        setEGLContextClientVersion(2)
        setRenderer(mBlurRenderer)
        renderMode = RENDERMODE_WHEN_DIRTY
        mBlurRenderer.mOnFrameCompleteListener = Runnable {
            mBlurRenderer.generateBitmap(mSourceView)
            mSourceView.post(mRunRequestRender)
        }
    }

    fun setSourceView(sourceView: View) {
        mSourceView = sourceView;
        post {
            Log.d(TAG, "onCreate: surfaceBlurView: onGlobalLayoutListener");
            mHandlerDelay.postDelayed(mRunRequestRender,2);
        }
    }
}