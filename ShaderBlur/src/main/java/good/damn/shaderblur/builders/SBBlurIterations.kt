package good.damn.shaderblur.builders

import android.opengl.GLES30
import good.damn.shaderblur.drawers.SBDrawerTexture
import good.damn.shaderblur.drawers.SBDrawerVertexArray
import good.damn.shaderblur.renderer.post.SBPostProcess
import good.damn.shaderblur.shaders.SBShaderTexture
import good.damn.shaderblur.texture.SBTexture
import good.damn.shaderblur.texture.SBTextureAttachment
import java.util.LinkedList

internal class SBBlurIterations private constructor(
    private val list: LinkedList<
        SBMBlurIteration
    >,
    val lastTexture: SBTexture
) {

    fun create() {
        list.forEach {
            it.postProcessHorizontal.create()
            it.postProcessVertical.create()
        }
    }

    fun draw() {
        list.forEach {
            it.postProcessHorizontal.draw()
            it.postProcessVertical.draw()
        }
    }

    fun delete() {
        list.forEach {
            it.postProcessHorizontal.apply {
                deleteFramebuffer()
                deleteTexture()
            }

            it.postProcessVertical.apply {
                deleteFramebuffer()
                deleteTexture()
            }
        }
    }

    fun changeBounds(
        width: Int,
        height: Int
    ) {
        list.forEach {
            val w = (width * it.scaleFactor).toInt()
            val h = (height * it.scaleFactor).toInt()

            it.postProcessHorizontal.changeBounds(
                w, h
            )

            it.postProcessVertical.changeBounds(
                w, h
            )
        }
    }

    class Builder(
        private val shaderHorizontal: SBShaderTexture,
        private val shaderVertical: SBShaderTexture,
        private val drawerQuad: SBDrawerVertexArray,
        private var textureInput: SBTexture
    ) {

        private val mList = LinkedList<
            SBMBlurIteration
        >()

        fun addIteration(
            scaleFactor: Float
        ) = apply {
            val textureHorizontal = SBTexture()
            val postProcessHorizontal = SBPostProcess(
                SBTextureAttachment(
                    GLES30.GL_COLOR_ATTACHMENT0,
                    textureHorizontal
                ),
                drawerQuad,
                drawerInputTexture = SBDrawerTexture(
                    GLES30.GL_TEXTURE0,
                    textureInput
                ),
                shaderHorizontal
            )

            textureInput = SBTexture()

            val postProcessVertical = SBPostProcess(
                SBTextureAttachment(
                    GLES30.GL_COLOR_ATTACHMENT0,
                    textureInput
                ),
                drawerQuad,
                drawerInputTexture = SBDrawerTexture(
                    GLES30.GL_TEXTURE0,
                    textureHorizontal
                ),
                shaderVertical
            )

            mList.add(
                SBMBlurIteration(
                    scaleFactor,
                    postProcessHorizontal,
                    postProcessVertical
                )
            )
        }

        fun build() = SBBlurIterations(
            mList,
            textureInput
        )
    }

    private data class SBMBlurIteration(
        val scaleFactor: Float,
        val postProcessHorizontal: SBPostProcess,
        val postProcessVertical: SBPostProcess,
    )
}