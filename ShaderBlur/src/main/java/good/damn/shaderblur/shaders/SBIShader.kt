package good.damn.shaderblur.shaders

interface SBIShader {

    fun use()

    fun setupUniforms(
        program: Int
    )
}