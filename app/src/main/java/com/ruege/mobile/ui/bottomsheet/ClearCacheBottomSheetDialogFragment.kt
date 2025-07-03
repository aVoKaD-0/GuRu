package com.ruege.mobile.ui.bottomsheet

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.ruege.mobile.databinding.LayoutClearCacheContentBinding
import com.ruege.mobile.ui.viewmodel.TasksViewModel
import com.ruege.mobile.ui.viewmodel.TheoryViewModel
import com.ruege.mobile.ui.viewmodel.VariantViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ClearCacheBottomSheetDialogFragment : BottomSheetDialogFragment() {

    private val theoryViewModel: TheoryViewModel by activityViewModels()
    private val tasksViewModel: TasksViewModel by activityViewModels()
    private val variantViewModel: VariantViewModel by activityViewModels()

    private var _binding: LayoutClearCacheContentBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = LayoutClearCacheContentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnConfirmClearCache.setOnClickListener {
            showConfirmationDialog()
        }
    }

    private fun showConfirmationDialog() {
        val itemsToClear = mutableListOf<String>()
        if (binding.checkboxClearTheory.isChecked) itemsToClear.add("теорию")
        if (binding.checkboxClearTasks.isChecked) itemsToClear.add("задания")
        if (binding.checkboxClearVariants.isChecked) itemsToClear.add("варианты")

        if (itemsToClear.isEmpty()) {
            Toast.makeText(requireContext(), "Выберите, что нужно очистить", Toast.LENGTH_SHORT).show()
            return
        }

        val message = "Вы уверены, что хотите очистить выбранные данные (${itemsToClear.joinToString(", ")})? Это действие нельзя будет отменить."

        AlertDialog.Builder(requireContext())
            .setTitle("Подтверждение очистки")
            .setMessage(message)
            .setPositiveButton("Очистить") { dialog, _ ->
                clearSelectedData()
                dialog.dismiss()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun clearSelectedData() {
        if (binding.checkboxClearTheory.isChecked) {
            theoryViewModel.clearAllDownloadedTheory()
        }
        if (binding.checkboxClearTasks.isChecked) {
            tasksViewModel.clearAllDownloadedTasks()
        }
        if (binding.checkboxClearVariants.isChecked) {
            variantViewModel.clearAllDownloadedVariants()
        }

        Toast.makeText(requireContext(), "Выбранные данные успешно очищены", Toast.LENGTH_LONG).show()
        dismiss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "ClearCacheBottomSheet"

        fun newInstance(): ClearCacheBottomSheetDialogFragment {
            return ClearCacheBottomSheetDialogFragment()
        }
    }
} 