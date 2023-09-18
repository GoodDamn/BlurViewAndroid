package good.damn.first

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.hardware.display.DisplayManager
import android.opengl.GLES20.*
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

    private val TAG = "OpenGLRendererBlur"
    private val COORDINATES_PER_VERTEX = 3 // number of coords
    private val vertexOffset = COORDINATES_PER_VERTEX * 4 // (x,y,z) vertex per 4 bytes

    private lateinit var mInputBitmap: Bitmap

    private lateinit var vertexBuffer: FloatBuffer
    private lateinit var drawListBuffer: ShortBuffer

    private val mCanvas = Canvas()

    private var mOffsetX = 5
    private var mOffsetY = 5

    private var mWidth = 1
    private var mHeight = 1

    private var mPPProgram = 0
    private var mProgram = 0

    private val mDrawOrder: ShortArray = shortArrayOf(0, 1, 2, 0, 2, 3)

    private val mSquareCoords: FloatArray = floatArrayOf(
        -1.0f, 1.0f, 0.0f, // top left
        -1.0f, -1.0f, 0.0f, // bottom left
        1.0f, -1.0f, 0.0f, // bottom right
        1.0f, 1.0f, 0.0f // top right
    )

    private lateinit var mBlurTexture: IntArray
    private lateinit var mBlurFrameBuffer: IntArray
    private lateinit var mBlurDepthBuffer: IntArray

    private lateinit var mTexBuffer: IntBuffer

    private val mTempTexture = intArrayOf(1)

    //private val mTextureLocation = intArrayOf(1);

    private val mVertexShaderCode =
        "attribute vec4 position;" +
                "void main() {" +
                "gl_Position = position;" +
                "}"

    private val mFragmentShaderCode =
        "precision mediump float;" +
        "uniform vec2 u_res;" +
        "uniform sampler2D u_tex;" +
        "void main () {" +
            "gl_FragColor = vec4(1.0)-texture2D(u_tex, gl_FragCoord.xy / u_res);" +
        "}"

    private val mPPShaderCode =
        "precision mediump float;" +
        "uniform vec2 u_res;" +
        "uniform sampler2D u_tex;" +
        "void main () {" +
            "vec2 coords = vec2(gl_FragCoord.x, u_res.y-gl_FragCoord.y);" +
            "gl_FragColor = texture2D(u_tex, coords.xy / u_res);" +
        "}"

    var mScaleFactor = 1.0f
        set(value) {
            field = value
            mOffsetX = (mWidth / (mWidth * value)).toInt()
            mOffsetY = (mHeight / (mHeight * value)).toInt()
        }

    private fun loadShader(type: Int, code: String): Int {
        val shader = glCreateShader(type)
        glShaderSource(shader, code)
        glCompileShader(shader)
        return shader
    }

    fun generateBitmap(view: View) {
        mInputBitmap =
            Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888)
        mCanvas.setBitmap(mInputBitmap)
        mCanvas.translate(0f, -view.scrollY.toFloat())
        view.draw(mCanvas)
    }

    override fun onSurfaceCreated(gl: GL10?, p1: EGLConfig?) {
        gl?.glClearColor(1.0f, 0.0f, 0.0f, 1.0f)
        Log.d(TAG, "onSurfaceCreated: ")

        val byteBuffer =
            ByteBuffer.allocateDirect(mSquareCoords.size * 4) // allocate 48 bytes (12 * 4(float))
        byteBuffer.order(ByteOrder.nativeOrder())
        vertexBuffer = byteBuffer.asFloatBuffer()
        vertexBuffer.put(mSquareCoords)
        vertexBuffer.position(0)

        val drawByteBuffer: ByteBuffer =
            ByteBuffer.allocateDirect(mDrawOrder.size * 2) // allocate 12 bytes (6*2(short))
        drawByteBuffer.order(ByteOrder.nativeOrder())
        drawListBuffer = drawByteBuffer.asShortBuffer()
        drawListBuffer.put(mDrawOrder)
        drawListBuffer.position(0)

        mProgram = glCreateProgram()
        glAttachShader(mProgram, loadShader(GL_VERTEX_SHADER, mVertexShaderCode))
        glAttachShader(mProgram, loadShader(GL_FRAGMENT_SHADER, mFragmentShaderCode))
        glLinkProgram(mProgram)

        mPPProgram = glCreateProgram()
        glAttachShader(mPPProgram, loadShader(GL_VERTEX_SHADER, mVertexShaderCode))
        glAttachShader(mPPProgram, loadShader(GL_FRAGMENT_SHADER, mPPShaderCode))
        glLinkProgram(mPPProgram)

        glUseProgram(mProgram)

        // Config texture
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        Log.d(TAG, "onSurfaceChanged: $width $height")
        gl?.glViewport(0, 0, width, height)
        mWidth = width
        mHeight = height

        mInputBitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)

        mBlurFrameBuffer = intArrayOf(1)
        mBlurTexture = intArrayOf(1)
        mBlurDepthBuffer = intArrayOf(1)

        glGenFramebuffers(1,mBlurFrameBuffer,0)
        glGenRenderbuffers(1,mBlurDepthBuffer,0)
        glGenTextures(1,mBlurTexture, 0)

        glBindTexture(GL_TEXTURE_2D, mBlurTexture[0])
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR)


        val buf = IntArray(mWidth * mHeight)
        mTexBuffer = ByteBuffer.allocateDirect(buf.size * Float.SIZE_BYTES)
            .order(ByteOrder.nativeOrder()).asIntBuffer()

        glTexImage2D(GL_TEXTURE_2D,
            0, GL_RGB, mWidth, mHeight, 0,
            GL_RGB, GL_UNSIGNED_SHORT_5_6_5, mTexBuffer)

        glBindRenderbuffer(GL_RENDERBUFFER, mBlurDepthBuffer[0])
        glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT, mWidth,mHeight)
    }

    override fun onDrawFrame(gl: GL10?) {
        // Config rectangular vertices
        gl?.glClear(GL_COLOR_BUFFER_BIT)

        //glActiveTexture(GL_TEXTURE0)
        //glBindTexture(GL_TEXTURE_2D, mBlurTexture[0])
        renderTexture()

        renderPostProcess()
    }

    private fun renderTexture() {
        //bindBlurFrameBuffer()
        glUseProgram(mProgram)

        glViewport(0,0,mWidth,mHeight)

        glBindFramebuffer(GL_FRAMEBUFFER, mBlurFrameBuffer[0])

        glFramebufferTexture2D(GL_FRAMEBUFFER,
            GL_COLOR_ATTACHMENT0,
            GL_TEXTURE_2D,
            mBlurTexture[0],
            0)

        glFramebufferRenderbuffer(GL_FRAMEBUFFER,
            GL_DEPTH_ATTACHMENT,
            GL_RENDERBUFFER,
            mBlurDepthBuffer[0])

        val status = glCheckFramebufferStatus(GL_FRAMEBUFFER)
        if (status != GL_FRAMEBUFFER_COMPLETE) {
            return
        }

        glClearColor(1f,0f,0f,1f)
        glClear(GL_DEPTH_BUFFER_BIT or GL_COLOR_BUFFER_BIT)

        GLUtils.texImage2D(GL_TEXTURE_2D, 0, mInputBitmap, 0)
        glGenerateMipmap(GL_TEXTURE_2D)

        val positionHandle = glGetAttribLocation(mProgram, "position")
        glEnableVertexAttribArray(positionHandle)
        glVertexAttribPointer(
            positionHandle,
            COORDINATES_PER_VERTEX,
            GL_FLOAT,
            false,
            vertexOffset,
            vertexBuffer
        )

        glActiveTexture(GL_TEXTURE0)
        glBindTexture(GL_TEXTURE_2D, mBlurTexture[0])
        glUniform1i(glGetUniformLocation(mProgram, "u_tex"), 0)

        // Load uniforms
        glUniform2f(
            glGetUniformLocation(mProgram, "u_res"),
            mWidth.toFloat(),
            mHeight.toFloat()
        )

        glDrawElements(GL_TRIANGLES, mDrawOrder.size, GL_UNSIGNED_SHORT, drawListBuffer)
        glDisableVertexAttribArray(positionHandle)
    }

    private fun renderPostProcess() {
        glBindFramebuffer(GL_FRAMEBUFFER, 0) // default fbo

        glUseProgram(mPPProgram)
        glClearColor(1f,0f,0f,1f)
        glClear(GL_DEPTH_BUFFER_BIT or GL_COLOR_BUFFER_BIT)

        val positionHandle = glGetAttribLocation(mPPProgram, "position")
        glEnableVertexAttribArray(positionHandle)
        glVertexAttribPointer(
            positionHandle,
            COORDINATES_PER_VERTEX,
            GL_FLOAT,
            false,
            vertexOffset,
            vertexBuffer)

        glActiveTexture(GL_TEXTURE0)
        glBindTexture(GL_TEXTURE_2D, mBlurTexture[0])
        glUniform1i(glGetUniformLocation(mPPProgram, "u_tex"), 0)

        // Load uniforms
        glUniform2f(
            glGetUniformLocation(mPPProgram, "u_res"),
            mWidth.toFloat(),
            mHeight.toFloat())

        glDrawElements(GL_TRIANGLES,
            mDrawOrder.size,
            GL_UNSIGNED_SHORT,
            drawListBuffer)
        glDisableVertexAttribArray(positionHandle)
    }

    private fun clean() {
        glDeleteFramebuffers(1, mBlurFrameBuffer, 0)
        glDeleteTextures(1, mBlurTexture, 0)
        glDeleteRenderbuffers(1, mBlurDepthBuffer, 0)
    }
}