package com.ruege.mobile.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.ruege.mobile.databinding.FragmentRecentAttemptsBinding
import com.ruege.mobile.ui.adapter.RecentAttemptsAdapter
import com.ruege.mobile.ui.viewmodel.PracticeStatisticsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class RecentAttemptsFragment : Fragment() {

    private var _binding: FragmentRecentAttemptsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PracticeStatisticsViewModel by activityViewModels()
    private lateinit var recentAttemptsAdapter: RecentAttemptsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecentAttemptsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeRecentAttempts()
    }

    private fun setupRecyclerView() {
        recentAttemptsAdapter = RecentAttemptsAdapter()
        binding.rvRecentAttempts.apply {
            adapter = recentAttemptsAdapter
            layoutManager = LinearLayoutManager(context)
        }
    }

    private fun observeRecentAttempts() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.recentAttempts.observe(viewLifecycleOwner) { attempts ->
                    binding.pbLoadingRecentAttempts.visibility = View.GONE
                    if (attempts == null) {
                        binding.pbLoadingRecentAttempts.visibility = View.VISIBLE
                        binding.tvNoRecentAttempts.visibility = View.GONE
                        binding.rvRecentAttempts.visibility = View.GONE
                    } else if (attempts.isEmpty()) {
                        binding.tvNoRecentAttempts.visibility = View.VISIBLE
                        binding.rvRecentAttempts.visibility = View.GONE
                    } else {
                        binding.tvNoRecentAttempts.visibility = View.GONE
                        binding.rvRecentAttempts.visibility = View.VISIBLE
                        recentAttemptsAdapter.submitList(attempts)
                    }
                }
            }
        }
        if (viewModel.recentAttempts.value == null) {
             binding.pbLoadingRecentAttempts.visibility = View.VISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance() = RecentAttemptsFragment()
    }
} 