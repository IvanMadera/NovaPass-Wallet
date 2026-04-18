package com.example.novapass.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.novapass.TicketViewModel
import com.example.novapass.ui.screens.TicketListScreen

@Composable
fun NovaPassNavGraph(viewModel: TicketViewModel = viewModel()) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "ticketList") {
        composable("ticketList") {
            TicketListScreen(
                viewModel = viewModel,
                onAddTicket = { name, uri, category, eventDate, eventTime, location, section, row, seat, pageIndex ->
                    viewModel.addTicket(name, uri, category, eventDate, eventTime, location, section, row, seat, pageIndex)
                }
            )
        }
    }
}
