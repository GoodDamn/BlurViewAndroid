package good.damn.first

import android.graphics.Bitmap
import android.graphics.Canvas
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.GLUtils
import android.util.Log
import android.view.View
import com.google.android.material.resources.CancelableFontCallback
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class OpenGLRendererBlur: GLSurfaceView.Renderer {

    private val TAG = "OpenGLRendererBlur";
    private val COORDINATES_PER_VERTEX = 3; // number of coords
    private val vertexOffset = COORDINATES_PER_VERTEX * 4; // (x,y,z) vertex per 4 bytes

    private lateinit var mInputBitmap: Bitmap;

    private var positionHandle: Int = 0;

    private lateinit var vertexBuffer: FloatBuffer;
    private lateinit var drawListBuffer: ShortBuffer;

    private var mOffsetX: Int = 5;
    private var mOffsetY: Int = 5;

    private var mWidth: Int = 1;
    private var mHeight: Int = 1;

    private var program: Int = 0;

    private val drawOrder: ShortArray = shortArrayOf(0,1,2,0,2,3);

    private val squareCoords: FloatArray = floatArrayOf(
        -1.0f,1.0f,0.0f, // top left
        -1.0f,-1.0f,0.0f, // bottom left
        1.0f,-1.0f,0.0f, // bottom right
        1.0f,1.0f,0.0f // top right
    );

    private val textureLocation = intArrayOf(1);

    private val vertexShaderCode: String =
        "attribute vec4 position;" +
        "void main() {" +
            "gl_Position = position;" +
        "}";

    private val fragmentShaderCode:String =
        "precision mediump float;" +
        "uniform int width;" +
        "uniform int height;" +
        "uniform int offsetX;" +
        "uniform int offsetY;" +
        "uniform sampler2D u_tex;" +
        "int getPixelScaled(float ii, int add, int offset) {" +
            "return (int(ii) / offset + add) * offset;" +
        "}" +
        "void main () {" +
            "vec4 sum = vec4(0);" +
            "vec2 u_res = vec2(width,height);" +
            "vec2 coords = vec2(gl_FragCoord.x, u_res.y - gl_FragCoord.y);" +
            "vec2 normalized = coords / u_res;" +
            "const int kernelRadius = 8;" +
            "for (int k = -kernelRadius; k <= kernelRadius; k++) {" +
                "int x = getPixelScaled(coords.x, k, offsetX);" +
                "int z = getPixelScaled(coords.y, k, offsetY);" +
                "sum += texture2D(u_tex, vec2(x,getPixelScaled(coords.y,0,offsetY)) / u_res);" +
                "sum += texture2D(u_tex, vec2(x,z) / u_res);" +
                "sum += texture2D(u_tex, vec2(x,getPixelScaled(coords.y,k,offsetY)) / u_res);" +
            "}" +
            "gl_FragColor = sum / vec4(kernelRadius*6+3);" +
            "return;" +
        "}";

    var mScaleFactor: Float = 1.0f
        get() = field
        set(value) {
            field = value;
            mOffsetX = (mWidth / (mWidth * value)).toInt();
            mOffsetY = (mHeight / (mHeight * value)).toInt();
        };

    private fun loadShader(type: Int, code:String):Int{
        val shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader,code);
        GLES20.glCompileShader(shader);
        return shader;
    }

    fun generateBitmap(view: View) {
        view.isDrawingCacheEnabled = true;
        mInputBitmap = Bitmap.createBitmap(view.drawingCache,0,0,mWidth,mHeight);
        view.isDrawingCacheEnabled = false;
    }

    override fun onSurfaceCreated(gl: GL10?, p1: EGLConfig?) {
        gl?.glClearColor(1.0f,0.0f,1.0f,1.0f);
        Log.d(TAG, "onSurfaceCreated: ");

        val byteBuffer: ByteBuffer = ByteBuffer.allocateDirect(squareCoords.size*4); // allocate 48 bytes (12 * 4(float))
        byteBuffer.order(ByteOrder.nativeOrder());
        vertexBuffer = byteBuffer.asFloatBuffer();
        vertexBuffer.put(squareCoords);
        vertexBuffer.position(0);

        val drawByteBuffer: ByteBuffer = ByteBuffer.allocateDirect(drawOrder.size*2) // allocate 12 bytes (6*2(short))
        drawByteBuffer.order(ByteOrder.nativeOrder());
        drawListBuffer = drawByteBuffer.asShortBuffer();
        drawListBuffer.put(drawOrder);
        drawListBuffer.position(0);

        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);
        program = GLES20.glCreateProgram();
        GLES20.glAttachShader(program, vertexShader);
        GLES20.glAttachShader(program, fragmentShader);
        GLES20.glLinkProgram(program);
        GLES20.glUseProgram(program);

        // Config texture
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glGenTextures(textureLocation.size, textureLocation,0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureLocation[0]);

    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        Log.d(TAG, "onSurfaceChanged: $width $height");
        gl?.glViewport(0,0,width,height);
        mWidth = width;
        mHeight = height;

        mInputBitmap = Bitmap.createBitmap(1,1, Bitmap.Config.ARGB_8888);
    }

    override fun onDrawFrame(gl: GL10?) {
        // Config rectangular vertices
        gl?.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        positionHandle = GLES20.glGetAttribLocation(program,"position");
        GLES20.glEnableVertexAttribArray(positionHandle);
        GLES20.glVertexAttribPointer(positionHandle, COORDINATES_PER_VERTEX, GLES20.GL_FLOAT, false, vertexOffset, vertexBuffer);

        // Load texture
        GLES20.glUniform1i(GLES20.glGetUniformLocation(program,"u_tex"),0);
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D,0,mInputBitmap,0);
        GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D);

        // Load uniforms
        GLES20.glUniform1i(GLES20.glGetUniformLocation(program, "width"),mWidth);
        GLES20.glUniform1i(GLES20.glGetUniformLocation(program, "height"), mHeight);
        GLES20.glUniform1i(GLES20.glGetUniformLocation(program, "offsetX"), mOffsetX);
        GLES20.glUniform1i(GLES20.glGetUniformLocation(program, "offsetY"), mOffsetY);

        GLES20.glDrawElements(GLES20.GL_TRIANGLES,drawOrder.size, GLES20.GL_UNSIGNED_SHORT, drawListBuffer);
        GLES20.glDisableVertexAttribArray(positionHandle);
        Log.d(TAG, "onDrawFrame: $mWidth $mHeight");
    }

}