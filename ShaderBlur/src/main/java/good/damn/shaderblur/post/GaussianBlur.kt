package good.damn.shaderblur.post

import android.graphics.Bitmap
import android.graphics.Canvas
import android.opengl.GLES20.*
import android.opengl.GLUtils
import android.util.Log
import android.view.View
import good.damn.shaderblur.opengl.OpenGLUtils
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer

class GaussianBlur {

    private val TAG = "GaussianBlur"

    var texture: IntArray = intArrayOf(1)
    var targetView: View? = null

    private lateinit var mVertexBuffer: FloatBuffer
    private lateinit var mIndicesBuffer: ShortBuffer

    private var mWidth = 0f
    private var mHeight = 0f

    private var mVBlurProgram = 0
    private var mHBlurProgram = 0

    private val mCanvas = Canvas()

    private var mInputBitmap: Bitmap? = null

    private var mBlurHDepthBuffer: IntArray? = null
    //private var mBlurHTexture: IntArray? = null
    private var mBlurHFrameBuffer: IntArray? = null

    private var mBlurVDepthBuffer: IntArray? = null
    private var mBlurVFrameBuffer: IntArray? = null

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


    fun create(
        vertexBuffer: FloatBuffer,
        indicesBuffer: ShortBuffer,
        mVertexShaderCode: String
    ) {
        mVertexBuffer = vertexBuffer
        mIndicesBuffer = indicesBuffer

        mVBlurProgram = glCreateProgram()
        glAttachShader(
            mVBlurProgram,
            OpenGLUtils.loadShader(GL_VERTEX_SHADER, mVertexShaderCode)
        )
        glAttachShader(
            mVBlurProgram,
            OpenGLUtils.loadShader(GL_FRAGMENT_SHADER, mVBlurShaderCode)
        )
        glLinkProgram(mVBlurProgram)

        mHBlurProgram = glCreateProgram()
        glAttachShader(
            mHBlurProgram,
            OpenGLUtils.loadShader(GL_VERTEX_SHADER, mVertexShaderCode)
        )
        glAttachShader(
            mHBlurProgram,
            OpenGLUtils.loadShader(GL_FRAGMENT_SHADER, mHBlurShaderCode)
        )
        glLinkProgram(mHBlurProgram)
    }

    fun layout(
        width: Int,
        height: Int
    ) {
        mWidth = width * 0.5f
        mHeight = height * 0.5f

        mInputBitmap = Bitmap.createBitmap(
            width,
            height,
            Bitmap.Config.ARGB_8888)

        mBlurHFrameBuffer = intArrayOf(1)
        mBlurHDepthBuffer = intArrayOf(1)

        mBlurVFrameBuffer = intArrayOf(1)
        mBlurVDepthBuffer = intArrayOf(1)

        glGenFramebuffers(1, mBlurHFrameBuffer, 0)
        glGenRenderbuffers(1, mBlurHDepthBuffer, 0)

        glGenFramebuffers(1, mBlurVFrameBuffer, 0)
        glGenRenderbuffers(1, mBlurVDepthBuffer, 0)

        glGenTextures(1, texture, 0)
        configTexture(texture[0])

        val w = mWidth.toInt()
        val h = mHeight.toInt()

        configBuffers(w,h,mBlurHDepthBuffer!![0])
        //configBuffers(w,h,mBlurVDepthBuffer!![0])
    }

    fun clean() {
        if (mInputBitmap != null && !mInputBitmap!!.isRecycled) {
            mInputBitmap!!.recycle()
        }

        if (mBlurHFrameBuffer != null) {
            glDeleteFramebuffers(1, mBlurHFrameBuffer!!, 0)
        }

        /*if (mBlurHTexture != null) {
            glDeleteTextures(1, mBlurHTexture!!, 0)
        }*/

        if (mBlurHDepthBuffer != null) {
            glDeleteRenderbuffers(1, mBlurHDepthBuffer!!, 0)
        }
    }

    fun draw() {
        drawBitmap()
        horizontal()
        //vertical()
    }

