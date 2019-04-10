package net.masonapps.zombiecamera.settings


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import net.masonapps.zombiecamera.Assets
import net.masonapps.zombiecamera.R

/**
 *
 */
class AssetListFragment : Fragment() {

    private lateinit var viewModel: ChooserViewModel
    
    companion object {
        private const val KEY_ASSET_TYPE = "asset-type"
        fun newInstance(assetType: String): AssetListFragment {
            val fragment = AssetListFragment()
            fragment.arguments = Bundle().apply { putString(KEY_ASSET_TYPE, assetType) }
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_asset_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (parentFragment !is ChooserFragment) throw IllegalStateException("${AssetListFragment::class.java.name} must be a child fragment of ${ChooserFragment::class.java.simpleName}")

        val chooserFragment = parentFragment as ChooserFragment
        viewModel = ViewModelProviders.of(chooserFragment).get(ChooserViewModel::class.java)

        if(view is RecyclerView){
            view.layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
            view.addItemDecoration(DividerItemDecoration(context, RecyclerView.HORIZONTAL))
            val adapter = AssetAdapter(Assets.getSkinList(context!!.assets)) {

            }
            view.adapter = adapter
        }
    }
}
