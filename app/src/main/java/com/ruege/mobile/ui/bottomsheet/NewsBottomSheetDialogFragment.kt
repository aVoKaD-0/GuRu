package com.ruege.mobile.ui.bottomsheet

import android.app.Dialog
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.ruege.mobile.R
import dagger.hilt.android.AndroidEntryPoint

private const val ARG_NEWS_TITLE = "news_title"
private const val ARG_NEWS_DESCRIPTION = "news_description"
private const val ARG_NEWS_DATE = "news_date"
private const val ARG_NEWS_IMAGE_URL = "news_image_url"

@AndroidEntryPoint
class NewsBottomSheetDialogFragment : BottomSheetDialogFragment() {

    private var newsTitle: String? = null
    private var newsDescription: String? = null
    private var newsDate: String? = null
    private var newsImageUrl: String? = null

    private var tvTitle: TextView? = null
    private var tvDate: TextView? = null
    private var tvDescription: TextView? = null
    private var ivImage: ImageView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            newsTitle = it.getString(ARG_NEWS_TITLE)
            newsDescription = it.getString(ARG_NEWS_DESCRIPTION)
            newsDate = it.getString(ARG_NEWS_DATE)
            newsImageUrl = it.getString(ARG_NEWS_IMAGE_URL)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_sheet_news_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvTitle = view.findViewById(R.id.news_bs_title)
        tvDate = view.findViewById(R.id.news_bs_date)
        tvDescription = view.findViewById(R.id.news_bs_description)

        tvTitle?.text = newsTitle ?: "Новость"
        tvDate?.text = newsDate ?: "Дата не указана"
        tvDescription?.text = newsDescription ?: "Описание отсутствует."
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.setOnShowListener { dialogInterface ->
            val bottomSheetDialog = dialogInterface as BottomSheetDialog
            val parentLayout =
                bottomSheetDialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            parentLayout?.let { bottomSheet ->
                val behavior = BottomSheetBehavior.from(bottomSheet)
                val layoutParams = bottomSheet.layoutParams

                val windowHeight = getWindowHeight()
                layoutParams.height = windowHeight
                bottomSheet.layoutParams = layoutParams
                
                behavior.peekHeight = (windowHeight * 0.95).toInt()
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.isFitToContents = false 
                behavior.skipCollapsed = true

                behavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                    override fun onStateChanged(bottomSheet: View, newState: Int) {
                        if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                            dismiss()
                        }
                    }
                    override fun onSlide(bottomSheet: View, slideOffset: Float) {}
                })
            }
        }
        return dialog
    }

    private fun getWindowHeight(): Int {
        val displayMetrics = DisplayMetrics()
        activity?.windowManager?.defaultDisplay?.getMetrics(displayMetrics)
        return displayMetrics.heightPixels
    }

    companion object {
        const val TAG_NEWS_BS = "NewsBottomSheet"

        @JvmStatic
        fun newInstance(title: String, description: String, date: String, imageUrl: String?): NewsBottomSheetDialogFragment {
            return NewsBottomSheetDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_NEWS_TITLE, title)
                    putString(ARG_NEWS_DESCRIPTION, description)
                    putString(ARG_NEWS_DATE, date)
                    putString(ARG_NEWS_IMAGE_URL, imageUrl)
                }
            }
        }
    }
}
