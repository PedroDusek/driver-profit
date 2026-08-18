package com.driverpro.core.ui.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.LocalCarWash
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.LocalParking
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Toll
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.driverpro.core.ui.theme.CategoryAccentColors
import com.driverpro.domain.model.ExpenseCategory

/** Ícone e cor de destaque fixos por categoria, para [IconChip], [DonutChart] e [CategoryLegendRow]. */
data class CategoryVisual(val icon: ImageVector, val color: Color)

fun ExpenseCategory.visual(): CategoryVisual = when (this) {
    ExpenseCategory.FUEL -> CategoryVisual(Icons.Default.LocalGasStation, CategoryAccentColors.fuel)
    ExpenseCategory.CHARGING -> CategoryVisual(Icons.Default.BatteryChargingFull, CategoryAccentColors.charging)
    ExpenseCategory.MAINTENANCE -> CategoryVisual(Icons.Default.Build, CategoryAccentColors.maintenance)
    ExpenseCategory.CAR_WASH -> CategoryVisual(Icons.Default.LocalCarWash, CategoryAccentColors.carWash)
    ExpenseCategory.TOLL -> CategoryVisual(Icons.Default.Toll, CategoryAccentColors.toll)
    ExpenseCategory.PARKING -> CategoryVisual(Icons.Default.LocalParking, CategoryAccentColors.parking)
    ExpenseCategory.INSURANCE -> CategoryVisual(Icons.Default.Security, CategoryAccentColors.insurance)
    ExpenseCategory.VEHICLE_TAX -> CategoryVisual(Icons.Default.AccountBalance, CategoryAccentColors.vehicleTax)
    ExpenseCategory.FINANCING -> CategoryVisual(Icons.Default.AccountBalanceWallet, CategoryAccentColors.financing)
    ExpenseCategory.OTHER -> CategoryVisual(Icons.Default.MoreHoriz, CategoryAccentColors.other)
}
