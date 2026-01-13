package good.damn.shaderblur.post_effects.blur

import android.graphics.Bitmap
import android.opengl.GLSurfaceView
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import android.opengl.GLES20.*
import android.opengl.GLES30
import android.opengl.GLUtils
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

    private var mfWidth = 1f
    private var mfHeight = 1f

    private var miWidth = 1
    private var miHeight = 1

    private var mProgram: Int = 0

    private var mUniformTexture = 0
    private var mUniformSize = 0

    private val mVertexShaderCode =
        "attribute vec4 position;" +
                "void main() {" +
                "gl_Position = position;" +
                "}"

    private val mFragmentVerticalShaderCode =
        "precision mediump float;" +
    "uniform vec2 u_res;" +
    "uniform sampler2D u_tex;" +
    "void main () {" +
        "vec2 crs = vec2(gl_FragCoord.x, u_res.y-gl_FragCoord.y);" +
        "vec2 scaled = vec2(crs.x/u_res.x, crs.y / u_res.y);" +
        (if (shadeColor == null)
            "gl_FragColor = texture2D(u_tex, scaled * ${mScaleFactor * mScaleFactor});"
        else "gl_FragColor = mix(" +
            "texture2D(u_tex, scaled * ${mScaleFactor * mScaleFactor})," +
            "vec4(${shadeColor[0]}, ${shadeColor[1]}, ${shadeColor[2]}, 1.0)," +
            "${shadeColor[3]});") +
    "}"

    private val mVertexArrayQuad = SBArrayVertexConfigurator(
        SBEnumArrayVertexConfiguration.BYTE
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

        mProgram = OpenGLUtils.createProgram(
            mVertexShaderCode,
            mFragmentVerticalShaderCode
        )

        glLinkProgram(
            mProgram
        )

        mUniformTexture = glGetUniformLocation(
            mProgram,
            "u_tex"
        )

        mUniformSize = glGetUniformLocation(
            mProgram,
            "u_res"
        )


        mHorizontalBlur = HorizontalBlur(
            mVertexShaderCode,
            mScaleFactor,
            mBlurRadius,
            mVertexArrayQuad
        )

        mVerticalBlur = VerticalBlur(
            mVertexShaderCode,
            mBlurRadius,
            mScaleFactor,
            mVertexArrayQuad
        )
    }

    override fun onSurfaceChanged(
        gl: GL10?,
        width: Int,
        height: Int
    ) {
        mfWidth = width.toFloat()
        mfHeight = height.toFloat()

        miWidth = width
        miHeight = height

        mHorizontalBlur?.onSurfaceChanged(
            width,
            height
        )

        mVerticalBlur?.onSurfaceChanged(
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

            mHorizontalBlur?.onDrawFrame(
                mHorizontalBlur!!.texture
            )
            mVerticalBlur?.onDrawFrame(
                mHorizontalBlur!!.texture
            )
        }

        glBindFramebuffer(
            GL_FRAMEBUFFER,
            0
        )

        glViewport(
            0,0,
            miWidth, miHeight
        )

        glUseProgram(
            mProgram
        )
        glEnableVertexAttribArray(
            mAttrPosition
        )

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

        glDrawElements(
            GL_TRIANGLES,
            mIndicesBuffer.capacity(),
            GL_UNSIGNED_BYTE,
            mIndicesBuffer
        )

        glDisableVertexAttribArray(
            mAttrPosition
        )
    }

    fun clean() {
        mHorizontalBlur?.clean()
        mVerticalBlur?.clean()
        if (mProgram == 0) {
            return
        }
        glDeleteProgram(
            mProgram
        )
    }
}