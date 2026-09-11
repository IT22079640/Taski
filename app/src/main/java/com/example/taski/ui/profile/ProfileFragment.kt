package com.example.taski.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.taski.R
import com.example.taski.reminder.NotificationPermissionHelper

class ProfileFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_profile, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindReminderStatus(view)
    }

    override fun onResume() {
        super.onResume()
        view?.let { bindReminderStatus(it) }
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
}
