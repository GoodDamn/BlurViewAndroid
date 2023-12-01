package good.damn.shaderblur.opengl

import android.opengl.GLES20.*
import android.opengl.GLSurfaceView
import android.util.Log
import android.view.View
import good.damn.shaderblur.post.GaussianBlur
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class Renderer(targetView: View) : GLSurfaceView.Renderer {

    private val TAG = "OpenGLRendererBlur"

    var mOnFrameCompleteListener: Runnable? = null

    private val mCOORDINATES_PER_VERTEX = 3 // number of coords
    private val mVertexOffset = mCOORDINATES_PER_VERTEX * 4 // (x,y,z) vertex per 4 bytes

    private lateinit var mVertexBuffer: FloatBuffer
    private lateinit var mIndicesBuffer: ShortBuffer

    private val mIndices: ShortArray = shortArrayOf(0, 1, 2, 0, 2, 3)

    private val mSquareCoords: FloatArray = floatArrayOf(
        -1.0f, 1.0f,  // top left
        -1.0f, -1.0f, // bottom left
        1.0f, -1.0f,  // bottom right
        1.0f, 1.0f,   // top right
    )

    private val mVertexShaderCode =
        "attribute vec4 position;" +
        "void main() {" +
            "gl_Position = position;" +
        "}"

    private val mFragmentShaderCode =
        "precision mediump float;" +
        "uniform vec2 u_res;" +
        "uniform sampler2D u_tex;" +
        "void main () {" +
            "vec2 crs = vec2(gl_FragCoord.x, u_res.y-gl_FragCoord.y);" +
            "gl_FragColor = texture2D(u_tex, vec2(crs.x,crs.y) / u_res);" +
        "}"

    private var mGlProgram = 0

    private var mWidth = 1f
    private var mHeight = 1f

    private val mBlurEffect = GaussianBlur()

    init {
        mBlurEffect.targetView = targetView
    }

    override fun onSurfaceCreated(gl: GL10?, p1: EGLConfig?) {
        gl?.glClearColor(1.0f, 0.0f, 0.0f, 1.0f)
        Log.d(TAG, "onSurfaceCreated: ")

        val byteBuffer =
            ByteBuffer.allocateDirect(mSquareCoords.size * 4)
        byteBuffer.order(ByteOrder.nativeOrder())
        mVertexBuffer = byteBuffer.asFloatBuffer()
        mVertexBuffer.put(mSquareCoords)
        mVertexBuffer.position(0)

        val drawByteBuffer: ByteBuffer =
            ByteBuffer.allocateDirect(mIndices.size * 2)
        drawByteBuffer.order(ByteOrder.nativeOrder())
        mIndicesBuffer = drawByteBuffer.asShortBuffer()
        mIndicesBuffer.put(mIndices)
        mIndicesBuffer.position(0)

        mGlProgram = glCreateProgram()
        glAttachShader(mGlProgram, OpenGLUtils
            .loadShader(GL_FRAGMENT_SHADER,
                mFragmentShaderCode)
        )

        glAttachShader(mGlProgram, OpenGLUtils
            .loadShader(GL_VERTEX_SHADER,
                mVertexShaderCode)
        )

        glLinkProgram(mGlProgram)

        mBlurEffect.create(
            mVertexBuffer,
            mIndicesBuffer,
            mVertexShaderCode
        )
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        Log.d(TAG, "onSurfaceChanged: $width $height")
        gl?.glViewport(0, 0, width, height)
        mWidth = width.toFloat()
        mHeight = height.toFloat()

        mBlurEffect.layout(
            width,
            height)
    }

    override fun onDrawFrame(gl: GL10?) {
        gl?.glClear(GL_COLOR_BUFFER_BIT)
        mBlurEffect.draw()

        glBindFramebuffer(GL_FRAMEBUFFER, 0) // default fbo

        glActiveTexture(GL_TEXTURE0)
        glBindTexture(GL_TEXTURE_2D, mBlurEffect.texture[0])

        glViewport(0,0, mWidth.toInt(), mHeight.toInt())
        glClearColor(1f,1f,0f,1f)
        glClear(GL_DEPTH_BUFFER_BIT or GL_COLOR_BUFFER_BIT)

        glUseProgram(mGlProgram)


        val positionHandle = glGetAttribLocation(mGlProgram, "position")
        glEnableVertexAttribArray(positionHandle)

        glVertexAttribPointer(
            positionHandle,
            2,
            GL_FLOAT,
            false,
            8,
            mVertexBuffer
        )


        glUniform1i(glGetUniformLocation(mGlProgram, "u_tex"), 0)

        glUniform2f(
            glGetUniformLocation(mGlProgram, "u_res"),
            mWidth,
            mHeight
        )

        glDrawElements(
            GL_TRIANGLES,
            mIndicesBuffer.capacity(), // rect
            GL_UNSIGNED_SHORT,
            mIndicesBuffer
        )
        glDisableVertexAttribArray(positionHandle)

        mOnFrameCompleteListener?.run()
    }

    fun clean() {
        mBlurEffect.clean()
    }

}