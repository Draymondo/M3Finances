package com.naveenapps.expensemanager.feature.envelope.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.naveenapps.expensemanager.core.domain.usecase.envelope.GetEnvelopesUseCase
import com.naveenapps.expensemanager.core.domain.usecase.tools.TrackToolUsageUseCase
import com.naveenapps.expensemanager.core.model.ToolType
import com.naveenapps.expensemanager.core.navigation.AppComposeNavigator
import com.naveenapps.expensemanager.core.navigation.ExpenseManagerScreens
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class EnvelopeListViewModel(
    getEnvelopesUseCase: GetEnvelopesUseCase,
    private val trackToolUsageUseCase: TrackToolUsageUseCase,
    private val appComposeNavigator: AppComposeNavigator,
) : ViewModel() {

    private val _state = MutableStateFlow(
        EnvelopeListState(
            isLoading = true,
            envelopes = emptyList(),
        ),
    )
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            trackToolUsageUseCase(ToolType.ENVELOPES)
        }
        getEnvelopesUseCase.invoke().onEach { envelopes ->
            _state.update {
                it.copy(isLoading = false, envelopes = envelopes)
            }
        }.launchIn(viewModelScope)
    }

    private fun openCreateScreen(envelopeId: String? = null) {
        appComposeNavigator.navigate(ExpenseManagerScreens.EnvelopeCreate(envelopeId))
    }

    private fun closePage() {
        appComposeNavigator.popBackStack()
    }

    fun processAction(action: EnvelopeListAction) {
        when (action) {
            EnvelopeListAction.ClosePage -> closePage()
            EnvelopeListAction.OpenEnvelopeCreate -> openCreateScreen()
            is EnvelopeListAction.OpenEnvelopeDetail -> openCreateScreen(action.envelopeId)
        }
    }
}
