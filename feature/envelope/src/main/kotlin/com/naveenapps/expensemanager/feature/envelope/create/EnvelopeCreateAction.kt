package com.naveenapps.expensemanager.feature.envelope.create

sealed class EnvelopeCreateAction {

    data object ClosePage : EnvelopeCreateAction()

    data object Save : EnvelopeCreateAction()

    data object Delete : EnvelopeCreateAction()

    data object ShowDeleteDialog : EnvelopeCreateAction()

    data object DismissDeleteDialog : EnvelopeCreateAction()

    data object ShowCategorySelection : EnvelopeCreateAction()

    data object DismissCategorySelection : EnvelopeCreateAction()

    data class SelectCategory(val categoryId: String) : EnvelopeCreateAction()
}
