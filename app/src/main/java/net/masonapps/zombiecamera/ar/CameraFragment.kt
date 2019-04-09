package net.masonapps.zombiecamera.ar


import android.animation.ValueAnimator
import android.content.ContentValues
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.media.CamcorderProfile
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.preference.PreferenceManager
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.PixelCopy
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.google.android.material.snackbar.Snackbar
import com.google.ar.core.AugmentedFace
import com.google.ar.core.TrackingState
import com.google.ar.sceneform.ArSceneView
import com.google.ar.sceneform.FrameTime
import com.google.ar.sceneform.Scene
import com.google.ar.sceneform.rendering.*
import kotlinx.android.synthetic.main.fragment_camera.*
import net.masonapps.zombiecamera.Assets
import net.masonapps.zombiecamera.R
import net.masonapps.zombiecamera.io.BitmapUtils
import net.masonapps.zombiecamera.io.ExternalStorage
import net.masonapps.zombiecamera.settings.AssetListFragment
import net.masonapps.zombiecamera.videorecording.VideoRecorder
import java.io.File
import java.util.*
import java.util.concurrent.CompletableFuture
import kotlin.collections.HashMap

private const val FRAGMENT_FACES = "fragment-faces"
private const val FRAGMENT_CONTROLS = "fragment-controls"
private const val FIELD_CAMERA_STREAM = "cameraStream"
private const val FIELD_CAMERA_TEXTURE = "cameraTexture"
private const val RC_IMAGE = 222

class CameraFragment : Fragment() {

    private lateinit var arFragment: WritingArFragment
    private lateinit var arSceneView: ArSceneView
    private lateinit var scene: Scene
    private lateinit var faceOverlaySurface: FaceOverlaySurface

    private var cameraQuadMaterial: Material? = null
    private var blendedFaceMaterial: Material? = null

    private val faceNodeMap: HashMap<AugmentedFace, FaceOverlayNode> = HashMap()
    private val uvCoords = floatArrayOf(
        -1f, -1f,
        -1f, 1f,
        1f, -1f,
        1f, 1f
    )
    private val cameraQuadNode: CameraQuadNode by lazy { CameraQuadNode() }

    lateinit var videoRecorder: VideoRecorder
    private lateinit var viewModel: CameraViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_camera, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(CameraViewModel::class.java)

        arFragment = childFragmentManager.findFragmentById(R.id.ar_fragment) as WritingArFragment

        arSceneView = arFragment.arSceneView
        scene = arSceneView.scene
        faceOverlaySurface = FaceOverlaySurface(context!!)

        videoRecorder = VideoRecorder()
        val orientation = resources.configuration.orientation
        videoRecorder.setVideoQuality(CamcorderProfile.QUALITY_1080P, orientation)
        videoRecorder.setSceneView(arSceneView)

        showCameraControls()

        setupFaceTexturePreferences()


        loadMaterials()
        setupScene()
    }

    private fun setupFaceTexturePreferences() {
        viewModel.eyesAsset.observe(viewLifecycleOwner, Observer { asset ->
            Log.d("CameraFragment", "eye path = ${asset?.assetPath}")
            faceOverlaySurface.eyeAsset = asset
            faceOverlaySurface.updateSurfaceTexture()
            saveToPreferences { it.putString(Assets.KEY_EYE, asset?.assetPath ?: Assets.PATH_NONE) }
        })

        viewModel.mouthAsset.observe(viewLifecycleOwner, Observer { asset ->
            Log.d("CameraFragment", "mouth path = ${asset?.assetPath}")
            faceOverlaySurface.mouthAsset = asset
            faceOverlaySurface.updateSurfaceTexture()
            saveToPreferences { it.putString(Assets.KEY_MOUTH, asset?.assetPath ?: Assets.PATH_NONE) }
        })

        viewModel.skinAsset.observe(viewLifecycleOwner, Observer { asset ->
            Log.d("CameraFragment", "skin path = ${asset?.assetPath}")
            faceOverlaySurface.skinAsset = asset
            faceOverlaySurface.updateSurfaceTexture()
            saveToPreferences { it.putString(Assets.KEY_SKIN, asset?.assetPath ?: Assets.PATH_NONE) }
        })

        context?.also { ctx ->
            val prefs = PreferenceManager.getDefaultSharedPreferences(ctx)
            val eyePath = prefs.getString(Assets.KEY_EYE, Assets.DEFAULT_EYE)
            Assets.getEyeList(ctx.assets).find { it.assetPath == eyePath }?.let { viewModel.setEyesAsset(it) }

            val mouthPath = prefs.getString(Assets.KEY_MOUTH, Assets.DEFAULT_MOUTH)
            Assets.getMouthList(ctx.assets).find { it.assetPath == mouthPath }?.let { viewModel.setMouthAsset(it) }

            val skinPath = prefs.getString(Assets.KEY_SKIN, Assets.DEFAULT_SKIN)
            Assets.getSkinList(ctx.assets).find { it.assetPath == skinPath }?.let { viewModel.setSkinAsset(it) }
        }
    }

