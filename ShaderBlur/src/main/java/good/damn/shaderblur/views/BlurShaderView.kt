package good.damn.shaderblur.views

import android.content.Context
import android.opengl.GLSurfaceView
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import good.damn.shaderblur.opengl.Renderer
import kotlinx.coroutines.Runnable

class BlurShaderView(context: Context): GLSurfaceView(context) {

    private val TAG = "BlurShaderView";
    private var mBlurRenderer: good.damn.shaderblur.opengl.Renderer? = null;

    private lateinit var mSourceView: View

    private val mHandlerDelay = Handler(Looper.getMainLooper())

    private val mRunRequestRender = Runnable {
        mSourceView.viewTreeObserver.addOnDrawListener {
            requestRender()
        }
    }

    fun setSourceView(sourceView: View) {
        mSourceView = sourceView;
        mBlurRenderer = Renderer(mSourceView)
        setEGLContextClientVersion(2)
        setRenderer(mBlurRenderer)
        renderMode = RENDERMODE_WHEN_DIRTY

        post {
            Log.d(TAG, "onCreate: surfaceBlurView: onGlobalLayoutListener");
            mHandlerDelay.postDelayed(mRunRequestRender,2);
        }
    }

    fun clean() {
        mBlurRenderer?.clean()
    }
}