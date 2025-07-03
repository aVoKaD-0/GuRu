package com.ruege.mobile.ui.bottomsheet

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.ruege.mobile.databinding.LayoutSupportLinksBottomSheetBinding

class SupportLinksBottomSheetDialogFragment : BottomSheetDialogFragment() {

    private var _binding: LayoutSupportLinksBottomSheetBinding? = null
    private val binding get() = _binding!!

    private val websiteUrl = "http://46.8.232.191/"
    private val telegramUrl = "https://t.me/guru_ege"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = LayoutSupportLinksBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnOpenWebsite.setOnClickListener {
            openUrl(websiteUrl)
            dismiss()
        }

        binding.btnOpenTelegram.setOnClickListener {
            openUrl(telegramUrl)
            dismiss()
        }
    }

    private fun openUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            android.widget.Toast.makeText(context, "Не удалось открыть ссылку", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "SupportLinksBottomSheet"
        fun newInstance(): SupportLinksBottomSheetDialogFragment {
            return SupportLinksBottomSheetDialogFragment()
        }
    }
} 