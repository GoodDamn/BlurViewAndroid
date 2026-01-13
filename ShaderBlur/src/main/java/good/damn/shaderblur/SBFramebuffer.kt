package good.damn.shaderblur

import android.opengl.GLES20
import android.opengl.GLES20.GL_FRAMEBUFFER
import android.opengl.GLES20.glBindFramebuffer
import android.opengl.GLES20.glDeleteFramebuffers
import android.opengl.GLES20.glGenFramebuffers

class SBFramebuffer {

    private val mId = intArrayOf(-1)

    fun generate() {
        glGenFramebuffers(
            1,
            mId,
            0
        )
    }

    fun bind() {
        glBindFramebuffer(
            GL_FRAMEBUFFER,
            mId[0]
        )
    }

    fun unbind() {
        glBindFramebuffer(
            GL_FRAMEBUFFER,
            0
        )
    }

    fun delete() {
        glDeleteFramebuffers(
            mId.size,
            mId,
            0
        )
    }
}