package piuk.blockchain.android.cards

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.recyclerview.widget.LinearLayoutManager
import piuk.blockchain.android.databinding.PickerLayoutBinding
import piuk.blockchain.android.ui.base.SlidingModalBottomDialog
import piuk.blockchain.android.util.AfterTextChangedWatcher
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import java.io.Serializable
import java.util.Locale

class SearchPickerItemBottomSheet : SlidingModalBottomDialog<PickerLayoutBinding>() {

    private val searchResults = mutableListOf<PickerItem>()
    private val adapter by unsafeLazy {
        PickerItemsAdapter {
            (parentFragment as? PickerItemListener)?.onItemPicked(it)
            dismiss()
        }
    }
    private val items: List<PickerItem> by unsafeLazy {
        (arguments?.getSerializable(PICKER_ITEMS) as? List<PickerItem>) ?: emptyList()
    }

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): PickerLayoutBinding =
        PickerLayoutBinding.inflate(inflater, container, false)

    override fun initControls(binding: PickerLayoutBinding) {
        with(binding) {
            countryCodePickerSearch.addTextChangedListener(object : AfterTextChangedWatcher() {
                override fun afterTextChanged(searchQuery: Editable) {
                    search(searchQuery.toString())
                }
            })

            val layoutManager = LinearLayoutManager(activity)

            pickerRecyclerView.layoutManager = layoutManager
            pickerRecyclerView.adapter = adapter
            adapter.items = items

            countryCodePickerSearch.setOnEditorActionListener { _, _, _ ->
                val imm: InputMethodManager = countryCodePickerSearch.context
                    .getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(countryCodePickerSearch.windowToken, 0)
                true
            }
            configureRootViewMinHeight()
        }
    }

    private fun configureRootViewMinHeight() {
        val displayMetrics = DisplayMetrics()
        activity?.windowManager?.defaultDisplay?.getMetrics(displayMetrics)?.let {
            binding.rootView.minimumHeight = (displayMetrics.heightPixels * 0.6).toInt()
        }
    }

    private fun search(searchQuery: String) {
        searchResults.clear()
        for (item in items) {
            if (item.label.toLowerCase(Locale.getDefault()).contains(searchQuery.toLowerCase(Locale.getDefault()))) {
                searchResults.add(item)
            }
        }
        adapter.items = searchResults
    }

    companion object {
        private const val PICKER_ITEMS = "PICKER_ITEMS"
        fun newInstance(items: List<PickerItem>): SearchPickerItemBottomSheet =
            SearchPickerItemBottomSheet().apply {
                arguments = Bundle().also {
                    it.putSerializable(PICKER_ITEMS, items as Serializable)
                }
            }
    }
}

interface PickerItem {
    val label: String
    val code: String
    val icon: String?
}

interface PickerItemListener {
    fun onItemPicked(item: PickerItem)
}