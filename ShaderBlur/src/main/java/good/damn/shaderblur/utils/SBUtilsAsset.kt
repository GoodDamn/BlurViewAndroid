package good.damn.shaderblur.utils

import java.io.File
import java.io.FileInputStream
import java.nio.charset.Charset

object SBUtilsAsset {

    private val CHARSET = Charset.forName(
        "UTF-8"
    )

    @JvmStatic
    fun loadString(
        file: File
    ): String {
        val inp = FileInputStream(
            file
        )

        val b = MGUtilsFile.readBytes(
            inp
        )

        inp.close()

        return String(
            b,
            CHARSET
        )
    }

    @JvmStatic
    fun loadString(
        path: String
    ): String {

        if (!pubFile.exists()) {
            throw Exception(pubFile.path)
        }

        return loadString(
            pubFile
        )
    }
}