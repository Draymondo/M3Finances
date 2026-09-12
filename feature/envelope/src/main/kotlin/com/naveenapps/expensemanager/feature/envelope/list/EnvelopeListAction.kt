package com.naveenapps.expensemanager.feature.envelope.list

sealed class EnvelopeListAction {

    data object ClosePage : EnvelopeListAction()

    data object OpenEnvelopeCreate : EnvelopeListAction()

    data class OpenEnvelopeDetail(val envelopeId: String) : EnvelopeListAction()
}
