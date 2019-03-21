package net.masonapps.zombiecamera.gallery

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import net.masonapps.zombiecamera.R

private const val ARG_URI = "mediaUri"

class GalleryPageFragment : Fragment() {
    private var uri: Uri? = null

    companion object {
        @JvmStatic
        fun newInstance(uri: Uri) =
            GalleryPageFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_URI, uri.toString())
                }
            }
    }

    private lateinit var viewModel: GalleryViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            uri = Uri.parse(it.getString(ARG_URI))
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.gallery_page_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(GalleryViewModel::class.java)
    }

}
