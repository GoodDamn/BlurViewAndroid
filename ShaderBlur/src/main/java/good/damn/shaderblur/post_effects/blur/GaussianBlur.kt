package good.damn.shaderblur.post_effects.blur

import android.graphics.Bitmap
import android.opengl.GLSurfaceView
import good.damn.shaderblur.opengl.OpenGLUtils
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import android.opengl.GLES20.*
import android.opengl.GLUtils
import java.nio.IntBuffer

class GaussianBlur: GLSurfaceView.Renderer {

    companion object {
        private const val STRIDE = 8
        private const val SIZE = 2
    }

    lateinit var bitmap: Bitmap

    private var mfWidth = 1f
    private var mfHeight = 1f

    private var miWidth = 1
    private var miHeight = 1

    private lateinit var mVertexBuffer: FloatBuffer
    private lateinit var mIndicesBuffer: ByteBuffer

    private lateinit var mPixels: IntArray

    private var mProgram: Int = 0
    private var mTexture = intArrayOf(1)

    private val mIndices: ByteArray = byteArrayOf(
        0, 1, 2,
        0, 2, 3
    )

    private val mSquareCoords: FloatArray = floatArrayOf(
        -1.0f, 1.0f,  // top left
        -1.0f, -1.0f, // bottom left
        1.0f, -1.0f,  // bottom right
        1.0f, 1.0f,   // top right
    )

    private var mAttrPosition = 0
    private var mUniformTexture = 0
    private var mUniformSize = 0

    private val mVertexShaderCode =
        "attribute vec4 position;" +
                "void main() {" +
                "gl_Position = position;" +
                "}"

    private val mFragmentShaderCode =
        "precision mediump float;" +
    "uniform vec2 u_res;" +
    "uniform sampler2D u_tex;" +

    "float gauss(float inp, float aa, float stDevSQ) {" +
        "return aa * exp(-(inp*inp)/stDevSQ);" +
    "}" +
    "void main () {" +
        "float stDev = 8.0;" +
        "float stDevSQ = 2.0 * stDev * stDev;" +
        "float aa = 0.398 / stDev;" +
        "vec2 crs = vec2(gl_FragCoord.x, u_res.y-gl_FragCoord.y);" +
        "const float rad = 7.0;" +
        "vec4 sum = vec4(0.0);" +
        "float normDistSum = 0.0;" +
        "float gt;" +
        "for (float i = -rad; i <= rad;i++) {" +
            "gt = gauss(i,aa,stDevSQ);" +
            "normDistSum += gt;" +
            "sum += texture2D(u_tex, vec2(crs.x,crs.y+i)/u_res) * gt;" +
        "}" +
        "gl_FragColor = sum / vec4(normDistSum);" +
    "}"

    override fun onSurfaceCreated(
        gl: GL10?,
        config: EGLConfig?
    ) {

        val byteBuffer =
            ByteBuffer.allocateDirect(mSquareCoords.size * 4)
        byteBuffer.order(ByteOrder.nativeOrder())
        mVertexBuffer = byteBuffer.asFloatBuffer()
        mVertexBuffer.put(mSquareCoords)
        mVertexBuffer.position(0)

        val drawByteBuffer: ByteBuffer =
            ByteBuffer.allocateDirect(mIndices.size * 2)
        drawByteBuffer.order(ByteOrder.nativeOrder())
        mIndicesBuffer = drawByteBuffer
        mIndicesBuffer.put(mIndices)
        mIndicesBuffer.position(0)


        mProgram = OpenGLUtils.createProgram(
            mVertexShaderCode,
            mFragmentShaderCode
        )

        glLinkProgram(
            mProgram
        )


        mAttrPosition = glGetAttribLocation(
            mProgram,
            "position"
        )

        mUniformTexture = glGetUniformLocation(
            mProgram,
            "u_tex"
        )

        mUniformSize = glGetUniformLocation(
            mProgram,
            "u_res"
        )


        glGenTextures(
            1,
            mTexture,
            0
        )

        glBindTexture(
            GL_TEXTURE_2D,
            mTexture[0]
        )

        glTexParameteri(
            GL_TEXTURE_2D,
            GL_TEXTURE_WRAP_S,
            GL_CLAMP_TO_EDGE
        )
        glTexParameteri(
            GL_TEXTURE_2D,
            GL_TEXTURE_WRAP_T,
            GL_CLAMP_TO_EDGE
        )
        glTexParameteri(GL_TEXTURE_2D,
            GL_TEXTURE_MAG_FILTER,
            GL_LINEAR)
        glTexParameteri(GL_TEXTURE_2D,
            GL_TEXTURE_MIN_FILTER,
            GL_LINEAR)

        glActiveTexture(
            GL_TEXTURE0
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

        mPixels = IntArray(
            miWidth * miHeight
        )
    }

    override fun onDrawFrame(
        gl: GL10?
    ) {

        GLUtils.texImage2D(
            GL_TEXTURE_2D,
            0,
            bitmap,
            0
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

        glBindTexture(
            GL_TEXTURE_2D,
            mTexture[0]
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

    }
}