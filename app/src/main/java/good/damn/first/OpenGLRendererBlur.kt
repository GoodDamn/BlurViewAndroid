package good.damn.first

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.opengl.GLES20
import android.opengl.GLES32
import android.opengl.GLSurfaceView
import android.opengl.GLUtils
import android.os.CancellationSignal
import android.os.Handler
import android.util.Log
import android.view.ViewGroup
import androidx.core.graphics.applyCanvas
import androidx.core.view.drawToBitmap
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL
import javax.microedition.khronos.opengles.GL10
import kotlin.io.path.fileVisitor

class OpenGLRendererBlur: GLSurfaceView.Renderer {

    /*
        // Linear blur

uniform sampler2D u_tex0;
uniform vec2 u_tex0Resolution;

uniform vec2 u_resolution;
uniform vec2 u_mouse;
uniform float u_time;

void main () {
    vec2 st = gl_FragCoord.xy/u_resolution.xy;
    vec4 sum = vec4(0);

    const int k = 4;
    /*
    int kernel[9];
    kernel[0] = k-3;
    kernel[1] = k-2;
    kernel[2] = kernel[0];
    kernel[3] = kernel[1];
    kernel[4] = k;
    kernel[5] = kernel[1];
    kernel[6] = kernel[0];
    kernel[7] = kernel[1];
    kernel[8] = kernel[0];
    */
    int factor = 0;

    const int kernelRadius = 7;
	const int limit = kernelRadius-1;
    const int from = 2 - kernelRadius;
    for (int x = from; x < limit; x++) {
            for (int y = from; y < limit; y++) {
       			int kBlur = 1;//kernel[(x+1)*3+y+1];
                factor += kBlur;
                vec2 pos = (gl_FragCoord.xy + vec2(x,y)) * vec2(kBlur)/u_resolution;
                sum += texture2D(u_tex0,pos);
            }
        }

    gl_FragColor = sum/vec4(factor);
}

         second variant
        vec2 normalized = gl_FragCoord.xy/u_res;" +
            "const int kernelRadius = 6;" +
            "const int limit = kernelRadius-1;" +
            "const int from = 2 - kernelRadius;" +
            "int factor = 0;"+
            "for (int x = from; x < limit; x++) {" +
                "for (int y = from; y < limit; y++) {" +
                    "vec2 pos = (gl_FragCoord.xy + vec2(x,y))/u_res;" +
                    "factor += 1;" +
                    "sum += texture2D(u_tex,vec2(pos.x,1.0-pos.y));"
                "}" +
            "}" +
            "gl_FragColor = sum/vec4(factor);"
    *
    * */

    private val TAG = "OpenGLRendererBlur";
    private val COORDINATES_PER_VERTEX = 3; // number of coords
    private val vertexOffset = COORDINATES_PER_VERTEX * 4; // (x,y,z) vertex per 4 bytes

    private lateinit var inputBitmap: Bitmap;

    private var positionHandle: Int = 0;

    private lateinit var vertexBuffer: FloatBuffer;
    private lateinit var drawListBuffer: ShortBuffer;

    private var program: Int = 0;

    private val drawOrder: ShortArray = shortArrayOf(0,1,2,0,2,3);

    private val squareCoords: FloatArray = floatArrayOf(
        -1.0f,1.0f,0.0f, // top left
        -1.0f,-1.0f,0.0f, // bottom left
        1.0f,-1.0f,0.0f, // bottom right
        1.0f,1.0f,0.0f // top right
    );

    private val vertexShaderCode: String =
        "attribute vec4 position;" +
                "void main(){" +
                "   gl_Position = position;" +
                "}"

    private val fragmentShaderCode:String =
        "precision mediump float;"+
        "uniform int width;" +
        "uniform int height;" +
        "uniform float scaleFactor;"+
        //"uniform float kernel[${kernel.size}];" +
        "uniform sampler2D u_tex;" +
        "void main () {" +
            //"int wid = int(gl_FragCoord.x);" +
            //"int hei = int(gl_FragCoord.y);" +
            //"int kw = int(float(width) * scaleFactor);" +
            //"int kh = int(float(height) * scaleFactor);" +
            //"if (hei-hei/kh*kh == 0 && wid-wid/kw*kw == 0){" +
                "vec4 sum = vec4(0);" +
                "vec2 u_res = vec2(width,height);" +
                "vec2 normalized = gl_FragCoord.xy/u_res;" +
                "const int kernelRadius = 5;" +
                "const int limit = kernelRadius-1;" +
                "const int from = 2 - kernelRadius;" +
                "int factor = 0;" +
                "for (int x = from; x < limit; x++) {" +
                    "for (int y = from; y < limit; y++) {" +
                        "vec2 pos = (gl_FragCoord.xy * scaleFactor + vec2(x,y))/u_res;" +
                        "factor += 1;" +
                        "sum += texture2D(u_tex,vec2(pos.x,1.0-pos.y));" +
                    "}" +
                "}" +
                "gl_FragColor = sum/vec4(factor);"+
            //    "return;" +
            //"}" +
            //"gl_FragColor = texture2D(u_tex, gl_FragCoord.xy * scaleFactor);" +
       "}"

    private val textureLocation = intArrayOf(1);


    private fun loadShader(type: Int, code:String):Int{
        val shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader,code);
        GLES20.glCompileShader(shader);
        return shader;
    }

    fun setInputBitmap(scaledBitmap: Bitmap) {
        inputBitmap = scaledBitmap;
    }

    var scaleFactor: Float = 1.0f;

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
        inputBitmap = Bitmap.createBitmap(1,1, Bitmap.Config.ARGB_8888);
    }

    override fun onDrawFrame(gl: GL10?) {
        gl?.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        positionHandle = GLES20.glGetAttribLocation(program,"position");
        GLES20.glEnableVertexAttribArray(positionHandle);
        GLES20.glVertexAttribPointer(positionHandle, COORDINATES_PER_VERTEX, GLES20.GL_FLOAT, false, vertexOffset, vertexBuffer);

        // Load texture
        GLES20.glUniform1i(GLES20.glGetUniformLocation(program,"u_tex"),0);
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D,0,inputBitmap,0);
        GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D);
        GLES20.glUniform1i(GLES20.glGetUniformLocation(program, "width"),inputBitmap.width);
        GLES20.glUniform1i(GLES20.glGetUniformLocation(program, "height"), inputBitmap.height);
        GLES20.glUniform1f(GLES20.glGetUniformLocation(program, "scaleFactor"), scaleFactor);

        //GLES20.glUniform1fv(GLES20.glGetUniformLocation(program, "kernel"),kernel.size,kernel,0);

        GLES20.glDrawElements(GLES20.GL_TRIANGLES,drawOrder.size, GLES20.GL_UNSIGNED_SHORT, drawListBuffer);
        GLES20.glDisableVertexAttribArray(positionHandle);
        Log.d(TAG, "onDrawFrame: ");

    }

}