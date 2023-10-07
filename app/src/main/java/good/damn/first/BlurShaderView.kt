package good.damn.first

import android.content.Context
import android.opengl.GLSurfaceView
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import kotlinx.coroutines.Runnable
import java.io.File
import java.io.FileOutputStream

class BlurShaderView(context: Context): GLSurfaceView(context) {

    private val TAG = "BlurShaderView";
    private var mBlurRenderer: OpenGLRendererBlur? = null;

    private lateinit var mSourceView: View

    private val mHandlerDelay = Handler(Looper.getMainLooper())

    private val mRunRequestRender = Runnable {
        mSourceView.viewTreeObserver.addOnDrawListener {
            requestRender()
        }
        /*mBlurRenderer.mOnFrameCompleteListener = Runnable {
            requestRender()
        }*/
    }

    fun setSourceView(sourceView: View) {
        mSourceView = sourceView;
        mBlurRenderer = OpenGLRendererBlur(mSourceView)
        setEGLContextClientVersion(2)
        setRenderer(mBlurRenderer)
        renderMode = RENDERMODE_WHEN_DIRTY

        post {
            Log.d(TAG, "onCreate: surfaceBlurView: onGlobalLayoutListener");
            mHandlerDelay.postDelayed(mRunRequestRender,2);
        }
    }
}