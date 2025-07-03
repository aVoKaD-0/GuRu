package com.ruege.mobile.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.ruege.mobile.ui.bottomsheet.EssayCheckBottomSheetDialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.ruege.mobile.databinding.FragmentEssayBinding
import com.ruege.mobile.model.TaskItem
import com.ruege.mobile.ui.adapter.EssayAdapter
import com.ruege.mobile.ui.viewmodel.EssayViewModel
import com.ruege.mobile.utils.Resource
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class EssayFragment : Fragment(), EssayAdapter.EssayItemClickListener {

    private var _binding: FragmentEssayBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: EssayAdapter
    private val viewModel: EssayViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEssayBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeEssayTasks()
    }

    private fun setupRecyclerView() {
        adapter = EssayAdapter(this)
        binding.recyclerViewEssay.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewEssay.adapter = adapter
    }

    private fun observeEssayTasks() {
        viewModel.essayTasks.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    binding.essayProgressBar.visibility = View.VISIBLE
                    binding.recyclerViewEssay.visibility = View.GONE
                    binding.essayErrorText.visibility = View.GONE
                }
                is Resource.Success -> {
                    binding.essayProgressBar.visibility = View.GONE
                    binding.recyclerViewEssay.visibility = View.VISIBLE
                    binding.essayErrorText.visibility = View.GONE
                    resource.data?.let { adapter.updateItems(it) }
                }
                is Resource.Error -> {
                    binding.essayProgressBar.visibility = View.GONE
                    binding.recyclerViewEssay.visibility = View.GONE
                    binding.essayErrorText.visibility = View.VISIBLE
                    binding.essayErrorText.text = resource.message
                }
                else -> {

                }
            }
        }
    }

    override fun onEssayItemClick(item: TaskItem) {
        Timber.d("Essay clicked: ${item.title}")
        val bottomSheet = EssayCheckBottomSheetDialogFragment.newInstance(item)
        bottomSheet.show(parentFragmentManager, EssayCheckBottomSheetDialogFragment.TAG)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 