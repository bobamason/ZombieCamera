package net.masonapps.zombiecamera.io

import android.os.Environment
import java.io.File

object ExternalStorage {
    
    private val directory: File by lazy {
        val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "FacePaint")
        if(!dir.exists()) dir.mkdirs()
        return@lazy dir
    }
    
    fun createPhotoFile(): File = File(directory, FilenameGenerator.buildPhotoFilename())
    
    fun createVideoFile(): File = File(directory, FilenameGenerator.buildVideoFilename())
}