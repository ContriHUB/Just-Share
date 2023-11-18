package com.invincible.jedishare.data.chat

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.ContentResolver
import android.content.Context
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import android.webkit.MimeTypeMap
import com.invincible.jedishare.domain.chat.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.*

@SuppressLint("MissingPermission")
class AndroidBluetoothController(
    private val context: Context
): BluetoothController {

    private val bluetoothManager by lazy {
        context.getSystemService(BluetoothManager::class.java)
    }
    private val bluetoothAdapter by lazy {
        bluetoothManager?.adapter
    }

    private var dataTransferService: BluetoothDataTransferService? = null

    private val _isConnected = MutableStateFlow(false)
    override val isConnected: StateFlow<Boolean>
        get() = _isConnected.asStateFlow()

    private val _scannedDevices = MutableStateFlow<List<BluetoothDeviceDomain>>(emptyList())
    override val scannedDevices: StateFlow<List<BluetoothDeviceDomain>>
        get() = _scannedDevices.asStateFlow()

    private val _pairedDevices = MutableStateFlow<List<BluetoothDeviceDomain>>(emptyList())
    override val pairedDevices: StateFlow<List<BluetoothDeviceDomain>>
        get() = _pairedDevices.asStateFlow()

    private val _errors = MutableSharedFlow<String>()
    override val errors: SharedFlow<String>
        get() = _errors.asSharedFlow()

    private val foundDeviceReceiver = FoundDeviceReceiver { device ->
        _scannedDevices.update { devices ->
            val newDevice = device.toBluetoothDeviceDomain()
            if (newDevice in devices) devices else devices + newDevice
        }
    }

    private val bluetoothStateReceiver = BluetoothStateReceiver { isConnected, bluetoothDevice ->
        if (bluetoothAdapter?.bondedDevices?.contains(bluetoothDevice) == true) {
            _isConnected.update { isConnected }
        } else {
            CoroutineScope(Dispatchers.IO).launch {
                _errors.emit("Can't connect to a non-paired device.")
            }
        }
    }

    private var currentServerSocket: BluetoothServerSocket? = null
    private var currentClientSocket: BluetoothSocket? = null

    init {
        updatePairedDevices()
        context.registerReceiver(
            bluetoothStateReceiver,
            IntentFilter().apply {
                addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)
                addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
                addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            }
        )
    }

    override fun startDiscovery() {
        if (!hasPermission(Manifest.permission.BLUETOOTH_SCAN)) {
            return
        }

        context.registerReceiver(
            foundDeviceReceiver,
            IntentFilter(BluetoothDevice.ACTION_FOUND)
        )

        updatePairedDevices()

        bluetoothAdapter?.startDiscovery()
    }

    override fun stopDiscovery() {
        if (!hasPermission(Manifest.permission.BLUETOOTH_SCAN)) {
            return
        }

        bluetoothAdapter?.cancelDiscovery()
    }

    override fun startBluetoothServer(): Flow<ConnectionResult> {
        return flow {
            if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
                throw SecurityException("No BLUETOOTH_CONNECT permission")
            }

            currentServerSocket = bluetoothAdapter?.listenUsingRfcommWithServiceRecord(
                "chat_service",
                UUID.fromString(SERVICE_UUID)
            )

            var shouldLoop = true
            while (shouldLoop) {
                currentClientSocket = try {
                    currentServerSocket?.accept()
                } catch (e: IOException) {
                    shouldLoop = false
                    null
                }
                emit(ConnectionResult.ConnectionEstablished)
                currentClientSocket?.let {
                    currentServerSocket?.close()
                    val service = BluetoothDataTransferService(it)
                    dataTransferService = service

                    FileData(FileInfo("null",null,null,null),"Hello".toByteArray(),true).toByteArray()
                        ?.let { it1 -> service.sendMessage(it1) }

                    emitAll(
                        service
                            .listenForIncomingMessages()
                            .map {
                                ConnectionResult.TransferSucceeded(it)
                            }
                    )
                }
            }
        }.onCompletion {
            closeConnection()
        }.flowOn(Dispatchers.IO)
    }

    override fun connectToDevice(device: BluetoothDeviceDomain, uri: Uri?): Flow<ConnectionResult> {
        return flow {
            if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
                throw SecurityException("No BLUETOOTH_CONNECT permission")
            }

            currentClientSocket = bluetoothAdapter
                ?.getRemoteDevice(device.address)
                ?.createRfcommSocketToServiceRecord(
                    UUID.fromString(SERVICE_UUID)
                )
            stopDiscovery()

            if(currentClientSocket == null){
                Log.e("HELLOME","socket is null")
            }
            else{
                Log.e("HELLOME","socket is not null" + currentClientSocket.toString())
            }

            currentClientSocket?.let { socket ->
                try {
                    socket.connect()
                    emit(ConnectionResult.ConnectionEstablished)

                    Log.e("HELLOME","two")
//                    while(true){
//
//                    }
                    BluetoothDataTransferService(socket).also {
                        dataTransferService = it

                        //////////////////////////////////////////////////////////////////////////////////
//                        val stream: InputStream? = uri?.let { it1 ->
//                            context.contentResolver.openInputStream(
//                                it1
//                            )
//                        }
//                        var fileInfo: FileInfo? = null
//                        var byteArray: ByteArray
//
//                        stream.use { inputStream ->
//                            val outputStream = ByteArrayOutputStream()
//                            inputStream?.copyTo(outputStream)
//                            byteArray = outputStream.toByteArray()
//                            Log.e("HELLOME", "IN ByteArray = " + byteArray.size.toString())
//
//                            // Get file information
//                            fileInfo = uri?.let { it1 -> getFileDetailsFromUri(it1) }
//                        }
//
//                        // Serialize FileInfo and image data to byte array
//                        val fileData = FileData(fileInfo!!, byteArray)
//
//                        fileData.toByteArray()?.let { dataTransferService?.sendMessage(it) }
                        //////////////////////////////////////////////////////////////////////////////////



//
//                        FileData(FileInfo("null",null,null,null),"Hello".toByteArray(),true).toByteArray()
//                            ?.let { it1 -> it.sendMessage(it1) }

                        emitAll(
                            it.listenForIncomingMessages()
                               .map { ConnectionResult.TransferSucceeded(it) }
                        )
                    }
                    Log.e("HELLOME","one")
                } catch (e: Exception) {
                    socket.close()
                    currentClientSocket = null
                    emit(ConnectionResult.Error("Connection was interrupted"))
                    Log.e("HELLOME", "main prob = "+e.toString())
                }
            }
        }.onCompletion {
            closeConnection()
        }.flowOn(Dispatchers.IO)
    }

