package com.ruege.mobile.ui.bottomsheet

import android.app.Dialog
import android.content.res.Configuration
import android.os.Bundle
import timber.log.Timber
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.ruege.mobile.databinding.BottomSheetShpargalkaBinding
import com.ruege.mobile.ui.viewmodel.ShpargalkaViewModel
import com.ruege.mobile.utils.PdfDownloadHelper
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ShpargalkaBottomSheetDialogFragment : BottomSheetDialogFragment() {

    private val shpargalkaViewModel: ShpargalkaViewModel by activityViewModels()

    private var _binding: BottomSheetShpargalkaBinding? = null
    private val binding get() = _binding!!

    private var currentPdfId: Int? = null
    private var currentTitle: String? = null
    private var currentDescription: String? = null
    private lateinit var pdfDownloadHelper: PdfDownloadHelper

    companion object {
        const val TAG = "ShpargalkaBottomSheetDialogFragment_TAG"
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
                    Timber.d("Invalid contentId format: $contentId")
                }
            } else {
                Timber.d("ContentId is null")
            }
        }
        if (currentPdfId == null) {
            Toast.makeText(requireContext(), "Ошибка: Некорректный ID шпаргалки.", Toast.LENGTH_LONG).show()
            dismiss()
        }
        pdfDownloadHelper = PdfDownloadHelper(requireActivity())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetShpargalkaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        pdfDownloadHelper.onRequestPermissionsResult(requestCode, grantResults)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvShpargalkaTitle.text = currentTitle ?: "Шпаргалка"
        
        binding.pdfView.isNestedScrollingEnabled = true

        Timber.d("onViewCreated: Setting initial UI state for PDF loading.")
        binding.progressBar.visibility = View.VISIBLE
        binding.pdfView.visibility = View.GONE
        binding.tvErrorMessage.visibility = View.GONE
        binding.btnDownloadPdf.visibility = View.GONE 

        setupObservers()

        binding.btnDownloadPdf.setOnClickListener {
            currentPdfId?.let { pdfId ->
                binding.btnDownloadPdf.isEnabled = false
                binding.progressBar.visibility = View.VISIBLE
                binding.tvErrorMessage.visibility = View.GONE
                shpargalkaViewModel.downloadPdf(pdfId) { success ->
                    binding.progressBar.visibility = View.GONE
                    binding.btnDownloadPdf.isEnabled = true
                    if (success) {
                        val localFile = shpargalkaViewModel.getLocalPdfFile(pdfId)
                        if (localFile != null && localFile.exists()) {
                            pdfDownloadHelper.downloadPdfToDownloads(
                                localFile,
                                "${currentTitle?.replace(Regex("""[\\/:*?"<>|]"""), "_") ?: "shpargalka"}.pdf",
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
            Timber.d("onViewCreated: Calling shpargalkaViewModel.loadShpargalkaPdf for ID: $it")
            shpargalkaViewModel.loadShpargalkaPdf(it)
        }
    }

    private fun setupObservers() {
        shpargalkaViewModel.getPdfLoadingStatus().observe(viewLifecycleOwner) { isLoading ->
            Timber.d("Observer PdfLoadingStatus: isLoading = $isLoading")
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            if (isLoading) {
                binding.pdfView.visibility = View.GONE
                binding.tvErrorMessage.visibility = View.GONE
            }
        }

        shpargalkaViewModel.currentPdfFile().observe(viewLifecycleOwner) { file ->
            Timber.d("Observer currentPdfFile: file exists = ${file?.exists()}")
            if (file != null && file.exists()) {
                binding.pdfView.visibility = View.VISIBLE
                binding.tvErrorMessage.visibility = View.GONE

                val isNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES

                binding.pdfView.fromFile(file)
                    .enableSwipe(true)
                    .swipeHorizontal(false)
                    .enableDoubletap(true)
                    .defaultPage(0)
                    .nightMode(isNightMode)
                    .onError { t -> Timber.d("Error loading PDF into PDFView", t) }
                    .load()
                binding.btnDownloadPdf.visibility = View.VISIBLE 
            } else if (shpargalkaViewModel.getPdfLoadingStatus().value == false) {
                if (shpargalkaViewModel.getPdfLoadError().value == null) {
                    Timber.d("currentPdfFile is null, loading is false, and no error. Displaying generic error.")
                    binding.tvErrorMessage.text = "Не удалось загрузить PDF файл."
                    binding.tvErrorMessage.visibility = View.VISIBLE
                    binding.pdfView.visibility = View.GONE
                    binding.btnDownloadPdf.visibility = View.VISIBLE
                }
            }
        }

        shpargalkaViewModel.getPdfLoadError().observe(viewLifecycleOwner) { error ->
            Timber.d("Observer PdfLoadError: error = $error")
            if (error != null) {
                binding.tvErrorMessage.visibility = View.VISIBLE
                binding.tvErrorMessage.text = error
                binding.pdfView.visibility = View.GONE
                binding.btnDownloadPdf.visibility = View.VISIBLE
            } else {
                binding.tvErrorMessage.visibility = View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
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

