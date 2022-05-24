package com.example.sensorapplication.viewmodels

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import kotlin.math.atan
import kotlin.math.pow
import kotlin.math.sqrt

class DeviceViewModel: ViewModel() {

    var prevAccAngle: MutableState<Double> = mutableStateOf(0.0)
    val recordingButtonText : MutableState<String> = mutableStateOf(START_RECORDING)
    var prevFilteredAccAngle: MutableState<Double> = mutableStateOf(0.0)
    var prevCombinedAngle: MutableState<Double> = mutableStateOf(0.0)
    var prevX: MutableState<Double> = mutableStateOf(0.0)
    var prevY: MutableState<Double> = mutableStateOf(0.0)
    var prevZ: MutableState<Double> = mutableStateOf(0.0)
    var prevGyroY: MutableState<Double> = mutableStateOf(0.0)
    var isRecording : MutableState<Boolean> = mutableStateOf(false)


    fun updateRecordingStatus(){
        isRecording.value = !isRecording.value
        recordingButtonText.value = if (isRecording.value) STOP_RECORDING else START_RECORDING
    }
    fun updateFromAccelerometer(values: FloatArray){
        prevX.value =
            (FILTER_VALUE_ACC * prevX.value + (1 - FILTER_VALUE_ACC) * values[0])
        prevY.value =
            (FILTER_VALUE_ACC * prevY.value + (1 - FILTER_VALUE_ACC) * values[1])
        prevZ.value =
            (FILTER_VALUE_ACC * prevZ.value + (1 - FILTER_VALUE_ACC) * values[2])
        prevAccAngle.value =
            180 / Math.PI * atan(prevX.value / sqrt((prevY.value.pow(2.0) + prevZ.value.pow(2.0)).toDouble()))


        prevFilteredAccAngle.value =
            FILTER_VALUE_ACC * prevFilteredAccAngle.value + (1 - FILTER_VALUE_ACC) * prevAccAngle.value

    }

    fun updateFromGyroscope(values: FloatArray){
        prevGyroY.value = values[1].toDouble()
        prevCombinedAngle.value =
            (1 - FILTER_VALUE_COMB) * (prevCombinedAngle.value + prevGyroY.value) + FILTER_VALUE_COMB * prevAccAngle.value

    }

    companion object{
        private const val FILTER_VALUE_ACC = 0.1f
        private const val FILTER_VALUE_COMB = 0.5f
        private const val START_RECORDING = "Start Recording"
        private const val STOP_RECORDING = "Stop Recording"
    }

}