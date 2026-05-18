package com.example.novapass.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.novapass.NovaPassApplication
import com.example.novapass.feature.tickets.presentation.TicketViewModel
import com.example.novapass.feature.tickets.presentation.TicketViewModelFactory
import com.example.novapass.feature.tickets.ui.screens.TicketListScreen

// ─────────────────────────────────────────────────────────────────────────
// NovaPassNavGraph — grafo de navegación de la app.
// Toda la lógica de negocio vive en TicketViewModel; el NavGraph
// ya no necesita lambdas suspend ni parámetros de formulario.
// ─────────────────────────────────────────────────────────────────────────
@Composable
fun NovaPassNavGraph() {
    val navController = rememberNavController()
    val application = LocalContext.current.applicationContext as NovaPassApplication
    val viewModel: TicketViewModel = viewModel(
        factory = TicketViewModelFactory(application.appContainer)
    )

    NavHost(navController = navController, startDestination = "ticketList") {
        composable("ticketList") {
            TicketListScreen(viewModel = viewModel)
        }
    }
}
