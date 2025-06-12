package com.ruege.mobile.ui.bottomsheet

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import com.github.barteksc.pdfviewer.PDFView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.ruege.mobile.R
import com.ruege.mobile.ui.viewmodel.ShpargalkaViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import com.ruege.mobile.MainActivity

@AndroidEntryPoint
class ShpargalkaBottomSheetDialogFragment : BottomSheetDialogFragment() {

    private val shpargalkaViewModel: ShpargalkaViewModel by activityViewModels()

    private lateinit var pdfView: PDFView
    private lateinit var progressBar: ProgressBar
    private lateinit var errorMessageTextView: TextView
    private lateinit var downloadButton: Button
    private lateinit var titleTextView: TextView

    private var currentPdfId: Int? = null
    private var currentTitle: String? = null
    private var currentDescription: String? = null 

    companion object {
        const val TAG_SHPARGALKA_BS = "ShpargalkaBottomSheetDialogFragment_TAG"
        private const val ARG_CONTENT_ID = "content_id"
        private const val ARG_TITLE = "title"
        private const val ARG_DESCRIPTION = "description"

        @JvmStatic
        fun newInstance(contentId: String, title: String, description: String?): ShpargalkaBottomSheetDialogFragment {
            val fragment = ShpargalkaBottomSheetDialogFragment()
            val args = Bundle()
            args.putString(ARG_CONTENT_ID, contentId)
            args.putString(ARG_TITLE, title)
            args.putString(ARG_DESCRIPTION, description)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            val contentId = it.getString(ARG_CONTENT_ID)
            currentTitle = it.getString(ARG_TITLE)
            currentDescription = it.getString(ARG_DESCRIPTION)
            if (contentId != null) {
                val parts = contentId.split("_")
                if (parts.size == 2 && parts[0] == "shpargalka") {
                    currentPdfId = parts[1].toIntOrNull()
                } else {
                    Log.e(TAG_SHPARGALKA_BS, "Invalid contentId format: $contentId")
                }
            } else {
                Log.e(TAG_SHPARGALKA_BS, "ContentId is null")
            }
        }
        if (currentPdfId == null) {
            Toast.makeText(requireContext(), "Ошибка: Некорректный ID шпаргалки.", Toast.LENGTH_LONG).show()
            dismiss()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.bottom_sheet_shpargalka, container, false)
        pdfView = view.findViewById(R.id.pdf_view)
        progressBar = view.findViewById(R.id.progress_bar)
        errorMessageTextView = view.findViewById(R.id.tv_error_message)
        downloadButton = view.findViewById(R.id.btn_download_pdf)
        titleTextView = view.findViewById(R.id.tv_shpargalka_title)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        titleTextView.text = currentTitle ?: "Шпаргалка"
        
        pdfView.isNestedScrollingEnabled = true

        Log.d(TAG_SHPARGALKA_BS, "onViewCreated: Setting initial UI state for PDF loading.")
        progressBar.visibility = View.VISIBLE
        pdfView.visibility = View.GONE
        errorMessageTextView.visibility = View.GONE
        downloadButton.visibility = View.GONE 

        setupObservers()

        downloadButton.setOnClickListener {
            currentPdfId?.let { pdfId ->
                downloadButton.isEnabled = false
                progressBar.visibility = View.VISIBLE
                errorMessageTextView.visibility = View.GONE
                shpargalkaViewModel.downloadPdf(pdfId) { success ->
                    progressBar.visibility = View.GONE
                    downloadButton.isEnabled = true
                    if (success) {
                        val localFile = shpargalkaViewModel.getLocalPdfFile(pdfId)
                        if (localFile != null && localFile.exists()) {
                            (activity as? MainActivity)?.downloadPdfToDownloads(
                                localFile,
                                "${currentTitle?.replace(Regex("[\\/:*?\"<>|]"), "_") ?: "shpargalka"}.pdf",
                                currentDescription ?: currentTitle ?: ""
                            )
                        } else {
                            Toast.makeText(requireContext(), "Ошибка: Файл скачан, но не найден.", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(requireContext(), "Не удалось скачать PDF.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        currentPdfId?.let {
            Log.d(TAG_SHPARGALKA_BS, "onViewCreated: Calling shpargalkaViewModel.loadShpargalkaPdf for ID: $it")
            shpargalkaViewModel.loadShpargalkaPdf(it)
        }
    }

    private fun setupObservers() {
        shpargalkaViewModel.getPdfLoadingStatus().observe(viewLifecycleOwner) { isLoading ->
            Log.d(TAG_SHPARGALKA_BS, "Observer PdfLoadingStatus: isLoading = $isLoading")
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            if (isLoading) {
                pdfView.visibility = View.GONE
                errorMessageTextView.visibility = View.GONE
                // Пока идет загрузка, кнопка "Скачать" может быть неактивна или скрыта
                // downloadButton.visibility = View.GONE 
            } else {
                // Если загрузка завершилась (успешно или нет), кнопка скачивания должна быть видна
                // если PDF отображен или есть ошибка (чтобы пользователь мог попробовать скачать)
                // downloadButton.visibility = View.VISIBLE
            }
        }

        shpargalkaViewModel.currentPdfFile().observe(viewLifecycleOwner) { file ->
            Log.d(TAG_SHPARGALKA_BS, "Observer currentPdfFile: file exists = ${file?.exists()}")
            if (file != null && file.exists()) {
                pdfView.visibility = View.VISIBLE
                errorMessageTextView.visibility = View.GONE
                pdfView.fromFile(file)
                    .enableSwipe(true)
                    .swipeHorizontal(false)
                    .enableDoubletap(true)
                    .defaultPage(0)
                    .onError { t -> Log.e(TAG_SHPARGALKA_BS, "Error loading PDF into PDFView", t) }
                    .load()
                downloadButton.visibility = View.VISIBLE 
            } else if (shpargalkaViewModel.getPdfLoadingStatus().value == false) {
                if (shpargalkaViewModel.getPdfLoadError().value == null) {
                    Log.d(TAG_SHPARGALKA_BS, "currentPdfFile is null, loading is false, and no error. Displaying generic error.")
                    errorMessageTextView.text = "Не удалось загрузить PDF файл."
                    errorMessageTextView.visibility = View.VISIBLE
                    pdfView.visibility = View.GONE
                    downloadButton.visibility = View.VISIBLE
                }
            }
        }

        shpargalkaViewModel.getPdfLoadError().observe(viewLifecycleOwner) { error ->
            Log.d(TAG_SHPARGALKA_BS, "Observer PdfLoadError: error = $error")
            if (error != null) {
                errorMessageTextView.visibility = View.VISIBLE
                errorMessageTextView.text = error
                pdfView.visibility = View.GONE
                downloadButton.visibility = View.VISIBLE
            } else {
                errorMessageTextView.visibility = View.GONE
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.setOnShowListener { d ->
            val bottomSheet = (d as BottomSheetDialog).findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let {
                val behavior = BottomSheetBehavior.from(it)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.skipCollapsed = true
                val layoutParams = it.layoutParams
                layoutParams.height = (resources.displayMetrics.heightPixels * 1).toInt()
                it.layoutParams = layoutParams
            }
        }
        return dialog
    }
}

