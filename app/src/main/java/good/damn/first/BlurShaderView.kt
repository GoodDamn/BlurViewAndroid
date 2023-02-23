package good.damn.first

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.opengl.GLSurfaceView
import android.util.Log
import android.view.ViewGroup
import kotlin.math.log

class BlurShaderView(context: Context): GLSurfaceView(context) {

    private val TAG = "BlurShaderView"
    private var targetViewGroup: ViewGroup? = null;
    private val openGLRendererBlur = OpenGLRendererBlur();

    private var clippedWidth: Int = 1;
    private var clippedHeight: Int = 1;
    var scaleFactor: Float = 1.0f
        set(value){
            field = value;
            if (field < 0.01f)
                field = 1.0f;

            clippedWidth = (width * field).toInt();
            clippedHeight = (height * field).toInt();
            openGLRendererBlur.scaleFactor = field;
        };

    init {
        setEGLContextClientVersion(2);
        setRenderer(openGLRendererBlur);
        renderMode = RENDERMODE_WHEN_DIRTY;
    }

    fun setTargetViewGroup(targetViewGroup: ViewGroup){
        this.targetViewGroup = targetViewGroup;
        post {
            Log.d(TAG, "onCreate: surfaceBlurView: onGlobalLayoutListener");
            scaleFactor = scaleFactor;
            targetViewGroup.viewTreeObserver.addOnDrawListener {
                requestRender();
            }
        }
    }

    override fun requestRender() {
        if (targetViewGroup == null) {
            Log.d(TAG, "requestRender: target view group is null. Request is missed.");
            return;
        };

        Log.d(TAG, "requestRender: $width $height $clippedWidth $clippedHeight")

        targetViewGroup!!.isDrawingCacheEnabled = true;
        val clippedBitmap =
            Bitmap.createBitmap(targetViewGroup!!.drawingCache, 0,0, width,height);
        targetViewGroup!!.isDrawingCacheEnabled = false;

        openGLRendererBlur.setInputBitmap(Bitmap.createScaledBitmap(
            clippedBitmap,
            clippedWidth, clippedHeight, false
        ));
        clippedBitmap.recycle();

        super.requestRender();
    }
}