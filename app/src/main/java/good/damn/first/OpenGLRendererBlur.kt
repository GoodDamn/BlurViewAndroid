package good.damn.first

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.hardware.display.DisplayManager
import android.opengl.GLES20
import android.opengl.GLES30.*;
import android.opengl.GLSurfaceView
import android.opengl.GLUtils
import android.util.Log
import android.view.Display
import android.view.View
import android.widget.ScrollView
import java.io.File
import java.io.FileOutputStream
import java.nio.Buffer
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.IntBuffer
import java.nio.ShortBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class OpenGLRendererBlur : GLSurfaceView.Renderer {

    private val TAG = "OpenGLRendererBlur";
    private val COORDINATES_PER_VERTEX = 3; // number of coords
    private val vertexOffset = COORDINATES_PER_VERTEX * 4; // (x,y,z) vertex per 4 bytes

    private lateinit var mInputBitmap: Bitmap;

    private var positionHandle: Int = 0;

    private lateinit var vertexBuffer: FloatBuffer;
    private lateinit var drawListBuffer: ShortBuffer;

    private val mCanvas = Canvas();

    private var mOffsetX: Int = 5;
    private var mOffsetY: Int = 5;

    private var mWidth: Int = 1;
    private var mHeight: Int = 1;

    private var mProgram: Int = 0;

    private val mDrawOrder: ShortArray = shortArrayOf(0, 1, 2, 0, 2, 3);

    private val mSquareCoords: FloatArray = floatArrayOf(
        -1.0f, 1.0f, 0.0f, // top left
        -1.0f, -1.0f, 0.0f, // bottom left
        1.0f, -1.0f, 0.0f, // bottom right
        1.0f, 1.0f, 0.0f // top right
    );

    private lateinit var mBlurTexture: IntArray;
    private lateinit var mBlurFrameBuffer: IntArray;
    private lateinit var mBlurDepthBuffer: IntArray;

    private val mTempTexture = intArrayOf(1);

    //private val mTextureLocation = intArrayOf(1);

    private val mVertexShaderCode: String =
        "attribute vec4 position;" +
                "void main() {" +
                "gl_Position = position;" +
                "}";

    private val mFragmentShaderCode: String =
        "precision mediump float;" +
        "uniform vec2 u_res;" +
        "uniform sampler2D u_tex;" +
        "void main () {" +
            "vec2 coords = vec2(gl_FragCoord.x, u_res.y-gl_FragCoord.y);" +
            "gl_FragColor = texture2D(u_tex, gl_FragCoord.xy / u_res);" +
        "}";

    var mScaleFactor: Float = 1.0f
        set(value) {
            field = value;
            mOffsetX = (mWidth / (mWidth * value)).toInt();
            mOffsetY = (mHeight / (mHeight * value)).toInt();
        };

    private fun loadShader(type: Int, code: String): Int {
        val shader = glCreateShader(type);
        glShaderSource(shader, code);
        glCompileShader(shader);
        return shader;
    }

    fun generateBitmap(view: View) {
        mInputBitmap =
            Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888);
        mCanvas.setBitmap(mInputBitmap);
        mCanvas.translate(0f, -view.scrollY.toFloat());
        view.draw(mCanvas);
    }

    override fun onSurfaceCreated(gl: GL10?, p1: EGLConfig?) {
        gl?.glClearColor(1.0f, 0.0f, 0.0f, 1.0f);
        Log.d(TAG, "onSurfaceCreated: ");

        val byteBuffer: ByteBuffer =
            ByteBuffer.allocateDirect(mSquareCoords.size * 4); // allocate 48 bytes (12 * 4(float))
        byteBuffer.order(ByteOrder.nativeOrder());
        vertexBuffer = byteBuffer.asFloatBuffer();
        vertexBuffer.put(mSquareCoords);
        vertexBuffer.position(0);

        val drawByteBuffer: ByteBuffer =
            ByteBuffer.allocateDirect(mDrawOrder.size * 2) // allocate 12 bytes (6*2(short))
        drawByteBuffer.order(ByteOrder.nativeOrder());
        drawListBuffer = drawByteBuffer.asShortBuffer();
        drawListBuffer.put(mDrawOrder);
        drawListBuffer.position(0);

        val vertexShader = loadShader(GL_VERTEX_SHADER, mVertexShaderCode);
        val fragmentShader = loadShader(GL_FRAGMENT_SHADER, mFragmentShaderCode);
        mProgram = glCreateProgram();
        glAttachShader(mProgram, vertexShader);
        glAttachShader(mProgram, fragmentShader);
        glLinkProgram(mProgram);
        glUseProgram(mProgram);

        // Config texture

        // Create frame buffer object
        /*mBlurFrameBuffer = createFrameBuffer();

        // Create texture attachment
        mBlurTexture = createTextureAttachment(mWidth, mHeight);

        // Create depth texture attachment
        createDepthTextureAttachment(mWidth, mHeight);

        // Create depth(render) buffer attachment
        mBlurDepthBuffer = createDepthBufferAttachment(mWidth, mHeight);*/

        glActiveTexture(GL_TEXTURE0);
        glGenTextures(1, mTempTexture,0);
        glBindTexture(GL_TEXTURE_2D, mTempTexture[0]);
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        Log.d(TAG, "onSurfaceChanged: $width $height");
        gl?.glViewport(0, 0, width, height);
        mWidth = width;
        mHeight = height;

        mInputBitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
    }

    override fun onDrawFrame(gl: GL10?) {
        // Config rectangular vertices
        gl?.glClear(GL_COLOR_BUFFER_BIT);

        // Load texture

        glUniform1i(glGetUniformLocation(mProgram, "u_tex"), 0);
        Log.d(TAG, "onDrawFrame: INPUT_BITMAP: ${mInputBitmap.width} ${mInputBitmap.height}")
        GLUtils.texImage2D(GL_TEXTURE_2D, 0, mInputBitmap, 0);
        glGenerateMipmap(GL_TEXTURE_2D);

        positionHandle = glGetAttribLocation(mProgram, "position");
        glEnableVertexAttribArray(positionHandle);
        glVertexAttribPointer(
            positionHandle,
            COORDINATES_PER_VERTEX,
            GL_FLOAT,
            false,
            vertexOffset,
            vertexBuffer
        );

        // Load uniforms
        glUniform2f(glGetUniformLocation(mProgram, "u_res"), mWidth.toFloat(), mHeight.toFloat());

        glDrawElements(GL_TRIANGLES, mDrawOrder.size, GL_UNSIGNED_SHORT, drawListBuffer);
        glDisableVertexAttribArray(positionHandle);
    }

    fun bindBlurFrameBuffer() {
        bindFrameBuffer(mBlurFrameBuffer, mWidth, mHeight);
    }

    fun unbindCurrentFramebuffer() {
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glViewport(0,0,mWidth,mHeight);
    }

    private fun clean() {
        GLES20.glDeleteFramebuffers(1, mBlurFrameBuffer,0);
        GLES20.glDeleteTextures(1, mBlurTexture,0);
        GLES20.glDeleteRenderbuffers(1, mBlurDepthBuffer,0);
    }

    private fun bindFrameBuffer(frameBuffer: IntArray,
                                width: Int,
                                height: Int) {
        //glBindTexture(GL_TEXTURE_2D, 0); // make sure that texture isn't bound
        glBindFramebuffer(GL_FRAMEBUFFER, frameBuffer[0]);
        glViewport(0,0,height,width);
    }

    private fun createFrameBuffer(): IntArray {
        val fbo = intArrayOf(1);
        glGenFramebuffers(1, fbo,0);
        glBindFramebuffer(GL_FRAMEBUFFER, fbo[0]);
        glDrawBuffers(1, fbo, 0);
        return fbo;
    }

    private fun createTextureAttachment(width: Int, height: Int): IntArray {
        val frameBufferTexture = intArrayOf(1);
        glGenTextures(1, frameBufferTexture,0);
        glBindTexture(GL_TEXTURE_2D, frameBufferTexture[0]);
        glTexImage2D(
            GL_TEXTURE_2D, 0, GL_RGB, width, height,
            0, GL_RGB, GL_UNSIGNED_BYTE, null
        );

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        //glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        //glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

        glFramebufferTexture2D(
            GL_FRAMEBUFFER,
            GL_COLOR_ATTACHMENT0,
            GL_TEXTURE_2D, frameBufferTexture[0], 0
        );

        return frameBufferTexture;
    }

    private fun createDepthTextureAttachment(width: Int, height: Int): IntArray {
        val texture = intArrayOf(1);
        glGenTextures(1, texture,0);
        glBindTexture(GL_TEXTURE_2D, texture[0]);
        glTexImage2D(
            GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT32F, width, height,
            0, GL_DEPTH_COMPONENT, GL_FLOAT, null
        );

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

        glFramebufferTexture2D(
            GL_FRAMEBUFFER,
            GL_DEPTH_ATTACHMENT,
            GL_TEXTURE_2D,
            texture[0],
            0
        );
        return texture;
    }

    private fun createDepthBufferAttachment(width: Int, height: Int): IntArray {
        val renderBuffer = intArrayOf(1);
        GLES20.glGenRenderbuffers(1, renderBuffer, 0);
        glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT, width, height);
        glFramebufferRenderbuffer(
            GL_FRAMEBUFFER,
            GL_DEPTH_ATTACHMENT,
            GL_RENDERBUFFER,
            renderBuffer[0]
        );
        return renderBuffer;
    }
}