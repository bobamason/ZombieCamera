package net.masonapps.zombiecamera.ar


import android.animation.ValueAnimator
import android.content.ActivityNotFoundException
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.CamcorderProfile
import android.media.MediaMetadataRetriever
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.PixelCopy
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.bumptech.glide.Glide
import com.google.android.material.snackbar.Snackbar
import com.google.ar.core.AugmentedFace
import com.google.ar.core.TrackingState
import com.google.ar.sceneform.ArSceneView
import com.google.ar.sceneform.FrameTime
import com.google.ar.sceneform.Scene
import com.google.ar.sceneform.math.Matrix
import com.google.ar.sceneform.rendering.CameraStream
import com.google.ar.sceneform.rendering.ExternalTexture
import com.google.ar.sceneform.rendering.Material
import com.google.ar.sceneform.rendering.ModelRenderable
import kotlinx.android.synthetic.main.fragment_camera.*
import net.masonapps.zombiecamera.Assets
import net.masonapps.zombiecamera.R
import net.masonapps.zombiecamera.audio.ButtonSounds
import net.masonapps.zombiecamera.io.BitmapUtils
import net.masonapps.zombiecamera.io.ExternalStorage
import net.masonapps.zombiecamera.settings.FaceChooserFragment
import net.masonapps.zombiecamera.videorecording.VideoRecorder
import java.io.File
import java.util.*
import java.util.concurrent.CompletableFuture
import kotlin.collections.HashMap

private const val FRAGMENT_FACES = "fragment-faces"
private const val FIELD_CAMERA_STREAM = "cameraStream"
private const val FIELD_CAMERA_TEXTURE = "cameraTexture"
private const val RC_IMAGE = 222

class CameraFragment : Fragment() {

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

    private val transformedUvCoords = uvCoords.copyOf()
    private val projectionMatrix = Matrix()
    private lateinit var videoRecorder: VideoRecorder
    private lateinit var viewModel: CameraViewModel
    private lateinit var buttonSounds: ButtonSounds

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_camera, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProviders.of(activity!!).get(CameraViewModel::class.java)
        buttonSounds = ButtonSounds()

        val arFragment = childFragmentManager.findFragmentById(R.id.ar_fragment) as WritingArFragment

        arSceneView = arFragment.arSceneView
        scene = arSceneView.scene

        videoRecorder = VideoRecorder()
        val orientation = resources.configuration.orientation
        videoRecorder.setVideoQuality(CamcorderProfile.QUALITY_1080P, orientation)
        videoRecorder.setSceneView(arSceneView)

        settingsButton.setOnClickListener {
            showFaceChooserSheet()
        }

