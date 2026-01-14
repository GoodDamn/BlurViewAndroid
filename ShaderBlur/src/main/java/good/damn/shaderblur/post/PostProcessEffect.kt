package good.damn.shaderblur.post

import android.opengl.GLES20.*
import android.opengl.GLES30
import good.damn.shaderblur.SBFramebuffer
import good.damn.shaderblur.drawers.SBDrawerScreenSize
import good.damn.shaderblur.drawers.SBDrawerTexture
import good.damn.shaderblur.drawers.SBDrawerVertexArray
import good.damn.shaderblur.shaders.SBBinderAttribute
import good.damn.shaderblur.shaders.SBShaderTexture
import good.damn.shaderblur.texture.SBTextureAttachment

class PostProcessEffect(
    private val outputTexture: SBTextureAttachment,
    private val drawerQuad: SBDrawerVertexArray,
    private val drawerInputTexture: SBDrawerTexture
) {

    private val mFramebuffer = SBFramebuffer()

    private val mShader = SBShaderTexture()

    private val mDrawerScreenSize = SBDrawerScreenSize()

    fun create(
        vertexCode: String,
        fragmentCode: String
    ) {
        mShader.setupFromSource(
            vertexCode,
            fragmentCode,
            SBBinderAttribute.Builder()
                .bindPosition()
                .build()
        )

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

        /*mDrawerScreenSize.apply {
            glViewport(
                0,0,
                width,
                height
            )
        }*/

        mShader.use()

        drawerInputTexture.draw(
            mShader
        )

        mDrawerScreenSize.draw(
            mShader
        )

        drawerQuad.draw()
        drawerInputTexture.unbind(
            mShader
        )
    }

    fun clean() {
        mFramebuffer.delete()
        outputTexture.texture.delete()
        mShader.delete()
    }
}