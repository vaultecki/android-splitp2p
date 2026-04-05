package net.vaultcity.splitp2p

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import org.json.JSONObject
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.Signature

// ---------------------------------------------------------------------------
// Android Keystore — Ed25519 identity key
// ---------------------------------------------------------------------------

fun hasIdentityKey(alias: String): Boolean {
    val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    return ks.containsAlias(alias)
}

fun generateKeystoreEd25519(alias: String) {
    val kpg = KeyPairGenerator.getInstance("Ed25519", "AndroidKeyStore")
    kpg.initialize(
        KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
        ).apply {
            setDigests(KeyProperties.DIGEST_NONE)
        }.build()
    )
    kpg.generateKeyPair()
}

fun deleteKeyFromKeystore(alias: String): Boolean = try {
    val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    if (ks.containsAlias(alias)) { ks.deleteEntry(alias); true } else false
} catch (e: Exception) { false }

/**
 * Returns the raw 32-byte Ed25519 public key as a 64-char hex string.
 *
 * The Android Keystore returns the key DER-encoded (SubjectPublicKeyInfo):
 *   12-byte ASN.1 header + 32-byte raw key
 * Python pynacl uses only the raw 32 bytes — so we take the last 32.
 */
fun getPublicKeyAsHex(alias: String): String {
    val ks   = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    val full = ks.getCertificate(alias).publicKey.encoded
    // Ed25519 DER = header (variable, usually 12 bytes) + 32-byte raw key
    // The raw key is always the last 32 bytes.
    val raw  = full.takeLast(32).toByteArray()
    return raw.joinToString("") { "%02x".format(it) }
}

/**
 * Signs a byte array with the Ed25519 key stored in the Android Keystore.
 * Returns the 64-byte signature as a 128-char hex string.
 */
fun signWithKeystore(alias: String, data: ByteArray): String {
    val ks  = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    val key = ks.getKey(alias, null) as java.security.PrivateKey
    val sig = Signature.getInstance("Ed25519").run {
        initSign(key)
        update(data)
        sign()
    }
    return sig.joinToString("") { "%02x".format(it) }
}

// Convenience: sign a string encoded as UTF-8
fun signJsonWithKeystore(alias: String, json: String): String =
    signWithKeystore(alias, json.toByteArray(Charsets.UTF_8))


// ---------------------------------------------------------------------------
// Canonical JSON serialization — must match Python exactly
//
// Rules:
//   - Keys sorted alphabetically
//   - No whitespace (compact)
//   - No null values: String? -> "" or 0, Boolean/Int is_deleted -> 0/1
//   - UTF-8 encoded
//
// Python equivalent:
//   json.dumps(d, sort_keys=True, separators=(',', ':'))
// ---------------------------------------------------------------------------

/** Produces compact, sorted JSON from a map. Matches Python json.dumps(sort_keys=True). */
private fun sortedJson(fields: Map<String, Any>): String {
    val obj = JSONObject()
    fields.toSortedMap().forEach { (k, v) -> obj.put(k, v) }
    // JSONObject.toString() produces compact JSON
    return obj.toString()
}

fun userCanonicalBytes(user: User): ByteArray =
    sortedJson(mapOf(
        "group_id"      to user.group_id,
        "lamport_clock" to user.lamport_clock,
        "name"          to user.name,
        "public_key"    to user.public_key,
        "timestamp"     to user.timestamp,
    )).toByteArray(Charsets.UTF_8)

fun splitCanonicalBytes(split: Split): ByteArray =
    sortedJson(mapOf(
        "amount"        to split.amount,
        "author_pubkey" to split.author_pubkey,
        "belongs_to"    to split.belongs_to,
        "debtor_key"    to split.debtor_key,
        "id"            to split.id,
        "lamport_clock" to split.lamport_clock,
        "payer_key"     to split.payer_key,
        "timestamp"     to split.timestamp,
    )).toByteArray(Charsets.UTF_8)

fun expenseCanonicalBytes(expense: Expense): ByteArray =
    sortedJson(mapOf(
        "amount"            to expense.amount,
        "author_pubkey"     to expense.author_pubkey,
        "category"          to (expense.category ?: ""),
        "description"       to (expense.description ?: ""),
        "expense_date"      to expense.expense_date,
        "group_id"          to expense.group_id,
        "id"                to expense.id,
        "is_deleted"        to expense.is_deleted.toInt(),
        "lamport_clock"     to expense.lamport_clock,
        "original_amount"   to (expense.original_amount ?: 0L),
        "original_currency" to (expense.original_currency ?: ""),
        "timestamp"         to expense.timestamp,
    )).toByteArray(Charsets.UTF_8)

fun settlementCanonicalBytes(s: Settlement): ByteArray =
    sortedJson(mapOf(
        "amount"        to s.amount,
        "author_pubkey" to s.author_pubkey,
        "from_key"      to s.from_key,
        "group_id"      to s.group_id,
        "id"            to s.id,
        "is_deleted"    to s.is_deleted.toInt(),
        "lamport_clock" to s.lamport_clock,
        "timestamp"     to s.timestamp,
        "to_key"        to s.to_key,
    )).toByteArray(Charsets.UTF_8)

fun commentCanonicalBytes(c: Comment): ByteArray =
    sortedJson(mapOf(
        "author_pubkey" to c.author_pubkey,
        "belongs_to"    to c.belongs_to,
        "comment"       to (c.comment ?: ""),
        "id"            to c.id,
        "is_deleted"    to c.is_deleted.toInt(),
        "lamport_clock" to c.lamport_clock,
        "timestamp"     to c.timestamp,
    )).toByteArray(Charsets.UTF_8)

fun attachmentCanonicalBytes(a: Attachment): ByteArray =
    // is_stored intentionally excluded — local-only field
    sortedJson(mapOf(
        "author_pubkey" to a.author_pubkey,
        "belongs_to"    to a.belongs_to,
        "filename"      to a.filename,
        "id"            to a.id,
        "lamport_clock" to a.lamport_clock,
        "mime"          to (a.mime ?: ""),
        "sha256"        to a.sha256,
        "size"          to (a.size ?: 0L),
        "timestamp"     to a.timestamp,
    )).toByteArray(Charsets.UTF_8)

