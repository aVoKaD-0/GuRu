package com.ruege.mobile.ui.bottomsheet

import android.app.Dialog
import android.os.Bundle
import android.util.DisplayMetrics
import timber.log.Timber
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.ruege.mobile.databinding.BottomSheetTheoryBinding
import com.ruege.mobile.ui.viewmodel.TheoryViewModel
import dagger.hilt.android.AndroidEntryPoint
import android.content.res.Configuration

@AndroidEntryPoint
class TheoryBottomSheetDialogFragment : BottomSheetDialogFragment() {

    private val theoryViewModel: TheoryViewModel by activityViewModels()

    private var _binding: BottomSheetTheoryBinding? = null
    private val binding get() = _binding!!

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
    ): View {
        _binding = BottomSheetTheoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.theoryBottomSheetTitle.text = theoryTitle ?: "Теория"

        setupWebView()
        observeTheoryContent()
        observeErrorMessages()

        contentId?.let {
            Timber.d("Загрузка теории для contentId: $it")
            theoryViewModel.loadTheoryContent(it)
        } ?: run {
            Timber.d("contentId is null, не могу загрузить теорию")
            showError("ID контента не найден.")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupWebView() {
        binding.theoryBottomSheetWebview.settings.apply {
            javaScriptEnabled = true
        }
    }

    private fun observeTheoryContent() {
        theoryViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            if(isLoading) showLoading()
        }

        theoryViewModel.theoryContent.observe(viewLifecycleOwner, Observer { theoryContentDto ->
            if (theoryContentDto != null) {
                val contentMatches = theoryContentDto.id.toString() == contentId || theoryContentDto.createdAt.isEmpty()
                
                if (contentMatches) {
                    val htmlContent = theoryContentDto.content
                    if (!htmlContent.isNullOrEmpty()) {
                        val isDarkTheme = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
                        val styledHtml = applyStylesToHtml(htmlContent, isDarkTheme)
                        binding.theoryBottomSheetWebview.loadDataWithBaseURL(null, styledHtml ?: htmlContent, "text/html", "UTF-8", null)
                        showContent()
                        Timber.d("Теория '${theoryTitle}' загружена.")
                    } else {
                        showError("Содержимое теории пусто.")
                        Timber.d("Содержимое теории для '${theoryTitle}' пустое.")
                    }
                }
            }
        })
    }

    private fun applyStylesToHtml(htmlContent: String, isDarkTheme: Boolean): String {
        val themeStyle = if (isDarkTheme) {
            """
            <style>
                body {
                    background-color: #121212;
                    color: #FFFFFF;
                }
                a { color: #BB86FC; }
                pre { background-color: #1E1E1E; padding: 10px; border-radius: 5px; }
            </style>
            """.trimIndent()
        } else {
            """
            <style>
                body {
                    background-color: #FFFFFF;
                    color: #000000;
                }
                a { color: #6200EE; }
                pre { background-color: #F5F5F5; padding: 10px; border-radius: 5px; }
            </style>
            """.trimIndent()
        }
        return "<html><head>$themeStyle</head><body>$htmlContent</body></html>"
    }

    private fun observeErrorMessages() {
        theoryViewModel.errorMessage.observe(viewLifecycleOwner, Observer { errorMessage ->
            if (!errorMessage.isNullOrEmpty() && binding.theoryBottomSheetWebview.visibility != View.VISIBLE) {
                showError(errorMessage)
                Timber.d("Ошибка загрузки теории '${theoryTitle}': $errorMessage")
            }
        })
    }

    private fun showLoading() {
        binding.theoryBottomSheetProgress.visibility = View.VISIBLE
        binding.theoryBottomSheetWebview.visibility = View.GONE
        binding.theoryBottomSheetError.visibility = View.GONE
    }

    private fun showContent() {
        binding.theoryBottomSheetProgress.visibility = View.GONE
        binding.theoryBottomSheetWebview.visibility = View.VISIBLE
        binding.theoryBottomSheetError.visibility = View.GONE
    }

    private fun showError(message: String) {
        binding.theoryBottomSheetProgress.visibility = View.GONE
        binding.theoryBottomSheetWebview.visibility = View.GONE
        binding.theoryBottomSheetError.text = message
        binding.theoryBottomSheetError.visibility = View.VISIBLE
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog

        val currentNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        when (currentNightMode) {
            Configuration.UI_MODE_NIGHT_NO -> Timber.d("onCreateDialog: Current theme is Light.")
            Configuration.UI_MODE_NIGHT_YES -> Timber.d("onCreateDialog: Current theme is Dark.")
            Configuration.UI_MODE_NIGHT_UNDEFINED -> Timber.d("onCreateDialog: Current theme is Undefined.")
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
        private const val ARG_CONTENT_ID = "content_id"
        private const val ARG_TITLE = "title"
        const val TAG = "TheoryBottomSheet"

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