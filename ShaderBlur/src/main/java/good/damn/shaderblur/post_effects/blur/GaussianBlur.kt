package good.damn.shaderblur.post_effects.blur

import android.graphics.Bitmap
import android.opengl.GLSurfaceView
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import android.opengl.GLES20.*
import android.opengl.GLUtils
import good.damn.shaderblur.drawers.SBDrawerVertexArray
import good.damn.shaderblur.post.PostProcessEffect
import good.damn.shaderblur.shaders.SBBinderAttribute
import good.damn.shaderblur.shaders.SBShaderTexture
import good.damn.shaderblur.texture.SBTexture
import good.damn.shaderblur.texture.SBTextureAttachment
import good.damn.shaderblur.utils.SBUtilsBuffer
import good.damn.shaderblur.vertex.SBArrayVertexConfigurator
import good.damn.shaderblur.vertex.SBEnumArrayVertexConfiguration
import good.damn.shaderblur.vertex.SBPointerAttribute

class GaussianBlur(
    private val mBlurRadius: Int,
    private val mScaleFactor: Float,
    shadeColor: FloatArray? = null
): GLSurfaceView.Renderer {

    var bitmap: Bitmap? = null

    private val mVertexShaderCode =
        "attribute vec4 position;" +
                "void main() {" +
                "gl_Position = position;" +
                "}"

    private val mFragmentCodeOutput = """
        #version 310 es
        precision mediump float;
        
        out vec4 FragColor;
        
        uniform vec2 uScreenSize;
        uniform sampler2D uTexture;
        void main () {
            vec2 scaled = vec2(
                gl_FragCoord.x / uScreenSize.x,
                gl_FragCoord.y / uScreenSize.y
            );
            
            ${generateFragColor(
                shadeColor
            )}
        }
    """.trimIndent()

    private val mVertexArrayQuad = SBArrayVertexConfigurator(
        SBEnumArrayVertexConfiguration.BYTE
    )

    private val mDrawerVertexArray = SBDrawerVertexArray(
        mVertexArrayQuad
    )

    private val mTextureHorizontal = SBTextureAttachment(
        GL_COLOR_ATTACHMENT0,
        SBTexture()
    )

    private val mTextureVertical = SBTextureAttachment(
        GL_COLOR_ATTACHMENT0,
        SBTexture()
    )

    private val mBlurHorizontal = PostProcessEffect(
        mTextureHorizontal,
        mDrawerVertexArray
    )

    private val mBlurVertical = PostProcessEffect(
        mTextureVertical,
        mDrawerVertexArray
    )

    private val mShaderOutput = SBShaderTexture()

    override fun onSurfaceCreated(
        gl: GL10?,
        config: EGLConfig?
    ) {
        mVertexArrayQuad.configure(
            SBUtilsBuffer.createFloat(
                floatArrayOf(
                    -1.0f, 1.0f,  // top left
                    -1.0f, -1.0f, // bottom left
                    1.0f, -1.0f,  // bottom right
                    1.0f, 1.0f,   // top right
                )
            ),
            SBUtilsBuffer.createByte(
                byteArrayOf(
                    0, 1, 2,
                    0, 2, 3
                )
            ),
            SBPointerAttribute.Builder()
                .pointPosition2()
                .build()
        )

        mBlurHorizontal.create()
        mBlurVertical.create()

        mShaderOutput.setupFromSource(
            mVertexShaderCode,
            mFragmentCodeOutput,
            SBBinderAttribute.Builder()
                .bindPosition()
                .build()
        )
    }

    override fun onSurfaceChanged(
        gl: GL10?,
        width: Int,
        height: Int
    ) {
        val scaledWidth = (
            width * mScaleFactor
        ).toInt()

        val scaledHeight = (
            height * mScaleFactor
        ).toInt()

        mBlurHorizontal.changeBounds(
            scaledWidth,
            scaledHeight
        )

        mBlurVertical.changeBounds(
            width,
            height
        )
    }

    override fun onDrawFrame(
        gl: GL10?
    ) {
        bitmap?.let {
            GLUtils.texImage2D(
                GL_TEXTURE_2D,
                0,
                it,
                0
            )
            mBlurHorizontal.draw()
            mBlurVertical.draw()
        }

        glBindFramebuffer(
            GL_FRAMEBUFFER,
            0
        )

        glViewport(
            0,0,
            miWidth, miHeight
        )

        mShaderOutput.use()

        glVertexAttribPointer(
            mAttrPosition,
            SIZE,
            GL_FLOAT,
            false,
            STRIDE,
            mVertexBuffer
        )

        glActiveTexture(
            GL_TEXTURE0
        )

        glBindTexture(
            GL_TEXTURE_2D,
            mHorizontalBlur!!.texture
        )

        glUniform1i(
            mUniformTexture,
            0
        )

        glUniform2f(
            mUniformSize,
            mfWidth,
            mfHeight
        )

        mDrawerVertexArray.draw()
    }

    fun clean() {
        mBlurHorizontal.clean()
        mBlurVertical.clean()
        mShaderOutput.delete()
    }

    private inline fun generateFragColor(
        shadeColor: FloatArray?
    ) = if (
        shadeColor == null
    ) "FragColor = texture(uTexture, scaled);"
    else """
        FragColor = mix(
            texture(
                uTexture,
                scaled
            ),
            vec4(${shadeColor[0]}, ${shadeColor[1]}, ${shadeColor[2]}, 1.0),
            ${shadeColor[3]}
        );
    """.trimIndent()
}