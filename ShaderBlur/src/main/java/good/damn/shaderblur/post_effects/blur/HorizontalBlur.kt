package good.damn.shaderblur.post_effects.blur

import good.damn.shaderblur.opengl.OpenGLUtils

import android.opengl.GLES20.*
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class HorizontalBlur(
    vertexCode: String,
    scaleFactor: Float,
    blurRadius: Int,
    vertexBuffer: FloatBuffer,
    indicesBuffer: ByteBuffer,
): PostProcessEffect(
    vertexCode,
    fragmentCode = "precision mediump float;" +
            "uniform vec2 u_res;" +
            "uniform sampler2D u_tex;" +

            "float gauss(float inp, float aa, float stDevSQ) {" +
            "return aa * exp(-(inp*inp)/stDevSQ);" +
            "}" +
            "void main () {" +
            "float stDev = 8.0;" +
            "float stDevSQ = 2.0 * stDev * stDev;" +
            "float aa = 0.398 / stDev;" +
            "const float rad = $blurRadius.0;" +
            "vec4 sum = vec4(0.0);" +
            "float normDistSum = 0.0;" +
            "float gt;" +
            "vec2 offset = vec2(gl_FragCoord.x - rad, gl_FragCoord.y);" +
            "for (float i = -rad; i <= rad;i++) {" +
            "gt = gauss(i,aa,stDevSQ);" +
            "normDistSum += gt;" +
            "offset.x++;" +
            "sum += texture2D(" +
            "u_tex," +
            "offset / u_res) * gt;" +
            "}" +
            "gl_FragColor = sum / vec4(normDistSum);" +
            "}",
    scaleFactor,
    vertexBuffer,
    indicesBuffer
) {
    companion object {
        private const val TAG = "HorizontalBlur"
    }
}