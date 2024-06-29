package good.damn.shaderblur.opengl

import android.opengl.GLES20.*

class OpenGLUtils {

    companion object {
        fun createProgram(
            vertexCode: String,
            fragmentCode: String
        ): Int {
            val v = loadShader(
                GL_VERTEX_SHADER,
                vertexCode
            )
            val f = loadShader(
                GL_FRAGMENT_SHADER,
                fragmentCode
            )

            val program = glCreateProgram()
            glAttachShader(
                program,
                v
            )

            glAttachShader(
                program,
                f
            )

            return program
        }
        fun loadShader(type: Int, code: String): Int {
            val shader = glCreateShader(type)
            glShaderSource(shader, code)
            glCompileShader(shader)
            return shader
        }
    }
}