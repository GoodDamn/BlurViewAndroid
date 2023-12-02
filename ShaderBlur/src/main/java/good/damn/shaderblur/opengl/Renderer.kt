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

    var mWidth = 1f
    var mHeight = 1f

    private var mBlurEffect: GaussianBlur

    init {
        mBlurEffect = GaussianBlur(targetView)
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
        gl?.glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)
        mBlurEffect.draw()
        mOnFrameCompleteListener?.run()
    }

    fun clean() {
        mBlurEffect.clean()
    }

}