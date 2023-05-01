package good.damn.first

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.opengl.GLSurfaceView
import android.util.Log
import android.view.View
import android.view.ViewGroup
import kotlin.math.log

class BlurShaderView(context: Context): GLSurfaceView(context) {

    private val TAG = "BlurShaderView"
    private val openGLRendererBlur = OpenGLRendererBlur();

    private var mSourceView: View? = null;

    var scaleFactor: Float = 0.5f
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
            sourceView.viewTreeObserver.addOnDrawListener {
                requestRender();
            }
        }
    }

    override fun requestRender() {
        if (mSourceView == null) {
            Log.d(TAG, "requestRender: target view group is null. Request is missed.");
            return;
        };

        mSourceView!!.isDrawingCacheEnabled = true;

        openGLRendererBlur.generateBitmap(mSourceView!!.getDrawingCache());

        mSourceView!!.isDrawingCacheEnabled = false;

        super.requestRender();
    }
}