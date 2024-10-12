package com.invincible.jedishare.data.chat

import android.bluetooth.BluetoothSocket
import android.util.Log
import com.invincible.jedishare.domain.chat.BluetoothMessage
import com.invincible.jedishare.domain.chat.TransferFailedException
import com.invincible.jedishare.presentation.BluetoothViewModel
import com.invincible.jedishare.presentation.components.CustomProgressIndicator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import java.security.MessageDigest
import javax.crypto.KeyGenerator
import java.security.SecureRandom

const val BUFFER_SIZE = 990 // Adjust the buffer size as needed
const val FILE_DELIMITER = "----FILE_DELIMITER----"
val repeatedString = FILE_DELIMITER.repeat(40)

class BluetoothDataTransferService(
    private val socket: BluetoothSocket
) {

    private val incomingDataStream = ByteArrayOutputStream()

    // Generate AES key and IV
    private val secretKey: ByteArray = generateKey()
    private val iv: ByteArray = generateIv()

    // Function to generate an AES key
    private fun generateKey(): ByteArray {
        val keyGen = KeyGenerator.getInstance("AES")
        keyGen.init(256)
        return keyGen.generateKey().encoded
    }

    // Function to generate an IV
    private fun generateIv(): ByteArray {
        val iv = ByteArray(16) // AES block size is 16 bytes
        SecureRandom().nextBytes(iv)
        return iv
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
                val bufferRed = buffer.copyOfRange(0, byteCount)
                Log.e("HELLOME", "Received: " + bufferRed.size.toString())

//                processIncomingData(bufferRed)
//                checkForFiles(viewModel)?.let { fileBytes ->
//                    emit(fileBytes)
//                }
                // Decrypt the received data
                val decryptedData = decryptData(bufferRed)
                Log.e("MYTAG", "Bytes Read (Decrypted): " + decryptedData.size)

                // Emit the decrypted data
                emit(decryptedData)
            }
        }.flowOn(Dispatchers.IO)
    }

    private fun processIncomingData(data: ByteArray) {
        incomingDataStream.write(data)
    }

    private fun checkForFiles(viewModel: BluetoothViewModel?): ByteArray? {
        val data = incomingDataStream.toByteArray()
        val delimiterIndex =
            findIndexOfSubArray(incomingDataStream.toByteArray(), repeatedString.toByteArray())


//        Log.e("MYTAG","delimiter size" + FILE_DELIMITER.toByteArray().size.toString())
//        Log.e("MYTAG","byte array size" + data.size)

//        val delimiterIndex = data.indexOf(FILE_DELIMITER.toByteArray())
        if (delimiterIndex == 0) {
//            val fileBytes = data.copyOfRange(0, delimiterIndex)
//            incomingDataStream.reset()
//            Log.e("MYTAG","END OF FILE" + delimiterIndex)
//            Log.e("MYTAG","main array size" + incomingDataStream.toByteArray().size.toString())
//            Log.e("MYTAG","delimiter size" + repeatedString.toByteArray().size.toString())
            viewModel?.isFirst = true
//            incomingDataStream.write(data, delimiterIndex + FILE_DELIMITER.length, data.size - (delimiterIndex + FILE_DELIMITER.length))
//            return fileBytes
            incomingDataStream.reset()
            return null
//            return incomingDataStream.toByteArray()
        }
//        else if(viewModel?.isFirst == true && data.size == 990){
//            incomingDataStream.reset()
//            return null
//        }
        incomingDataStream.reset()
        return data
    }

    fun findIndexOfSubArray(mainArray: ByteArray, subArray: ByteArray): Int {
        if (mainArray.size == repeatedString.toByteArray().size)
            return 0
//        for (i in 0 until mainArray.size - subArray.size + 1) {
//            if (mainArray.copyOfRange(i, i + subArray.size).contentEquals(subArray)) {
//                return i
//            }
//        }
        return -1 // Return -1 if subArray is not found in mainArray
    }

    suspend fun sendMessage(bytes: ByteArray): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val encryptedData = encryptData(bytes)
                socket.outputStream.write(encryptedData)
                Log.e("HELLOME", "Sent: " + bytes.size.toString())

            } catch (e: IOException) {
                return@withContext false
            }

            true
        }
    }

    // encrypts the provided data
    private fun encryptData(data: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        val secretKeySpec = SecretKeySpec(secretKey, "AES")
        val iv = ByteArray(16)
        SecureRandom().nextBytes(iv)
        val ivParameterSpec = IvParameterSpec(iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivParameterSpec)
        return iv + cipher.doFinal(data)
    }

    // decrypts the provided data
    private fun decryptData(data: ByteArray): ByteArray {
        val iv = data.copyOfRange(0, 16)
        val actualData = data.copyOfRange(16, data.size)
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        val secretKeySpec = SecretKeySpec(secretKey, "AES")
        val ivParameterSpec = IvParameterSpec(iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec)
        return cipher.doFinal(actualData)
    }

    // generates a SHA-256 hash of the provided data
    private fun generateHash(data: ByteArray): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(data)
    }

    // verifying hash of the provided data matches the expectedHash
    private fun verifyHash(data: ByteArray, expectedHash: ByteArray): Boolean {
        val hash = generateHash(data)
        return hash.contentEquals(expectedHash)
    }
}