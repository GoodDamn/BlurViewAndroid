package good.damn.shaderblur.drawers

import android.opengl.GLES30
import android.opengl.GLES30.glBindVertexArray
import android.opengl.GLES30.glDrawElements
import good.damn.shaderblur.vertex.SBArrayVertexConfigurator

class SBDrawerVertexArray(
    private val configurator: SBArrayVertexConfigurator
) {

    fun draw() {
        glBindVertexArray(
            configurator.vertexArrayId
        )

        glDrawElements(
            GLES30.GL_TRIANGLES,
            configurator.indicesCount,
            configurator.config.type,
            0
        )

        glBindVertexArray(0)
    }
}