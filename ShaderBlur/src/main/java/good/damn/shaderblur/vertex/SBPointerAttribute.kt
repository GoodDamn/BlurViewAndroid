package good.damn.shaderblur.vertex

import android.opengl.GLES30.GL_FLOAT
import android.opengl.GLES30.glEnableVertexAttribArray
import android.opengl.GLES30.glVertexAttribPointer
import java.util.LinkedList

class SBPointerAttribute private constructor(
    private val pointers: List<SBMPointerAttribute>,
    private val stride: Int
) {
    fun bindPointers() {
        pointers.forEach {
            glEnableVertexAttribArray(
                it.attrib
            )

            glVertexAttribPointer(
                it.attrib,
                it.size,
                GL_FLOAT,
                false,
                stride,
                it.offset
            )
        }
    }

    class Builder {
        private val list = LinkedList<
            SBMPointerAttribute
        >()

        private var mOffset = 0
        fun pointPosition2(): Builder {
            list.add(
                SBMPointerAttribute(
                    0,
                    mOffset,
                    2
                )
            )
            mOffset += 2 * 4
            return this
        }

        fun build() = SBPointerAttribute(
            list,
            mOffset
        )
    }

    private data class SBMPointerAttribute(
        val attrib: Int,
        val offset: Int,
        val size: Int
    )
}