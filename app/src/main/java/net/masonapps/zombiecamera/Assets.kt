package net.masonapps.zombiecamera

import android.content.res.AssetManager


open class TextureAsset(val assetPath: String, val previewPath: String? = null)

object Assets {

    private const val DIRECTORY_LUTS = "luts"
    private const val DIRECTORY_EYES = "eyes"
    private const val DIRECTORY_MOUTHS = "mouths"
    private const val DIRECTORY_SKINS = "skins"

    const val KEY_EYE = "key-eye"
    const val KEY_MOUTH = "key-mouth"
    const val KEY_SKIN = "key-skin"
    const val PATH_NONE = "none"
    const val DEFAULT_LUT = "$DIRECTORY_LUTS/RobotVision3D16.png"
    const val DEFAULT_EYE = "$DIRECTORY_EYES/template_eyes.png"
    const val DEFAULT_MOUTH = "$DIRECTORY_MOUTHS/template_mouth.png"
    const val DEFAULT_SKIN = "$DIRECTORY_SKINS/skin_scars1.png"
    const val BLENDED_MATERIAL = "sceneform_face_mesh.sfb"
    const val CAMERA_QUAD_MATERIAL = "bottom_layer.sfb"

    private var eyeList: List<TextureAsset>? = null
    private var mouthList: List<TextureAsset>? = null
    private var skinList: List<TextureAsset>? = null

    fun getEyeList(assetManager: AssetManager): List<TextureAsset> {
        if (eyeList == null) {
            eyeList =
                assetManager.list(DIRECTORY_EYES)?.map { TextureAsset("$DIRECTORY_EYES/$it") }?.toList()
        }
        return eyeList!!
    }

    fun getMouthList(assetManager: AssetManager): List<TextureAsset> {
        if (mouthList == null) {
            mouthList =
                assetManager.list(DIRECTORY_MOUTHS)?.map { TextureAsset("$DIRECTORY_MOUTHS/$it") }
                    ?.toList()
        }
        return mouthList!!
    }

    fun getSkinList(assetManager: AssetManager): List<TextureAsset> {
        if (skinList == null) {
            skinList = assetManager.list(DIRECTORY_SKINS)?.map { TextureAsset("$DIRECTORY_SKINS/$it") }
                ?.toList()
        }
        return skinList!!
    }
}