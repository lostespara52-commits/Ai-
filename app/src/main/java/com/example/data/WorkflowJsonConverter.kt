package com.example.data

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object WorkflowJsonConverter {
    fun toJson(steps: List<WorkflowStep>): String {
        val array = JSONArray()
        for (step in steps) {
            val obj = JSONObject().apply {
                put("stepNumber", step.stepNumber)
                put("title", step.title)
                put("actionType", step.actionType)
                put("targetAppOrUrl", step.targetAppOrUrl)
                put("parameter", step.parameter)
                put("requiresApproval", step.requiresApproval)
                put("status", step.status)
                put("outputNote", step.outputNote)
            }
            array.put(obj)
        }
        return array.toString()
    }

    fun fromJson(json: String): List<WorkflowStep> {
        if (json.isBlank()) return emptyList()
        return try {
            val array = JSONArray(json)
            val list = mutableListOf<WorkflowStep>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    WorkflowStep(
                        stepNumber = obj.optInt("stepNumber", i + 1),
                        title = obj.optString("title", "Step ${i + 1}"),
                        actionType = obj.optString("actionType", "AI_PROMPT"),
                        targetAppOrUrl = obj.optString("targetAppOrUrl", ""),
                        parameter = obj.optString("parameter", ""),
                        requiresApproval = obj.optBoolean("requiresApproval", false),
                        status = obj.optString("status", "PENDING"),
                        outputNote = obj.optString("outputNote", "")
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }
}

object CredentialCipher {
    private const val KEY_ALIAS = "AuraCredentialMasterKey"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"

    private fun getOrCreateSecretKey(): SecretKey? {
        return try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            val existing = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
            if (existing != null) {
                existing.secretKey
            } else {
                val keyGenerator = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES,
                    ANDROID_KEYSTORE
                )
                val spec = KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()
                keyGenerator.init(spec)
                keyGenerator.generateKey()
            }
        } catch (e: Exception) {
            null
        }
    }

    fun encrypt(plainText: String): String {
        if (plainText.isBlank()) return ""
        return try {
            val secretKey = getOrCreateSecretKey() ?: return Base64.encodeToString(
                plainText.toByteArray(Charsets.UTF_8),
                Base64.NO_WRAP
            )
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val iv = cipher.iv
            val encrypted = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            val combined = ByteArray(1 + iv.size + encrypted.size)
            combined[0] = iv.size.toByte()
            System.arraycopy(iv, 0, combined, 1, iv.size)
            System.arraycopy(encrypted, 0, combined, 1 + iv.size, encrypted.size)
            Base64.encodeToString(combined, Base64.NO_WRAP)
        } catch (e: Exception) {
            Base64.encodeToString(plainText.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        }
    }

    fun decrypt(cipherText: String): String {
        if (cipherText.isBlank()) return ""
        return try {
            val raw = Base64.decode(cipherText, Base64.NO_WRAP)
            val secretKey = getOrCreateSecretKey() ?: return String(raw, Charsets.UTF_8)
            val ivSize = raw[0].toInt()
            if (ivSize <= 0 || ivSize >= raw.size) return String(raw, Charsets.UTF_8)
            val iv = raw.copyOfRange(1, 1 + ivSize)
            val encrypted = raw.copyOfRange(1 + ivSize, raw.size)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(128, iv))
            String(cipher.doFinal(encrypted), Charsets.UTF_8)
        } catch (e: Exception) {
            ""
        }
    }

    fun maskKey(plainKey: String): String {
        if (plainKey.isBlank()) return "Not Configured"
        if (plainKey.length <= 8) return "••••••••"
        return "${plainKey.take(4)}••••••••${plainKey.takeLast(4)}"
    }
}
