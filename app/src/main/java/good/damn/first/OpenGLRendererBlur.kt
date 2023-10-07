package good.damn.first

import android.graphics.Bitmap
import android.graphics.Canvas
import android.opengl.GLES20
import android.opengl.GLES20.*
import android.opengl.GLSurfaceView
import android.opengl.GLUtils
import android.util.Log
import android.view.View
import androidx.annotation.NonNull
import good.damn.first.opengl.OpenGLUtils
import good.damn.first.opengl.OpenGLUtils.Companion.loadShader
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.log

class OpenGLRendererBlur(targetView: View) : GLSurfaceView.Renderer {

    private val TAG = "OpenGLRendererBlur"

    var mOnFrameCompleteListener: Runnable? = null

    private val mCOORDINATES_PER_VERTEX = 3 // number of coords
    private val mVertexOffset = mCOORDINATES_PER_VERTEX * 4 // (x,y,z) vertex per 4 bytes

    private lateinit var mInputBitmap: Bitmap

    private lateinit var mVertexBuffer: FloatBuffer
    private lateinit var mDrawListBuffer: ShortBuffer

    private lateinit var mBlurDepthBuffer: IntArray
    private lateinit var mBlurTexture: IntArray
    private lateinit var mBlurFrameBuffer: IntArray

    private lateinit var mTargetView: View;

    private val mCanvas = Canvas()

    private var mWidth = 1
    private var mHeight = 1

    private var mVBlurProgram = 0
    private var mHBlurProgram = 0

    private val mDrawOrder: ShortArray = shortArrayOf(0, 1, 2, 0, 2, 3)

    private val mSquareCoords: FloatArray = floatArrayOf(
        -1.0f, 1.0f, 0.0f, // top left
        -1.0f, -1.0f, 0.0f, // bottom left
        1.0f, -1.0f, 0.0f, // bottom right
        1.0f, 1.0f, 0.0f // top right
    )

    private val mVertexShaderCode =
        "attribute vec4 position;" +
                "void main() {" +
                "gl_Position = position;" +
                "}"

    private val mVBlurShaderCode =
        "precision mediump float;" +
                "uniform vec2 u_res;" +
                "uniform sampler2D u_tex;" +
                "float gauss(float inp, float aa, float stDevSQ) {" +
                "return aa * exp(-(inp*inp)/stDevSQ);" +
                "}" +
                "vec2 downScale(float scale, vec2 inp) {" +
                    "vec2 scRes = u_res * scale;" +
                    "vec2 r = inp / scRes;" +
                    "return vec2(float(int(r.x))*scRes.x, float(int(r.y))*scRes.y);" +
                "}" +
                "void main () {" +
                "float stDev = 8.0;" +
                "float stDevSQ = 2.0 * stDev * stDev;" +
                "float aa = 0.398 / stDev;" +
                "vec2 crs = vec2(gl_FragCoord.x, u_res.y-gl_FragCoord.y);" +
                "const float rad = 7.0;" +
                "vec4 sum = vec4(0.0);" +
                "float normDistSum = 0.0;" +
                "float gt;" +
                "for (float i = -rad; i <= rad;i++) {" +
                    "gt = gauss(i,aa,stDevSQ);" +
                    "normDistSum += gt;" +
                    "sum += texture2D(u_tex, vec2(crs.x,crs.y+i)/u_res) * gt;" +
                "}" +
                "gl_FragColor = sum / vec4(normDistSum);" +
                "}"

