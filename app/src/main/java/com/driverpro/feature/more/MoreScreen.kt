package com.driverpro.feature.more

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.SettingsBackupRestore
import androidx.compose.material.icons.filled.Weekend
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.driverpro.R
import com.driverpro.core.ui.component.IconChip
import com.driverpro.core.ui.component.ListItemCard
import com.driverpro.core.ui.theme.driverProTopAppBarColors

/**
 * Hub das seções secundárias (v0.14.0) — Uso pessoal, Manutenção e
 * Exportar/importar deixaram de ser ícones na TopAppBar do Dashboard e viraram
 * entradas aqui, aberta pelo item "Mais" da barra inferior.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(
    onBack: () -> Unit,
    onOpenPersonalUsage: () -> Unit,
    onOpenMaintenance: () -> Unit,
    onOpenBackup: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                colors = driverProTopAppBarColors(),
                title = { Text(stringResource(R.string.nav_more)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = innerPadding.calculateTopPadding() + 8.dp,
                bottom = innerPadding.calculateBottomPadding() + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                MoreEntry(
                    icon = Icons.Default.Weekend,
                    title = stringResource(R.string.personal_usage_title),
                    onClick = onOpenPersonalUsage,
                )
            }
            item {
                MoreEntry(
                    icon = Icons.Default.Build,
                    title = stringResource(R.string.maintenance_title),
                    onClick = onOpenMaintenance,
                )
            }
            item {
                MoreEntry(
                    icon = Icons.Default.SettingsBackupRestore,
                    title = stringResource(R.string.backup_title),
                    onClick = onOpenBackup,
                )
            }
        }
    }
}

@Composable
private fun MoreEntry(icon: ImageVector, title: String, onClick: () -> Unit) {
    ListItemCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        leading = {
            IconChip(
                icon = icon,
                tint = MaterialTheme.colorScheme.primary,
                contentDescription = null,
            )
        },
        trailing = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
    ) {
        Text(text = title, style = MaterialTheme.typography.titleLarge)
    }
}
