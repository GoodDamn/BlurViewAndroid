package good.damn.shaderblur.builders

import good.damn.shaderblur.models.SBMBlur
import java.util.LinkedList

class SBBlur private constructor(
    internal val list: LinkedList<
        SBMBlur
    >
) {
    class Builder {
        private val list = LinkedList<
                SBMBlur
        >()

        fun addIteration(
            scaleFactor: Float
        ) = apply {
            list.add(
                SBMBlur(
                    scaleFactor
                )
            )
        }

        fun build() = SBBlur(
            list
        )
    }
}