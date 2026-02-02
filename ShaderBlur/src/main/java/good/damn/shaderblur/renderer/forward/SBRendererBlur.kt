package good.damn.shaderblur.renderer.forward

import android.graphics.Bitmap
import android.opengl.GLES20.GL_COLOR_BUFFER_BIT
import android.opengl.GLES20.GL_DEPTH_BUFFER_BIT
import android.opengl.GLES20.glClear
import android.opengl.GLES20.glClearColor
import android.opengl.GLES20.glViewport
import android.opengl.GLSurfaceView
import good.damn.shaderblur.builders.SBBlur
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class SBRendererBlur(
    blurRadius: Int,
    blur: SBBlur,
    shadeColor: FloatArray? = null
): GLSurfaceView.Renderer {

    var isFrameDrawn = true

    private val mBlurEffect = SBBlurGaussian(
        blurRadius,
        blur,
        shadeColor
    )

    override fun onSurfaceCreated(
        gl: GL10?,
        p1: EGLConfig?
    ) {
        mBlurEffect.onSurfaceCreated(
            gl,
            p1
        )
    }

    override fun onSurfaceChanged(
        gl: GL10?,
        width: Int,
        height: Int
    ) {
        glViewport(
            0,
            0,
            width,
            height
        )

        mBlurEffect.onSurfaceChanged(
            gl,
            width,
            height
        )
    }

    override fun onDrawFrame(
        gl: GL10?
    ) {
        isFrameDrawn = false
        glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)
        glClearColor(
            1.0f,
            1.0f,
            1.0f,
            1.0f
        )
        mBlurEffect.onDrawFrame(
            gl
        )
        isFrameDrawn = true
    }

    fun requestRender(
        bitmap: Bitmap
    ) {
        mBlurEffect.bitmap = bitmap
    }

    fun clean() {
        mBlurEffect.clean()
    }

}