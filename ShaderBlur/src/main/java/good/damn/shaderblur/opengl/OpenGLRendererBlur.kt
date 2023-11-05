package good.damn.shaderblur.opengl

import android.graphics.Bitmap
import android.graphics.Canvas
import android.opengl.GLES20
import android.opengl.GLES20.*
import android.opengl.GLSurfaceView
import android.opengl.GLUtils
import android.util.Log
import android.view.View
import good.damn.shaderblur.opengl.OpenGLUtils.Companion.loadShader
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class OpenGLRendererBlur(targetView: View) : GLSurfaceView.Renderer {

    private val TAG = "OpenGLRendererBlur"

    var mOnFrameCompleteListener: Runnable? = null

    private val mCOORDINATES_PER_VERTEX = 3 // number of coords
    private val mVertexOffset = mCOORDINATES_PER_VERTEX * 4 // (x,y,z) vertex per 4 bytes

    private var mInputBitmap: Bitmap? = null

    private lateinit var mVertexBuffer: FloatBuffer
    private lateinit var mDrawListBuffer: ShortBuffer

    private var mBlurDepthBuffer: IntArray? = null
    private var mBlurTexture: IntArray? = null
    private var mBlurFrameBuffer: IntArray? = null

    private var mTargetView: View;

    private val mCanvas = Canvas()

    private var mClipWidth = 1
    private var mClipHeight = 1

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
                "uniform vec2 tex_res;" +
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
        glAttachShader(mHBlurProgram, loadShader(GL_VERTEX_SHADER, mVertexShaderCode))
        glAttachShader(mHBlurProgram, loadShader(GL_FRAGMENT_SHADER, mHBlurShaderCode))
        glLinkProgram(mHBlurProgram)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        Log.d(TAG, "onSurfaceChanged: $width $height")
        gl?.glViewport(0, 0, width, height)
        mWidth = width
        mHeight = height

        mClipWidth = (mWidth * 0.5f).toInt()
        mClipHeight = (mHeight * 0.5f).toInt()

        mInputBitmap = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888)

        mBlurFrameBuffer = intArrayOf(1)
        mBlurTexture = intArrayOf(1)
        mBlurDepthBuffer = intArrayOf(1)

        glGenFramebuffers(1, mBlurFrameBuffer, 0)
        glGenRenderbuffers(1, mBlurDepthBuffer, 0)
        glGenTextures(1, mBlurTexture, 0)

        glBindTexture(GL_TEXTURE_2D, mBlurTexture!![0])
        glTexParameteri(
            GL_TEXTURE_2D,
            GL_TEXTURE_WRAP_S,
            GL_CLAMP_TO_EDGE
        )
        glTexParameteri(
            GL_TEXTURE_2D,
            GL_TEXTURE_WRAP_T,
            GL_CLAMP_TO_EDGE
        )
        glTexParameteri(GL_TEXTURE_2D,
            GL_TEXTURE_MAG_FILTER,
            GL_LINEAR)
        glTexParameteri(GL_TEXTURE_2D,
            GL_TEXTURE_MIN_FILTER,
            GL_LINEAR)

        val buf = IntArray(mClipWidth * mClipHeight)
        val mTexBuffer = ByteBuffer.allocateDirect(buf.size * Float.SIZE_BYTES)
            .order(ByteOrder.nativeOrder()).asIntBuffer()

        glTexImage2D(
            GL_TEXTURE_2D,
            0, GL_RGB, mClipWidth, mClipHeight, 0,
            GL_RGB, GL_UNSIGNED_SHORT_5_6_5, mTexBuffer
        )

        glBindRenderbuffer(GL_RENDERBUFFER, mBlurDepthBuffer!![0])
        glRenderbufferStorage(
            GL_RENDERBUFFER,
            GL_DEPTH_COMPONENT16,
            mClipWidth,
            mClipHeight
        )

    }

    override fun onDrawFrame(gl: GL10?) {
        gl?.glClear(GL_COLOR_BUFFER_BIT)
        drawBitmap()
        renderHorizontalBlur()
        renderPostProcess()
        mOnFrameCompleteListener?.run()
    }

    private fun drawBitmap() {
        mCanvas.setBitmap(mInputBitmap)
        mCanvas.translate(0f, -mTargetView.scrollY.toFloat())
        mTargetView.draw(mCanvas)
    }

    private fun renderHorizontalBlur() {
        glViewport(0, 0, mClipWidth, mClipHeight)

        glBindFramebuffer(GL_FRAMEBUFFER, mBlurFrameBuffer!![0])

        glFramebufferTexture2D(
            GL_FRAMEBUFFER,
            GL_COLOR_ATTACHMENT0,
            GL_TEXTURE_2D,
            mBlurTexture!![0],
            0)

        glFramebufferRenderbuffer(
            GL_FRAMEBUFFER,
            GL_DEPTH_ATTACHMENT,
            GL_RENDERBUFFER,
            mBlurDepthBuffer!![0])

        if (glCheckFramebufferStatus(GL_FRAMEBUFFER)
            != GL_FRAMEBUFFER_COMPLETE) {
            Log.d(TAG, "onDrawFrame: FRAME_BUFFER_NOT_COMPLETE")
            return
        }

        glUseProgram(mHBlurProgram)
        glClearColor(1f, 0f, 0f, 1f)
        glClear(GL_DEPTH_BUFFER_BIT or GL_COLOR_BUFFER_BIT)

        val positionHandle = glGetAttribLocation(mHBlurProgram, "position")
        glEnableVertexAttribArray(positionHandle)
        glVertexAttribPointer(
            positionHandle,
            mCOORDINATES_PER_VERTEX,
            GL_FLOAT,
            false,
            mVertexOffset,
            mVertexBuffer
        )

        GLUtils.texImage2D(GL_TEXTURE_2D, 0, mInputBitmap, 0)
        glGenerateMipmap(GL_TEXTURE_2D)

        glActiveTexture(GL_TEXTURE0)
        glBindTexture(GL_TEXTURE_2D, mBlurTexture!![0])
        glUniform1i(glGetUniformLocation(mHBlurProgram, "u_tex"), 0)

        glUniform2f(
            glGetUniformLocation(mHBlurProgram, "u_res"),
            mWidth.toFloat(),
            mHeight.toFloat()
        )

        glUniform2f(
            glGetUniformLocation(mHBlurProgram, "tex_res"),
            mClipWidth.toFloat(),
            mClipHeight.toFloat()
        )

        glDrawElements(
            GL_TRIANGLES,
            mDrawOrder.size,
            GL_UNSIGNED_SHORT,
            mDrawListBuffer
        )
        glDisableVertexAttribArray(positionHandle)
    }

    private fun renderPostProcess() {
        val texture = mBlurTexture!![0]
        glBindFramebuffer(GL_FRAMEBUFFER, 0) // default fbo

        glUseProgram(mVBlurProgram)
        glViewport(0,0,mWidth,mHeight)
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

    fun clean() {
        if (mInputBitmap != null && !mInputBitmap!!.isRecycled) {
            mInputBitmap?.recycle()
        }

        if (mBlurFrameBuffer != null) {
            glDeleteFramebuffers(1, mBlurFrameBuffer!!, 0)
        }

        if (mBlurTexture != null) {
            glDeleteTextures(1, mBlurTexture!!, 0)
        }

        if (mBlurDepthBuffer != null) {
            glDeleteRenderbuffers(1, mBlurDepthBuffer!!, 0)
        }
    }
}