    private fun horizontal() {
        glViewport(0, 0, mWidth.toInt(), mHeight.toInt())

        glBindFramebuffer(GL_FRAMEBUFFER, mBlurHFrameBuffer!![0])

        glFramebufferTexture2D(
            GL_FRAMEBUFFER,
            GL_COLOR_ATTACHMENT0,
            GL_TEXTURE_2D,
            texture[0],
            0)

        glFramebufferRenderbuffer(
            GL_FRAMEBUFFER,
            GL_DEPTH_ATTACHMENT,
            GL_RENDERBUFFER,
            mBlurHDepthBuffer!![0])

        if (glCheckFramebufferStatus(GL_FRAMEBUFFER)
            != GL_FRAMEBUFFER_COMPLETE) {
            Log.d(TAG, "onDrawFrame: FRAME_BUFFER_NOT_COMPLETE")
            return
        }

        glUseProgram(mHBlurProgram)
        glClearColor(0f, 1f, 0f, 1f)
        glClear(GL_DEPTH_BUFFER_BIT or GL_COLOR_BUFFER_BIT)

        val positionHandle = glGetAttribLocation(mHBlurProgram, "position")
        glEnableVertexAttribArray(positionHandle)
        glVertexAttribPointer(
            positionHandle,
            2,
            GL_FLOAT,
            false,
            8,
            mVertexBuffer
        )

        GLUtils.texImage2D(GL_TEXTURE_2D, 0, mInputBitmap, 0)
        glGenerateMipmap(GL_TEXTURE_2D)

        glActiveTexture(GL_TEXTURE0)
        glBindTexture(GL_TEXTURE_2D, texture[0])
        glUniform1i(glGetUniformLocation(mHBlurProgram, "u_tex"), 0)

        glUniform2f(
            glGetUniformLocation(mHBlurProgram, "u_res"),
            mWidth,
            mHeight
        )

        glUniform2f(
            glGetUniformLocation(mHBlurProgram, "tex_res"),
            mWidth,
            mHeight
        )

        glDrawElements(
            GL_TRIANGLES,
            mIndicesBuffer.capacity(), // rect
            GL_UNSIGNED_SHORT,
            mIndicesBuffer
        )
        glDisableVertexAttribArray(positionHandle)
    }

    /*private fun vertical() {
        glBindFramebuffer(
            GL_FRAMEBUFFER,
            mBlurVFrameBuffer!![0]
        )

        glFramebufferTexture2D(
            GL_FRAMEBUFFER,
            GL_COLOR_ATTACHMENT0,
            GL_TEXTURE_2D,
            texture[0],
            0)

        glFramebufferRenderbuffer(
            GL_FRAMEBUFFER,
            GL_DEPTH_ATTACHMENT,
            GL_RENDERBUFFER,
            mBlurVDepthBuffer!![0])

        glUseProgram(mVBlurProgram)
        glViewport(0,0,mWidth.toInt(),mHeight.toInt())
        glClearColor(1f, 1f, 1f, 1f)
        glClear(GL_DEPTH_BUFFER_BIT or GL_COLOR_BUFFER_BIT)

        val positionHandle = glGetAttribLocation(mVBlurProgram, "position")
        glEnableVertexAttribArray(positionHandle)
        glVertexAttribPointer(
            positionHandle,
            2,
            GL_FLOAT,
            false,
            8,
            mVertexBuffer
        )

        glCopyTexImage2D(
            GL_TEXTURE_2D,
            0, GL_RGB,0,0,mWidth.toInt(),mHeight.toInt(),
            0)

        glGenerateMipmap(GL_TEXTURE_2D)

        glActiveTexture(GL_TEXTURE0)
        glBindTexture(GL_TEXTURE_2D, texture[0])
        glUniform1i(glGetUniformLocation(mVBlurProgram, "u_tex"), 0)

        glUniform2f(
            glGetUniformLocation(mVBlurProgram, "u_res"),
            mWidth,
            mHeight
        )

        glDrawElements(
            GL_TRIANGLES,
            mIndicesBuffer.capacity(), // rect
            GL_UNSIGNED_SHORT,
            mIndicesBuffer
        )
        glDisableVertexAttribArray(positionHandle)
    }*/

    private fun configTexture(texture: Int) {
        glBindTexture(GL_TEXTURE_2D, texture)
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
    }

    private fun configBuffers(
        w:Int,
        h:Int,
        depthBuffer: Int
    ) {
        val buf = IntArray(w * h)
        val mTexBuffer = ByteBuffer.allocateDirect(buf.size * Float.SIZE_BYTES)
            .order(ByteOrder.nativeOrder()).asIntBuffer()

        glTexImage2D(
            GL_TEXTURE_2D,
            0, GL_RGB, w, h, 0,
            GL_RGB, GL_UNSIGNED_SHORT_5_6_5, mTexBuffer
        )

        glBindRenderbuffer(GL_RENDERBUFFER, depthBuffer)
        glRenderbufferStorage(
            GL_RENDERBUFFER,
            GL_DEPTH_COMPONENT16,
            w,
            h
        )
    }

    private fun drawBitmap() {
        if (targetView == null) {
            return
        }
        mCanvas.setBitmap(mInputBitmap)
        mCanvas.translate(0f, -targetView!!.scrollY.toFloat())
        targetView!!.draw(mCanvas)
    }
}