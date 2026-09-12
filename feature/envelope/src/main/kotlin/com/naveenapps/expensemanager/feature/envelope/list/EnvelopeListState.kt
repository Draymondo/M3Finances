package com.naveenapps.expensemanager.feature.envelope.list

import androidx.compose.runtime.Stable
import com.naveenapps.expensemanager.core.domain.usecase.envelope.EnvelopeUiModel

@Stable
data class EnvelopeListState(
    val isLoading: Boolean,
    val envelopes: List<EnvelopeUiModel>,
)
