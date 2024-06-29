package good.damn.shaderblur.opengl

import android.graphics.Bitmap
import android.opengl.GLES20.*
import android.opengl.GLSurfaceView
import android.util.Log
import good.damn.shaderblur.post_effects.blur.GaussianBlur
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class BlurRenderer : GLSurfaceView.Renderer {

    companion object {
        private const val TAG = "Renderer"
    }

    private var mBlurEffect = GaussianBlur()

    override fun onSurfaceCreated(
        gl: GL10?,
        p1: EGLConfig?
    ) {
        glClearColor(
            1.0f,
            0.0f,
            0.0f,
            1.0f
        )

        glClear(
            GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT
        )

        Log.d(TAG, "onSurfaceCreated: ")
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
        Log.d(TAG, "onSurfaceChanged: $width $height")
        gl?.glViewport(
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
        gl?.glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)
        mBlurEffect.onDrawFrame(
            gl
        )
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