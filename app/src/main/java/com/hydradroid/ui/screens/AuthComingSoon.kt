package com.hydradroid.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hydradroid.ui.components.HydraButton
import com.hydradroid.ui.i18n.ls
import com.hydradroid.ui.theme.HydraColors

/** Placeholder for Hydra account sign-in: real auth is not wired yet. */
@Composable
fun AuthComingSoonScreen(onBack: () -> Unit) {
    val s = ls()
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Schedule, null, tint = HydraColors.SecondaryText60, modifier = Modifier.size(64.dp))
        Spacer(Modifier.height(16.dp))
        Text(
            s.t("Sign in"),
            style = MaterialTheme.typography.headlineMedium,
            color = HydraColors.TextBright,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            s.t("Coming soon"),
            style = MaterialTheme.typography.titleMedium,
            color = HydraColors.BrandTeal,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            s.t("Hydra account sign-in is not available yet and will appear in a future update."),
            style = MaterialTheme.typography.bodyMedium,
            color = HydraColors.Body,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        HydraButton(s.t("Back"), onBack, kind = "outline")
    }
}