        cameraButton.setOnClickListener { v ->
            if (!arFragment.hasWritePermission()) {
                arFragment.launchPermissionSettings()
                Snackbar.make(
                    v,
                    "Photo and video recording requires the WRITE EXTERNAL STORAGE permission.",
                    Snackbar.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }

            buttonSounds.playShutterClick()
            takePhoto()
        }

        recordButton.setOnClickListener { v ->
            if (!arFragment.hasWritePermission()) {
                arFragment.launchPermissionSettings()
                Snackbar.make(
                    v,
                    "Photo and video recording requires the WRITE EXTERNAL STORAGE permission.",
                    Snackbar.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }

            if (videoRecorder.isRecording) {
                buttonSounds.playStopVideoRecording()
                videoRecorder.stopRecordingVideo()
                viewModel.setRecording(false)
                videoRecorder.videoPath?.let { file -> saveVideo(file) }
            } else {
                val recording = videoRecorder.startRecordingVideo()
                if (recording) buttonSounds.playStartVideoRecording()
                viewModel.setRecording(recording)
            }
        }

        videoToggleButton.setOnClickListener {
            viewModel.toggleVideoEnabled()
        }

        thumbnailButton.setOnClickListener {
            val uri = viewModel.recentThumbnailUri.value ?: return@setOnClickListener
            val intent = Intent(Intent.ACTION_VIEW, uri)
            try {
                startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                Snackbar.make(
                    it,
                    "Sorry the file cannot be opened.",
                    Snackbar.LENGTH_SHORT
                ).show()
            }
        }

        viewModel.isRecording.observe(viewLifecycleOwner, Observer {
            if (it) {
                recordButton.setImageResource(R.drawable.ic_stop_36dp)
                recordButton.setBackgroundResource(R.drawable.ic_stop_btn_bg)
            } else {
                recordButton.setImageResource(R.drawable.ic_record_24dp)
                recordButton.setBackgroundResource(R.drawable.ic_record_btn_bg)
            }
        })
        viewModel.isVideoEnabled.observe(viewLifecycleOwner, Observer {
            if (it) {
                videoToggleButton.setImageResource(R.drawable.ic_photo_24dp)
                imageCameraMode.setImageResource(R.drawable.ic_video_48dp)
                cameraButton.visibility = View.GONE
                recordButton.visibility = View.VISIBLE
            } else {
                videoToggleButton.setImageResource(R.drawable.ic_video_24dp)
                imageCameraMode.setImageResource(R.drawable.ic_photo_48dp)
                cameraButton.visibility = View.VISIBLE
                recordButton.visibility = View.GONE
            }
            animateCameraMode()
        })

        viewModel.recentThumbnailUri.observe(viewLifecycleOwner, Observer { uri ->
            if (uri != null) {
                thumbnailButton.visibility = View.VISIBLE
                thumbnailButton.setImageDrawable(null)
                val context = context ?: return@Observer
                val contentResolver = context.contentResolver
                val type = contentResolver.getType(uri)
                when {
                    type == null -> return@Observer
                    type.contains("image", true) -> {
                        Glide.with(context)
                            .load(uri)
                            .into(thumbnailButton)
                    }
                    type.contains("video", true) -> {
                        val retriever = MediaMetadataRetriever()
                        retriever.setDataSource(context, uri)
                        val bitmap = retriever.getFrameAtTime(-1)
                        Glide.with(context)
                            .load(bitmap)
                            .into(thumbnailButton)
                    }
                }
            } else {
                thumbnailButton.visibility = View.GONE
            }
        })

        buttonImport.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            context?.packageManager?.resolveActivity(intent, PackageManager.MATCH_ALL)?.also {
                startActivityForResult(intent, RC_IMAGE)
            }
        }

        loadMaterials()
        setupScene()
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

    override fun onDestroy() {
        buttonSounds.release()
        super.onDestroy()
    }

    private fun animateCameraMode() {
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

    private fun saveVideo(file: File) {
        val videoPath = file.absolutePath
        //                            Snackbar.make(v, "Video saved: $videoPath", Snackbar.LENGTH_SHORT).show()
        Log.d("CameraFragment", "Video saved: $videoPath")

        // Send  notification of updated content.
        val values = ContentValues()
        values.put(MediaStore.Video.Media.TITLE, "Clown")
        values.put(MediaStore.Video.Media.DESCRIPTION, "")
        values.put(MediaStore.Video.Media.DATE_TAKEN, System.currentTimeMillis())
        values.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
        values.put(MediaStore.Video.Media.DATA, videoPath)
        val uri = context?.contentResolver?.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
        viewModel.setRecentThumbnailUri(uri)
    }

    private fun takePhoto() {
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

    private fun loadMaterials() {
        val ctx = context!!
        faceOverlaySurface = FaceOverlaySurface(ctx)
        faceOverlaySurface.eyeAsset = Assets.defaultEye
        faceOverlaySurface.skinAsset = Assets.defaultSkin
        faceOverlaySurface.updateSurfaceTexture()

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

        CompletableFuture.allOf(cameraQuadMaterialFuture, blendedMaterialFuture)
            .handle { _, throwable ->
                if (throwable != null) {
                    Log.e("CameraFragment", "failed to load material ${throwable.localizedMessage}")
                    throwable.printStackTrace()
                }

                cameraQuadMaterial = cameraQuadMaterialFuture.get()?.material
                cameraQuadNode.material = cameraQuadMaterial
                cameraQuadNode.setParent(scene)
                setupCameraTexture()
                
                blendedFaceMaterial = blendedMaterialFuture.get()?.material

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

            arSceneView.arFrame?.let { cameraQuadNode.updateProjection(it) }


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

    private fun setupCameraTexture() {
        val cameraTexture: ExternalTexture? = getCameraTexture()
        cameraTexture?.let { cameraQuadMaterial?.setExternalTexture("exTexture", it) }
    }

    private fun getCameraTexture(): ExternalTexture? {
        val cameraStreamField = arSceneView.javaClass.getDeclaredField(FIELD_CAMERA_STREAM)
        cameraStreamField.isAccessible = true
        val cameraStream: CameraStream? = cameraStreamField.get(arSceneView) as CameraStream
        val cameraTextureField = cameraStream?.javaClass?.getDeclaredField(FIELD_CAMERA_TEXTURE)
        cameraTextureField?.apply { isAccessible = true }
        return cameraTextureField?.get(cameraStream) as ExternalTexture
    }

    private fun showFaceChooserSheet() {
        childFragmentManager.apply {
            val ft = beginTransaction()
            val prev = findFragmentByTag(FRAGMENT_FACES)
            if (prev != null) {
                ft.remove(prev)
            }
            FaceChooserFragment.newInstance().show(ft, FRAGMENT_FACES)
        }
    }
}
