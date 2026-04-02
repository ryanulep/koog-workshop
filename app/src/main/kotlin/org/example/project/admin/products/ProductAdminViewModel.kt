@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)

package org.example.project.admin.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicLong
import kotlin.reflect.KClass

class ProductAdminViewModel(
    private val productService: org.example.project.domain.admin.ProductService
) : ViewModel() {

    private val loadVersion = AtomicLong(0L)
    private val _uiState = MutableStateFlow(ProductAdminUiState())

    val uiState: StateFlow<ProductAdminUiState> = _uiState.asStateFlow()

    fun refresh() = viewModelScope.launch {
        reload()
    }

    fun updateNameQuery(query: String) = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(
            filter = _uiState.value.filter.copy(nameQuery = query)
        )
        reload()
    }

    fun updateCategory(category: org.example.project.domain.catalog.ProductCategory?) = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(
            filter = _uiState.value.filter.copy(category = category)
        )
        reload()
    }

    fun updateMerchant(merchantId: org.example.project.domain.shared.MerchantId?) = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(
            filter = _uiState.value.filter.copy(merchantId = merchantId)
        )
        reload()
    }

    fun updateActiveFilter(activeFilter: org.example.project.domain.admin.ProductActiveFilter) = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(
            filter = _uiState.value.filter.copy(activeFilter = activeFilter)
        )
        reload()
    }

    fun selectProduct(productId: org.example.project.domain.shared.ProductId) = viewModelScope.launch {
        selectProductInternal(productId)
    }

    fun adjustSelectedStock(quantityChange: Int) = viewModelScope.launch {
        val productId1 = _uiState.value.selectedProductId ?: return@launch
        this@ProductAdminViewModel.performMutation(
            failureMessage = "Unable to update stock for product ${productId1.value}.",
            productId = productId1
        ) {
            productService.adjustStock(productId1, quantityChange)
        }
    }

    fun setSelectedProductActive(isActive: Boolean) = viewModelScope.launch {
        val productId1 =
            _uiState.value.selectedProductId ?: return@launch
        this@ProductAdminViewModel.performMutation(
            failureMessage = "Unable to update the product state for ${productId1.value}.",
            productId = productId1
        ) {
            productService.setProductActive(productId1, isActive)
        }
    }

    private suspend fun selectProductInternal(productId: org.example.project.domain.shared.ProductId) {
        val version = loadVersion.incrementAndGet()
        val current = _uiState.value
        _uiState.value = current.copy(
            errorMessage = null,
            selectedProductId = productId,
            selectedProduct = current.selectedProduct?.takeIf { product -> product.id == productId }
        )

        val nextState = try {
            val detail = productService.loadProductDetailOrNull(productId)
            if (detail == null) {
                current.copy(
                    errorMessage = "Product ${productId.value} was not found.",
                    selectedProductId = null,
                    selectedProduct = null
                )
            } else {
                current.copy(
                    errorMessage = null,
                    selectedProductId = productId,
                    selectedProduct = detail
                )
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (throwable: Throwable) {
            current.copy(
                errorMessage = throwable.message ?: "Unable to load product details.",
                selectedProductId = productId,
                selectedProduct = null
            )
        }

        if (loadVersion.get() == version) {
            _uiState.value = nextState
        }
    }

    private suspend fun reload() {
        val version = loadVersion.incrementAndGet()
        val current = _uiState.value
        _uiState.value = current.copy(errorMessage = null)

        val nextState = try {
            val merchants = productService.loadMerchantOptions().toPersistentList()
            val products = productService.loadProducts(current.filter).toPersistentList()
            val selectedProductId = current.selectedProductId
                ?.takeIf { selectedId -> products.any { product -> product.id == selectedId } }
                ?: products.firstOrNull()?.id
            val selectedProduct = selectedProductId?.let { productService.loadProductDetailOrNull(it) }

            current.copy(
                errorMessage = null,
                merchants = merchants,
                products = products,
                selectedProductId = selectedProduct?.id ?: selectedProductId,
                selectedProduct = selectedProduct
            )
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (throwable: Throwable) {
            current.copy(
                errorMessage = throwable.message ?: "Unable to load product operations."
            )
        }

        if (loadVersion.get() == version) {
            _uiState.value = nextState
        }
    }

    private suspend fun performMutation(
        failureMessage: String,
        productId: org.example.project.domain.shared.ProductId,
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

        // Only reload the specific product detail and update the list item
        val version = loadVersion.incrementAndGet()
        val nextState = try {
            val updatedDetail = productService.loadProductDetailOrNull(productId)
            if (updatedDetail == null) {
                current.copy(
                    errorMessage = "Product ${productId.value} was not found after update."
                )
            } else {
                // Update the product in the list
                val updatedProducts = current.products.map { product ->
                    if (product.id == productId) {
                        product.copy(
                            stock = updatedDetail.stock,
                            isActive = updatedDetail.isActive
                        )
                    } else {
                        product
                    }
                }.toPersistentList()

                current.copy(
                    errorMessage = null,
                    products = updatedProducts,
                    selectedProduct = updatedDetail
                )
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (throwable: Throwable) {
            current.copy(
                errorMessage = throwable.message ?: failureMessage
            )
        }

        if (loadVersion.get() == version) {
            _uiState.value = nextState
        }
    }

    companion object {
        fun factory(productService: org.example.project.domain.admin.ProductService): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: KClass<T>, extras: CreationExtras): T {
                    if (modelClass == ProductAdminViewModel::class) {
                        return ProductAdminViewModel(productService) as T
                    }
                    throw IllegalArgumentException(
                        "Unknown ViewModel class: ${modelClass.simpleName ?: "unknown"}"
                    )
                }
            }
    }
}
