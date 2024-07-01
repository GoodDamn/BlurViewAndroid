package good.damn.shaderblur.post_effects.blur

import android.opengl.GLES20.*
import android.util.Log
import good.damn.shaderblur.opengl.OpenGLUtils
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class VerticalBlur(
    private val mVertexCode: String,
    private val mVertexBuffer: FloatBuffer,
    private val mIndicesBuffer: ByteBuffer,
    private val mScaleFactor: Float,
    blurRadius: Int,
) {

    companion object {
        private const val TAG = "VerticalBlur"
    }

    private val mFragmentShaderCode =
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
            "const float rad = $blurRadius.0;" +
            "vec4 sum = vec4(0.0);" +
            "float normDistSum = 0.0;" +
            "float gt;" +
            "vec2 offset = vec2(gl_FragCoord.x, gl_FragCoord.y - rad);" +
            "for (float i = -rad; i <= rad;i++) {" +
                "gt = gauss(i,aa,stDevSQ);" +
                "normDistSum += gt;" +
                "offset.y++;" +
                "sum += texture2D(u_tex, offset/u_res) * gt;" +
            "}" +
            "gl_FragColor = sum / vec4(normDistSum);" +
        "}"

    private var mProgram: Int = 0

    private val mDepthBuffer = intArrayOf(1)
    private val mFrameBuffer = intArrayOf(1)

    private var mAttrPosition: Int = 0
    private var mUniformSize: Int = 0
    private var mUniformTexture: Int = 0

    private var mScaledWidth = 1
    private var mScaledHeight = 1

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
        height: Int
    ) {
        mScaledWidth = (width * mScaleFactor).toInt()
        mScaledHeight = (height * mScaleFactor).toInt()

        val mTexBuffer = ByteBuffer.allocateDirect(
            mScaledWidth * mScaledHeight * Float.SIZE_BYTES
        ).order(
            ByteOrder.nativeOrder()
        ).asIntBuffer()

        glTexImage2D(
            GL_TEXTURE_2D,
            0, GL_RGB, mScaledWidth, mScaledHeight, 0,
            GL_RGB, GL_UNSIGNED_SHORT_5_6_5, mTexBuffer
        )

        glBindRenderbuffer(
            GL_RENDERBUFFER,
            mDepthBuffer[0]
        )

        glRenderbufferStorage(
            GL_RENDERBUFFER,
            GL_DEPTH_COMPONENT16,
            mScaledWidth,
            mScaledHeight
        )
    }

    fun onDrawFrame(
        texture: Int
    ) {
        glViewport(
            0, 0,
            mScaledWidth,
            mScaledHeight
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
            != GL_FRAMEBUFFER_COMPLETE
        ) {
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
            mScaledWidth.toFloat(),
            mScaledHeight.toFloat()
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