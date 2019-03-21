package net.masonapps.zombiecamera.io

import java.text.SimpleDateFormat
import java.util.*

object FilenameGenerator {
    
    private val dateFormat: SimpleDateFormat by lazy { SimpleDateFormat("yyyyMMddHHmmss", Locale.US) }

    fun buildPhotoFilename(): String {
        return "photo_${dateFormat.format(Date())}.jpg"
    }

    fun buildVideoFilename(): String {
        return "video_${dateFormat.format(Date())}.mp4"
    }
}