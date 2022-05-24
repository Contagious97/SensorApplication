package com.example.sensorapplication

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.os.Bundle
import android.content.Intent
import com.example.sensorapplication.uiutils.MsgUtils
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.os.Build
import android.os.Handler
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sensorapplication.viewmodels.ScanViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import java.util.ArrayList

class ScanActivity : ComponentActivity() {
    private var mBluetoothAdapter: BluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    private var mScanning = false
    private var mHandler: Handler? = null
    private var mDeviceList: ArrayList<BluetoothDevice>? = null
    private var scanViewModel: ScanViewModel = ScanViewModel();

    /**
     * Below: Manage bluetooth initialization and life cycle
     * via Activity.onCreate, onStart and onStop.
     */
    @OptIn(ExperimentalPermissionsApi::class)
    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mDeviceList = ArrayList()
        mHandler = Handler()

        requestPermissions(Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION,
        Manifest.permission.WRITE_EXTERNAL_STORAGE){
            setContent{
                val infoText = scanViewModel.textView.value

                Scaffold(topBar = { TopAppBar(title = { Text(text = "Sensors App") }) }
                ) {

                    Column(modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Button(onClick = {
                            scanViewModel.clearDeviceList()
                            scanForDevices(true)
                        },Modifier.height(55.dp)) {
                            Text(text = "Scan for movesense devices")
                        }
                        Spacer(modifier = Modifier.padding(10.dp))
                        Button(onClick = {
                            val intent = Intent(this@ScanActivity, DeviceSensorActivity::class.java)
                            startActivity(intent)
                        },Modifier.height(55.dp)) {
                            Text(text = "Go to device sensors")
                        }
                        Spacer(modifier = Modifier.padding(10.dp))
                        Box{
                            Text(fontSize = 20.sp, text = infoText)
                        }
                        DeviceList()
                        //Sample(multiplePermissionsState = multiplePermissionsState)
                    }
                }

            }
        }

    }

    private fun requestPermissions(vararg permissions: String, onResult: (List<String>) -> Unit) {
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            val failed = result.filter { !it.value }.keys
            onResult(failed.toList())
        }.launch(arrayOf(*permissions))
    }

    @OptIn(ExperimentalMaterialApi::class)
    @Composable
    private fun DeviceList(){
        val deviceList = scanViewModel.deviceList.value;
        val nameList : MutableList<String> = mutableListOf()
        val addressList : MutableList<String> = mutableListOf()

        try {
            for (device in deviceList){
                nameList.add(device.name)
                addressList.add(device.address)
            }
        } catch (e:SecurityException){
            e.printStackTrace()
        }
        LazyColumn(){
            items(deviceList.size){
                item -> Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 5.dp)
                    ,
                elevation = 10.dp,
                onClick = {onDeviceSelected(scanViewModel.deviceList.value[item])}
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(text = nameList[item])
                    Text(text = addressList[item])
                }
            }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        // stop scanning
        scanForDevices(false)
        scanViewModel.clearDeviceList()
    }


    /*
     * Device selected, start DeviceActivity (displaying data)
     */
    private fun onDeviceSelected(device: BluetoothDevice) {
        // BluetoothDevice objects are parceable, i.e. we can "send" the selected device
        // to the DeviceActivity packaged in an intent.
        val intent = Intent(this@ScanActivity, MovesenseActivity::class.java)
        intent.putExtra(SELECTED_DEVICE, device)
        startActivity(intent)
    }

    /*
     * Scan for BLE devices.
     */
    private fun scanForDevices(enable: Boolean) {
        val scanner = mBluetoothAdapter!!.bluetoothLeScanner
        try {
            if (enable) {
                if (!mScanning) {
                    // stop scanning after a pre-defined scan period, SCAN_PERIOD
                    mHandler!!.postDelayed({
                        if (mScanning) {
                            mScanning = false
                            scanner.stopScan(mScanCallback)
                            MsgUtils.showToast("BLE scan stopped", this@ScanActivity)
                        }
                    }, SCAN_PERIOD)
                    mScanning = true
                    scanner.startScan(mScanCallback)
                    scanViewModel.setTextView("No devices found!")
                    MsgUtils.showToast("BLE scan started", this)
                }
            } else {
                if (mScanning) {
                    mScanning = false
                    scanner.stopScan(mScanCallback)
                    MsgUtils.showToast("BLE scan stopped", this)
                }
            }
        } catch (e:SecurityException){
            e.printStackTrace()
        }

    }

    /*
     * Implementation of scan callback methods
     */
    private val mScanCallback: ScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            val device = result.device
            try {
                val name = device.name
                mHandler!!.post {
                    if (name != null && name.contains(MOVESENSE)
                        && !mDeviceList!!.contains(device)
                    ) {

                            scanViewModel.addDevice(device = device)
                        val info = """Found ${scanViewModel.deviceList.value.size} device(s)
Touch to connect"""
                        scanViewModel.setTextView(info)
                        Log.i(LOG_TAG, device.toString())
                    }
                }
            } catch (e:SecurityException){
                e.printStackTrace()
            }

        }

        override fun onBatchScanResults(results: List<ScanResult>) {
            super.onBatchScanResults(results)
            Log.i(LOG_TAG, "onBatchScanResult")
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Log.i(LOG_TAG, "onScanFailed")
        }


    }

    companion object {
        const val MOVESENSE = "Movesense"
        var SELECTED_DEVICE = "Selected device"
        private const val SCAN_PERIOD: Long = 5000 // milliseconds
        private const val LOG_TAG = "ScanActivity"
    }
}