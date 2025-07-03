package com.ruege.mobile.ui.fragment

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import timber.log.Timber
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
import androidx.recyclerview.widget.RecyclerView
import com.facebook.shimmer.ShimmerFrameLayout
import com.getkeepsafe.taptargetview.TapTarget
import com.getkeepsafe.taptargetview.TapTargetSequence
import com.ruege.mobile.ui.adapter.TheoryAdapter
import com.ruege.mobile.databinding.FragmentTheoryBinding
import com.ruege.mobile.model.ContentItem
import com.ruege.mobile.ui.bottomsheet.TheoryBottomSheetDialogFragment
import com.ruege.mobile.ui.viewmodel.OnboardingViewModel
import com.ruege.mobile.ui.viewmodel.TheoryViewModel
import com.ruege.mobile.utils.Resource
import dagger.hilt.android.AndroidEntryPoint
import android.widget.CheckBox
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.content.Context

@AndroidEntryPoint
class TheoryFragment : Fragment(), TheoryAdapter.OnContentClickListener, TheoryAdapter.OnItemSelectionListener, TheoryAdapter.OnItemDeleteListener {

    companion object {
        private const val TAG = "TheoryFragment"
    }

    private var _binding: FragmentTheoryBinding? = null
    private val binding get() = _binding!!
    private lateinit var theoryAdapter: TheoryAdapter
    private val viewModel: TheoryViewModel by activityViewModels()
    private val onboardingViewModel: OnboardingViewModel by activityViewModels()

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
        
        setupOnboarding()
    }
    
    private fun setupRecyclerView() {
        theoryAdapter = TheoryAdapter(this)
        theoryAdapter.setOnItemSelectionListener(this)
        theoryAdapter.setOnItemDeleteListener(this)
        binding.recyclerViewTheory.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = theoryAdapter
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
                        theoryAdapter.submitList(items)
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
                        theoryAdapter.submitList(staleData)
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
            Timber.d("Теория выбрана: \${item.title}, ID: \${item.contentId}")

            val bottomSheet = TheoryBottomSheetDialogFragment.newInstance(
                item.contentId,
                item.title
            )
            bottomSheet.show(parentFragmentManager, TheoryBottomSheetDialogFragment.TAG)
        } catch (e: Exception) {
            Timber.d("Ошибка при открытии теории: \${e.message}", e)
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

    private fun setupOnboarding() {
        onboardingViewModel.isHomeOnboardingFinished.observe(viewLifecycleOwner) { finished ->
            if (finished == true) {
                (parentFragment as? HomeFragment)?.navigateToTab(0)
                showTheoryOnboarding()
                onboardingViewModel.setHomeOnboardingFinished(false)
            }
        }
    }

    private fun showTheoryOnboarding() {
        val prefs = requireActivity().getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val onboardingShown = prefs.getBoolean("onboarding_theory_shown", false)

        if (onboardingShown) {
            return
        }

        Handler(Looper.getMainLooper()).postDelayed({
            if (_binding == null) return@postDelayed

            val fabTarget = TapTarget.forView(binding.fabDownload, "(6/7) если хочешь изучить все вне цивилизации", "Скачивай нужные темы и изучай их оффлайн.")
                .outerCircleColor(com.ruege.mobile.R.color.primary)
                .targetCircleColor(android.R.color.white)
                .textColor(android.R.color.white)
                .dimColor(android.R.color.black)
                .drawShadow(true)
                .cancelable(false)
                .tintTarget(false)
                .transparentTarget(true)

            val theoryItemView = (binding.recyclerViewTheory.findViewHolderForAdapterPosition(0) as? TheoryAdapter.TheoryViewHolder)?.itemView
            if (theoryItemView == null) {
                prefs.edit().putBoolean("onboarding_theory_shown", true).apply()
                return@postDelayed
            }

            val theoryItemTarget = TapTarget.forView(theoryItemView, "(7/7) приступай к изучению", "Нажми на любую тему, чтобы открыть ее.")
                .outerCircleColor(com.ruege.mobile.R.color.primary)
                .targetCircleColor(android.R.color.white)
                .textColor(android.R.color.white)
                .dimColor(android.R.color.black)
                .drawShadow(true)
                .cancelable(false)
                .tintTarget(false)
                .transparentTarget(true)

            val wasFabVisible = binding.fabDownload.visibility == View.VISIBLE
            binding.fabDownload.visibility = View.VISIBLE

            TapTargetSequence(requireActivity())
                .targets(fabTarget, theoryItemTarget)
                .listener(object : TapTargetSequence.Listener {
                    override fun onSequenceFinish() {
                        if (!wasFabVisible) {
                           binding.fabDownload.visibility = View.GONE 
                        }
                        prefs.edit().putBoolean("onboarding_theory_shown", true).apply()
                    }

                    override fun onSequenceStep(lastTarget: TapTarget?, targetClicked: Boolean) {}

                    override fun onSequenceCanceled(lastTarget: TapTarget?) {
                        if (!wasFabVisible) {
                            binding.fabDownload.visibility = View.GONE
                        }
                        prefs.edit().putBoolean("onboarding_theory_shown", true).apply()
                    }
                }).start()

        }, 1000) 
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 