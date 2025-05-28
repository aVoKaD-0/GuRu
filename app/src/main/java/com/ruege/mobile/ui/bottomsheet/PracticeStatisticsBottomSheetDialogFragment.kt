package com.ruege.mobile.ui.bottomsheet

import android.app.Dialog
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.tabs.TabLayoutMediator
import com.ruege.mobile.R
import com.ruege.mobile.data.local.entity.ProgressEntity // Предполагаем, что данные из ProgressEntity
import com.ruege.mobile.model.PracticeStatisticItem // Из нашего адаптера
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

    // Инжектируем ViewModel
    private val practiceStatisticsViewModel: PracticeStatisticsViewModel by activityViewModels()

    private lateinit var statisticsAdapter: PracticeStatisticsAdapter

    private var rvStatistics: RecyclerView? = null
    private var pbLoading: ProgressBar? = null
    private var tvEmpty: TextView? = null

    // View для нового макета (fragment_practice_statistics.xml)
    private var viewPager: androidx.viewpager2.widget.ViewPager2? = null
    private var tabLayout: com.google.android.material.tabs.TabLayout? = null
    private var tvStatisticsTitle: TextView? = null
    private var cardOverallStats: androidx.cardview.widget.CardView? = null
    // ... другие View из cardOverallStats, если нужны прямые ссылки ...
    private var tvError: TextView? = null

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
        // _binding = BottomSheetPracticeStatisticsBinding.bind(view) // Удаляем эту строку

        setupViewPagerAndTabs()
        observeOverallStatistics()
        // observeProgressData() // Удаляем или комментируем, так как ViewModel изменился
    }

    private fun observeOverallStatistics() {
        viewLifecycleOwner.lifecycleScope.launch {
            // Используем collect, так как practiceStatisticsViewModel.overallStatisticsUiModel теперь LiveData
            practiceStatisticsViewModel.overallStatisticsUiModel.observe(viewLifecycleOwner) { stats ->
                if (stats != null) {
                    binding.tvAttemptCount.text = stats.totalAttempts.toString() // Прямой доступ через binding
                    binding.tvCorrectCount.text = stats.correctAttempts.toString() // Прямой доступ через binding
                    binding.tvSuccessRate.text = "${stats.successRate}%" // Прямой доступ через binding
                    binding.progressOverall.progress = stats.successRate          // Прямой доступ через binding
                    binding.cardOverallStats.visibility = View.VISIBLE // Показываем карточку
                    binding.tvError.visibility = View.GONE
                } else {
                    binding.cardOverallStats.visibility = View.GONE // Скрываем, если нет данных
                    // Можно показать tvError с сообщением, если stats == null долгое время
                    // binding.tvError.text = "Данные общей статистики отсутствуют."
                    // binding.tvError.visibility = View.VISIBLE
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
                else -> null
            }
        }.attach()
    }

    // TODO: Этот метод нужно будет адаптировать или удалить,
    // так как RecyclerView теперь будет внутри ViewPager
    /*
    private fun setupRecyclerView() {
        statisticsAdapter = PracticeStatisticsAdapter(emptyList()) // Инициализируем пустым списком
        rvStatistics?.apply {
            adapter = statisticsAdapter
            layoutManager = LinearLayoutManager(context)
        }
    }
    */

    // Пример метода для обновления общей статистики
    private fun updateOverallStatistics(progressEntities: List<ProgressEntity>) {
        // Найдите нужные TextView внутри cardOverallStats
        val tvAttemptCount: TextView? = view?.findViewById(R.id.tvAttemptCount)
        val tvCorrectCount: TextView? = view?.findViewById(R.id.tvCorrectCount)
        val tvSuccessRate: TextView? = view?.findViewById(R.id.tvSuccessRate)
        val progressOverall: ProgressBar? = view?.findViewById(R.id.progressOverall)

        // TODO: Рассчитать общую статистику на основе PracticeAttemptEntity или PracticeStatisticsEntity.
        // Текущая ProgressEntity не содержит достаточно данных для детальной статистики.
        // Ниже приведены заглушки.

        var totalAttempts = 0 // TODO: Получить из агрегированных данных
        var totalCorrect = 0  // TODO: Получить из агрегированных данных

        // Пример: агрегация по ProgressEntity (очень упрощенно, только для примера)
        // Не используйте это в продакшене для точной статистики попыток.
        // totalAttempts = progressEntities.count { it.contentId.startsWith("task_group_") && it.percentage > 0 } 
        // totalCorrect = progressEntities.count { it.contentId.startsWith("task_group_") && it.percentage == 100 }
        
        val overallSuccessRate = if (totalAttempts > 0) (totalCorrect * 100 / totalAttempts) else 0

        tvAttemptCount?.text = totalAttempts.toString() 
        tvCorrectCount?.text = totalCorrect.toString()
        tvSuccessRate?.text = "$overallSuccessRate%"
        progressOverall?.progress = overallSuccessRate

        // Если более точных данных пока нет, можно оставить так:
        // tvAttemptCount?.text = "N/A"
        // tvCorrectCount?.text = "N/A"
        // tvSuccessRate?.text = "N/A"
        // progressOverall?.progress = 0
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
                        // Log.d("BottomSheetBehavior", "New state: $newState") // Раскомментировать для отладки
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