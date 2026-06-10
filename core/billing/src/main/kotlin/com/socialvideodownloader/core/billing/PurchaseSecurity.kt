package com.socialvideodownloader.core.billing

import android.util.Base64
import android.util.Log
import java.security.KeyFactory
import java.security.Signature
import java.security.spec.X509EncodedKeySpec

/**
 * Verifies Google Play purchase signatures locally with the app's licensing public key.
 *
 * Google signs every purchase's `originalJson` with the developer account's RSA private key, so a
 * client can confirm a purchase is genuine (not replayed/forged on a rooted device) by checking the
 * signature against the matching public key from Play Console → Monetization setup.
 *
 * Verification is **opt-in**: until [BASE64_PUBLIC_KEY] is configured it is skipped (returns `true`)
 * so the upgrade flow keeps working before the key is wired in, mirroring the "configure in Play
 * Console" TODO in [PlayBillingRepository]. Once the key is set, forged purchases are rejected.
 */
internal object PurchaseSecurity {
    // Base64-encoded RSA public key from Google Play Console (Monetization setup →
    // Licensing), injected at build time via `play.billing.public.key` (see
    // build.gradle.kts). Blank by default => verification is skipped (opt-in).
    private val BASE64_PUBLIC_KEY: String = BuildConfig.BASE64_PUBLIC_KEY

    private const val TAG = "PurchaseSecurity"
    private const val KEY_FACTORY_ALGORITHM = "RSA"
    private const val SIGNATURE_ALGORITHM = "SHA1withRSA"

    /**
     * Returns `true` when [signature] is a valid Play signature for [signedData], or when no public
     * key is configured. Returns `false` only when a key is configured and verification fails.
     */
    fun verifyPurchase(
        signedData: String,
        signature: String,
    ): Boolean {
        if (BASE64_PUBLIC_KEY.isBlank()) return true
        if (signedData.isBlank() || signature.isBlank()) return false
        return try {
            val keyBytes = Base64.decode(BASE64_PUBLIC_KEY, Base64.DEFAULT)
            val publicKey =
                KeyFactory.getInstance(KEY_FACTORY_ALGORITHM)
                    .generatePublic(X509EncodedKeySpec(keyBytes))
            val verifier =
                Signature.getInstance(SIGNATURE_ALGORITHM).apply {
                    initVerify(publicKey)
                    update(signedData.toByteArray())
                }
            verifier.verify(Base64.decode(signature, Base64.DEFAULT))
        } catch (e: Exception) {
            Log.e(TAG, "Purchase signature verification failed", e)
            false
        }
    }
}
