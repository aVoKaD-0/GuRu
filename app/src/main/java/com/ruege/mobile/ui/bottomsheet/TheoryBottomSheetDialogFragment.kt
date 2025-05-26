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
import com.ruege.mobile.data.network.dto.response.TheoryContentDto
import com.ruege.mobile.ui.viewmodel.ContentViewModel
import com.ruege.mobile.MainActivity
import dagger.hilt.android.AndroidEntryPoint

private const val ARG_CONTENT_ID = "content_id"
private const val ARG_TITLE = "title"
private const val TAG_THEORY_BS = "TheoryBottomSheet"

@AndroidEntryPoint
class TheoryBottomSheetDialogFragment : BottomSheetDialogFragment() {

    private val contentViewModel: ContentViewModel by activityViewModels()

    private var theoryTitleTextView: TextView? = null
    private var theoryWebView: WebView? = null
    private var theoryProgressBar: ProgressBar? = null
    private var theoryErrorTextView: TextView? = null

    private var contentId: String? = null
    private var theoryTitle: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            contentId = it.getString(ARG_CONTENT_ID)
            theoryTitle = it.getString(ARG_TITLE)
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

        theoryTitleTextView = view.findViewById(R.id.theory_bottom_sheet_title)
        theoryWebView = view.findViewById(R.id.theory_bottom_sheet_webview)
        theoryProgressBar = view.findViewById(R.id.theory_bottom_sheet_progress)
        theoryErrorTextView = view.findViewById(R.id.theory_bottom_sheet_error)

        theoryTitleTextView?.text = theoryTitle ?: "Теория"

        setupWebView()
        observeTheoryContent()
        observeErrorMessages()

        contentId?.let {
            Log.d(TAG_THEORY_BS, "Загрузка теории для contentId: $it")
            contentViewModel.loadTheoryContent(it)
        } ?: run {
            Log.e(TAG_THEORY_BS, "contentId is null, не могу загрузить теорию")
            showError("ID контента не найден.")
        }
    }

    private fun setupWebView() {
        theoryWebView?.settings?.apply {
            javaScriptEnabled = true // Отключено по умолчанию для безопасности, но может понадобиться
            // domStorageEnabled = true
            // loadWithOverviewMode = true
            // useWideViewPort = true
            // builtInZoomControls = true
            // displayZoomControls = false
        }
    }

    private fun observeTheoryContent() {
        contentViewModel.theoryContent.observe(viewLifecycleOwner, Observer { theoryContentDto ->
            if (theoryContentDto != null && theoryContentDto.id.toString() == contentId) {
                val htmlContent = theoryContentDto.content
                if (!htmlContent.isNullOrEmpty()) {
                    val styledHtml = (activity as? MainActivity)?.applyStylesToHtml(htmlContent, isDarkTheme())
                    theoryWebView?.loadDataWithBaseURL(null, styledHtml ?: htmlContent, "text/html", "UTF-8", null)
                    showContent()
                    Log.d(TAG_THEORY_BS, "Теория '${theoryTitle}' загружена.")
                } else {
                    showError("Содержимое теории пусто.")
                    Log.w(TAG_THEORY_BS, "Содержимое теории для '${theoryTitle}' пустое.")
                }
            } else if (theoryContentDto == null && contentId != null) {
                 // Возможно, данные еще не загружены или были очищены.
                 // Если contentId есть, ViewModel должен был начать загрузку.
                 // Можно добавить логику повторного запроса или просто ожидать.
                 Log.d(TAG_THEORY_BS, "theoryContentDto is null, ожидаем загрузки для $contentId")
            }
        })
    }

    private fun observeErrorMessages() {
        contentViewModel.errorMessage.observe(viewLifecycleOwner, Observer { errorMessage ->
            // Показываем ошибку только если она относится к текущему контенту
            // (Нужен более точный механизм определения, к какому запросу относится ошибка)
            // Пока что, если есть ошибка и контент не отображен, показываем ее.
            if (!errorMessage.isNullOrEmpty() && theoryWebView?.visibility != View.VISIBLE) {
                showError(errorMessage)
                Log.e(TAG_THEORY_BS, "Ошибка загрузки теории '${theoryTitle}': $errorMessage")
            }
        })
    }

    private fun showLoading() {
        theoryProgressBar?.visibility = View.VISIBLE
        theoryWebView?.visibility = View.GONE
        theoryErrorTextView?.visibility = View.GONE
    }

    private fun showContent() {
        theoryProgressBar?.visibility = View.GONE
        theoryWebView?.visibility = View.VISIBLE
        theoryErrorTextView?.visibility = View.GONE
    }

    private fun showError(message: String) {
        theoryProgressBar?.visibility = View.GONE
        theoryWebView?.visibility = View.GONE
        theoryErrorTextView?.text = message
        theoryErrorTextView?.visibility = View.VISIBLE
    }
    
    private fun isDarkTheme(): Boolean {
        // Предполагаем, что MainActivity имеет метод для проверки темы
        return (activity as? MainActivity)?.isDarkTheme() ?: false
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
                layoutParams.height = windowHeight // Заполняем на всю высоту
                bottomSheet.layoutParams = layoutParams
                
                behavior.peekHeight = (windowHeight * 0.95).toInt() // Высота в свернутом состоянии
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
        @JvmStatic
        fun newInstance(contentId: String, title: String): TheoryBottomSheetDialogFragment {
            return TheoryBottomSheetDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_CONTENT_ID, contentId)
                    putString(ARG_TITLE, title)
                }
            }
        }
    }
} 