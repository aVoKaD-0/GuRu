package com.ruege.mobile.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.ruege.mobile.data.local.entity.VariantEntity
import com.ruege.mobile.databinding.FragmentVariantsBinding
import com.ruege.mobile.ui.adapter.VariantAdapter
import com.ruege.mobile.ui.bottomsheet.VariantDetailBottomSheetDialogFragment
import com.ruege.mobile.ui.viewmodel.VariantViewModel
import com.ruege.mobile.utils.Resource
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber
import com.google.android.material.dialog.MaterialAlertDialogBuilder

@AndroidEntryPoint
class VariantsFragment : Fragment(), VariantAdapter.OnVariantClickListener, VariantAdapter.OnItemSelectionListener, VariantAdapter.OnItemDeleteListener {

    private var _binding: FragmentVariantsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: VariantViewModel by viewModels()
    private lateinit var variantAdapter: VariantAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVariantsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupClickListeners()

        binding.recyclerViewVariants.visibility = View.GONE
        binding.errorTextViewVariants.visibility = View.GONE

        observeVariants()
        observeDownloadStatus()
    }

    private fun setupRecyclerView() {
        variantAdapter = VariantAdapter(this, this, this)
        binding.recyclerViewVariants.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = variantAdapter
        }
    }

    private fun setupClickListeners() {
        binding.selectAllCheckboxVariants.setOnCheckedChangeListener { _, isChecked ->
            viewModel.selectAllVariants(isChecked)
        }
        binding.fabDownloadVariants.setOnClickListener {
            viewModel.downloadSelectedVariants()
        }
    }

    private fun observeVariants() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.variantsState.collectLatest { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        binding.shimmerContentVariants.startShimmer()
                        binding.shimmerContentVariants.visibility = View.VISIBLE
                        binding.recyclerViewVariants.visibility = View.GONE
                        binding.errorTextViewVariants.visibility = View.GONE
                    }
                    is Resource.Success -> {
                        binding.shimmerContentVariants.stopShimmer()
                        binding.shimmerContentVariants.visibility = View.GONE
                        val items = resource.data
                        if (items != null && items.isNotEmpty()) {
                            variantAdapter.submitList(items)
                            val hasSelectedForDownload = items.any { it.isSelected && !it.isDownloaded }
                            binding.fabDownloadVariants.visibility = if (hasSelectedForDownload) View.VISIBLE else View.GONE
                            binding.recyclerViewVariants.visibility = View.VISIBLE
                            binding.errorTextViewVariants.visibility = View.GONE
                        } else {
                            binding.errorTextViewVariants.text = "Нет доступных вариантов."
                            binding.errorTextViewVariants.visibility = View.VISIBLE
                            binding.recyclerViewVariants.visibility = View.GONE
                        }
                    }
                    is Resource.Error -> {
                        binding.shimmerContentVariants.stopShimmer()
                        binding.shimmerContentVariants.visibility = View.GONE
                        binding.recyclerViewVariants.visibility = View.GONE
                        binding.errorTextViewVariants.text = "Ошибка загрузки: ${resource.message}"
                        binding.errorTextViewVariants.visibility = View.VISIBLE
                        binding.errorTextViewVariants.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark))
                    }
                    else -> { 
                    }
                }
            }
        }
    }

    private fun observeDownloadStatus() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isDownloading.collectLatest { isDownloading ->
                binding.fabDownloadVariants.isEnabled = !isDownloading
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.downloadEvent.collectLatest { resource ->
                val message = resource.message ?: when (resource) {
                    is Resource.Success -> "Действие выполнено успешно"
                    is Resource.Error -> "Произошла неизвестная ошибка"
                    else -> ""
                }
                if (message.isNotBlank()) {
                    Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onVariantClick(variant: VariantEntity) {
        Timber.d("Вариант выбран: ${variant.name}, ID: ${variant.variantId}")
        if (isAdded) {
            val existingFragment = parentFragmentManager.findFragmentByTag(VariantDetailBottomSheetDialogFragment.TAG_VARIANT_DETAIL_BS)
            if (existingFragment == null) {
                val bottomSheet = VariantDetailBottomSheetDialogFragment.newInstance(
                    variant.variantId.toString(),
                    variant.name
                )
                bottomSheet.show(parentFragmentManager, VariantDetailBottomSheetDialogFragment.TAG_VARIANT_DETAIL_BS)
            }
        }
    }

    override fun onSelectionChanged(variant: VariantEntity, isSelected: Boolean) {
        viewModel.toggleVariantSelection(variant.variantId, isSelected)
    }

    override fun onDeleteClick(variant: VariantEntity) {
        if (!variant.isDownloaded) return

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Удаление варианта")
            .setMessage("Вы уверены, что хотите удалить скачанный вариант \"${variant.name}\"? Весь прогресс по нему будет стерт.")
            .setPositiveButton("Удалить") { _, _ ->
                viewModel.deleteDownloadedVariant(variant.variantId)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 