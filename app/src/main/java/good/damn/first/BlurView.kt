package good.damn.first

import android.content.Context
import android.graphics.*
import android.os.Handler
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.Script
import android.renderscript.ScriptIntrinsicBlur
import android.util.Log
import android.view.Display
import android.view.KeyEvent.DispatcherState
import android.view.View
import android.view.ViewGroup
import android.view.ViewParent
import android.view.ViewTreeObserver
import android.view.WindowManager
import kotlin.math.log

class BlurView(context: Context?) :
    View(context) {

    private val TAG = "BlurView";

    private val paint:Paint = Paint();
    private val paintText: Paint = Paint();

    private lateinit var inputView: ViewGroup;
    private var renderScript: RenderScript;
    private var scriptIntrinsicBlur: ScriptIntrinsicBlur;

    private var isPaused:Boolean = false;

    private var offsetX:Float = 0.0f;
    private var offsetY:Float = 0.0f;
    private var pixelsX:Int = 1;
    private var pixelsY:Int = 1;

    var radius = 2.0f
        get() = field
        set(value) {
            field = value
            scriptIntrinsicBlur.setRadius(radius);
        };

    var scaleFactor:Int = 1
        get() = field
        set(value) {
            field = value
            val of: Float = 1.0f+scaleFactor/100;
            offsetX = width * of - width;
            offsetY = height * of - height;

            pixelsX = measuredWidth/scaleFactor;
            pixelsY = measuredHeight/scaleFactor;
            Log.d(TAG, "set ScaleFactor: $measuredWidth $measuredHeight");
        };

    fun start(){
        isPaused = false;
        invalidate();
    }

    fun pause(){
        isPaused = true;
    }

    fun destroy(){
        scriptIntrinsicBlur.destroy();
        renderScript.destroy();
    }

    init {
        val display:Display? = (context?.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay;
        Log.d(TAG, ": Initialize refreshRate: ${display?.refreshRate}");
        paintText.color = Color.GREEN;
        paintText.textSize = 20.0f;
        paintText.style = Paint.Style.FILL;
        renderScript = RenderScript.create(context);
        scriptIntrinsicBlur = ScriptIntrinsicBlur.create(renderScript, Element.RGBA_8888(renderScript));
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        scaleFactor = 2;
        Log.d(TAG, "onMeasure: $measuredWidth $measuredHeight");
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom);
        inputView = (parent as ViewGroup).getChildAt(0) as ViewGroup;
    }

    override fun draw(canvas: Canvas?) {
        super.draw(canvas)

        //if (isPaused)
        //    return;

        val preRender: Long = System.currentTimeMillis();
        inputView.isDrawingCacheEnabled = true;
        val inputBitmap: Bitmap = Bitmap.createBitmap(inputView.drawingCache,0,0,width,height);
        inputView.isDrawingCacheEnabled = false;
        val scaledBitmap: Bitmap = Bitmap.createScaledBitmap(inputBitmap,
            pixelsX, pixelsY, false);
        inputBitmap.recycle();

        val blurredBitmap: Bitmap = Bitmap.createBitmap(scaledBitmap.width, scaledBitmap.height, Bitmap.Config.ARGB_8888);

        val allocationScaledBitmap: Allocation = Allocation.createFromBitmap(renderScript, scaledBitmap);
        scriptIntrinsicBlur.setInput(allocationScaledBitmap);

        val allocationBlur: Allocation = Allocation.createFromBitmap(renderScript, blurredBitmap);
        scriptIntrinsicBlur.forEach(allocationBlur);
        allocationBlur.copyTo(blurredBitmap);

        allocationScaledBitmap.destroy();
        allocationBlur.destroy();

        canvas?.drawBitmap(Bitmap.createScaledBitmap(blurredBitmap,
            (width+offsetX).toInt(),
            (height+offsetY).toInt(),false), -offsetX,-offsetY,paint);

        val elapsedTime: Long = System.currentTimeMillis() - preRender;
        canvas?.drawText(elapsedTime.toString(), 0f, 23f, paintText);
        canvas?.drawText("FPS: " + (1000/elapsedTime).toString(), 0f, 50f,paintText);
        canvas?.drawText("Radius: $radius ScaleFactor: $scaleFactor",0f,75f,paintText);
        Log.d(TAG, "draw: update blur");
        invalidate();
    }

}