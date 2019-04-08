package net.masonapps.zombiecamera.settings


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import net.masonapps.zombiecamera.Assets
import net.masonapps.zombiecamera.R
import net.masonapps.zombiecamera.ar.CameraViewModel

/**
 *
 */
class FaceChooserFragment : BottomSheetDialogFragment() {

    private lateinit var viewModel: CameraViewModel

    companion object {
        fun newInstance() = FaceChooserFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_face_chooser, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProviders.of(activity!!).get(CameraViewModel::class.java)
        if(view is RecyclerView){
            view.layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
            view.addItemDecoration(DividerItemDecoration(context, RecyclerView.HORIZONTAL))
            val adapter = AssetAdapter(Assets.getSkinList(context!!.assets)) {
                viewModel.setSkinAsset(it)
            }
            view.adapter = adapter
//            viewModel.selectedFace.value?.let{adapter.setSelectedItem(it)}
        }
    }
}
