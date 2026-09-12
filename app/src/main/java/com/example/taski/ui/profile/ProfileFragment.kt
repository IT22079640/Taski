package com.example.taski.ui.profile

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.taski.R
import com.example.taski.plan.DailyWorkCapacityStore
import com.example.taski.plan.FocusPlanBuilder
import com.example.taski.reminder.NotificationPermissionHelper
import com.example.taski.utils.DateUtils
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class ProfileFragment : Fragment() {
    private lateinit var capacityStore: DailyWorkCapacityStore

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_profile, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        capacityStore = DailyWorkCapacityStore.from(requireContext())
        bindReminderStatus(view)
        bindDailyWorkTime(view)
        view.findViewById<MaterialButton>(R.id.btn_daily_work_change).setOnClickListener {
            showDailyWorkTimeDialog()
        }
    }

    override fun onResume() {
        super.onResume()
        view?.let {
            bindReminderStatus(it)
            bindDailyWorkTime(it)
        }
    }

    private fun bindReminderStatus(view: View) {
        view.findViewById<TextView>(R.id.text_reminders_body).setText(
            if (NotificationPermissionHelper.canPostNotifications(requireContext())) {
                R.string.profile_reminders_body
            } else {
                R.string.profile_reminders_disabled_body
            }
        )
    }

    private fun bindDailyWorkTime(view: View) {
        val minutes = capacityStore.getMinutes()
        view.findViewById<TextView>(R.id.text_daily_work_value).text = getString(
            R.string.profile_daily_work_value,
            FocusPlanBuilder.windowLabel(minutes)
        )
    }

    private fun showDailyWorkTimeDialog() {
        val options = arrayOf(
            getString(R.string.plan_time_30m),
            getString(R.string.plan_time_1h),
            getString(R.string.plan_time_2h),
            getString(R.string.plan_time_3h),
            getString(R.string.plan_time_4h),
            getString(R.string.plan_time_custom)
        )
        val values = intArrayOf(
            FocusPlanBuilder.PRESET_THIRTY_MINUTES,
            FocusPlanBuilder.PRESET_ONE_HOUR_MINUTES,
            FocusPlanBuilder.PRESET_TWO_HOURS_MINUTES,
            FocusPlanBuilder.PRESET_THREE_HOURS_MINUTES,
            FocusPlanBuilder.PRESET_FOUR_HOURS_MINUTES,
            -1
        )
        val current = capacityStore.getMinutes()
        val checked = values.indexOf(current).let { if (it >= 0) it else values.lastIndex }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.profile_daily_work_dialog_title)
            .setSingleChoiceItems(options, checked) { dialog, which ->
                val minutes = values[which]
                if (minutes < 0) {
                    dialog.dismiss()
                    showCustomTimeDialog()
                } else {
                    capacityStore.setMinutes(minutes)
                    view?.let { bindDailyWorkTime(it) }
                    dialog.dismiss()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showCustomTimeDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_custom_focus_time, null, false)
        val layout = dialogView.findViewById<TextInputLayout>(R.id.layout_custom_hours)
        val input = dialogView.findViewById<TextInputEditText>(R.id.input_custom_hours)
        val currentMinutes = capacityStore.getMinutes()
        if (!FocusPlanBuilder.isPresetMinutes(currentMinutes) && currentMinutes > 0) {
            input.setText(DateUtils.minutesToHoursText(currentMinutes))
        }

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.plan_custom_time_title)
            .setView(dialogView)
            .setPositiveButton(R.string.plan_custom_time_apply, null)
            .setNegativeButton(R.string.cancel, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
                val hours = input.text?.toString()?.trim()?.toDoubleOrNull()
                val minutes = hours?.let { DateUtils.hoursToMinutes(it) } ?: 0
                if (hours == null || hours <= 0.0 || minutes <= 0) {
                    layout.error = getString(R.string.plan_custom_time_error)
                    return@setOnClickListener
                }
                if (minutes > FocusPlanBuilder.MAX_AVAILABLE_MINUTES) {
                    layout.error = getString(R.string.plan_custom_time_max_error)
                    return@setOnClickListener
                }
                capacityStore.setMinutes(minutes)
                view?.let { bindDailyWorkTime(it) }
                dialog.dismiss()
            }
        }
        dialog.show()
    }
}
