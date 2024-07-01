package good.damn.shaderblur.post_effects.blur

import android.hardware.camera2.params.Face
import android.opengl.GLES20.*
import android.util.Log
import good.damn.shaderblur.opengl.OpenGLUtils
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class VerticalBlur(
    vertexCode: String,
    blurRadius: Int,
    scaleFactor: Float,
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
        "vec2 offset = vec2(gl_FragCoord.x, gl_FragCoord.y - rad);" +
        "for (float i = -rad; i <= rad;i++) {" +
            "gt = gauss(i,aa,stDevSQ);" +
            "normDistSum += gt;" +
            "offset.y++;" +
            "sum += texture2D(u_tex, offset/u_res) * gt;" +
        "}" +
        "gl_FragColor = sum / vec4(normDistSum);" +
    "}",
    scaleFactor,
    vertexBuffer,
    indicesBuffer
) {

    companion object {
        private const val TAG = "VerticalBlur"
    }


}