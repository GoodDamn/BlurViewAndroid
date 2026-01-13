package good.damn.shaderblur.shaders

import android.opengl.GLES30
import java.util.LinkedList

class SBBinderAttribute private constructor(
    private val locations: List<SBMBinderLocation>
) {

    fun bindAttributes(
        program: Int
    ) {
        locations.forEach {
            GLES30.glBindAttribLocation(
                program,
                it.location,
                it.name
            )
        }
    }

    class Builder {

        private val list = LinkedList<
            SBMBinderLocation
        >()

        fun bindPosition(): Builder {
            list.add(
                SBMBinderLocation(
                    "position",
                    0
                )
            )
            return this
        }

        fun bindTextureCoordinates(): Builder {
            list.add(
                SBMBinderLocation(
                    "texCoord",
                    1
                )
            )
            return this
        }

        fun build() = SBBinderAttribute(
            list
        )
    }

    private data class SBMBinderLocation(
        val name: String,
        val location: Int
    )
}