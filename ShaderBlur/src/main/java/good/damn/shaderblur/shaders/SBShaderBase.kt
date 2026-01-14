package good.damn.shaderblur.shaders

import android.opengl.GLES20
import android.opengl.GLES30.*
import android.util.Log
import good.damn.shaderblur.utils.SBUtilsShader

abstract class SBShaderBase
: SBIShader {
    companion object {
        private const val TAG = "SBShaderBase"
    }

    private var mProgram = 0

    override fun use() {
        glUseProgram(
            mProgram
        )
    }

    override fun delete() {
        glDeleteProgram(
            mProgram
        )
    }

    fun compileFromSource(
        srcVertex: String,
        srcFragment: String,
        binderAttribute: SBBinderAttribute
    ): Int {
        mProgram = SBUtilsShader.createProgram(
            srcVertex,
            srcFragment
        )

        binderAttribute.bindAttributes(
            mProgram
        )

        return 0
    }

    fun compile(
        pathVertex: String,
        pathFragment: String,
        binderAttribute: SBBinderAttribute
    ): Int {
        /*mProgram = SBUtilsShader.createProgramFromAssets(
            pathVertex,
            pathFragment
        )

        binderAttribute.bindAttributes(
            mProgram
        )*/

        return 0
    }


    fun link() {
        glLinkProgram(
            mProgram
        )

        val status = intArrayOf(1)
        GLES20.glGetProgramiv(
            mProgram,
            GL_LINK_STATUS,
            status,
            0
        )
        Log.d(TAG, "link: STATUS: ${status[0]}")

        if (status[0] == GL_TRUE) {
            return
        }
        Log.d(TAG, "link: ERROR:")
    }

    fun setupFromSource(
        srcVertex: String,
        srcFragment: String,
        binderAttribute: SBBinderAttribute
    ) {
        compileFromSource(
            srcVertex,
            srcFragment,
            binderAttribute
        )
        link()
        setupUniforms(
            mProgram
        )
    }

    fun setup(
        pathVertex: String,
        pathFragment: String,
        binderAttribute: SBBinderAttribute
    ) {
        compile(
            pathVertex,
            pathFragment,
            binderAttribute
        )
        link()
        setupUniforms(
            mProgram
        )
    }

}