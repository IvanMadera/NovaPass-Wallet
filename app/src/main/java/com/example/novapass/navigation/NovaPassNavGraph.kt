package com.example.novapass.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.novapass.TicketViewModel
import com.example.novapass.ui.screens.PdfViewerScreen
import com.example.novapass.ui.screens.TicketListScreen

@Composable
fun NovaPassNavGraph(viewModel: TicketViewModel = viewModel()) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "ticketList") {
        composable("ticketList") {
            TicketListScreen(
                viewModel = viewModel,
                onTicketClick = { ticket ->
                    navController.navigate("ticketDetail/${Uri.encode(ticket.uri)}/${ticket.pageIndex}")
                },
                onAddTicket = { name, uri, category, eventDate, eventTime, location, section, row, seat, pageIndex ->
                    viewModel.addTicket(name, uri, category, eventDate, eventTime, location, section, row, seat, pageIndex)
                }
            )
        }
        composable(
            "ticketDetail/{pdfUri}/{pageIndex}",
            arguments = listOf(
                navArgument("pdfUri") { type = NavType.StringType },
                navArgument("pageIndex") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val pdfUriString = backStackEntry.arguments?.getString("pdfUri")
            val pageIndex = backStackEntry.arguments?.getInt("pageIndex") ?: 0
            if (pdfUriString != null) {
                PdfViewerScreen(
                    uri = pdfUriString.toUri(),
                    pageIndex = pageIndex,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
