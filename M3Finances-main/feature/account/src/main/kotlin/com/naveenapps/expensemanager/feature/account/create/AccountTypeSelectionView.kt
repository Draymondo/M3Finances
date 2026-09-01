package com.naveenapps.expensemanager.feature.account.create

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import com.naveenapps.expensemanager.core.designsystem.ui.components.AppFilterChip
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.naveenapps.designsystem.theme.NaveenAppsPreviewTheme
import com.naveenapps.designsystem.utils.AppPreviewsLightAndDarkMode
import com.naveenapps.expensemanager.core.model.AccountType
import com.naveenapps.expensemanager.feature.account.R

@Composable
fun AccountTypeSelectionView(
    selectedAccountType: AccountType,
    onAccountTypeChange: ((AccountType) -> Unit),
    modifier: Modifier = Modifier,
) {
    val accountTypes = remember {
        listOf(
            AccountTypeUi(
                type = AccountType.REGULAR,
                labelRes = R.string.bank_account,
                icon = Icons.Rounded.AccountBalance,
            ),
            AccountTypeUi(
                type = AccountType.CREDIT,
                labelRes = R.string.credit,
                icon = Icons.Rounded.CreditCard,
            ),
            AccountTypeUi(
                type = AccountType.MOBILE_MONEY,
                labelRes = R.string.mobile_money,
                icon = Icons.Rounded.AccountBalanceWallet,
            ),
            AccountTypeUi(
                type = AccountType.VAULT,
                labelRes = R.string.vault,
                icon = Icons.Rounded.Lock,
            ),
        )
    }

    val scrollState = rememberScrollState()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        accountTypes.forEach { item ->
            val isSelected = selectedAccountType == item.type

            AppFilterChip(
                filterName = stringResource(id = item.labelRes),
                isSelected = isSelected,
                filterIcon = item.icon,
                onClick = { onAccountTypeChange.invoke(item.type) }
            )
        }
    }
}

private data class AccountTypeUi(
    val type: AccountType,
    val labelRes: Int,
    val icon: ImageVector,
)

@AppPreviewsLightAndDarkMode
@Composable
private fun AccountTypeSelectionViewPreview() {
    NaveenAppsPreviewTheme(padding = 0.dp) {
        Column {
            AccountTypeSelectionView(
                modifier = Modifier
                    .padding(top = 16.dp, start = 16.dp, end = 16.dp)
                    .fillMaxWidth(),
                selectedAccountType = AccountType.REGULAR,
                onAccountTypeChange = {},
            )
            AccountTypeSelectionView(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                selectedAccountType = AccountType.CREDIT,
                onAccountTypeChange = {},
            )
            AccountTypeSelectionView(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                selectedAccountType = AccountType.MOBILE_MONEY,
                onAccountTypeChange = {},
            )
        }
    }
}
