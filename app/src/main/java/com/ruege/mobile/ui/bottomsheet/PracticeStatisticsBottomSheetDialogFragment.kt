package com.ruege.mobile.ui.bottomsheet

import android.app.Dialog
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.tabs.TabLayoutMediator
import com.ruege.mobile.R
import com.ruege.mobile.data.local.entity.ProgressEntity
import com.ruege.mobile.ui.adapter.PracticeStatisticsAdapter
import com.ruege.mobile.ui.adapter.PracticeStatisticsPagerAdapter
import com.ruege.mobile.ui.viewmodel.PracticeStatisticsViewModel
import com.ruege.mobile.databinding.FragmentPracticeStatisticsBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope

@AndroidEntryPoint
class PracticeStatisticsBottomSheetDialogFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentPracticeStatisticsBinding? = null
    private val binding get() = _binding!!

    private val practiceStatisticsViewModel: PracticeStatisticsViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPracticeStatisticsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViewPagerAndTabs()
        observeOverallStatistics()
    }

    private fun observeOverallStatistics() {
        viewLifecycleOwner.lifecycleScope.launch {
            practiceStatisticsViewModel.overallStatisticsUiModel.observe(viewLifecycleOwner) { stats ->
                if (stats != null) {
                    binding.tvAttemptCount.text = stats.totalAttempts.toString()
                    binding.tvCorrectCount.text = stats.correctAttempts.toString()
                    binding.tvSuccessRate.text = "${stats.successRate}%"
                    binding.progressOverall.progress = stats.successRate
                    binding.cardOverallStats.visibility = View.VISIBLE
                    binding.tvError.visibility = View.GONE
                } else {
                    binding.cardOverallStats.visibility = View.GONE
                }
            }
        }
    }

    private fun setupViewPagerAndTabs() {
        val adapter = PracticeStatisticsPagerAdapter(this)
        binding.viewPager.adapter = adapter

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "По номерам"
                1 -> "Последние попытки"
                2 -> "По вариантам"
                3 -> "Сочинения"
                else -> "Статистика"
            }
        }.attach()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.setOnShowListener { dialogInterface ->
            val bottomSheetDialog = dialogInterface as BottomSheetDialog
            bottomSheetDialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)?.let { bottomSheet ->
                
                val windowHeight = getWindowHeight()

                val layoutParams = bottomSheet.layoutParams
                layoutParams.height = windowHeight 
                bottomSheet.layoutParams = layoutParams

                val behavior = BottomSheetBehavior.from(bottomSheet)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED 
                behavior.isFitToContents = false 
                behavior.skipCollapsed = true    
                behavior.peekHeight = windowHeight 

                behavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                    override fun onStateChanged(bottomSheet: View, newState: Int) {
                        if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                            dismiss()
                        }
                    }
                    override fun onSlide(bottomSheet: View, slideOffset: Float) {}
                })
            }
        }
        return dialog
    }

    private fun getWindowHeight(): Int {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            val windowMetrics = activity?.windowManager?.currentWindowMetrics
            val insets = windowMetrics?.windowInsets?.getInsetsIgnoringVisibility(android.view.WindowInsets.Type.systemBars())
            (windowMetrics?.bounds?.height() ?: DisplayMetrics().heightPixels) - (insets?.top ?: 0) - (insets?.bottom ?: 0)
        } else {
            val displayMetrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            activity?.windowManager?.defaultDisplay?.getMetrics(displayMetrics)
            displayMetrics.heightPixels
        }
    }

    companion object {
        const val TAG = "PracticeStatisticsBottomSheet"
        fun newInstance(): PracticeStatisticsBottomSheetDialogFragment {
            return PracticeStatisticsBottomSheetDialogFragment()
        }
    }
} 