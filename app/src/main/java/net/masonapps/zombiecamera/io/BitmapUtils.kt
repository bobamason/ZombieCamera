package net.masonapps.zombiecamera.io

import android.graphics.Bitmap
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

object BitmapUtils {

    fun saveBitmapToFile(bitmap: Bitmap, file: File) {
        val outputStream = FileOutputStream(file)
        outputStream.use {
            val outData = ByteArrayOutputStream()
            outData.use {
                val format =
                    if (file.extension.equals("png", true)) Bitmap.CompressFormat.PNG
                    else Bitmap.CompressFormat.JPEG
                bitmap.compress(format, 100, outData)
                outData.writeTo(outputStream)
            }
        }
    }
}