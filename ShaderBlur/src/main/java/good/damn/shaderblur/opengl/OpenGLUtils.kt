package good.damn.shaderblur.opengl

import android.opengl.GLES20

class OpenGLUtils {

    companion object {
        fun loadShader(type: Int, code: String): Int {
            val shader = GLES20.glCreateShader(type)
            GLES20.glShaderSource(shader, code)
            GLES20.glCompileShader(shader)
            return shader
        }
    }
}