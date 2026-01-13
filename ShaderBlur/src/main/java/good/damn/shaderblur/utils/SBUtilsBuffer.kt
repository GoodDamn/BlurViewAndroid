package good.damn.shaderblur.utils

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

object SBUtilsBuffer {

    private val BYTE_ORDER = ByteOrder.nativeOrder()

    @JvmStatic
    fun allocateByte(
        size: Int
    ) = ByteBuffer.allocateDirect(
        size
    ).order(
        BYTE_ORDER
    )

    @JvmStatic
    fun allocateFloat(
        size: Int
    ) = ByteBuffer.allocateDirect(
        size * 4
    ).order(
        BYTE_ORDER
    ).asFloatBuffer()

    @JvmStatic
    fun createByte(
        i: ByteArray
    ) = allocateByte(
        i.size
    ).put(i).apply {
        position(0)
    }

    @JvmStatic
    fun createFloat(
        i: FloatArray
    ) = allocateFloat(
        i.size
    ).put(i).apply {
        position(0)
    }
}