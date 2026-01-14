package good.damn.shaderblur.texture

import android.graphics.Bitmap
import android.opengl.GLES20.GL_LINEAR
import android.opengl.GLES20.GL_NEAREST
import android.opengl.GLES20.GL_RGB
import android.opengl.GLES20.GL_TEXTURE_2D
import android.opengl.GLES20.GL_TEXTURE_MAG_FILTER
import android.opengl.GLES20.GL_TEXTURE_MIN_FILTER
import android.opengl.GLES20.GL_UNSIGNED_BYTE
import android.opengl.GLES20.glTexImage2D
import android.opengl.GLES20.glTexParameteri
import android.opengl.GLES30
import android.opengl.GLUtils

class SBTextureBitmap(
    val texture: SBTexture
) {
    fun texImage(
        bitmap: Bitmap
    ) {
        texture.bind()
        GLUtils.texImage2D(
            GL_TEXTURE_2D,
            0,
            bitmap,
            0
        )
        texture.unbind()
    }

    fun setupFiltering() {
        texture.bind()
        glTexParameteri(
            GL_TEXTURE_2D,
            GL_TEXTURE_MAG_FILTER,
            GL_LINEAR
        )

        glTexParameteri(
            GL_TEXTURE_2D,
            GL_TEXTURE_MIN_FILTER,
            GL_NEAREST
        )
        texture.unbind()
    }

}