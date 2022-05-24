package com.example.sensorapplication

import android.os.Bundle
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.SensorEvent
import android.hardware.Sensor
import android.os.Environment
import android.os.Handler
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sensorapplication.models.AnglePoint
import com.example.sensorapplication.serialization.SerializeToFile
import com.example.sensorapplication.uiutils.MsgUtils
import com.example.sensorapplication.viewmodels.DeviceViewModel
import java.text.DecimalFormat

class DeviceSensorActivity : ComponentActivity(), SensorEventListener {
    private val LOG_TAG = "Device Sensor Activity"
    private var mHandler: Handler? = null

    private var mSensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null
    private var gyroscope: Sensor? = null

    private var accValuesList: MutableList<AnglePoint>? = mutableListOf()
    private var combinedValuesList: MutableList<AnglePoint>? = mutableListOf()
    private var startTime: Long = 0

    private var deviceViewModel : DeviceViewModel = DeviceViewModel()
    override fun onSensorChanged(sensorEvent: SensorEvent) {
        updateSensorValues(sensorEvent)
    }

    override fun onAccuracyChanged(sensor: Sensor, i: Int) {}
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mHandler = Handler()

        mSensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = mSensorManager!!.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        gyroscope = mSensorManager!!.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        mSensorManager!!.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
        mSensorManager!!.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_NORMAL)


        setContent {
            var accAngle : Double = deviceViewModel.prevAccAngle.value
            var combinedAngle : Double = deviceViewModel.prevCombinedAngle.value
            var recordButtonText : String = deviceViewModel.recordingButtonText.value
            var df = DecimalFormat("#.##")
            Scaffold(topBar = { TopAppBar(title = { Text(text = "Sensors App") }) }
            ) {
                Column(modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box{
                        Text(fontSize=20.sp,text = "Pitch from accelerometer:")
                    }
                    Spacer(modifier = Modifier.padding(10.dp))
                    Box{
                        Text(fontSize = 20.sp,text = df.format(accAngle)+"\u00b0")
                    }
                    Spacer(modifier = Modifier.padding(10.dp))
                    Box{
                        Text(fontSize = 20.sp, text = "Pitch from accelerometer and gyroscope")
                    }
                    Spacer(modifier = Modifier.padding(10.dp))
                    Box{
                        Text(fontSize = 20.sp, text = df.format(combinedAngle)+"\u00b0")
                    }
                    Spacer(modifier = Modifier.padding(10.dp))
                    Button(onClick = { startRecording()}, colors = ButtonDefaults.buttonColors(
                        backgroundColor = if (deviceViewModel.isRecording.value) Color.Red else Color.Green
                    )) {
                        Text(text = recordButtonText)
                    }
                }
            }
        }
    }

    private fun startRecording() {
        var isRecording = deviceViewModel.isRecording.value
        if (!isRecording) {
            mHandler!!.postDelayed({
                isRecording = deviceViewModel.isRecording.value
                if (isRecording) {
                    //isRecording = false
                        deviceViewModel.updateRecordingStatus()
                    MsgUtils.showToast(
                        "Stopped recording data after 10s",
                        this@DeviceSensorActivity
                    )
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
            deviceViewModel.updateRecordingStatus()
            MsgUtils.showToast("Started recording", this@DeviceSensorActivity)
            startTime = System.currentTimeMillis()
        } else {
            deviceViewModel.updateRecordingStatus()
            MsgUtils.showToast("Stopped recording", this@DeviceSensorActivity)
            //recordButton!!.setText(R.string.start_record)
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

    override fun onResume() {
        super.onResume()
        mSensorManager!!.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
        mSensorManager!!.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_NORMAL)
    }

    override fun onPause() {
        super.onPause()
        mSensorManager!!.unregisterListener(this, accelerometer)
        mSensorManager!!.unregisterListener(this, gyroscope)
    }

    override fun onStop() {
        super.onStop()
        mSensorManager!!.unregisterListener(this, accelerometer)
        mSensorManager!!.unregisterListener(this, gyroscope)
    }

    private fun updateSensorValues(sensorEvent: SensorEvent) {
        val sensor = sensorEvent.sensor
        if (sensor.type == Sensor.TYPE_ACCELEROMETER) {

            deviceViewModel.updateFromAccelerometer(sensorEvent.values)

        } else if (sensor.type == Sensor.TYPE_GYROSCOPE) {
            deviceViewModel.updateFromGyroscope(sensorEvent.values)

        }
        val isRecording = deviceViewModel.isRecording.value
        val prevAccValue = deviceViewModel.prevAccAngle.value
        val prevCombinedValue = deviceViewModel.prevCombinedAngle.value
        if (isRecording) {
            accValuesList!!.add(AnglePoint(prevAccValue, System.currentTimeMillis() - startTime))
            combinedValuesList!!.add(
                AnglePoint(
                    prevCombinedValue,
                    System.currentTimeMillis() - startTime
                )
            )
        }
    }

    companion object {
        private const val RECORDING_LIMIT: Long = 10000
        private const val FILE_NAME_ACC = "acc-data.json"
        private const val FILE_NAME_COMBINED = "comb-data.json"
    }
}
