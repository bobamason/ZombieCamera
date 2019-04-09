package net.masonapps.zombiecamera

import android.content.res.AssetManager


open class TextureAsset(val assetPath: String, val previewPath: String? = null)

object Assets {

    private const val DIRECTORY_PREVIEW = "preview"
    private const val DIRECTORY_LUTS = "luts"
    private const val DIRECTORY_EYES = "eyes"
    private const val DIRECTORY_MOUTHS = "mouths"
    private const val DIRECTORY_SKINS = "skins"

    const val KEY_LUT = "key-lut"
    const val KEY_EYE = "key-eye"
    const val KEY_MOUTH = "key-mouth"
    const val KEY_SKIN = "key-skin"

    const val PATH_NONE = "none"

    const val DEFAULT_LUT = "$DIRECTORY_LUTS/War3D16.png"
    const val DEFAULT_EYE = "$DIRECTORY_EYES/template_eyes.png"
    const val DEFAULT_MOUTH = "$DIRECTORY_MOUTHS/template_mouth.png"
    const val DEFAULT_SKIN = "$DIRECTORY_SKINS/skin_scars1.png"

    const val BLENDED_MATERIAL = "sceneform_face_mesh.sfb"
    const val CAMERA_QUAD_MATERIAL = "bottom_layer.sfb"

    private var lutList: List<TextureAsset>? = null
    private var eyeList: List<TextureAsset>? = null
    private var mouthList: List<TextureAsset>? = null
    private var skinList: List<TextureAsset>? = null

    fun getAssetList(assetManager: AssetManager, key: String): List<TextureAsset>? = when (key) {
        KEY_LUT -> getLutList(assetManager)
        KEY_EYE -> getEyeList(assetManager)
        KEY_MOUTH -> getMouthList(assetManager)
        KEY_SKIN -> getSkinList(assetManager)
        else -> null
    }

    fun getLutList(assetManager: AssetManager): List<TextureAsset> {
        if (lutList == null) {
            lutList = createAssetList(assetManager, DIRECTORY_LUTS)
        }
        return lutList!!
    }

    fun getEyeList(assetManager: AssetManager): List<TextureAsset> {
        if (eyeList == null) {
            eyeList = createAssetList(assetManager, DIRECTORY_EYES)
        }
        return eyeList!!
    }

    fun getMouthList(assetManager: AssetManager): List<TextureAsset> {
        if (mouthList == null) {
            mouthList = createAssetList(assetManager, DIRECTORY_MOUTHS)
        }
        return mouthList!!
    }

    fun getSkinList(assetManager: AssetManager): List<TextureAsset> {
        if (skinList == null) {
            skinList = createAssetList(assetManager, DIRECTORY_SKINS)
        }
        return skinList!!
    }

    private fun createAssetList(assetManager: AssetManager, directory: String): List<TextureAsset>? {
        return assetManager.list(directory)
            ?.sorted()
            ?.map { TextureAsset("$directory/$it") }
            ?.toList()
    }
}