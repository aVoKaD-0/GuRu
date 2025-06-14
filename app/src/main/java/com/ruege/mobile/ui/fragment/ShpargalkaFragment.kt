package com.ruege.mobile.ui.fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.ruege.mobile.MainActivity
import com.ruege.mobile.ui.adapter.ContentAdapter
import com.ruege.mobile.databinding.FragmentShpargalkaBinding
import com.ruege.mobile.model.ContentItem
import com.ruege.mobile.ui.viewmodel.ShpargalkaViewModel
import com.ruege.mobile.utilss.Resource
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ShpargalkaFragment : Fragment(), ContentAdapter.OnContentClickListener, ContentAdapter.OnItemSelectionListener {

    private var _binding: FragmentShpargalkaBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ShpargalkaViewModel by viewModels()
    private lateinit var contentAdapter: ContentAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentShpargalkaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeShpargalkaState()
        viewModel.loadShpargalkaItems()
    }

    private fun setupRecyclerView() {
        if (isAdded) {
            contentAdapter = ContentAdapter(this)
            contentAdapter.setOnItemSelectionListener(this)
            binding.recyclerViewShpargalka.apply {
                layoutManager = LinearLayoutManager(requireContext())
                adapter = contentAdapter
                addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
            }
        }
    }

    private fun observeShpargalkaState() {
        viewModel.shpargalkaItemsState.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    binding.shimmerContentShpargalka.visibility = View.VISIBLE
                    binding.shimmerContentShpargalka.startShimmer()
                    binding.recyclerViewShpargalka.visibility = View.GONE
                    binding.errorTextViewShpargalka.visibility = View.GONE
                }
                is Resource.Success -> {
                    binding.shimmerContentShpargalka.stopShimmer()
                    binding.shimmerContentShpargalka.visibility = View.GONE
                    
                    val items = resource.data
                    if (items != null && items.isNotEmpty()) {
                        contentAdapter.submitList(items)
                        binding.recyclerViewShpargalka.visibility = View.VISIBLE
                        binding.errorTextViewShpargalka.visibility = View.GONE
                    } else {
                        binding.errorTextViewShpargalka.text = "Нет доступных шпаргалок."
                        binding.errorTextViewShpargalka.visibility = View.VISIBLE
                        binding.recyclerViewShpargalka.visibility = View.GONE
                    }
                }
                is Resource.Error -> {
                    binding.shimmerContentShpargalka.stopShimmer()
                    binding.shimmerContentShpargalka.visibility = View.GONE
                    binding.recyclerViewShpargalka.visibility = View.GONE
                    
                    binding.errorTextViewShpargalka.text = "Ошибка загрузки: ${resource.message}"
                    binding.errorTextViewShpargalka.visibility = View.VISIBLE
                    binding.errorTextViewShpargalka.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark))
                    
                    // Если есть старые данные, показываем их
                    val staleData = resource.data
                    if (staleData != null && staleData.isNotEmpty()) {
                        contentAdapter.submitList(staleData)
                        binding.recyclerViewShpargalka.visibility = View.VISIBLE
                        Toast.makeText(requireContext(), "Ошибка обновления: ${resource.message}. Показаны старые данные.", Toast.LENGTH_LONG).show()
                    }
                }
                else -> {
                    // Обработка для любых других возможных состояний
                    binding.shimmerContentShpargalka.stopShimmer()
                    binding.shimmerContentShpargalka.visibility = View.GONE
                    binding.recyclerViewShpargalka.visibility = View.GONE
                    binding.errorTextViewShpargalka.visibility = View.VISIBLE
                    binding.errorTextViewShpargalka.text = "Неизвестное состояние"
                }
            }
        }
    }

    override fun onContentClick(item: ContentItem) {
        try {
            Log.d("ShpargalkaFragment", "Шпаргалка выбрана: ${item.title}, ID: ${item.contentId}")
            val mainActivity = requireActivity() as? MainActivity
            mainActivity?.showBottomSheet(item.title, item.description ?: "", item.contentId, item.type)
        } catch (e: Exception) {
            Log.e("ShpargalkaFragment", "Ошибка при открытии шпаргалки: ${e.message}", e)
            Toast.makeText(requireContext(), "Не удалось открыть шпаргалку", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onItemSelectionChanged(item: ContentItem, isSelected: Boolean) {
        // Пока не используется для шпаргалок
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 