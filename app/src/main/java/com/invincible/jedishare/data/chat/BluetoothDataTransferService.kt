package com.invincible.jedishare.data.chat

import android.bluetooth.BluetoothSocket
import android.util.Log
import com.invincible.jedishare.domain.chat.BluetoothMessage
import com.invincible.jedishare.domain.chat.TransferFailedException
import com.invincible.jedishare.presentation.BluetoothViewModel
import com.invincible.jedishare.presentation.components.CustomProgressIndicator
import com.invincible.jedishare.utils.CryptoUtils
import com.invincible.jedishare.utils.KeyUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import javax.crypto.SecretKey
import java.security.KeyPair
import java.security.PublicKey

const val BUFFER_SIZE = 990 // Adjust the buffer size as needed
const val FILE_DELIMITER = "----FILE_DELIMITER----"
val repeatedString = FILE_DELIMITER.repeat(40)

class BluetoothDataTransferService(
    private val socket: BluetoothSocket,
    private var aesKey: SecretKey? = null
) {

    private val incomingDataStream = ByteArrayOutputStream()

    // Start the Diffie-Hellman Key Exchange
    fun startKeyExchange(): ByteArray {
        val keyPair: KeyPair = KeyUtils.generateDHKeyPair()
        val publicKeyBytes = KeyUtils.serializePublicKey(keyPair.public)
        // Send the public key bytes to the other device
        // Store the private key for computing the shared secret later
        dhPrivateKey = keyPair.private
        return publicKeyBytes
    }

    // Complete the Key Exchange and generate the AES key
    fun completeKeyExchange(otherPublicKeyBytes: ByteArray) {
        if (dhPrivateKey == null) {
            Log.e("BluetoothDataTransfer", "Private key is null. Key exchange failed!")
            throw IllegalStateException("Key exchange must be completed before data transfer.")
        }

        val otherPublicKey: PublicKey = KeyUtils.deserializePublicKey(otherPublicKeyBytes)
        val sharedSecret: ByteArray = KeyUtils.generateSharedSecret(dhPrivateKey!!, otherPublicKey)
        aesKey = KeyUtils.generateAESKeyFromSharedSecret(sharedSecret)
    }

    fun listenForIncomingMessages(viewModel: BluetoothViewModel): Flow<ByteArray> {
        return flow {
            if (!socket.isConnected) {
                return@flow
            }
            val buffer = ByteArray(BUFFER_SIZE)
            while (true) {
                val byteCount = try {
                    socket.inputStream.read(buffer)
                } catch (e: IOException) {
                    throw TransferFailedException()
                }
                val encryptedChunk = buffer.copyOfRange(0, byteCount)

                // Read the hash digest (for integrity check)
                val hashBuffer = ByteArray(32)
                socket.inputStream.read(hashBuffer)
                val receivedHash = hashBuffer.copyOfRange(0, hashBuffer.size)

                // Verify the hash to ensure data integrity
                val computedHash = CryptoUtils.generateSHA256Hash(encryptedChunk)
                if (!computedHash.contentEquals(receivedHash)) {
                    throw TransferFailedException()
                }

                // Ensure AES key is ready before decrypting
                if (aesKey == null) {
                    throw TransferFailedException()
                }

                // Decrypt the chunk using AES
                val decryptedData = CryptoUtils.decrypt(encryptedChunk, aesKey!!)

                Log.e("BluetoothDataTransfer", "Received and decrypted: ${decryptedData.size} bytes")

                processIncomingData(decryptedData)

                if (decryptedData.size == 880) {
                    viewModel?.isFirst = true
                } else {
                    emit(decryptedData)
                }
            }
        }.flowOn(Dispatchers.IO)
    }

    private fun processIncomingData(data: ByteArray) {
        incomingDataStream.write(data)
    }


    private fun checkForFiles(viewModel: BluetoothViewModel?): ByteArray? {
        val data = incomingDataStream.toByteArray()
        val delimiterIndex = findIndexOfSubArray(data, repeatedString.toByteArray())

        if (delimiterIndex >= 0) {
            // File boundary found, extract the file data
            val fileBytes = data.copyOfRange(0, delimiterIndex)
            incomingDataStream.reset()  // Reset the stream after processing the file
            // Write the remaining bytes back into the stream (after the delimiter)
            if (delimiterIndex + repeatedString.toByteArray().size < data.size) {
                incomingDataStream.write(
                    data,
                    delimiterIndex + repeatedString.toByteArray().size,
                    data.size - (delimiterIndex + repeatedString.toByteArray().size)
                )
            }
            viewModel?.isFirst = true
            return fileBytes
        }

        incomingDataStream.reset()
        return null
    }

    fun findIndexOfSubArray(mainArray: ByteArray, subArray: ByteArray): Int {
        // Properly find the delimiter in the incoming data
        for (i in 0 until mainArray.size - subArray.size + 1) {
            if (mainArray.copyOfRange(i, i + subArray.size).contentEquals(subArray)) {
                return i
            }
        }
        return -1 // Return -1 if subArray is not found
    }

    suspend fun sendMessage(bytes: ByteArray): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // Ensure AES key is ready before encrypting
                if (aesKey == null) {
                    throw IllegalStateException("AES key not initialized!")
                }

                // Encrypt the data chunk using AES
                val encryptedData = CryptoUtils.encrypt(bytes, aesKey!!)

                // Generate SHA-256 hash for integrity check
                val hashDigest = CryptoUtils.generateSHA256Hash(encryptedData)

                // Send the encrypted chunk
                socket.outputStream.write(encryptedData)

                // Send the hash digest for integrity verification
                socket.outputStream.write(hashDigest)

                Log.e("HELLOME", "Sent encrypted data: ${encryptedData.size} bytes")
                Log.e("HELLOME", "Sent hash digest: ${hashDigest.size} bytes")

            } catch (e: IOException) {
                return@withContext false
            }

            true
        }
    }
    private var dhPrivateKey: java.security.PrivateKey? = null
}