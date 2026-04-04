package net.vaultcity.splitp2p

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyPairGenerator
import java.security.KeyStore


fun hasIdentityKey(alias: String): Boolean {
    val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    return keyStore.containsAlias(alias)
}

fun deleteKeyFromKeystore(alias: String): Boolean {
    return try {
        // 1. Zugriff auf den Android KeyStore
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply {
            load(null)
        }

        // 2. Prüfen, ob der Alias existiert
        if (keyStore.containsAlias(alias)) {
            // 3. Den Schlüssel (und das zugehörige Zertifikat) löschen
            keyStore.deleteEntry(alias)
            true
        } else {
            false
        }
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}

fun generateKeystoreEd25519(alias: String) {
    val kpg = KeyPairGenerator.getInstance(
        "Ed25519",
        "AndroidKeyStore"
    )

    val parameterSpec = KeyGenParameterSpec.Builder(
        alias,
        KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
    ).apply {
        setDigests(KeyProperties.DIGEST_NONE) // Ed25519 benötigt kein separates Hashing
    }.build()

    kpg.initialize(parameterSpec)
    kpg.generateKeyPair()
}

fun getPublicKeyAsHex(alias: String): String {
    val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    val certificate = keyStore.getCertificate(alias)
    val publicKeyBytes = certificate.publicKey.encoded

    // Wir nutzen hier eine einfache Hex-Konvertierung
    return publicKeyBytes.joinToString("") { "%02x".format(it) }
}