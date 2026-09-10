package com.naveenapps.expensemanager.core.designsystem.components

import androidx.lifecycle.ViewModel
import com.naveenapps.expensemanager.core.designsystem.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

// TODO Modify this with categories and icons in future
internal val iconSelectionList = listOf(
    R.drawable.account_balance,
    R.drawable.account_balance_wallet,
    R.drawable.agriculture,
    R.drawable.apartment,
    R.drawable.apparel,
    R.drawable.bolt,
    R.drawable.car_rental,
    R.drawable.car_repair,
    R.drawable.child_care,
    R.drawable.cleaning_services,
    R.drawable.credit_card,
    R.drawable.devices,
    R.drawable.dinner_dining,
    R.drawable.directions_bike,
    R.drawable.directions_boat,
    R.drawable.directions_bus,
    R.drawable.directions_car,
    R.drawable.diversity,
    R.drawable.djamo,
    R.drawable.electric_rickshaw,
    R.drawable.electric_scooter,
    R.drawable.emoji_food_beverage,
    R.drawable.featured_seasonal_and_gifts,
    R.drawable.fitness_center,
    R.drawable.flight,
    R.drawable.flights_and_hotels,
    R.drawable.fluid_med,
    R.drawable.handyman,
    R.drawable.health_and_safety,
    R.drawable.hiking,
    R.drawable.home,
    R.drawable.home_health,
    R.drawable.interactive_space,
    R.drawable.kayaking,
    R.drawable.laptop_chromebook,
    R.drawable.liquor,
    R.drawable.local_cafe,
    R.drawable.local_gas_station,
    R.drawable.local_shipping,
    R.drawable.lunch_dining,
    R.drawable.medication,
    R.drawable.medication_liquid,
    R.drawable.movie,
    R.drawable.music_note,
    R.drawable.other_admission,
    R.drawable.payments,
    R.drawable.penguin,
    R.drawable.pets,
    R.drawable.pool,
    R.drawable.qr_code,
    R.drawable.redeem,
    R.drawable.restaurant,
    R.drawable.savings,
    R.drawable.school,
    R.drawable.shopping_cart,
    R.drawable.smartphone,
    R.drawable.snowmobile,
    R.drawable.sports_esports,
    R.drawable.sports_soccer,
    R.drawable.sports_tennis,
    R.drawable.store,
    R.drawable.train,
    R.drawable.travel,
    R.drawable.volunteer_activism,
    R.drawable.wallet,
    R.drawable.water_drop,
    R.drawable.wifi,
    R.drawable.work,
)


class IconSelectionViewModel() : ViewModel() {

    private val _icons = MutableStateFlow(iconSelectionList)
    val icons = _icons.asStateFlow()
}
