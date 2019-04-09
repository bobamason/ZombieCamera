package net.masonapps.zombiecamera.ar

import android.content.ActivityNotFoundException
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.bumptech.glide.Glide
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.fragment_camera_controls.*
import net.masonapps.zombiecamera.R
import net.masonapps.zombiecamera.audio.ButtonSounds

class CameraControlsFragment : Fragment() {

    companion object {
        fun newInstance() = CameraControlsFragment()
    }

    private lateinit var viewModel: CameraViewModel
    private lateinit var cameraFragment: CameraFragment
    private lateinit var buttonSounds: ButtonSounds

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_camera_controls, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        buttonSounds = ButtonSounds()

        if (parentFragment !is CameraFragment) throw IllegalStateException("${CameraControlsFragment::class.java.name} must be a child fragment of ${CameraFragment::class.java.simpleName}")

        cameraFragment = parentFragment!! as CameraFragment
        viewModel = ViewModelProviders.of(cameraFragment).get(CameraViewModel::class.java)

        settingsButton.setOnClickListener {
            cameraFragment.showFaceChooserSheet()
        }

        setupCameraButtons()

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
    }

    override fun onDestroy() {
        buttonSounds.release()
        super.onDestroy()
    }

    private fun setupCameraButtons() {
        cameraButton.setOnClickListener { v ->

            if (!cameraFragment.checkWritePermissions()) {
                return@setOnClickListener
            }

            buttonSounds.playShutterClick()
            cameraFragment.takePhoto()
        }

        recordButton.setOnClickListener { v ->
            if (!cameraFragment.checkWritePermissions()) {
                return@setOnClickListener
            }

            val videoRecorder = cameraFragment.videoRecorder
            if (videoRecorder.isRecording) {
                buttonSounds.playStopVideoRecording()
                videoRecorder.stopRecordingVideo()
                viewModel.setRecording(false)
                videoRecorder.videoPath?.let { file -> cameraFragment.saveVideo(file) }
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
                cameraButton.visibility = View.GONE
                recordButton.visibility = View.VISIBLE
            } else {
                videoToggleButton.setImageResource(R.drawable.ic_video_24dp)
                cameraButton.visibility = View.VISIBLE
                recordButton.visibility = View.GONE
            }
            cameraFragment.animateCameraMode(it)
        })
    }
}