//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
//        if (requestCode == RC_IMAGE && resultCode == Activity.RESULT_OK) {
//            val uri = data?.data ?: return
//            val key = uri.hashCode()
//            Texture.builder().setSource {
//                context?.contentResolver?.openInputStream(uri)
//            }.build().thenAccept {
//            }
//        }
//    }

    override fun onPause() {
        if (videoRecorder.isRecording) {
            viewModel.setRecording(false)
        }
        super.onPause()
    }

    fun animateCameraMode(isVideoEnabled: Boolean) {
        if (isVideoEnabled)
            imageCameraMode.setImageResource(R.drawable.ic_video_48dp)
        else
            imageCameraMode.setImageResource(R.drawable.ic_photo_48dp)
        
        val animator = ValueAnimator.ofFloat(0f, 1f, 1f, 1f, 0f)
        animator.interpolator = LinearInterpolator()
        animator.duration = 600L
        animator.addUpdateListener {
            val value = it.animatedValue as Float
            imageCameraMode.alpha = value
        }
        animator.doOnStart {
            imageCameraMode.alpha = 0f
            imageCameraMode.visibility = View.VISIBLE
        }
        animator.doOnEnd {
            imageCameraMode.alpha = 0f
            imageCameraMode.visibility = View.GONE
        }
        animator.start()
    }

    fun saveVideo(file: File) {
        val videoPath = file.absolutePath
        //                            Snackbar.make(v, "Video saved: $videoPath", Snackbar.LENGTH_SHORT).show()
        Log.d("CameraFragment", "Video saved: $videoPath")

        // Send  notification of updated content.
        val values = ContentValues()
        values.put(MediaStore.Video.Media.TITLE, "Zombie")
        values.put(MediaStore.Video.Media.DESCRIPTION, "")
        values.put(MediaStore.Video.Media.DATE_TAKEN, System.currentTimeMillis())
        values.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
        values.put(MediaStore.Video.Media.DATA, videoPath)
        val uri = context?.contentResolver?.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
        viewModel.setRecentThumbnailUri(uri)
    }

    fun takePhoto() {
        val bitmap = Bitmap.createBitmap(arSceneView.width, arSceneView.height, Bitmap.Config.ARGB_8888)
        val handlerThread = HandlerThread("PhotoThread")
        handlerThread.start()
        PixelCopy.request(arSceneView, bitmap, { result ->
            if (result == PixelCopy.SUCCESS) {
                val photoFile = ExternalStorage.createPhotoFile()
                BitmapUtils.saveBitmapToFile(bitmap, photoFile)
                // Send  notification of updated content.
                val values = ContentValues()
                values.put(MediaStore.Images.Media.TITLE, "Zombie")
                values.put(MediaStore.Images.Media.DESCRIPTION, "")
                values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis())
                values.put(MediaStore.Images.Media.BUCKET_ID, photoFile.toString().toLowerCase(Locale.US).hashCode())
                values.put(MediaStore.Images.Media.BUCKET_DISPLAY_NAME, photoFile.name.toLowerCase(Locale.US))
                values.put(MediaStore.Images.Media.DATA, photoFile.absolutePath)
                val uri = context?.contentResolver?.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                viewModel.setRecentThumbnailUri(uri)
            }
            handlerThread.quitSafely()
        }, Handler(handlerThread.looper))
    }

    fun checkWritePermissions(): Boolean {
        if (!arFragment.hasWritePermission()) {
            arFragment.launchPermissionSettings()
            Snackbar.make(
                view!!,
                "Photo and video recording requires the WRITE EXTERNAL STORAGE permission.",
                Snackbar.LENGTH_LONG
            ).show()
            return false
        }
        return true
    }

    private fun loadMaterials() {
        val ctx = context!!

        val cameraQuadMaterialFuture = ModelRenderable.builder()
            .setSource(ctx) {
                ctx.assets.open(Assets.CAMERA_QUAD_MATERIAL)
            }
            .build()

        val blendedMaterialFuture = ModelRenderable.builder()
            .setSource(ctx) {
                ctx.assets.open(Assets.BLENDED_MATERIAL)
            }
            .build()

        val lutFuture = Texture.builder()
            .setSampler(
                Texture.Sampler
                    .builder()
                    .setMinFilter(Texture.Sampler.MinFilter.LINEAR)
                    .setMagFilter(Texture.Sampler.MagFilter.LINEAR)
                    .setWrapMode(Texture.Sampler.WrapMode.CLAMP_TO_EDGE)
                    .build()
            )
            .setSource {
                ctx.assets.open(Assets.DEFAULT_LUT)
            }
            .build()

        CompletableFuture.allOf(cameraQuadMaterialFuture, blendedMaterialFuture, lutFuture)
            .handle { _, throwable ->
                if (throwable != null) {
                    Log.e("CameraFragment", "failed to load material ${throwable.localizedMessage}")
                    throwable.printStackTrace()
                }

                cameraQuadMaterial = cameraQuadMaterialFuture.get()?.material
                val lut = lutFuture.get()
                lut?.let { cameraQuadMaterial?.setTexture("lut", it) }
                cameraQuadMaterial?.setFloat("lutResolution", 16f)

//                cameraQuadMaterial?.let { getCameraStream()?.setCameraMaterial(it)}
//                setupCameraTexture()

                cameraQuadNode.material = cameraQuadMaterial
                cameraQuadNode.setParent(scene)
                setupCameraTexture()

                blendedFaceMaterial = blendedMaterialFuture.get()?.material
                lut?.let { blendedFaceMaterial?.setTexture("lut", it) }
                blendedFaceMaterial?.setFloat("lutResolution", 16f)

                blendedFaceMaterial?.setExternalTexture("exTexture", faceOverlaySurface.externalTexture)

                return@handle true
            }
    }

    private fun setupScene() {
        scene.addOnUpdateListener { frameTime: FrameTime ->
            if (blendedFaceMaterial == null) return@addOnUpdateListener

//            val cameraTextureIdField = arSceneView.javaClass.getDeclaredField("cameraTextureId")
//            cameraTextureIdField.isAccessible = true
//            val cameraTextureId = cameraTextureIdField.getInt(arSceneView)

            arSceneView.arFrame?.let { cameraQuadNode.updateFrame(it) }


            val faceList: Collection<AugmentedFace> =
                arSceneView.session?.getAllTrackables(AugmentedFace::class.java) ?: emptyList()

            // Make new AugmentedFaceNodes for any new faces.
            for (face in faceList) {
                if (!faceNodeMap.containsKey(face)) {
                    val faceNode = FaceOverlayNode(face)
                    faceNode.setParent(scene)
                    faceNode.faceMeshMaterial = blendedFaceMaterial
                    faceNodeMap[face] = faceNode
                }
            }

            // Remove any AugmentedFaceNodes associated with an AugmentedFace that stopped tracking.
            val iter: Iterator<Map.Entry<AugmentedFace, FaceOverlayNode>> = faceNodeMap.entries.iterator()
            while (iter.hasNext()) {
                val entry: Map.Entry<AugmentedFace, FaceOverlayNode> = iter.next()
                val face = entry.key
                if (face.trackingState == TrackingState.STOPPED) {
                    val faceNode = entry.value
                    faceNode.setParent(null)
                    faceNodeMap.remove(face)
                }
            }

        }
    }

    private inline fun saveToPreferences(action: (SharedPreferences.Editor) -> Unit) {
        context?.also {
            PreferenceManager.getDefaultSharedPreferences(it).edit(false, action)
        }
    }

    private fun setupCameraTexture() {
        val cameraTexture: ExternalTexture? = getCameraTexture()
        cameraTexture?.let { cameraQuadMaterial?.setExternalTexture("cameraTexture", it) }
    }

    private fun getCameraStream(): CameraStream? {
        val cameraStreamField = arSceneView.javaClass.getDeclaredField(FIELD_CAMERA_STREAM)
        cameraStreamField.isAccessible = true
        return cameraStreamField.get(arSceneView) as CameraStream
    }


    private fun getCameraTexture(): ExternalTexture? {
        val cameraStream = getCameraStream()
        val cameraTextureField = cameraStream?.javaClass?.getDeclaredField(FIELD_CAMERA_TEXTURE)
        cameraTextureField?.apply { isAccessible = true }
        return cameraTextureField?.get(cameraStream) as ExternalTexture
    }

    fun showCameraControls() {
        childFragmentManager.apply {
            val ft = beginTransaction()
            val prev = findFragmentByTag(FRAGMENT_CONTROLS)
            if (prev != null) {
                ft.remove(prev)
            }
            ft.replace(R.id.controlsContainer, CameraControlsFragment.newInstance(), FRAGMENT_CONTROLS)
            ft.commit()
        }
    }

    fun showFaceChooserSheet() {
        childFragmentManager.apply {
            val ft = beginTransaction()
            val prev = findFragmentByTag(FRAGMENT_FACES)
            if (prev != null) {
                ft.remove(prev)
            }
            ft.addToBackStack(FRAGMENT_FACES)
            ft.replace(R.id.controlsContainer, AssetListFragment.newInstance(Assets.KEY_SKIN), FRAGMENT_FACES)
            ft.commit()
        }
    }
}
