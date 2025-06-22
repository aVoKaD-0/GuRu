package com.ruege.mobile.ui.bottomsheet

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.ruege.mobile.R
import com.google.android.material.button.MaterialButton

class SupportLinksBottomSheetDialogFragment : BottomSheetDialogFragment() {

    private val websiteUrl = "https://guru.almaz.heloword.ru/"
    private val telegramUrl = "https://t.me/guru_ege"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.layout_support_links_bottom_sheet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnOpenWebsite: MaterialButton = view.findViewById(R.id.btn_open_website)
        val btnOpenTelegram: MaterialButton = view.findViewById(R.id.btn_open_telegram)

        btnOpenWebsite.setOnClickListener {
            openUrl(websiteUrl)
            dismiss()
        }

        btnOpenTelegram.setOnClickListener {
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

    companion object {
        const val TAG = "SupportLinksBottomSheet"
        fun newInstance(): SupportLinksBottomSheetDialogFragment {
            return SupportLinksBottomSheetDialogFragment()
        }
    }
} 