package se.kth.anderslm.movesense20.viewmodels

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.example.sensorapplication.utils.TypeConverter
import kotlin.math.atan
import kotlin.math.pow
import kotlin.math.sqrt

class MovesenseViewModel : ViewModel(){

    var deviceName : MutableState<String> = mutableStateOf("No device found")
    var connectionStatus : MutableState<String> = mutableStateOf("No connection")
    val recordingButtonText : MutableState<String> = mutableStateOf(START_RECORDING)
    var accelerometerPitch : MutableState<Double> = mutableStateOf(0.0)
    var combinedPitch : MutableState<Double> = mutableStateOf(0.0)
    var prevX : MutableState<Double> = mutableStateOf(0.0)
    var prevY : MutableState<Double> = mutableStateOf(0.0)
    var prevZ : MutableState<Double> = mutableStateOf(0.0)
    var prevGyroY : MutableState<Double> = mutableStateOf(0.0)
    var isRecording : MutableState<Boolean> = mutableStateOf(false)

    fun updateRecordingStatus(){
        isRecording.value = !isRecording.value
        recordingButtonText.value = if (isRecording.value) STOP_RECORDING else START_RECORDING
    }

    fun updateDeviceName(newDeviceName :String){
        deviceName.value = newDeviceName
    }

    fun updateConnectionStatus(newStatus :String){
        connectionStatus.value = newStatus
    }

    fun updateValues(sensorValues : ByteArray){

        val accX = TypeConverter.fourBytesToFloat(sensorValues, 6)
        val accY = TypeConverter.fourBytesToFloat(sensorValues, 10)
        val accZ = TypeConverter.fourBytesToFloat(sensorValues, 14)
        val yGyro = TypeConverter.fourBytesToFloat(sensorValues, 22)
        prevX.value =
            (FILTER_VALUE * prevX.value + (1 - FILTER_VALUE) * accX)
        prevY.value =
            (FILTER_VALUE * prevY.value + (1 - FILTER_VALUE) * accY)
        prevZ.value =
            (FILTER_VALUE * prevZ.value + (1 - FILTER_VALUE) * accZ)
        accelerometerPitch.value = 180 / Math.PI * atan(
            prevX.value / sqrt(
                prevY.value.pow(2.0) + prevZ.value.pow(2.0)
            )
        )
        prevGyroY.value =
            (FILTER_VALUE * prevGyroY.value + (1 - FILTER_VALUE) * yGyro)
        combinedPitch.value =
            (1 - FILTER_VALUE_COMB) * (combinedPitch.value + 0.07 * prevGyroY.value) + FILTER_VALUE_COMB * accelerometerPitch.value
    }

    companion object{
        private const val FILTER_VALUE = 0.1
        private const val FILTER_VALUE_COMB = 0.5
        private const val START_RECORDING = "Start Recording"
        private const val STOP_RECORDING = "Stop Recording"
    }

}