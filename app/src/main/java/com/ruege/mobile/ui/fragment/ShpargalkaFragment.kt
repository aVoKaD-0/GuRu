package com.ruege.mobile.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.ruege.mobile.ui.adapter.ShpargalkaAdapter
import com.ruege.mobile.databinding.FragmentShpargalkaBinding
import com.ruege.mobile.model.ContentItem
import com.ruege.mobile.ui.bottomsheet.ShpargalkaBottomSheetDialogFragment
import com.ruege.mobile.ui.viewmodel.ShpargalkaViewModel
import com.ruege.mobile.utils.Resource
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class ShpargalkaFragment : Fragment(), ShpargalkaAdapter.OnContentClickListener {

    private var _binding: FragmentShpargalkaBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ShpargalkaViewModel by viewModels()
    private lateinit var shpargalkaAdapter: ShpargalkaAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentShpargalkaBinding.inflate(inflater, container, false)
        
        setupRecyclerView()
        observeViewModel()

        return binding.root
    }

    private fun setupRecyclerView() {
        shpargalkaAdapter = ShpargalkaAdapter(this)
        binding.recyclerViewShpargalka.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = shpargalkaAdapter
            setHasFixedSize(true)
        }
    }

    private fun observeViewModel() {
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
                        shpargalkaAdapter.submitList(items)
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
                    
                    val staleData = resource.data
                    if (staleData != null && staleData.isNotEmpty()) {
                        shpargalkaAdapter.submitList(staleData)
                        binding.recyclerViewShpargalka.visibility = View.VISIBLE
                        Toast.makeText(requireContext(), "Ошибка обновления: ${resource.message}. Показаны старые данные.", Toast.LENGTH_LONG).show()
                    }
                }
                else -> {
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
            Timber.d("Шпаргалка выбрана: ${item.title}, ID: ${item.contentId}")
            val bottomSheet = ShpargalkaBottomSheetDialogFragment.newInstance(item.contentId, item.title, item.description)
            bottomSheet.show(childFragmentManager, ShpargalkaBottomSheetDialogFragment.TAG)
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при открытии шпаргалки: ${e.message}")
            Toast.makeText(requireContext(), "Не удалось открыть шпаргалку", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 