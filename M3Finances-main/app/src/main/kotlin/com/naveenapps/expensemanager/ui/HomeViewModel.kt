package com.naveenapps.expensemanager.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.naveenapps.expensemanager.core.domain.usecase.settings.reminder.UpdateReminderStatusUseCase
import com.naveenapps.expensemanager.core.model.CloudSyncOutcome
import com.naveenapps.expensemanager.core.model.CloudSyncResolution
import com.naveenapps.expensemanager.core.model.Resource
import com.naveenapps.expensemanager.core.notification.NotificationScheduler
import com.naveenapps.expensemanager.core.repository.CloudBackupRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch


class HomeViewModel(
    private val updateReminderStatusUseCase: UpdateReminderStatusUseCase,
    private val notificationScheduler: NotificationScheduler,
    private val cloudBackupRepository: CloudBackupRepository,
) : ViewModel() {

    private val _homeScreenBottomBarItems = MutableStateFlow(HomeScreenBottomBarItems.Home)
    val homeScreenBottomBarItems = _homeScreenBottomBarItems.asStateFlow()

    private val _showCloudSyncConflict = MutableStateFlow(false)
    val showCloudSyncConflict = _showCloudSyncConflict.asStateFlow()

    private val _isCloudConflictResolving = MutableStateFlow(false)
    val isCloudConflictResolving = _isCloudConflictResolving.asStateFlow()

    init {
        cloudBackupRepository.observeUnresolvedConflict().onEach { conflict ->
            _showCloudSyncConflict.value = conflict
        }.launchIn(viewModelScope)
    }

    fun setUISystem(homeScreenBottomBarItems: HomeScreenBottomBarItems) {
        _homeScreenBottomBarItems.value = homeScreenBottomBarItems
    }

    fun turnOnNotification() {
        viewModelScope.launch {
            updateReminderStatusUseCase.invoke(true)
            notificationScheduler.checkAndRestartReminder()
        }
    }

    fun useCloudData() {
        resolveCloudSyncConflict(CloudSyncResolution.USE_CLOUD)
    }

    fun keepPhoneData() {
        resolveCloudSyncConflict(CloudSyncResolution.KEEP_LOCAL)
    }

    private fun resolveCloudSyncConflict(resolution: CloudSyncResolution) {
        viewModelScope.launch {
            if (_isCloudConflictResolving.value) return@launch
            _isCloudConflictResolving.value = true
            when (val result = cloudBackupRepository.resolveSyncConflict(resolution)) {
                is Resource.Error -> {
                    _isCloudConflictResolving.value = false
                }
                is Resource.Success -> {
                    if (result.data == CloudSyncOutcome.Pushed) {
                        cloudBackupRepository.createDailySnapshotIfNeeded()
                    }
                    _isCloudConflictResolving.value = false
                    _showCloudSyncConflict.value = false
                }
            }
        }
    }
}
