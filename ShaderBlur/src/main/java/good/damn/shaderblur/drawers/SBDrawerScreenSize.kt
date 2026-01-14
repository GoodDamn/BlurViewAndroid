package good.damn.shaderblur.drawers

import android.opengl.GLES30
import good.damn.shaderblur.shaders.SBIShaderScreenSize
import good.damn.shaderblur.shaders.SBIShaderTexture
import good.damn.shaderblur.texture.SBTexture

class SBDrawerScreenSize {

    var width = 0f
    var height = 0f

    fun draw(
        shader: SBIShaderScreenSize
    ) {
        GLES30.glUniform2f(
            shader.uniformScreenSize,
            width,
            height
        )
    }
}