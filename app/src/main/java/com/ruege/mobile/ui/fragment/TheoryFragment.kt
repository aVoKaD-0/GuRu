package com.ruege.mobile.ui.fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.facebook.shimmer.ShimmerFrameLayout
import com.ruege.mobile.MainActivity
import com.ruege.mobile.ui.adapter.ContentAdapter
import com.ruege.mobile.databinding.FragmentTheoryBinding
import com.ruege.mobile.model.ContentItem
import com.ruege.mobile.ui.viewmodel.TheoryViewModel
import com.ruege.mobile.utilss.Resource
import dagger.hilt.android.AndroidEntryPoint
import android.widget.CheckBox
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder

@AndroidEntryPoint
class TheoryFragment : Fragment(), ContentAdapter.OnContentClickListener, ContentAdapter.OnItemSelectionListener, ContentAdapter.OnItemDeleteListener {

    companion object {
        private const val TAG = "TheoryFragment"
    }

    private var _binding: FragmentTheoryBinding? = null
    private val binding get() = _binding!!
    private lateinit var contentAdapter: ContentAdapter
    private val viewModel: TheoryViewModel by activityViewModels()

    private lateinit var shimmerLayout: ShimmerFrameLayout
    private lateinit var errorTextView: TextView
    private lateinit var selectAllCheckBox: CheckBox
    private lateinit var fabDownload: FloatingActionButton

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTheoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        shimmerLayout = binding.shimmerContent
        errorTextView = binding.errorTextView
        selectAllCheckBox = binding.selectAllCheckbox
        fabDownload = binding.fabDownload
        
        setupRecyclerView()
        observeTheoryState()
        observeSelectionState()
        observeBatchDownloadResult()
        observeDeleteStatus()

        selectAllCheckBox.setOnCheckedChangeListener { _, isChecked ->
            viewModel.selectAllTheories(isChecked)
        }

        fabDownload.setOnClickListener {
            viewModel.downloadSelectedTheories()
        }
    }
    
    private fun setupRecyclerView() {
        contentAdapter = ContentAdapter(this)
        contentAdapter.setOnItemSelectionListener(this)
        contentAdapter.setOnItemDeleteListener(this)
        binding.recyclerViewTheory.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = contentAdapter
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        }
    }

    private fun observeTheoryState() {
        viewModel.theoryItemsState.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    shimmerLayout.visibility = View.VISIBLE
                    shimmerLayout.startShimmer()
                    binding.recyclerViewTheory.visibility = View.GONE
                    errorTextView.visibility = View.GONE
                }
                is Resource.Success -> {
                    shimmerLayout.stopShimmer()
                    shimmerLayout.visibility = View.GONE
                    val items = resource.data
                    if (items != null && items.isNotEmpty()) {
                        contentAdapter.submitList(items)
                        binding.recyclerViewTheory.visibility = View.VISIBLE
                        errorTextView.visibility = View.GONE
                        updateSelectAllCheckboxState(items)
                    } else {
                        errorTextView.text = "Нет данных по теории."
                        errorTextView.visibility = View.VISIBLE
                        binding.recyclerViewTheory.visibility = View.GONE
                    }
                }
                is Resource.Error -> {
                    shimmerLayout.stopShimmer()
                    shimmerLayout.visibility = View.GONE
                    binding.recyclerViewTheory.visibility = View.GONE

                    val staleData = resource.data
                    if (staleData != null && staleData.isNotEmpty()) {
                        contentAdapter.submitList(staleData)
                        binding.recyclerViewTheory.visibility = View.VISIBLE
                        Toast.makeText(requireContext(), "Ошибка обновления: ${resource.message}. Показаны старые данные.", Toast.LENGTH_LONG).show()
                    } else {
                        errorTextView.text = "Ошибка загрузки: ${resource.message}"
                        errorTextView.visibility = View.VISIBLE
                        errorTextView.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark))
                    }
                }
                else -> {

                }
            }
        }
    }
    
    override fun onContentClick(item: ContentItem) {
        try {
            Log.d(TAG, "Теория выбрана: ${item.title}, ID: ${item.contentId}")
            
            val mainActivity = requireActivity() as? MainActivity
            mainActivity?.showBottomSheet(item.title, item.description ?: "", item.contentId, item.type)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при открытии теории: ${e.message}", e)
            Toast.makeText(requireContext(), "Не удалось открыть теорию", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onItemSelectionChanged(item: ContentItem, isSelected: Boolean) {
        viewModel.selectTheory(item, isSelected)
    }

    override fun onItemDelete(item: ContentItem) {
        if (!item.isDownloaded) return

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Удаление теории")
            .setMessage("Вы уверены, что хотите удалить теорию \"${item.title}\"?")
            .setPositiveButton("Удалить") { _, _ ->
                viewModel.deleteDownloadedTheory(item.contentId)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun observeSelectionState() {
        viewModel.isAnyTheorySelected.observe(viewLifecycleOwner) { isSelected ->
            fabDownload.visibility = if (isSelected) View.VISIBLE else View.GONE
        }
    }

    private fun observeDeleteStatus() {
        viewModel.deleteStatus.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    
                }
                is Resource.Success -> {
                    Toast.makeText(requireContext(), "Теория успешно удалена", Toast.LENGTH_SHORT).show()
                }
                is Resource.Error -> {
                    Toast.makeText(requireContext(), "Ошибка удаления: ${resource.message}", Toast.LENGTH_LONG).show()
                }
                else -> {

                }
            }
        }
    }

    private fun observeBatchDownloadResult() {
        viewModel.batchDownloadResult.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    fabDownload.isEnabled = false
                    Toast.makeText(requireContext(), "Начинаю скачивание...", Toast.LENGTH_SHORT).show()
                }
                is Resource.Success -> {
                    fabDownload.isEnabled = true
                    Toast.makeText(requireContext(), resource.data, Toast.LENGTH_LONG).show()
                }
                is Resource.Error -> {
                    fabDownload.isEnabled = true
                    Toast.makeText(requireContext(), resource.message, Toast.LENGTH_LONG).show()
                }
                else -> {

                }
            }
        }
    }

    private fun updateSelectAllCheckboxState(items: List<ContentItem>) {
        val allSelected = items.all { it.isSelected }
        selectAllCheckBox.setOnCheckedChangeListener(null) 
        selectAllCheckBox.isChecked = allSelected
        selectAllCheckBox.setOnCheckedChangeListener { _, isChecked ->
            viewModel.selectAllTheories(isChecked)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 