package good.damn.shaderblur.post_effects.blur

import android.graphics.Bitmap
import android.opengl.GLSurfaceView
import good.damn.shaderblur.opengl.OpenGLUtils
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

import android.opengl.GLES20.*
import android.opengl.GLUtils
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class HorizontalBlur(
    private val mVertexCode: String,
    private val mVertexBuffer: FloatBuffer,
    private val mIndicesBuffer: ByteBuffer,
    blurRadius: Int
) {

    companion object {
        private const val TAG = "HorizontalBlur"
    }

    private val mFragmentShaderCode = "precision mediump float;" +
            "uniform vec2 u_res;" +
            "uniform vec2 u_texRes;" +
            "uniform sampler2D u_tex;" +

            "float gauss(float inp, float aa, float stDevSQ) {" +
                "return aa * exp(-(inp*inp)/stDevSQ);" +
            "}" +
            "void main () {" +
                "float stDev = 8.0;" +
                "float stDevSQ = 2.0 * stDev * stDev;" +
                "float aa = 0.398 / stDev;" +
                "const float rad = $blurRadius.0;" +
                "vec2 scaledCoord = vec2(" +
                    "gl_FragCoord.x / u_res.x * u_texRes.x," +
                    "gl_FragCoord.y / u_res.y * u_texRes.y);" +
                "vec4 sum = vec4(0.0);" +
                "float normDistSum = 0.0;" +
                "float gt;" +
                "vec2 offset = vec2(gl_FragCoord.x - rad, gl_FragCoord.y);" +
                "for (float i = -rad; i <= rad;i++) {" +
                    "gt = gauss(i,aa,stDevSQ);" +
                    "normDistSum += gt;" +
                    "offset.x++;" +
                    "sum += texture2D(" +
                        "u_tex," +
                        "offset / u_texRes) * gt;" +
                    "}" +
                "gl_FragColor = sum / vec4(normDistSum);" +
            "}"

    private var mProgram: Int = 0

    private val mDepthBuffer = intArrayOf(1)
    private val mFrameBuffer = intArrayOf(1)

    private var mAttrPosition: Int = 0
    private var mUniformScaledSize: Int = 0
    private var mUniformSize: Int = 0
    private var mUniformTexture: Int = 0

    private var mfWidth = 1f
    private var mfHeight = 1f

    private var mfSWidth = 1f
    private var mfSHeight = 1f

    private var miWidth = 1
    private var miHeight = 1

    private var miSWidth = 1
    private var miSHeight = 1

    fun onSurfaceCreated() {
        mProgram = OpenGLUtils.createProgram(
            mVertexCode,
            mFragmentShaderCode
        )

        glLinkProgram(
            mProgram
        )

        mAttrPosition = glGetAttribLocation(
            mProgram,
            "position"
        )

        mUniformTexture = glGetUniformLocation(
            mProgram,
            "u_tex"
        )

        mUniformSize = glGetUniformLocation(
            mProgram,
            "u_res"
        )

        mUniformScaledSize = glGetUniformLocation(
            mProgram,
            "u_texRes"
        )

        glGenFramebuffers(
            1,
            mFrameBuffer,
            0
        )

        glGenRenderbuffers(
            1,
            mDepthBuffer,
            0
        )
    }

    fun onSurfaceChanged(
        width: Int,
        height: Int,
        scaleFactor: Float
    ) {
        mfWidth = width.toFloat()
        mfHeight = height.toFloat()

        miWidth = width
        miHeight = height

        mfSWidth = width * scaleFactor
        mfSHeight = height * scaleFactor

        miSWidth = mfSWidth.toInt()
        miSHeight = mfSHeight.toInt()

        val buf = IntArray(miSWidth * miSHeight)
        val mTexBuffer = ByteBuffer.allocateDirect(
            buf.size * Float.SIZE_BYTES
        ).order(
            ByteOrder.nativeOrder()
        ).asIntBuffer()

        glTexImage2D(
            GL_TEXTURE_2D,
            0, GL_RGB, miSWidth, miSHeight, 0,
            GL_RGB, GL_UNSIGNED_SHORT_5_6_5, mTexBuffer
        )

        glBindRenderbuffer(
            GL_RENDERBUFFER,
            mDepthBuffer[0]
        )

        glRenderbufferStorage(
            GL_RENDERBUFFER,
            GL_DEPTH_COMPONENT16,
            width,
            height
        )
    }

    fun onDrawFrame(
        bitmap: Bitmap,
        texture: Int
    ) {
        glViewport(
            0,0,
            miSWidth, miSHeight
        )

        glBindFramebuffer(
            GL_FRAMEBUFFER,
            mFrameBuffer[0]
        )

        glFramebufferTexture2D(
            GL_FRAMEBUFFER,
            GL_COLOR_ATTACHMENT0,
            GL_TEXTURE_2D,
            texture,
            0
        )

        if (glCheckFramebufferStatus(GL_FRAMEBUFFER)
            != GL_FRAMEBUFFER_COMPLETE) {
            Log.d(TAG, "onDrawFrame: FRAME_BUFFER_NOT_COMPLETE")
            return
        }

        glUseProgram(
            mProgram
        )

        glEnableVertexAttribArray(
            mAttrPosition
        )
        glVertexAttribPointer(
            mAttrPosition,
            2,
            GL_FLOAT,
            false,
            8,
            mVertexBuffer
        )

        GLUtils.texImage2D(
            GL_TEXTURE_2D,
            0,
            Bitmap.createScaledBitmap(
                bitmap,
                miSWidth,
                miSHeight,
                false
            ),
            0
        )

        glBindTexture(
            GL_TEXTURE_2D,
            texture
        )

        glUniform1i(
            mUniformTexture,
            0
        )

        glUniform2f(
            mUniformSize,
            mfWidth,
            mfHeight
        )

        glUniform2f(
            mUniformScaledSize,
            mfSWidth,
            mfSHeight
        )

        glDrawElements(
            GL_TRIANGLES,
            mIndicesBuffer.capacity(),
            GL_UNSIGNED_BYTE,
            mIndicesBuffer
        )

        glDisableVertexAttribArray(
            mAttrPosition
        )
    }

    fun clean() {

        glDeleteFramebuffers(
            1,
            mFrameBuffer,
            0
        )

        glDeleteRenderbuffers(
            1,
            mDepthBuffer,
            0
        )

        if (mProgram != 0) {
            glDeleteProgram(
                mProgram
            )
        }
    }
}