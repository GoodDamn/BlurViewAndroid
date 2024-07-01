package good.damn.shaderblur.post_effects.blur

import android.opengl.GLES20.*
import android.util.Log
import good.damn.shaderblur.opengl.OpenGLUtils
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

open class PostProcessEffect(
    vertexCode: String,
    fragmentCode: String,
    private val mScaleFactor: Float,
    private val mVertexBuffer: FloatBuffer,
    private val mIndicesBuffer: ByteBuffer,
) {

    companion object {
        private const val TAG = "PostProcessEffect"
    }

    var texture: Int = 0
        get() = mTexture[0]
        private set

    private val mFrameBuffer = intArrayOf(1)
    private val mTexture = intArrayOf(1)

    private var mProgram: Int = 0

    private var mAttrPosition: Int = 0
    private var mUniformSize: Int = 0
    private var mUniformTexture: Int = 0

    private var mScaledWidth = 1
    private var mScaledHeight = 1

    init {
        mProgram = OpenGLUtils.createProgram(
            vertexCode,
            fragmentCode
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

        glGenTextures(
            1,
            mTexture,
            0
        )
    }

    open fun onSurfaceChanged(
        width: Int,
        height: Int
    ) {
        mScaledWidth = (width * mScaleFactor).toInt()
        mScaledHeight = (height * mScaleFactor).toInt()

        glBindTexture(
            GL_TEXTURE_2D,
            texture
        )

        /*glTexParameteri(
            GL_TEXTURE_2D,
            GL_TEXTURE_WRAP_S,
            GL_CLAMP_TO_EDGE
        )
        glTexParameteri(
            GL_TEXTURE_2D,
            GL_TEXTURE_WRAP_T,
            GL_CLAMP_TO_EDGE
        )*/
        glTexParameteri(GL_TEXTURE_2D,
            GL_TEXTURE_MAG_FILTER,
            GL_LINEAR)
        glTexParameteri(GL_TEXTURE_2D,
            GL_TEXTURE_MIN_FILTER,
            GL_LINEAR)

        val mTexBuffer = ByteBuffer.allocateDirect(
            mScaledWidth * mScaledHeight * Float.SIZE_BYTES
        ).order(
            ByteOrder.nativeOrder()
        ).asIntBuffer()

        glTexImage2D(
            GL_TEXTURE_2D,
            0, GL_RGB, mScaledWidth, mScaledHeight, 0,
            GL_RGB, GL_UNSIGNED_BYTE, mTexBuffer
        )

        glBindTexture(
            GL_TEXTURE_2D,
            0
        )
    }

    open fun onDrawFrame(
        texture: Int
    ) {
        glViewport(
            0,0,
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

        glActiveTexture(
            GL_TEXTURE0
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

    open fun clean() {
        glDeleteFramebuffers(
            1,
            mFrameBuffer,
            0
        )

        glDeleteTextures(
            1,
            mTexture,
            0
        )

        /*glDeleteRenderbuffers(
            1,
            mRenderBuffer,
            0
        )*/

        if (mProgram != 0) {
            glDeleteProgram(
                mProgram
            )
        }
    }
}