package com.invincible.jedishare.utils

import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec
import java.security.MessageDigest

object CryptoUtils {
    private const val AES_TRANSFORMATION = "AES/ECB/PKCS5Padding"
    private const val AES_ALGORITHM = "AES"

    // Encrypt data using AES
    fun encrypt(data: ByteArray, key: SecretKey): ByteArray {
        val cipher = Cipher.getInstance(AES_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        return cipher.doFinal(data)
    }

    // Decrypt data using AES
    fun decrypt(data: ByteArray, key: SecretKey): ByteArray {
        val cipher = Cipher.getInstance(AES_TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key)
        return cipher.doFinal(data)
    }

    // Generate SHA-256 hash for integrity check
    fun generateSHA256Hash(data: ByteArray): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(data)
    }

    // Generate a SecretKey from a byte array (used for generating AES key)
    fun generateSecretKey(keyBytes: ByteArray): SecretKey {
        return SecretKeySpec(keyBytes, AES_ALGORITHM)
    }
}