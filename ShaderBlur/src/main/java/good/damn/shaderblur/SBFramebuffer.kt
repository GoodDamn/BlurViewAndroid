package good.damn.shaderblur

import android.opengl.GLES20
import android.opengl.GLES20.GL_COLOR_ATTACHMENT0
import android.opengl.GLES20.GL_FRAMEBUFFER
import android.opengl.GLES20.GL_FRAMEBUFFER_COMPLETE
import android.opengl.GLES20.GL_TEXTURE_2D
import android.opengl.GLES20.glBindFramebuffer
import android.opengl.GLES20.glCheckFramebufferStatus
import android.opengl.GLES20.glDeleteFramebuffers
import android.opengl.GLES20.glFramebufferTexture2D
import android.opengl.GLES20.glGenFramebuffers
import android.util.Log
import good.damn.shaderblur.texture.SBTextureAttachment

class SBFramebuffer {

    private val mId = intArrayOf(-1)

    fun generate() {
        glGenFramebuffers(
            mId.size,
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

    fun attachTexture(
        texture: SBTextureAttachment
    ) {
        glFramebufferTexture2D(
            GL_FRAMEBUFFER,
            texture.attachment,
            GL_TEXTURE_2D,
            texture.texture.id,
            0
        )

        if (glCheckFramebufferStatus(GL_FRAMEBUFFER)
            != GL_FRAMEBUFFER_COMPLETE
        ) {
            Log.d("SBFramebuffer", "onDrawFrame: FRAME_BUFFER_NOT_COMPLETE")
        }
    }
}