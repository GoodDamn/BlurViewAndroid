package good.damn.shaderblur.renderer.post

import android.opengl.GLES30
import good.damn.shaderblur.SBFramebuffer
import good.damn.shaderblur.drawers.SBDrawerScreenSize
import good.damn.shaderblur.drawers.SBDrawerTexture
import good.damn.shaderblur.drawers.SBDrawerVertexArray
import good.damn.shaderblur.shaders.SBBinderAttribute
import good.damn.shaderblur.shaders.SBShaderTexture
import good.damn.shaderblur.texture.SBTextureAttachment

class SBPostProcess(
    private val outputTexture: SBTextureAttachment,
    private val drawerQuad: SBDrawerVertexArray,
    private val drawerInputTexture: SBDrawerTexture,
    private val shader: SBShaderTexture
) {
    private val mFramebuffer = SBFramebuffer()
    private val mDrawerScreenSize = SBDrawerScreenSize()

    fun create() {
        outputTexture.texture.generate()
        mFramebuffer.generate()
    }

    fun changeBounds(
        width: Int,
        height: Int
    ) {
        outputTexture.glSetupTexture(
            width,
            height
        )

        mFramebuffer.apply {
            bind()
            attachTexture(
                outputTexture
            )
            intArrayOf(
                outputTexture.attachment
            ).apply {
                GLES30.glDrawBuffers(
                    size,
                    this,
                    0
                )
            }
            unbind()
        }

        mDrawerScreenSize.let {
            it.width = width.toFloat()
            it.height = height.toFloat()
        }
    }

    fun draw() {
        mFramebuffer.bind()

        shader.use()

        drawerInputTexture.draw(
            shader
        )

        mDrawerScreenSize.draw(
            shader
        )

        drawerQuad.draw()
        drawerInputTexture.unbind(
            shader
        )
    }

    fun clean() {
        mFramebuffer.delete()
        outputTexture.texture.delete()
        shader.delete()
    }
}