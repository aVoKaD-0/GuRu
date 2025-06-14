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
import com.ruege.mobile.ui.viewmodel.TheoryViewModel
import com.ruege.mobile.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import android.content.res.Configuration
import android.widget.Button
import android.widget.ImageView
import com.ruege.mobile.utilss.Resource
import android.widget.Toast

private const val ARG_CONTENT_ID = "content_id"
private const val ARG_TITLE = "title"
private const val TAG_THEORY_BS = "TheoryBottomSheet"

@AndroidEntryPoint
class TheoryBottomSheetDialogFragment : BottomSheetDialogFragment() {

    private val theoryViewModel: TheoryViewModel by activityViewModels()

    private var theoryTitleTextView: TextView? = null
    private var theoryWebView: WebView? = null
    private var theoryProgressBar: ProgressBar? = null
    private var theoryErrorTextView: TextView? = null
    private var downloadButton: Button? = null
    private var downloadStatusIcon: ImageView? = null

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
        observeDownloadStatus()
        observeDeleteStatus()

        contentId?.let {
            Log.d(TAG_THEORY_BS, "Загрузка теории для contentId: $it")
            theoryViewModel.loadTheoryContent(it)
            theoryViewModel.getDownloadedTheory(it).observe(viewLifecycleOwner) { downloadedTheory ->
                updateDownloadButton(downloadedTheory != null)
            }
        } ?: run {
            Log.e(TAG_THEORY_BS, "contentId is null, не могу загрузить теорию")
            showError("ID контента не найден.")
        }
        
        downloadButton?.setOnClickListener {
            downloadButton?.isEnabled = false // Блокируем кнопку на время операции
            contentId?.let { id ->
                val isCurrentlyDownloaded = theoryViewModel.getDownloadedTheory(id).value != null
                if (isCurrentlyDownloaded) {
                    theoryViewModel.deleteDownloadedTheory(id)
                } else {
                    theoryViewModel.downloadTheory(id)
                }
            }
        }
    }

    private fun setupWebView() {
        theoryWebView?.settings?.apply {
            javaScriptEnabled = true
        }
    }

    private fun observeTheoryContent() {
        theoryViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            if(isLoading) showLoading()
        }

        theoryViewModel.theoryContent.observe(viewLifecycleOwner, Observer { theoryContentDto ->
            if (theoryContentDto != null) {
                // Убедимся, что ID совпадает, или что это DTO, созданный из кеша (где ID может быть заглушкой)
                val contentMatches = theoryContentDto.id.toString() == contentId || theoryContentDto.createdAt.isEmpty()
                
                if (contentMatches) {
                    val htmlContent = theoryContentDto.content
                    if (!htmlContent.isNullOrEmpty()) {
                        val mainActivity = activity as? MainActivity
                        val currentDarkTheme = mainActivity?.isDarkThemeEnabled() ?: false
                        val styledHtml = mainActivity?.applyStylesToHtml(htmlContent, currentDarkTheme)
                        theoryWebView?.loadDataWithBaseURL(null, styledHtml ?: htmlContent, "text/html", "UTF-8", null)
                        showContent()
                        Log.d(TAG_THEORY_BS, "Теория '${theoryTitle}' загружена.")
                    } else {
                        showError("Содержимое теории пусто.")
                        Log.w(TAG_THEORY_BS, "Содержимое теории для '${theoryTitle}' пустое.")
                    }
                }
            }
        })
    }

    private fun observeErrorMessages() {
        theoryViewModel.errorMessage.observe(viewLifecycleOwner, Observer { errorMessage ->
            if (!errorMessage.isNullOrEmpty() && theoryWebView?.visibility != View.VISIBLE) {
                showError(errorMessage)
                Log.e(TAG_THEORY_BS, "Ошибка загрузки теории '${theoryTitle}': $errorMessage")
            }
        })
    }

    private fun observeDownloadStatus() {
        theoryViewModel.downloadStatus.observe(viewLifecycleOwner) { resource ->
            when(resource) {
                is Resource.Loading -> {
                    downloadButton?.isEnabled = false
                    downloadButton?.text = "Загрузка..."
                }
                is Resource.Success -> {
                    downloadButton?.isEnabled = true
                    Toast.makeText(requireContext(), "Теория успешно скачана", Toast.LENGTH_SHORT).show()
                }
                is Resource.Error -> {
                    downloadButton?.isEnabled = true
                    Toast.makeText(requireContext(), "Ошибка скачивания: ${resource.message}", Toast.LENGTH_LONG).show()
                }
                else -> { Log.d("TheoryBottomSheetDialogFragment", "Ошибка в TheoryBottomSHeetDialogFragment.kt") }
            } 
        }
    }

    private fun observeDeleteStatus() {
        theoryViewModel.deleteStatus.observe(viewLifecycleOwner) { resource ->
            when(resource) {
                is Resource.Loading -> {
                    downloadButton?.isEnabled = false
                    downloadButton?.text = "Удаление..."
                }
                is Resource.Success -> {
                    downloadButton?.isEnabled = true
                    Toast.makeText(requireContext(), "Теория удалена", Toast.LENGTH_SHORT).show()
                }
                is Resource.Error -> {
                    downloadButton?.isEnabled = true
                    Toast.makeText(requireContext(), "Ошибка удаления: ${resource.message}", Toast.LENGTH_LONG).show()
                }
                else -> { Log.d("TheoryBottomSheetDialogFragment", "Ошибка в TheoryBottomSHeetDialogFragment.kt2") }
            }
        }
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

    private fun updateDownloadButton(isDownloaded: Boolean) {
        if (isDownloaded) {
            downloadButton?.text = "Удалить"
            downloadStatusIcon?.visibility = View.VISIBLE
            downloadStatusIcon?.setImageResource(R.drawable.ic_download_done)
        } else {
            downloadButton?.text = "Скачать"
            downloadStatusIcon?.visibility = View.GONE
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog

        val currentNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        when (currentNightMode) {
            Configuration.UI_MODE_NIGHT_NO -> Log.d(TAG_THEORY_BS, "onCreateDialog: Current theme is Light.")
            Configuration.UI_MODE_NIGHT_YES -> Log.d(TAG_THEORY_BS, "onCreateDialog: Current theme is Dark.")
            Configuration.UI_MODE_NIGHT_UNDEFINED -> Log.d(TAG_THEORY_BS, "onCreateDialog: Current theme is Undefined.")
        }

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