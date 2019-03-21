package net.masonapps.zombiecamera.ar

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import net.masonapps.zombiecamera.TextureAsset

class CameraViewModel : ViewModel() {

    private val _isRecording: MutableLiveData<Boolean> by lazy { MutableLiveData(false) }
    private val _isVideoEnabled: MutableLiveData<Boolean> by lazy { MutableLiveData(false) }
    private val _recentThumbnailUri: MutableLiveData<Uri?> by lazy { MutableLiveData<Uri?>(null) }
    
    private val _eyesAsset: MutableLiveData<TextureAsset?> by lazy { MutableLiveData<TextureAsset?>(null) }
    private val _mouthAsset: MutableLiveData<TextureAsset?> by lazy { MutableLiveData<TextureAsset?>(null) }
    private val _skinAsset: MutableLiveData<TextureAsset?> by lazy { MutableLiveData<TextureAsset?>(null) }
    private val _woundsAsset: MutableLiveData<TextureAsset?> by lazy { MutableLiveData<TextureAsset?>(null) }
    
    val isRecording: LiveData<Boolean> = _isRecording
    val isVideoEnabled: LiveData<Boolean> = _isVideoEnabled
    val recentThumbnailUri: LiveData<Uri?> = _recentThumbnailUri

    val eyesAsset: MutableLiveData<TextureAsset?> = _eyesAsset
    val mouthAsset: MutableLiveData<TextureAsset?> = _mouthAsset
    val skinAsset: MutableLiveData<TextureAsset?> = _skinAsset
    val woundsAsset: MutableLiveData<TextureAsset?> = _woundsAsset

    fun setRecording(recording: Boolean) {
        _isRecording.value = recording and _isVideoEnabled.value!!
    }

    fun toggleVideoEnabled() {
        val currentValue = isVideoEnabled.value!!
        if(currentValue && _isRecording.value!!){
            setRecording(false)
        }
        _isVideoEnabled.value = !currentValue
    }

    fun setRecentThumbnailUri(uri: Uri?) {
        _recentThumbnailUri.postValue(uri)
    }
    
    fun setEyesAsset(asset: TextureAsset){
        _eyesAsset.postValue(asset)
    }
    
    fun setMouthAsset(asset: TextureAsset){
        _mouthAsset.postValue(asset)
    }
    
    fun setSkinAsset(asset: TextureAsset){
        _skinAsset.postValue(asset)
    }
    
    fun setWoundsAsset(asset: TextureAsset){
        _woundsAsset.postValue(asset)
    }
}