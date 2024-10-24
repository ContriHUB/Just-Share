package com.invincible.jedishare.utils

import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PublicKey
import java.security.spec.X509EncodedKeySpec
import javax.crypto.KeyAgreement
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

object KeyUtils {
    // Generate Diffie-Hellman Key Pair
    fun generateDHKeyPair(): KeyPair {
        val keyPairGenerator = KeyPairGenerator.getInstance("DH")
        keyPairGenerator.initialize(2048) // Strong 2048-bit key
        return keyPairGenerator.generateKeyPair()
    }

    // Serialize public key to send over the network
    fun serializePublicKey(publicKey: PublicKey): ByteArray {
        return publicKey.encoded
    }

    // Deserialize public key received from the other device
    fun deserializePublicKey(publicKeyBytes: ByteArray): PublicKey {
        val keyFactory = java.security.KeyFactory.getInstance("DH")
        val spec = X509EncodedKeySpec(publicKeyBytes)
        return keyFactory.generatePublic(spec)
    }

    // Generate shared secret using Diffie-Hellman
    fun generateSharedSecret(ownPrivateKey: java.security.PrivateKey, otherPublicKey: PublicKey): ByteArray {
        val keyAgreement = KeyAgreement.getInstance("DH")
        keyAgreement.init(ownPrivateKey)
        keyAgreement.doPhase(otherPublicKey, true)
        return keyAgreement.generateSecret()
    }

    // Generate AES key from shared secret
    fun generateAESKeyFromSharedSecret(sharedSecret: ByteArray): SecretKey {
        // Use the first 16 bytes of the shared secret for AES-128
        return SecretKeySpec(sharedSecret.copyOf(16), "AES")
    }
}