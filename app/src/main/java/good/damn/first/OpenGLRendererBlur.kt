package good.damn.first

import android.graphics.Bitmap
import android.graphics.Canvas
import android.opengl.GLES20.*
import android.opengl.GLSurfaceView
import android.opengl.GLUtils
import android.util.Log
import android.view.View
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

    private var mVBlurProgram = 0
    private var mHBlurProgram = 0

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

    private val mVBlurShaderCode =
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

        mHBlurProgram = glCreateProgram()
        glAttachShader(mHBlurProgram, loadShader(GL_VERTEX_SHADER, mVertexShaderCode))
        glAttachShader(mHBlurProgram, loadShader(GL_FRAGMENT_SHADER, mHBlurShaderCode))
        glLinkProgram(mHBlurProgram)

        mVBlurProgram = glCreateProgram()
        glAttachShader(mVBlurProgram, loadShader(GL_VERTEX_SHADER, mVertexShaderCode))
        glAttachShader(mVBlurProgram, loadShader(GL_FRAGMENT_SHADER, mVBlurShaderCode))
        glLinkProgram(mVBlurProgram)

        glUseProgram(mHBlurProgram)
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

        glGenFramebuffers(1, mBlurFrameBuffer, 0)
        glGenRenderbuffers(1, mBlurDepthBuffer, 0)
        glGenTextures(1, mBlurTexture, 0)

        glBindTexture(GL_TEXTURE_2D, mBlurTexture[0])
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR)


        val buf = IntArray(mWidth * mHeight)
        mTexBuffer = ByteBuffer.allocateDirect(buf.size * Float.SIZE_BYTES)
            .order(ByteOrder.nativeOrder()).asIntBuffer()

        glTexImage2D(
            GL_TEXTURE_2D,
            0, GL_RGB, mWidth, mHeight, 0,
            GL_RGB, GL_UNSIGNED_SHORT_5_6_5, mTexBuffer
        )

        glBindRenderbuffer(GL_RENDERBUFFER, mBlurDepthBuffer[0])
        glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT, mWidth, mHeight)
    }

    override fun onDrawFrame(gl: GL10?) {
        gl?.glClear(GL_COLOR_BUFFER_BIT)
        if (setFBO()) {
            return
        }
        GLUtils.texImage2D(GL_TEXTURE_2D, 0, mInputBitmap, 0)
        glGenerateMipmap(GL_TEXTURE_2D)

        glUseProgram(mHBlurProgram)
        renderBlur(mHBlurProgram)

        renderPostProcess()
    }

    private fun setFBO(): Boolean {

        glViewport(0, 0, mWidth, mHeight)

        glBindFramebuffer(GL_FRAMEBUFFER, mBlurFrameBuffer[0])

        glFramebufferTexture2D(
            GL_FRAMEBUFFER,
            GL_COLOR_ATTACHMENT0,
            GL_TEXTURE_2D,
            mBlurTexture[0],
            0)

        glFramebufferRenderbuffer(
            GL_FRAMEBUFFER,
            GL_DEPTH_ATTACHMENT,
            GL_RENDERBUFFER,
            mBlurDepthBuffer[0])

        return glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE
    }

    private fun renderBlur(program: Int) {
        glClearColor(1f, 0f, 0f, 1f)
        glClear(GL_DEPTH_BUFFER_BIT or GL_COLOR_BUFFER_BIT)

        val positionHandle = glGetAttribLocation(program, "position")
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
        glUniform1i(glGetUniformLocation(program, "u_tex"), 0)

        glUniform2f(
            glGetUniformLocation(program, "u_res"),
            mWidth.toFloat(),
            mHeight.toFloat()
        )

        glDrawElements(GL_TRIANGLES, mDrawOrder.size, GL_UNSIGNED_SHORT, drawListBuffer)
        glDisableVertexAttribArray(positionHandle)
    }

    private fun renderPostProcess() {
        glBindFramebuffer(GL_FRAMEBUFFER, 0) // default fbo

        glUseProgram(mVBlurProgram)
        //glViewport(0,0,mWidth,mHeight)
        glClearColor(1f, 0f, 0f, 1f)
        glClear(GL_DEPTH_BUFFER_BIT or GL_COLOR_BUFFER_BIT)

        val positionHandle = glGetAttribLocation(mVBlurProgram, "position")
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
            drawListBuffer
        )
        glDisableVertexAttribArray(positionHandle)
    }

    private fun clean() {
        glDeleteFramebuffers(1, mBlurFrameBuffer, 0)
        glDeleteTextures(1, mBlurTexture, 0)
        glDeleteRenderbuffers(1, mBlurDepthBuffer, 0)
    }
}