    private val mHBlurShaderCode =
        "precision mediump float;" +
                "uniform vec2 u_res;" +
                "uniform sampler2D u_tex;" +
                "float gauss(float inp, float aa, float stDevSQ) {" +
                    "return aa * exp(-(inp*inp)/stDevSQ);" +
                "}" +
                "void main () {" +
                    "float stDev = 8.0;" +
                    "float stDevSQ = 2.0 * stDev * stDev;" +
                    "float aa = 0.398 / stDev;" +
                    "const float rad = 7.0;" +
                    "vec4 sum = vec4(0.0);" +
                    "float normDistSum = 0.0;" +
                    "float gt;" +
                    "for (float i = -rad; i <= rad;i++) {" +
                        "gt = gauss(i,aa,stDevSQ);" +
                        "normDistSum += gt;" +
                        "sum += texture2D(u_tex, vec2(gl_FragCoord.x+i,gl_FragCoord.y)/u_res) * gt;" +
                    "}" +
                    "gl_FragColor = sum / vec4(normDistSum);" +
                "}"

    init {
        mTargetView = targetView
    }

    override fun onSurfaceCreated(gl: GL10?, p1: EGLConfig?) {
        gl?.glClearColor(1.0f, 0.0f, 0.0f, 1.0f)
        Log.d(TAG, "onSurfaceCreated: ")

        val byteBuffer =
            ByteBuffer.allocateDirect(mSquareCoords.size * 4) // allocate 48 bytes (12 * 4(float))
        byteBuffer.order(ByteOrder.nativeOrder())
        mVertexBuffer = byteBuffer.asFloatBuffer()
        mVertexBuffer.put(mSquareCoords)
        mVertexBuffer.position(0)

        val drawByteBuffer: ByteBuffer =
            ByteBuffer.allocateDirect(mDrawOrder.size * 2) // allocate 12 bytes (6*2(short))
        drawByteBuffer.order(ByteOrder.nativeOrder())
        mDrawListBuffer = drawByteBuffer.asShortBuffer()
        mDrawListBuffer.put(mDrawOrder)
        mDrawListBuffer.position(0)

        mVBlurProgram = glCreateProgram()
        glAttachShader(mVBlurProgram, loadShader(GL_VERTEX_SHADER, mVertexShaderCode))
        glAttachShader(mVBlurProgram, loadShader(GL_FRAGMENT_SHADER, mVBlurShaderCode))
        glLinkProgram(mVBlurProgram)

        mHBlurProgram = glCreateProgram()
        GLES20.glAttachShader(mHBlurProgram, OpenGLUtils.loadShader(GLES20.GL_VERTEX_SHADER, mVertexShaderCode))
        GLES20.glAttachShader(mHBlurProgram, OpenGLUtils.loadShader(GLES20.GL_FRAGMENT_SHADER, mHBlurShaderCode))
        GLES20.glLinkProgram(mHBlurProgram)
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

        GLES20.glGenFramebuffers(1, mBlurFrameBuffer, 0)
        GLES20.glGenRenderbuffers(1, mBlurDepthBuffer, 0)
        GLES20.glGenTextures(1, mBlurTexture, 0)

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mBlurTexture[0])
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_WRAP_S,
            GLES20.GL_CLAMP_TO_EDGE
        )
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_WRAP_T,
            GLES20.GL_CLAMP_TO_EDGE
        )
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_MAG_FILTER,
            GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_MIN_FILTER,
            GLES20.GL_LINEAR)

        val buf = IntArray(mWidth * mHeight)
        val mTexBuffer = ByteBuffer.allocateDirect(buf.size * Float.SIZE_BYTES)
            .order(ByteOrder.nativeOrder()).asIntBuffer()

        GLES20.glTexImage2D(
            GLES20.GL_TEXTURE_2D,
            0, GLES20.GL_RGB, mWidth, mHeight, 0,
            GLES20.GL_RGB, GLES20.GL_UNSIGNED_SHORT_5_6_5, mTexBuffer
        )

        GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, mBlurDepthBuffer[0])
        GLES20.glRenderbufferStorage(
            GLES20.GL_RENDERBUFFER,
            GLES20.GL_DEPTH_COMPONENT16,
            mWidth,
            mHeight
        )

    }

    override fun onDrawFrame(gl: GL10?) {
        gl?.glClear(GL_COLOR_BUFFER_BIT)
        generateBitmap()
        renderHorizontalBlur()
        renderPostProcess()
        mOnFrameCompleteListener?.run()
    }

    private fun generateBitmap() {
        mInputBitmap = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888)
        mCanvas.setBitmap(mInputBitmap)
        mCanvas.translate(0f, -mTargetView.scrollY.toFloat())
        mTargetView.draw(mCanvas)
    }

    private fun renderHorizontalBlur() {
        GLES20.glViewport(0, 0, mWidth, mHeight)

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mBlurFrameBuffer[0])

        GLES20.glFramebufferTexture2D(
            GLES20.GL_FRAMEBUFFER,
            GLES20.GL_COLOR_ATTACHMENT0,
            GLES20.GL_TEXTURE_2D,
            mBlurTexture[0],
            0)

        GLES20.glFramebufferRenderbuffer(
            GLES20.GL_FRAMEBUFFER,
            GLES20.GL_DEPTH_ATTACHMENT,
            GLES20.GL_RENDERBUFFER,
            mBlurDepthBuffer[0])

        if (GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER)
            != GLES20.GL_FRAMEBUFFER_COMPLETE) {
            Log.d(TAG, "onDrawFrame: FRAME_BUFFER_NOT_COMPLETE")
            return
        }

        GLES20.glUseProgram(mHBlurProgram)
        GLES20.glClearColor(1f, 0f, 0f, 1f)
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT or GLES20.GL_COLOR_BUFFER_BIT)

        val positionHandle = GLES20.glGetAttribLocation(mHBlurProgram, "position")
        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glVertexAttribPointer(
            positionHandle,
            mCOORDINATES_PER_VERTEX,
            GLES20.GL_FLOAT,
            false,
            mVertexOffset,
            mVertexBuffer
        )

        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, mInputBitmap, 0)
        GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mBlurTexture[0])
        GLES20.glUniform1i(GLES20.glGetUniformLocation(mHBlurProgram, "u_tex"), 0)

        GLES20.glUniform2f(
            GLES20.glGetUniformLocation(mHBlurProgram, "u_res"),
            mWidth.toFloat(),
            mHeight.toFloat()
        )

        GLES20.glDrawElements(
            GLES20.GL_TRIANGLES,
            mDrawOrder.size,
            GLES20.GL_UNSIGNED_SHORT,
            mDrawListBuffer
        )
        GLES20.glDisableVertexAttribArray(positionHandle)
    }

    private fun renderPostProcess() {
        val texture = mBlurTexture[0]
        glBindFramebuffer(GL_FRAMEBUFFER, 0) // default fbo

        glUseProgram(mVBlurProgram)
        glClearColor(1f, 0f, 0f, 1f)
        glClear(GL_DEPTH_BUFFER_BIT or GL_COLOR_BUFFER_BIT)

        val positionHandle = glGetAttribLocation(mVBlurProgram, "position")
        glEnableVertexAttribArray(positionHandle)
        glVertexAttribPointer(
            positionHandle,
            mCOORDINATES_PER_VERTEX,
            GL_FLOAT,
            false,
            mVertexOffset,
            mVertexBuffer
        )

        glActiveTexture(GL_TEXTURE0)
        glBindTexture(GL_TEXTURE_2D, texture)
        glUniform1i(glGetUniformLocation(mVBlurProgram, "u_tex"), 0)

        glUniform2f(
            glGetUniformLocation(mVBlurProgram, "u_res"),
            mWidth.toFloat(),
            mHeight.toFloat()
        )

        glDrawElements(
            GL_TRIANGLES,
            mDrawOrder.size,
            GL_UNSIGNED_SHORT,
            mDrawListBuffer
        )
        glDisableVertexAttribArray(positionHandle)
    }

    private fun clean() {
        glDeleteFramebuffers(1, mBlurFrameBuffer, 0)
        glDeleteTextures(1, mBlurTexture, 0)
        glDeleteRenderbuffers(1, mBlurDepthBuffer, 0)
    }
}