/*
*/
package com.example.sensorapplication



import android.bluetooth.*

import android.widget.TextView

import android.os.Bundle

import android.os.Environment
import android.os.Build

import android.os.Handler
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sensorapplication.models.AnglePoint
import com.example.sensorapplication.uiutils.MsgUtils
import com.example.sensorapplication.utils.TypeConverter
import se.kth.anderslm.movesense20.viewmodels.MovesenseViewModel
import java.text.DecimalFormat
import java.util.*
import com.example.sensorapplication.serialization.SerializeToFile

class MovesenseActivity : ComponentActivity() {
    private var accPitch = 0.0
    private var comPitch = 0.0

    private var startTime: Long = 0
    private var accValuesList: MutableList<AnglePoint>? = mutableListOf()
    private var combinedValuesList: MutableList<AnglePoint>? = mutableListOf()
    private val IMU_COMMAND2 = "Meas/IMU6/13" // see documentation
    private val MOVESENSE_REQUEST: Byte = 1
    private val MOVESENSE_RESPONSE: Byte = 2
    private val REQUEST_ID: Byte = 99
    private var mSelectedDevice: BluetoothDevice? = null
    private var mBluetoothGatt: BluetoothGatt? = null
    private var mHandler: Handler? = null


    private var movesenseViewModel:MovesenseViewModel = MovesenseViewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val intent = intent
        // Get the selected device from the intent
        mSelectedDevice = intent.getParcelableExtra(ScanActivity.SELECTED_DEVICE)
        if (mSelectedDevice == null) {
            MsgUtils.createDialog("Error", "No device found", this).show()
        } else {
            try {
                movesenseViewModel.updateDeviceName(mSelectedDevice!!.name)
            } catch (e:SecurityException){
                e.printStackTrace()
            }
        }
        mHandler = Handler()

        setContent {
            var accAngle : Double = movesenseViewModel.accelerometerPitch.value
            var combinedAngle : Double = movesenseViewModel.combinedPitch.value
            var deviceName : String = movesenseViewModel.deviceName.value
            var recordButtonText : String = movesenseViewModel.recordingButtonText.value
            Scaffold(topBar = { TopAppBar(title = { Text(text = "Sensors App") }) }
            ) {
                Column(modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(fontSize = 22.sp, text = "Movesense device")
                    Text(fontSize = 22.sp, text = deviceName)
                    Spacer(modifier = Modifier.padding(30.dp))
                    Text(text = "Angle from accelerometer:")
                    Spacer(modifier = Modifier.padding(10.dp))
                    Text(fontSize = 20.sp,text = df.format(accAngle)+"\u00b0")
                    Spacer(modifier = Modifier.padding(10.dp))
                    Text(text = "Angle from accelerometer and gyroscope:")
                    Spacer(modifier = Modifier.padding(10.dp))
                    Text(fontSize = 20.sp,text = df.format(combinedAngle)+"\u00b0")
                    Spacer(modifier = Modifier.padding(10.dp))
                    Button(onClick = { startRecording()}, colors = ButtonDefaults.buttonColors(
                        backgroundColor = if (movesenseViewModel.isRecording.value) Color.Red else Color.Green
                    )) {
                        Text(text = recordButtonText)
                    }
                    }
                }
            }
        }


    private fun startRecording() {
        var isRecording = movesenseViewModel.isRecording.value
        if (!isRecording) {
            mHandler!!.postDelayed({
                isRecording = movesenseViewModel.isRecording.value
                if (isRecording) {
                    movesenseViewModel.updateRecordingStatus()
                    MsgUtils.showToast("Stopped recording data after 10s", this@MovesenseActivity)
                    SerializeToFile.writeToFile(
                        accValuesList,
                        getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                        FILE_NAME_ACC
                    )
                    SerializeToFile.writeToFile(
                        combinedValuesList,
                        getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                        FILE_NAME_COMBINED
                    )
                }
            }, RECORDING_LIMIT)
            combinedValuesList!!.clear()
            accValuesList!!.clear()
            movesenseViewModel.updateRecordingStatus()
            MsgUtils.showToast("Started recording", this@MovesenseActivity)
            startTime = System.currentTimeMillis()
        } else {
            movesenseViewModel.updateRecordingStatus()
            MsgUtils.showToast("Stopped recording", this@MovesenseActivity)
            SerializeToFile.writeToFile(
                accValuesList,
                getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                FILE_NAME_ACC
            )
            SerializeToFile.writeToFile(
                combinedValuesList,
                getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                FILE_NAME_COMBINED
            )
        }
    }

    override fun onStart() {
        super.onStart()
        if (mSelectedDevice != null) {
            // Connect and register call backs for bluetooth gatt
                try {
                    mBluetoothGatt = mSelectedDevice!!.connectGatt(this, false, mBtGattCallback)
                } catch (e:SecurityException){
                    e.printStackTrace()
                }
        }
    }

    override fun onStop() {
        super.onStop()
        if (mBluetoothGatt != null) {
            try {
                mBluetoothGatt!!.disconnect()
                mBluetoothGatt!!.close()
            } catch (e: SecurityException) {
                // ugly, but this is to handle a bug in some versions in the Android BLE API
                e.printStackTrace()
            }
        }
    }

