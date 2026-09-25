package com.naveenapps.expensemanager.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.naveenapps.expensemanager.core.model.TransactionSource

class MobileMoneyNotificationListenerService : NotificationListenerService() {

    private val supportedPackageNames = setOf(
        "com.wave.personal",
        "com.djamo.app",
        "ci.moovmoney.mmpayapi",
        "com.mobiblanc.moov.mymoov_ci",
        "mtnft.momo.consumer",
        "com.orange.myorange.oci",
        "com.paypal.android.p2pmobile",
        "com.google.android.apps.messaging",
        "com.samsung.android.messaging"
    )

    private val workEnqueuer by lazy { NotificationWorkEnqueuer(applicationContext) }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        sbn ?: return

        val packageName = sbn.packageName
        if (packageName !in supportedPackageNames) {
            return
        }

        val extras = sbn.notification.extras
        // getCharSequence (et non getString) : certains fournisseurs publient un CharSequence
        // enrichi et l'ancien getString renvoyait alors null, faisant perdre la notification.
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val body = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString().orEmpty()
        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString().orEmpty()
        val textLines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
            ?.mapNotNull { it?.toString() }
            .orEmpty()
        val messages = extras.getParcelableArray(Notification.EXTRA_MESSAGES)
            ?.filterIsInstance<Notification.MessagingStyle.Message>()
            ?.mapNotNull { it.text?.toString() }
            .orEmpty()

        val fullText = NotificationProcessorWorker.assembleNotificationText(
            title = title,
            body = body,
            bigText = bigText,
            subText = subText,
            textLines = textLines,
            messages = messages,
        )
        if (fullText.isBlank()) {
            return
        }

        // PRIV-001 : ne JAMAIS journaliser le contenu de la notification (montant, solde,
        // numéro de téléphone, commerçant, référence de transaction...), même en DEBUG.
        // Seuls le paquet source et la taille du texte assemblé sont tracés : suffisant
        // pour le diagnostic, totalement non sensible.
        Log.d(TAG, interceptedNotificationLogMessage(packageName, fullText))

        try {
            // Chaque événement capturé reçoit sa propre identité de travail WorkManager (UUID) :
            // deux notifications au texte identique sont donc bien deux travaux distincts.
            val eventId = workEnqueuer.enqueue(
                packageName = packageName,
                source = resolveSource(packageName),
                title = title,
                body = body,
                bigText = bigText,
                postTime = sbn.postTime,
                notificationKey = sbn.key,
                fullText = fullText,
            )
            Log.d(TAG, "Enqueued notification event $eventId from $packageName")
        } catch (exception: Exception) {
            // Ne jamais faire planter le listener : on trace au minimum pour ne pas perdre
            // silencieusement la notification.
            Log.e(TAG, "Failed to enqueue notification from $packageName", exception)
        }
    }

    private fun resolveSource(packageName: String): TransactionSource {
        return when (packageName) {
            "com.wave.personal" -> TransactionSource.WAVE
            "com.orange.myorange.oci" -> TransactionSource.ORANGE_MONEY
            "mtnft.momo.consumer" -> TransactionSource.MTN_MOMO
            "ci.moovmoney.mmpayapi", "com.mobiblanc.moov.mymoov_ci" -> TransactionSource.MOOV_MONEY
            "com.djamo.app" -> TransactionSource.DJAMO
            "com.paypal.android.p2pmobile" -> TransactionSource.PAYPAL
            "com.google.android.apps.messaging", "com.samsung.android.messaging" -> TransactionSource.SMS
            else -> TransactionSource.UNKNOWN
        }
    }

    companion object {
        private const val TAG = "MobileMoneyNotification"

        /**
         * Construit le message de journalisation d'une notification interceptée (PRIV-001).
         *
         * Le texte brut de la notification peut contenir des données financières sensibles
         * (montant, solde, numéro de téléphone, commerçant, référence de transaction) : il ne
         * doit JAMAIS apparaître dans le message. Seuls le paquet source et la longueur du
         * texte — métadonnée non sensible — y figurent.
         */
        internal fun interceptedNotificationLogMessage(packageName: String, fullText: String): String =
            "Intercepted notification from $packageName (textLength=${fullText.length})"
    }
}

