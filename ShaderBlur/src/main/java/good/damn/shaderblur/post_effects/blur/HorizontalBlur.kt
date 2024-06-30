package good.damn.shaderblur.post_effects.blur

import android.graphics.Bitmap
import android.opengl.GLSurfaceView
import good.damn.shaderblur.opengl.OpenGLUtils
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

import android.opengl.GLES20.*
import android.opengl.GLUtils
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class HorizontalBlur(
    private val mVertexCode: String,
    private val mVertexBuffer: FloatBuffer,
    private val mIndicesBuffer: ByteBuffer
) {

    companion object {
        private const val TAG = "HorizontalBlur"
    }

    private val mFragmentShaderCode = "precision mediump float;" +
            "uniform vec2 u_res;" +
            "uniform sampler2D u_tex;" +

            "float gauss(float inp, float aa, float stDevSQ) {" +
                "return aa * exp(-(inp*inp)/stDevSQ);" +
            "}" +
            "void main () {" +
                "float stDev = 8.0;" +
                "float stDevSQ = 2.0 * stDev * stDev;" +
                "float aa = 0.398 / stDev;" +
                "const float rad = 7.0;" +
                "vec2 crs = vec2(" +
                    "gl_FragCoord.x," +
                    "u_res.y-gl_FragCoord.y);" +
                "vec4 sum = vec4(0.0);" +
                "float normDistSum = 0.0;" +
                "float gt;" +
                "for (float i = -rad; i <= rad;i++) {" +
                    "gt = gauss(i,aa,stDevSQ);" +
                    "normDistSum += gt;" +
                    "sum += texture2D(" +
                        "u_tex," +
                        "vec2(" +
                            "gl_FragCoord.x+i," +
                            "gl_FragCoord.y" +
                        ") / u_res) * gt;" +
                    "}" +
                "gl_FragColor = sum / vec4(normDistSum);" +
            "}"

    private var mProgram: Int = 0

    private val mDepthBuffer = intArrayOf(1)
    private val mFrameBuffer = intArrayOf(1)

    private var mAttrPosition: Int = 0
    private var mUniformSize: Int = 0
    private var mUniformTexture: Int = 0

    private var mfWidth = 1f
    private var mfHeight = 1f

    private var miWidth = 1
    private var miHeight = 1

    fun onSurfaceCreated() {
        mProgram = OpenGLUtils.createProgram(
            mVertexCode,
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

        glGenFramebuffers(
            1,
            mFrameBuffer,
            0
        )

        glGenRenderbuffers(
            1,
            mDepthBuffer,
            0
        )
    }

    fun onSurfaceChanged(
        width: Int,
        height: Int
    ) {
        mfWidth = width.toFloat()
        mfHeight = height.toFloat()

        miWidth = width
        miHeight = height

        val buf = IntArray(width * height)
        val mTexBuffer = ByteBuffer.allocateDirect(
            buf.size * Float.SIZE_BYTES
        ).order(ByteOrder.nativeOrder())
            .asIntBuffer()

        glTexImage2D(
            GL_TEXTURE_2D,
            0, GL_RGB, width, height, 0,
            GL_RGB, GL_UNSIGNED_SHORT_5_6_5, mTexBuffer
        )

        glBindRenderbuffer(
            GL_RENDERBUFFER,
            mDepthBuffer[0]
        )

        glRenderbufferStorage(
            GL_RENDERBUFFER,
            GL_DEPTH_COMPONENT16,
            width,
            height
        )
    }

    fun onDrawFrame(
        bitmap: Bitmap,
        texture: Int
    ) {
        glViewport(
            0,0,
            miWidth, miHeight
        )

        glBindFramebuffer(
            GL_FRAMEBUFFER,
            mFrameBuffer[0]
        )

        glFramebufferTexture2D(
            GL_FRAMEBUFFER,
            GL_COLOR_ATTACHMENT0,
            GL_TEXTURE_2D,
            texture,
            0
        )

        if (glCheckFramebufferStatus(GL_FRAMEBUFFER)
            != GL_FRAMEBUFFER_COMPLETE) {
            Log.d(TAG, "onDrawFrame: FRAME_BUFFER_NOT_COMPLETE")
            return
        }

        glUseProgram(
            mProgram
        )

        glClearColor(0f, 1f, 0f, 1f)
        glClear(GL_DEPTH_BUFFER_BIT or GL_COLOR_BUFFER_BIT)

        glEnableVertexAttribArray(
            mAttrPosition
        )
        glVertexAttribPointer(
            mAttrPosition,
            2,
            GL_FLOAT,
            false,
            8,
            mVertexBuffer
        )

        GLUtils.texImage2D(
            GL_TEXTURE_2D,
            0,
            bitmap,
            0
        )

        glBindTexture(
            GL_TEXTURE_2D,
            texture
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
}