//    override suspend fun trySendMessage(message: String): BluetoothMessage? {
//        if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
//            return null
//        }
//
//        if (dataTransferService == null) {
//            return null
//        }
//
//        val bluetoothMessage = BluetoothMessage(
//            message = message,
//            senderName = bluetoothAdapter?.name ?: "Unknown name",
//            isFromLocalUser = true
//        )
//
//        dataTransferService?.sendMessage(bluetoothMessage.toByteArray())
//
//        return bluetoothMessage
//    }

    override suspend fun trySendFile(uri: Uri): FileData? {
        if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
            return null
        }

        if (dataTransferService == null) {
            return null
        }

        val stream: InputStream? = context.contentResolver.openInputStream(uri)
        var fileInfo: FileInfo? = null
        var byteArray: ByteArray

        stream.use { inputStream ->
            val outputStream = ByteArrayOutputStream()
            inputStream?.copyTo(outputStream)
            byteArray = outputStream.toByteArray()
            Log.e("HELLOME", "IN ByteArray = " + byteArray.size.toString())

            // Get file information
            fileInfo = getFileDetailsFromUri(uri)
        }

        // Serialize FileInfo and image data to byte array
        val fileData = FileData(fileInfo!!, byteArray)

        fileData.toByteArray()?.let { dataTransferService?.sendMessage(it) }

        return fileData
    }



    override fun closeConnection() {
        currentClientSocket?.close()
        currentServerSocket?.close()
        currentClientSocket = null
        currentServerSocket = null
    }

    override fun release() {
        context.unregisterReceiver(foundDeviceReceiver)
        context.unregisterReceiver(bluetoothStateReceiver)
        closeConnection()
    }

    private fun updatePairedDevices() {
        if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
            return
        }
        bluetoothAdapter
            ?.bondedDevices
            ?.map { it.toBluetoothDeviceDomain() }
            ?.also { devices ->
                _pairedDevices.update { devices }
            }
    }

    private fun hasPermission(permission: String): Boolean {
        return context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
    }

    fun getFileDetailsFromUri(
        uri: Uri
    ): FileInfo {
        var fileName: String? = null
        var format: String? = null
        var size: String? = null
        var mimeType: String? = null

        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            //val nameColumn = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
            val nameColumn = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)

            val sizeColumn = cursor.getColumnIndex(MediaStore.MediaColumns.SIZE)
            val mimeTypeColumn = cursor.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE)

            if (cursor.moveToFirst()) {
                fileName = cursor.getString(nameColumn)
                Log.e("HELLOMEEE", fileName.toString())
                format = fileName?.substringAfterLast('.', "")
                size = cursor.getLong(sizeColumn).toString()
                mimeType = cursor.getString(mimeTypeColumn)

                if (mimeType.isNullOrBlank()) {
                    mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(format?.toLowerCase())
                }
            }
        }

        return FileInfo(fileName, format, size, mimeType)
    }

    companion object {
        const val SERVICE_UUID = "27b7d1da-08c7-4505-a6d1-2459987e5e2d"
    }
}
