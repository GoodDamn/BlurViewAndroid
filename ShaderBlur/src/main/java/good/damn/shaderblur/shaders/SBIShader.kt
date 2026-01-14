package good.damn.shaderblur.shaders

interface SBIShader {

    fun delete()

    fun use()

    fun setupUniforms(
        program: Int
    )
}