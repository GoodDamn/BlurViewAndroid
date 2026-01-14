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
    private val texture: SBTextureAttachment,
    private val drawerQuad: SBDrawerVertexArray
) {

    private val mFramebuffer = SBFramebuffer()

    private val mShader = SBShaderTexture()

    private val mDrawerTexture = SBDrawerTexture(
        GLES30.GL_TEXTURE0,
        texture.texture
    )

    private val mDrawerScreenSize = SBDrawerScreenSize()

    fun create(
        vertexCode: String,
        fragmentCode: String
    ) {
        mShader.setup(
            vertexCode,
            fragmentCode,
            SBBinderAttribute.Builder()
                .bindPosition()
                .build()
        )

        texture.texture.generate()
        mFramebuffer.generate()
    }

    fun changeBounds(
        width: Int,
        height: Int
    ) {
        texture.glSetupTexture(
            width,
            height
        )

        mFramebuffer.apply {
            bind()
            attachTexture(
                texture
            )
            unbind()
        }

        mDrawerScreenSize.let {
            it.width = width
            it.height = height
        }
    }

    fun draw() {
        mFramebuffer.bind()

        mDrawerScreenSize.apply {
            glViewport(
                0,0,
                width,
                height
            )
        }

        mShader.use()

        mDrawerTexture.draw(
            mShader
        )

        mDrawerScreenSize.draw(
            mShader
        )

        drawerQuad.draw()
        mDrawerTexture.unbind(
            mShader
        )
    }

    fun clean() {
        mFramebuffer.delete()
        texture.texture.delete()
        mShader.delete()
    }
}