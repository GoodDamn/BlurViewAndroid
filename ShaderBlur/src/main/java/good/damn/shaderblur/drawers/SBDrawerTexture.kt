package good.damn.shaderblur.drawers

import android.opengl.GLES30
import good.damn.shaderblur.shaders.SBIShaderTexture
import good.damn.shaderblur.texture.SBTexture

class SBDrawerTexture(
    private val activeTexture: Int,
    private val texture: SBTexture
) {

    fun draw(
        shader: SBIShaderTexture
    ) {
        texture.bind()
        GLES30.glActiveTexture(
            activeTexture
        )

        GLES30.glUniform1i(
            shader.uniformTexture,
            activeTexture
        )
    }


    fun unbind(
        shader: SBIShaderTexture
    ) {
        texture.unbind()
        GLES30.glUniform1i(
            shader.uniformTexture,
            0
        )
    }
}