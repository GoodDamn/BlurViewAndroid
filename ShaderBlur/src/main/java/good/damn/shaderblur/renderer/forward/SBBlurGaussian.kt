package good.damn.shaderblur.renderer.forward

import android.graphics.Bitmap
import android.opengl.GLSurfaceView
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import android.opengl.GLES20.*
import good.damn.shaderblur.builders.SBBlur
import good.damn.shaderblur.builders.SBBlurIterations
import good.damn.shaderblur.drawers.SBDrawerScreenSize
import good.damn.shaderblur.drawers.SBDrawerTexture
import good.damn.shaderblur.drawers.SBDrawerVertexArray
import good.damn.shaderblur.shaders.SBBinderAttribute
import good.damn.shaderblur.shaders.SBShaderTexture
import good.damn.shaderblur.texture.SBTexture
import good.damn.shaderblur.texture.SBTextureBitmap
import good.damn.shaderblur.utils.SBUtilsBuffer
import good.damn.shaderblur.vertex.SBArrayVertexConfigurator
import good.damn.shaderblur.vertex.SBEnumArrayVertexConfiguration
import good.damn.shaderblur.vertex.SBPointerAttribute
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sqrt

class SBBlurGaussian(
    blurRadius: Int,
    blurIterations: SBBlur,
    shadeColor: FloatArray? = null
): GLSurfaceView.Renderer {

    var bitmap: Bitmap? = null

    private val mKernelStr = (
        blurRadius * 2 + 1
    ).run {
            val strBuilder = StringBuilder()
            val stDev2 = 2f * blurRadius * blurRadius
            val leftExp = 1.0f / sqrt(stDev2 * PI)

            for (i in -blurRadius until blurRadius) {
                strBuilder.append(
                    leftExp * exp(-(i * i).toFloat() / stDev2)
                )

                strBuilder.append(
                    ", "
                )
            }

            strBuilder.append(
                leftExp * exp(-(blurRadius * blurRadius).toFloat() / stDev2)
            )

            return@run """
                #define KERNEL_SIZE $this
                const float kernel[KERNEL_SIZE] = float[KERNEL_SIZE](
                    $strBuilder
                );
            """.trimIndent()
    }

    private val mVertexShaderCode = """
        #version 310 es
        precision mediump float;
        layout(location = 0) in vec2 position;
        
        void main() {
            gl_Position = vec4(position, 1.0, 1.0);
        }
    """.trimIndent()

    private val mFragmentCodeOutput = """
        #version 310 es
        precision mediump float;
        
        out vec4 FragColor;
        
        uniform vec2 uScreenSize;
        uniform sampler2D uTexture;
        void main () {
            vec2 s = gl_FragCoord.xy / uScreenSize;
            vec2 scaled = vec2(
                s.x,
                1.0 - s.y
            );
            
            ${generateFragColor(
                shadeColor
            )}
        }
    """.trimIndent()

    private val mFragmentCodeHorizontal = """
        #version 310 es
        precision highp float;
        
        $mKernelStr
        
        layout(location = 0) out vec3 outputColor;
        uniform vec2 uScreenSize;
        uniform sampler2D uTexture;

        void main () {
            vec4 sum = vec4(0.0);
            float normDistSum = 0.0;
            
            vec2 offset = vec2(
                gl_FragCoord.x - $blurRadius.0,
                gl_FragCoord.y
            );
            
            for (int i = 0; i < KERNEL_SIZE; i++) {
                offset.x++;
                float gt = kernel[i];
                normDistSum += gt;
                sum += texture(
                    uTexture,
                    offset / uScreenSize
                ) * gt;
            }
            
            outputColor = sum.xyz / vec3(normDistSum);
        }
    """.trimIndent()

    private val mFragmentCodeVertical = """
        #version 310 es
        precision highp float;
        
        $mKernelStr
        
        layout(location = 0) out vec3 outputColor;
        uniform vec2 uScreenSize;
        uniform sampler2D uTexture;
        
        void main () {
            vec4 sum = vec4(0.0);
            float normDistSum = 0.0;
            vec2 offset = vec2(
                gl_FragCoord.x,
                gl_FragCoord.y - $blurRadius.0
            );
            
            for (int i = 0; i < KERNEL_SIZE; i++) {
                offset.y++;
                float gt = kernel[i];
                normDistSum += gt;
                sum += texture(
                    uTexture,
                    offset / uScreenSize
                ) * gt;
            }
            
            outputColor = sum.xyz / vec3(normDistSum);
        }
    """.trimIndent()

    private val mVertexArrayQuad = SBArrayVertexConfigurator(
        SBEnumArrayVertexConfiguration.BYTE
    )

    private val mDrawerVertexArray = SBDrawerVertexArray(
        mVertexArrayQuad
    )

    private val mShaderHorizontal = SBShaderTexture()
    private val mShaderVertical = SBShaderTexture()
    private val mShaderOutput = SBShaderTexture()

    private val mTextureInput = SBTextureBitmap(
        SBTexture()
    )

    private val mBlurIterations = SBBlurIterations.Builder(
        mShaderHorizontal,
        mShaderVertical,
        mDrawerVertexArray,
        mTextureInput.texture
    ).run {
        blurIterations.list.forEach {
            addIteration(
                it.scaleFactor
            )
        }
        return@run build()
    }


    private val mDrawerScreenSize = SBDrawerScreenSize()
    private val mDrawerOutputTexture = SBDrawerTexture(
        GL_TEXTURE0,
        mBlurIterations.lastTexture
    )

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

        mTextureInput.texture.generate()
        mTextureInput.setupFiltering()

        SBBinderAttribute.Builder()
            .bindPosition()
            .build().apply {
                mShaderHorizontal.setupFromSource(
                    mVertexShaderCode,
                    mFragmentCodeHorizontal,
                    this
                )

                mShaderVertical.setupFromSource(
                    mVertexShaderCode,
                    mFragmentCodeVertical,
                    this
                )

                mShaderOutput.setupFromSource(
                    mVertexShaderCode,
                    mFragmentCodeOutput,
                    this
                )
            }

        mBlurIterations.create()
    }

    override fun onSurfaceChanged(
        gl: GL10?,
        width: Int,
        height: Int
    ) {
        mDrawerScreenSize.let {
            it.width = width.toFloat()
            it.height = height.toFloat()
        }

        mBlurIterations.changeBounds(
            width, height
        )
    }

    override fun onDrawFrame(
        gl: GL10?
    ) {
        val bitmap = bitmap
            ?: return

        mTextureInput.texImage(
            bitmap
        )

        mBlurIterations.draw()

        glBindFramebuffer(
            GL_FRAMEBUFFER,
            0
        )

        mShaderOutput.apply {
            use()
            mDrawerOutputTexture.draw(
                this
            )
            mDrawerScreenSize.draw(
                this
            )
            mDrawerVertexArray.draw()
            mDrawerOutputTexture.unbind(
                this
            )
        }
    }

    fun clean() {
        mBlurIterations.delete()
        mShaderHorizontal.delete()
        mShaderVertical.delete()
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