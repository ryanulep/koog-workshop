@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)

package org.example.project.admin.merchants

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.example.project.domain.admin.MerchantAdminService
import org.example.project.domain.admin.MerchantDetail
import org.example.project.domain.shared.MerchantId
import org.example.project.domain.shared.ShippingMethodId
import java.util.concurrent.atomic.AtomicLong
import kotlin.reflect.KClass

class MerchantAdminViewModel(
    private val merchantAdminService: org.example.project.domain.admin.MerchantAdminService
) : ViewModel() {

    private val loadVersion = AtomicLong(0L)
    private val _uiState = MutableStateFlow(MerchantAdminUiState())

    val uiState: StateFlow<MerchantAdminUiState> = _uiState.asStateFlow()

    fun refresh() = viewModelScope.launch {
        reload()
    }

    fun selectMerchant(merchantId: org.example.project.domain.shared.MerchantId) = viewModelScope.launch {
        selectMerchantInternal(merchantId)
    }

    fun setSelectedMerchantActive(isActive: Boolean) = viewModelScope.launch {
        val merchantId1 =
            _uiState.value.selectedMerchantId ?: return@launch
        this@MerchantAdminViewModel.performMutation(
            failureMessage = "Unable to update merchant ${merchantId1.value}.",
            merchantId = merchantId1
        ) {
            merchantAdminService.setMerchantActive(merchantId1, isActive)
        }
    }

    fun setShippingMethodActive(shippingMethodId: org.example.project.domain.shared.ShippingMethodId, isActive: Boolean) = viewModelScope.launch {
        val merchantId1 =
            _uiState.value.selectedMerchantId ?: return@launch
        this@MerchantAdminViewModel.performMutation(
            failureMessage = "Unable to update shipping method ${shippingMethodId.value}.",
            merchantId = merchantId1
        ) {
            merchantAdminService.setShippingMethodActive(shippingMethodId, isActive)
        }
    }

    fun updateShippingAssignmentSelection(
        shippingMethodId: org.example.project.domain.shared.ShippingMethodId,
        isSelected: Boolean
    ) {
        val current = _uiState.value
        val nextSelection = if (isSelected) {
            current.selectedShippingMethodIds.add(shippingMethodId)
        } else {
            current.selectedShippingMethodIds.remove(shippingMethodId)
        }
        _uiState.value = current.copy(selectedShippingMethodIds = nextSelection)
    }

    fun saveShippingAssignments() = viewModelScope.launch {
        val current1 = _uiState.value
        val merchantId1 = current1.selectedMerchantId ?: return@launch
        this@MerchantAdminViewModel.performMutation(
            failureMessage = "Unable to save shipping assignments for merchant ${merchantId1.value}.",
            merchantId = merchantId1
        ) {
            merchantAdminService.replaceMerchantShippingMethods(
                merchantId = merchantId1,
                shippingMethodIds = current1.selectedShippingMethodIds
            )
        }
    }

    private suspend fun selectMerchantInternal(merchantId: org.example.project.domain.shared.MerchantId) {
        val version = loadVersion.incrementAndGet()
        val current = _uiState.value
        _uiState.value = current.copy(
            errorMessage = null,
            selectedMerchantId = merchantId,
            selectedMerchant = current.selectedMerchant?.takeIf { detail -> detail.merchant.id == merchantId }
        )

        val nextState = try {
            val detail = merchantAdminService.loadMerchantDetailOrNull(merchantId)
            if (detail == null) {
                current.copy(
                    errorMessage = "Merchant ${merchantId.value} was not found.",
                    selectedMerchantId = null,
                    selectedMerchant = null,
                    selectedShippingMethodIds = persistentSetOf()
                )
            } else {
                current.copy(
                    errorMessage = null,
                    selectedMerchantId = merchantId,
                    selectedMerchant = detail,
                    selectedShippingMethodIds = detail.toPersistentShippingSelection()
                )
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (throwable: Throwable) {
            current.copy(
                errorMessage = throwable.message ?: "Unable to load merchant details.",
                selectedMerchantId = merchantId,
                selectedMerchant = null,
                selectedShippingMethodIds = persistentSetOf()
            )
        }

        if (loadVersion.get() == version) {
            _uiState.value = nextState
        }
    }

    private suspend fun reload(preferredMerchantId: org.example.project.domain.shared.MerchantId? = _uiState.value.selectedMerchantId) {
        val version = loadVersion.incrementAndGet()
        val current = _uiState.value
        _uiState.value = current.copy(errorMessage = null)

        val nextState = try {
            val merchants = merchantAdminService.loadMerchants().toPersistentList()
            val selectedMerchantId = preferredMerchantId
                ?.takeIf { selectedId -> merchants.any { merchant -> merchant.id == selectedId } }
                ?: merchants.firstOrNull()?.id
            val selectedMerchant = selectedMerchantId?.let { merchantAdminService.loadMerchantDetailOrNull(it) }

            current.copy(
                errorMessage = null,
                merchants = merchants,
                selectedMerchantId = selectedMerchant?.merchant?.id ?: selectedMerchantId,
                selectedMerchant = selectedMerchant,
                selectedShippingMethodIds = selectedMerchant.toPersistentShippingSelection()
            )
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (throwable: Throwable) {
            current.copy(
                errorMessage = throwable.message ?: "Unable to load merchant operations."
            )
        }

        if (loadVersion.get() == version) {
            _uiState.value = nextState
        }
    }

    private suspend fun performMutation(
        failureMessage: String,
        merchantId: org.example.project.domain.shared.MerchantId,
        action: suspend () -> Boolean
    ) {
        val current = _uiState.value
        _uiState.value = current.copy(errorMessage = null)

        val success = try {
            action()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (throwable: Throwable) {
            _uiState.value = current.copy(
                errorMessage = throwable.message ?: failureMessage
            )
            return
        }

        if (!success) {
            _uiState.value = current.copy(
                errorMessage = failureMessage
            )
            return
        }

        reload(preferredMerchantId = merchantId)
    }

    private fun org.example.project.domain.admin.MerchantDetail?.toPersistentShippingSelection(): PersistentSet<org.example.project.domain.shared.ShippingMethodId> =
        this?.assignedShippingMethods
            ?.fold(persistentSetOf()) { selection, shippingMethod ->
                selection.add(shippingMethod.id)
            }
            ?: persistentSetOf()

    companion object {
        fun factory(merchantAdminService: org.example.project.domain.admin.MerchantAdminService): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: KClass<T>, extras: CreationExtras): T {
                    if (modelClass == MerchantAdminViewModel::class) {
                        return MerchantAdminViewModel(merchantAdminService) as T
                    }
                    throw IllegalArgumentException(
                        "Unknown ViewModel class: ${modelClass.simpleName ?: "unknown"}"
                    )
                }
            }
    }
}
