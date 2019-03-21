package net.masonapps.zombiecamera.audio

import android.media.MediaActionSound

class ButtonSounds {
    
    private val mediaActionSound = MediaActionSound()
    
    init {
        mediaActionSound.load(MediaActionSound.SHUTTER_CLICK)
        mediaActionSound.load(MediaActionSound.START_VIDEO_RECORDING)
        mediaActionSound.load(MediaActionSound.STOP_VIDEO_RECORDING)
    }
    
    fun playShutterClick(){
        try {
            mediaActionSound.play(MediaActionSound.SHUTTER_CLICK)
        } catch (t: Throwable){
            t.printStackTrace()
        }
    }
    
    fun playStartVideoRecording(){
        try {
            mediaActionSound.play(MediaActionSound.START_VIDEO_RECORDING)
        } catch (t: Throwable){
            t.printStackTrace()
        }
    }
    
    fun playStopVideoRecording(){
        try {
            mediaActionSound.play(MediaActionSound.STOP_VIDEO_RECORDING)
        } catch (t: Throwable){
            t.printStackTrace()
        }
    }
    
    fun release(){
        try {
            mediaActionSound.release()
        } catch (t: Throwable){
            t.printStackTrace()
        }
    }
}