/**
     * Callbacks for bluetooth gatt changes/updates
     * The documentation is not always clear, but most callback methods seems to
     * be executed on a worker thread - hence use a Handler when updating the ui.
     */

    private val mBtGattCallback: BluetoothGattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothGatt.STATE_CONNECTED) {
                mBluetoothGatt = gatt
                mHandler!!.post { movesenseViewModel.updateConnectionStatus("Connected") }
                // Discover services
                try {
                    gatt.discoverServices()
                } catch (e:SecurityException){
                    e.printStackTrace()
                }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                // Close connection and display info in ui
                mBluetoothGatt = null
                mHandler!!.post { movesenseViewModel.updateConnectionStatus("Disconnected") }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                // Debug: list discovered services
                val services = gatt.services
                for (service in services) {
                    Log.i(LOG_TAG, service.uuid.toString())
                }

                // Get the Movesense 2.0 IMU service
                val movesenseService = gatt.getService(MOVESENSE_2_0_SERVICE)
                if (movesenseService != null) {
                    // debug: service present, list characteristics
                    val characteristics = movesenseService.characteristics
                    for (chara in characteristics) {
                        Log.i(LOG_TAG, chara.uuid.toString())
                    }
                    // Write a command, as a byte array, to the command characteristic
                    // Callback: onCharacteristicWrite
                    val commandChar = movesenseService.getCharacteristic(
                        MOVESENSE_2_0_COMMAND_CHARACTERISTIC
                    )
                    // command example: 1, 99, "/Meas/Acc/13"
                    val command = TypeConverter.stringToAsciiArray(REQUEST_ID, IMU_COMMAND2)
                    commandChar.value = command
                    try {
                        val wasSuccess = mBluetoothGatt!!.writeCharacteristic(commandChar)
                        Log.i("writeCharacteristic", "was success=$wasSuccess")
                    } catch (e:SecurityException){
                        e.printStackTrace()
                    }

                } else {
                    mHandler!!.post {
                        MsgUtils.createDialog(
                            "Alert!",
                            "Service not found",
                            this@MovesenseActivity
                        )
                            .show()
                    }
                }
            }
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            Log.i(LOG_TAG, "onCharacteristicWrite " + characteristic.uuid.toString())

            // Enable notifications on data from the sensor. First: Enable receiving
            // notifications on the client side, i.e. on this Android device.
            val movesenseService = gatt.getService(MOVESENSE_2_0_SERVICE)
            val dataCharacteristic = movesenseService.getCharacteristic(
                MOVESENSE_2_0_DATA_CHARACTERISTIC
            )
            // second arg: true, notification; false, indication

            try {
                val success = gatt.setCharacteristicNotification(dataCharacteristic, true)
                if (success) {
                    Log.i(LOG_TAG, "setCharactNotification success")
                    // Second: set enable notification server side (sensor). Why isn't
                    // this done by setCharacteristicNotification - a flaw in the API?
                    val descriptor = dataCharacteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG)
                    descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    gatt.writeDescriptor(descriptor) // callback: onDescriptorWrite
                } else {
                    Log.i(LOG_TAG, "setCharacteristicNotification failed")
                }
            } catch (e:SecurityException){
                e.printStackTrace()
            }

        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int
        ) {
            Log.i(LOG_TAG, "onDescriptorWrite, status $status")
            if (CLIENT_CHARACTERISTIC_CONFIG == descriptor.uuid) if (status == BluetoothGatt.GATT_SUCCESS) {
                // if success, we should receive data in onCharacteristicChanged
                mHandler!!.post { movesenseViewModel.updateConnectionStatus("Notifictations enabled") }
            }
        }

/**
         * Callback called on characteristic changes, e.g. when a sensor data value is changed.
         * This is where we receive notifications on new sensor data.
         */

        @RequiresApi(api = Build.VERSION_CODES.M)
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            // debug
            // Log.i(LOG_TAG, "onCharacteristicChanged " + characteristic.getUuid());

            // if response and id matches
            if (MOVESENSE_2_0_DATA_CHARACTERISTIC == characteristic.uuid) {
                val data = characteristic.value
                //Log.i(LOG_TAG,"Response: " + data[0]);
                if (data[0] == MOVESENSE_RESPONSE && data[1] == REQUEST_ID) {
                    // NB! use length of the array to determine the number of values in this
                    // "packet", the number of values in the packet depends on the frequency set(!)
                    val len = data.size
                    //                    Log.i(LOG_TAG,"Data: " + Arrays.toString(data) + "data length: " + len);
                    if (len <= 30) {
                        movesenseViewModel.updateValues(data)

                        mHandler!!.post {
                            val isRecording = movesenseViewModel.isRecording.value
                            if (isRecording) {
                                //recordButton!!.text = "Stop Recording"
                                accValuesList!!.add(
                                    AnglePoint(
                                        accPitch,
                                        System.currentTimeMillis() - startTime
                                    )
                                )
                                combinedValuesList!!.add(
                                    AnglePoint(
                                        comPitch,
                                        System.currentTimeMillis() - startTime
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            Log.i(LOG_TAG, "onCharacteristicRead " + characteristic.uuid.toString())
        }
    }

    companion object {
        // Movesense 2.0 UUIDs (should be placed in resources file)
        val MOVESENSE_2_0_SERVICE = UUID.fromString("34802252-7185-4d5d-b431-630e7050e8f0")
        val MOVESENSE_2_0_COMMAND_CHARACTERISTIC =
            UUID.fromString("34800001-7185-4d5d-b431-630e7050e8f0")
        val MOVESENSE_2_0_DATA_CHARACTERISTIC =
            UUID.fromString("34800002-7185-4d5d-b431-630e7050e8f0")
        // UUID for the client characteristic, which is necessary for notifications
        val CLIENT_CHARACTERISTIC_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
        private const val RECORDING_LIMIT: Long = 10000
        private val df = DecimalFormat("#.##")
        private const val FILE_NAME_ACC = "accSens-data.json"
        private const val FILE_NAME_COMBINED = "combSens-data.json"
        private const val LOG_TAG = "DeviceActivity"
    }
}

