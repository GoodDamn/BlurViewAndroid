package good.damn.shaderblur.texture

import android.opengl.GLES20.GL_LINEAR
import android.opengl.GLES20.GL_RGB
import android.opengl.GLES20.GL_TEXTURE_2D
import android.opengl.GLES20.GL_TEXTURE_MAG_FILTER
import android.opengl.GLES20.GL_TEXTURE_MIN_FILTER
import android.opengl.GLES20.GL_UNSIGNED_BYTE
import android.opengl.GLES20.glTexImage2D
import android.opengl.GLES20.glTexParameteri

class SBTextureAttachment(
    val attachment: Int,
    val texture: SBTexture
) {
    fun glSetupTexture(
        width: Int,
        height: Int
    ) {
        texture.bind()
        glTexParameteri(
            GL_TEXTURE_2D,
            GL_TEXTURE_MAG_FILTER,
            GL_LINEAR
        )

        glTexParameteri(
            GL_TEXTURE_2D,
            GL_TEXTURE_MIN_FILTER,
            GL_LINEAR
        )

        glTexImage2D(
            GL_TEXTURE_2D,
            0,
            GL_RGB,
            width,
            height,
            0,
            GL_RGB,
            GL_UNSIGNED_BYTE,
            null
        )
        texture.unbind()
    }
}