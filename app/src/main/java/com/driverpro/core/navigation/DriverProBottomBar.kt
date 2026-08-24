package com.driverpro.core.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.driverpro.R
import com.driverpro.core.ui.theme.DarkNavUnselected

/**
 * Seção de primeiro nível do aplicativo — uma aba da barra inferior.
 *
 * São **abas**, e não telas empilhadas: trocar de aba não empilha histórico,
 * e cada uma guarda o próprio estado (posição de rolagem, filtro escolhido).
 * O [route] é o que amarra a aba ao destino do grafo, para a barra saber qual
 * pintar de verde sem que ninguém precise passar isso à mão.
 */
enum class DriverProTab(val route: String) {
    DASHBOARD(DriverProDestination.DASHBOARD),
    EARNINGS(DriverProDestination.EARNINGS_LIST),
    EXPENSES(DriverProDestination.EXPENSES_LIST),
    VEHICLES(DriverProDestination.VEHICLE_LIST),
    MORE(DriverProDestination.MORE),
}

/**
 * Barra inferior fixa, presente em todas as seções de primeiro nível.
 *
 * Antes ela existia **só no dashboard**, e as demais seções eram telas
 * empilhadas com seta de voltar: ir de Ganhos para Gastos exigia voltar ao
 * dashboard primeiro. Agora as cinco são irmãs, e qualquer uma leva a
 * qualquer outra em um toque.
 *
 * **Verde quando ativa, cinza quando não**, e nada mais. O
 * `NavigationBarItem` do Material 3 desenha, por padrão, uma pílula colorida
 * (`secondaryContainer`) atrás do ícone selecionado — é a assinatura do M3, e
 * é justamente o que a referência não tem. Zerar o `indicatorColor` apaga a
 * cápsula e deixa a cor do próprio ícone e do rótulo carregarem o estado.
 *
 * Cor não é o único sinal: o item ativo também recebe `selected = true`, que
 * o TalkBack anuncia. Quem não distingue verde de cinza continua sabendo onde
 * está.
 */
@Composable
fun DriverProBottomBar(
    selected: DriverProTab,
    onSelect: (DriverProTab) -> Unit,
) {
    Column {
        // Fio separando a barra do conteúdo. Na referência é o único limite
        // entre os dois: a barra tem a mesma cor do fundo da página, então
        // sem essa linha ela não existe como região.
        HorizontalDivider(
            thickness = Dp.Hairline,
            color = MaterialTheme.colorScheme.outlineVariant,
        )
        NavigationBar(
            containerColor = MaterialTheme.colorScheme.background,
            tonalElevation = 0.dp,
        ) {
            TabItem(
                tab = DriverProTab.DASHBOARD,
                selected = selected,
                onSelect = onSelect,
                icon = Icons.Default.Dashboard,
                label = stringResource(R.string.nav_dashboard),
            )
            TabItem(
                tab = DriverProTab.EARNINGS,
                selected = selected,
                onSelect = onSelect,
                icon = Icons.Default.Payments,
                label = stringResource(R.string.earnings_list_title),
            )
            TabItem(
                tab = DriverProTab.EXPENSES,
                selected = selected,
                onSelect = onSelect,
                icon = Icons.AutoMirrored.Filled.ReceiptLong,
                label = stringResource(R.string.expenses_list_title),
            )
            TabItem(
                tab = DriverProTab.VEHICLES,
                selected = selected,
                onSelect = onSelect,
                icon = Icons.Default.DirectionsCar,
                label = stringResource(R.string.vehicle_list_title),
            )
            TabItem(
                tab = DriverProTab.MORE,
                selected = selected,
                onSelect = onSelect,
                icon = Icons.Default.MoreHoriz,
                label = stringResource(R.string.nav_more),
            )
        }
    }
}

@Composable
private fun RowScope.TabItem(
    tab: DriverProTab,
    selected: DriverProTab,
    onSelect: (DriverProTab) -> Unit,
    icon: ImageVector,
    label: String,
) {
    val isSelected = tab == selected
    NavigationBarItem(
        selected = isSelected,
        // Tocar na aba em que já se está não faz nada — navegar de novo
        // recriaria a tela e descartaria a rolagem, que é o oposto do que o
        // toque significa.
        onClick = { if (!isSelected) onSelect(tab) },
        icon = { Icon(icon, contentDescription = null) },
        label = { Text(label) },
        colors = NavigationBarItemDefaults.colors(
            indicatorColor = Color.Transparent,
            selectedIconColor = MaterialTheme.colorScheme.primary,
            selectedTextColor = MaterialTheme.colorScheme.primary,
            unselectedIconColor = DarkNavUnselected,
            unselectedTextColor = DarkNavUnselected,
        ),
    )
}
