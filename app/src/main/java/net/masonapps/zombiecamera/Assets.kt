package net.masonapps.zombiecamera

open class BaseAsset(val name: String, val previewResId: Int){
    override fun toString(): String {
        return name
    }
}
class TextureAsset(name: String, previewResId: Int, val textureResId: Int) : BaseAsset(name, previewResId)

object Assets {
    
    const val BLENDED_MATERIAL = "sceneform_face_mesh.sfb"
    const val CAMERA_QUAD_MATERIAL = "bottom_layer.sfb"
    
    val eyeList: Array<TextureAsset> = arrayOf(
        TextureAsset("eyes dead white", R.drawable.ic_placeholder, R.raw.eyes_dead_white)
    )
    
    val mouthList: Array<TextureAsset> = arrayOf(
        TextureAsset("zombie 1", R.drawable.ic_placeholder, R.raw.zombie_test1)
    )
    
    val skinList: Array<TextureAsset> = arrayOf(
        TextureAsset("wounds 1", R.drawable.ic_placeholder, R.raw.wounds_1)
    )
    
    val defaultEye: TextureAsset = eyeList[0]
    val defaultMouth: TextureAsset = mouthList[0]
    val defaultSkin: TextureAsset = skinList[0]
}