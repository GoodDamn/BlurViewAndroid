package good.damn.shaderblur.texture

import android.opengl.GLES20.GL_TEXTURE_2D
import android.opengl.GLES20.glBindTexture
import android.opengl.GLES20.glDeleteTextures
import android.opengl.GLES20.glGenTextures
import java.net.IDN

class SBTexture {

    val id: Int
        get() = mId[0]

    private val mId = intArrayOf(-1)

    fun generate() {
        glGenTextures(
            mId.size,
            mId,
            0
        )
    }

    fun bind() {
        glBindTexture(
            GL_TEXTURE_2D,
            mId[0]
        )
    }

    fun unbind() {
        glBindTexture(
            GL_TEXTURE_2D,
            0
        )
    }

    fun delete() {
        glDeleteTextures(
            mId.size,
            mId,
            0
        )
    }

}