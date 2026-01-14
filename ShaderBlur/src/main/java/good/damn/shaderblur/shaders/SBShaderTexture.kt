package good.damn.shaderblur.shaders

import android.opengl.GLES20.glGetUniformLocation

class SBShaderTexture
: SBShaderBase(),
SBIShaderTexture,
SBIShaderScreenSize {

    override var uniformTexture = -1
        private set

    override var uniformScreenSize = -1
        private set

    override fun setupUniforms(
        program: Int
    ) {
        uniformTexture = glGetUniformLocation(
            program,
            "uTexture"
        )

        uniformScreenSize = glGetUniformLocation(
            program,
            "uScreenSize"
        )
    }

}