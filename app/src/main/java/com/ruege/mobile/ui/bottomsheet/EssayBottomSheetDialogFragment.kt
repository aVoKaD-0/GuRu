package com.ruege.mobile.ui.bottomsheet

import android.app.Dialog
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.ruege.mobile.R
import com.ruege.mobile.ui.viewmodel.EssayViewModel
import com.ruege.mobile.MainActivity
import dagger.hilt.android.AndroidEntryPoint

private const val ARG_CONTENT_ID = "content_id"
private const val ARG_TITLE = "title"

@AndroidEntryPoint
class EssayBottomSheetDialogFragment : BottomSheetDialogFragment() {

    private val viewModel: EssayViewModel by activityViewModels()

    private var titleTextView: TextView? = null
    private var contentWebView: WebView? = null
    private var progressBar: ProgressBar? = null
    private var errorTextView: TextView? = null

    private var contentId: String? = null
    private var essayTitle: String? = null 

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            contentId = it.getString(ARG_CONTENT_ID)
            essayTitle = it.getString(ARG_TITLE) 
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_sheet_theory, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        titleTextView = view.findViewById(R.id.theory_bottom_sheet_title)
        contentWebView = view.findViewById(R.id.theory_bottom_sheet_webview)
        progressBar = view.findViewById(R.id.theory_bottom_sheet_progress)
        errorTextView = view.findViewById(R.id.theory_bottom_sheet_error)

        titleTextView?.text = essayTitle ?: "Сочинение" 

        setupWebView()
        observeEssayContent() 
        observeErrorMessages()

        contentId?.let {
            Log.d(TAG_ESSAY_BS, "Загрузка сочинения для contentId: $it")
            viewModel.loadEssayContent(it)
        } ?: run {
            Log.e(TAG_ESSAY_BS, "contentId is null, не могу загрузить сочинение")
            showError("ID контента не найден.")
        }
    }

    private fun setupWebView() {
        contentWebView?.settings?.apply {
            javaScriptEnabled = true
        }
    }

    private fun observeEssayContent() {
        viewModel.essayContent.observe(viewLifecycleOwner, Observer { essayContentDto ->
            if (essayContentDto != null && essayContentDto.id.toString() == contentId) {
                val htmlContent = essayContentDto.content
                if (!htmlContent.isNullOrEmpty()) {
                    val mainActivity = activity as? MainActivity
                    val currentDarkTheme = mainActivity?.isDarkThemeEnabled() ?: false
                    val styledHtml = mainActivity?.applyStylesToHtml(htmlContent, currentDarkTheme)
                    contentWebView?.loadDataWithBaseURL(null, styledHtml ?: htmlContent, "text/html", "UTF-8", null)
                    showContent()
                    Log.d(TAG_ESSAY_BS, "Сочинение '${essayTitle}' загружено.")
                } else {
                    showError("Содержимое сочинения пусто.")
                    Log.w(TAG_ESSAY_BS, "Содержимое сочинения для '${essayTitle}' пустое.")
                }
            } else if (essayContentDto == null && contentId != null) {
                 Log.d(TAG_ESSAY_BS, "essayContentDto is null, ожидаем загрузки для $contentId")
            }
        })
    }

    private fun observeErrorMessages() {
        viewModel.errorMessage.observe(viewLifecycleOwner, Observer { errorMessage ->
            if (!errorMessage.isNullOrEmpty() && contentWebView?.visibility != View.VISIBLE) {
                showError(errorMessage)
                Log.e(TAG_ESSAY_BS, "Ошибка загрузки сочинения '${essayTitle}': $errorMessage")
            }
        })
    }

    private fun showLoading() {
        progressBar?.visibility = View.VISIBLE
        contentWebView?.visibility = View.GONE
        errorTextView?.visibility = View.GONE
    }

    private fun showContent() {
        progressBar?.visibility = View.GONE
        contentWebView?.visibility = View.VISIBLE
        errorTextView?.visibility = View.GONE
    }

    private fun showError(message: String) {
        progressBar?.visibility = View.GONE
        contentWebView?.visibility = View.GONE
        errorTextView?.text = message
        errorTextView?.visibility = View.VISIBLE
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
        const val TAG_ESSAY_BS = "EssayBottomSheet"

        @JvmStatic
        fun newInstance(contentId: String, title: String): EssayBottomSheetDialogFragment {
            return EssayBottomSheetDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_CONTENT_ID, contentId)
                    putString(ARG_TITLE, title)
                }
            }
        }
    }
} 