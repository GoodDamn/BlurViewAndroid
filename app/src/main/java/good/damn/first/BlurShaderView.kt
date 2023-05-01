package good.damn.first

import android.content.Context
import android.opengl.GLSurfaceView
import android.os.Handler
import android.util.Log
import android.view.View
import java.io.File
import java.io.FileOutputStream

class BlurShaderView(context: Context): GLSurfaceView(context) {

    private val TAG = "BlurShaderView";
    private val openGLRendererBlur = OpenGLRendererBlur();

    private lateinit var mSourceView: View;

    private val mHandlerDelay: Handler = Handler();
    private val mRunnableDelay: Runnable = Runnable {
        mSourceView.viewTreeObserver.addOnDrawListener {
            requestRender();
        }
    }

    var scaleFactor: Float = 0.25f
        set(value) {
            field = value;
            openGLRendererBlur.mScaleFactor = scaleFactor;
        }

    init {
        setEGLContextClientVersion(2);
        setRenderer(openGLRendererBlur);
        renderMode = RENDERMODE_WHEN_DIRTY;
    }


    fun setSourceView(sourceView: View) {
        mSourceView = sourceView;
        post {
            Log.d(TAG, "onCreate: surfaceBlurView: onGlobalLayoutListener");
            mHandlerDelay.postDelayed(mRunnableDelay,2);
        }
    }

    override fun requestRender() {
        openGLRendererBlur.generateBitmap(mSourceView);
        super.requestRender();
    }
}