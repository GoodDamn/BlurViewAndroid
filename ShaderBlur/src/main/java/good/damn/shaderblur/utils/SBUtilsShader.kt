package good.damn.shaderblur.utils

import android.opengl.GLES20
import android.opengl.GLES20.GL_FRAGMENT_SHADER
import android.opengl.GLES20.GL_VERTEX_SHADER
import android.opengl.GLES20.glAttachShader
import android.opengl.GLES20.glCompileShader
import android.opengl.GLES20.glCreateProgram
import android.opengl.GLES20.glCreateShader
import android.opengl.GLES20.glShaderSource
import android.opengl.GLES30
import android.util.Log

object SBUtilsShader {

    @JvmStatic
    fun createProgramFromAssets(
        vertexPath: String,
        fragmentPath: String
    ): Int {
        return createProgram(
            MGUtilsAsset.loadString(
                vertexPath
            ),
            MGUtilsAsset.loadString(
                fragmentPath
            )
        )
    }

    @JvmStatic
    fun createProgram(
        vertex: String,
        fragment: String
    ): Int {
        val program = glCreateProgram()
        val frag = createShader(
            GL_FRAGMENT_SHADER,
            fragment
        )

        val vert = createShader(
            GL_VERTEX_SHADER,
            vertex
        )

        glAttachShader(
            program,
            frag
        )

        glAttachShader(
            program,
            vert
        )

        return program
    }

    @JvmStatic
    fun createShader(
        type: Int,
        source: String
    ): Int {
        val shader = glCreateShader(
            type
        )

        glShaderSource(
            shader,
            source
        )

        glCompileShader(
            shader
        )

        val status = intArrayOf(0)
        GLES20.glGetShaderiv(
            shader,
            GLES20.GL_COMPILE_STATUS,
            status,
            0
        )

        Log.d("TAG", "createShader: STATUS: ${status[0]} TYPE: $type;")
        if (status[0] == GLES20.GL_FALSE) {
            Log.d("TAG", "createShader: NOT COMPILED: ${GLES30.glGetShaderInfoLog(shader)}")
            return -1
        }

        return shader
    }
}