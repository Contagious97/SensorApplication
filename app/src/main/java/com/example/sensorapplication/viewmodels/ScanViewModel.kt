package com.example.sensorapplication.viewmodels

import android.bluetooth.BluetoothDevice
import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel

class ScanViewModel:ViewModel() {
    var textView:MutableState<String> = mutableStateOf("No devices found")
    var deviceList:MutableState<List<BluetoothDevice>> = mutableStateOf(mutableListOf())
    var isScanning:MutableState<Boolean> = mutableStateOf(false)


    fun clearDeviceList(){
        deviceList.value.toMutableList().clear()
    }

    fun setTextView(newText :String){
        textView.value = newText;
    }

    fun changeScanning(){
        isScanning.value = !isScanning.value
    }

    fun addDevice(device:BluetoothDevice){
        if (!deviceList.value.contains(device)){
            val newList = deviceList.value.toMutableList();
            newList.add(device);
            deviceList.value = newList
            Log.i("Info","Devices: ${deviceList.value}")
        }
    }
}