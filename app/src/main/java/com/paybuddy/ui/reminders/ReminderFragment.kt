package com.paybuddy.ui.reminders

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.firebase.firestore.FirebaseFirestore
import com.paybuddy.data.repository.MainRepository
import com.paybuddy.ui.screens.RemindersScreen
import com.paybuddy.ui.theme.PayBuddyTheme
import com.paybuddy.utils.ReminderMessageBuilder
import com.paybuddy.utils.SessionManager
import com.paybuddy.utils.WhatsAppHelper
import com.paybuddy.viewmodel.ViewModelFactory

class ReminderFragment : Fragment() {

    private lateinit var viewModel: ReminderViewModel
    private lateinit var sessionManager: SessionManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val db = FirebaseFirestore.getInstance()
        val repository = MainRepository(db)
        sessionManager = SessionManager(requireContext())
        val factory = ViewModelFactory(repository, sessionManager)
        
        viewModel = ViewModelProvider(this, factory)[ReminderViewModel::class.java]

        return ComposeView(requireContext()).apply {
            setContent {
                PayBuddyTheme {
                    val uiState by viewModel.uiState.collectAsState()

                    LaunchedEffect(Unit) {
                        viewModel.loadReminders()
                    }

                    RemindersScreen(
                        uiState = uiState,
                        onSendWhatsApp = { reminderItem ->
                            sendWhatsAppReminder(reminderItem)
                        },
                        onClearError = {
                            viewModel.clearError()
                        }
                    )
                }
            }
        }
    }

    private fun sendWhatsAppReminder(reminderItem: ReminderItem) {
        val installment = reminderItem.installment
        val phone = reminderItem.customerPhone

        if (phone.isBlank()) {
            Toast.makeText(context, "Customer phone number is missing", Toast.LENGTH_SHORT).show()
            return
        }

        val message = ReminderMessageBuilder.buildPaymentReminderMessage(
            customerName = reminderItem.customerName,
            itemName = reminderItem.itemName,
            pendingAmount = installment.remainingAmount,
            dueDate = installment.dueDate
        )

        val success = WhatsAppHelper.openWhatsAppReminder(requireContext(), phone, message)
        if (success) {
            viewModel.markReminderSent(installment.installmentId)
        } else {
            Toast.makeText(context, "Could not open WhatsApp. Please check if it's installed.", Toast.LENGTH_SHORT).show()
        }
    }
}
