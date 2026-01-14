package good.damn.shaderblur.drawers

import android.opengl.GLES30
import good.damn.shaderblur.shaders.SBIShaderScreenSize
import good.damn.shaderblur.shaders.SBIShaderTexture
import good.damn.shaderblur.texture.SBTexture

class SBDrawerScreenSize {

    var width = 0
    var height = 0

    fun draw(
        shader: SBIShaderScreenSize
    ) {
        GLES30.glUniform2i(
            shader.uniformScreenSize,
            width,
            height
        )
    }
}