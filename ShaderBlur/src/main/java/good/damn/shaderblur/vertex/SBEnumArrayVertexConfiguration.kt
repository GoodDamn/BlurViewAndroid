package good.damn.shaderblur.vertex

import android.opengl.GLES30

enum class SBEnumArrayVertexConfiguration(
    val type: Int,
    val indicesSize: Int
) {
    BYTE(
        GLES30.GL_UNSIGNED_BYTE,
        1
    )
}