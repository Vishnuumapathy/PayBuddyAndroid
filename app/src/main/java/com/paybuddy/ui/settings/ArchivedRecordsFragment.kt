package com.paybuddy.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.paybuddy.data.repository.MainRepository
import com.paybuddy.ui.screens.ArchivedRecordsScreen
import com.paybuddy.ui.theme.PayBuddyTheme
import com.paybuddy.utils.SessionManager
import com.paybuddy.viewmodel.CustomerViewModel
import com.paybuddy.viewmodel.SalesViewModel
import com.paybuddy.viewmodel.ViewModelFactory

class ArchivedRecordsFragment : Fragment() {

    private lateinit var customerViewModel: CustomerViewModel
    private lateinit var salesViewModel: SalesViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        android.util.Log.d("ArchivedDebug", "ArchivedRecordsFragment: onCreate")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        android.util.Log.d("ArchivedDebug", "ArchivedRecordsFragment: onCreateView")

        try {
            android.util.Log.d("ArchivedDebug", "ArchivedRecordsFragment: Before ViewModel creation")
            val context = requireContext().applicationContext
            val db = FirebaseFirestore.getInstance()
            val repository = MainRepository(db)
            val sessionManager = SessionManager(context)
            val factory = ViewModelFactory(repository, sessionManager)
            
            customerViewModel = ViewModelProvider(this, factory)[CustomerViewModel::class.java]
            salesViewModel = ViewModelProvider(this, factory)[SalesViewModel::class.java]
            android.util.Log.d("ArchivedDebug", "ArchivedRecordsFragment: After ViewModel creation")
        } catch (e: Exception) {
            android.util.Log.e("ArchivedDebug", "Error initializing ViewModels", e)
        }

        android.util.Log.d("ArchivedDebug", "ArchivedRecordsFragment: Before setting Compose content")
        return try {
            ComposeView(requireContext()).apply {
                setContent {
                    PayBuddyTheme {
                        if (::customerViewModel.isInitialized && ::salesViewModel.isInitialized) {
                            val vendorId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
                            ArchivedRecordsScreen(
                                vendorId = vendorId,
                                customerViewModel = customerViewModel,
                                salesViewModel = salesViewModel,
                                onBackClick = {
                                    findNavController().popBackStack()
                                }
                            )
                        } else {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Error initializing records", color = Color.White)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("ArchivedDebug", "Error creating ComposeView", e)
            View(requireContext())
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        android.util.Log.d("ArchivedDebug", "ArchivedRecordsFragment: onViewCreated")
    }
}
