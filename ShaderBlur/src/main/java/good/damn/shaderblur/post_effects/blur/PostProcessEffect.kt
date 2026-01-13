package good.damn.shaderblur.post_effects.blur

import android.opengl.GLES20
import android.opengl.GLES20.*
import android.opengl.GLES30
import android.util.Log
import good.damn.shaderblur.SBFramebuffer
import good.damn.shaderblur.opengl.OpenGLUtils
import good.damn.shaderblur.texture.SBTexture
import good.damn.shaderblur.texture.SBTextureAttachment
import good.damn.shaderblur.vertex.SBArrayVertexConfigurator
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

open class PostProcessEffect(
    vertexCode: String,
    fragmentCode: String,
    private val mScaleFactor: Float,
    private val vertexArray: SBArrayVertexConfigurator
) {

    companion object {
        private const val TAG = "PostProcessEffect"
    }

    private val mFramebuffer = SBFramebuffer()
    private val mTexture = SBTextureAttachment(
        GL_COLOR_ATTACHMENT0,
        SBTexture()
    )

    private var mProgram: Int = 0

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

        mTexture.texture.generate()
        mFramebuffer.generate()
    }

    open fun onSurfaceChanged(
        width: Int,
        height: Int
    ) {
        mScaledWidth = (width * mScaleFactor).toInt()
        mScaledHeight = (height * mScaleFactor).toInt()
        mTexture.glSetupTexture(
            width,
            height
        )
        mFramebuffer.apply {
            bind()
            attachTexture(
                mTexture
            )
            unbind()
        }
    }

    open fun onDrawFrame() {
        mFramebuffer.bind()

        glViewport(
            0,0,
            mScaledWidth,
            mScaledHeight
        )

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

        glActiveTexture(
            GL_TEXTURE0
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

    open fun clean() {
        mFramebuffer.delete()
        mTexture.texture.delete()

        if (mProgram != 0) {
            glDeleteProgram(
                mProgram
            )
        }
    }
}