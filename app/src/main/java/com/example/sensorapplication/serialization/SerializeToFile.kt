package com.example.sensorapplication.serialization


import android.util.Log
import com.example.sensorapplication.models.AnglePoint
import com.google.gson.Gson
import java.io.File
import java.io.FileOutputStream
import java.lang.Exception

object SerializeToFile {
    private val LOG_TAG = SerializeToFile::class.java.simpleName
    fun writeToFile(dataList: List<AnglePoint>?, folderPath: File?, fileName: String?) {




        /*var listOfData : MutableList<Object> = mutableListOf();
        Log.d(LOG_TAG,"Time and elevation: " + timeAndElevation.getTime() + "s |" +  timeAndElevation.getElevation() + "°");
        listOfData.add(timeAndElevation.getTime());
        listOfData.add(timeAndElevation.getElevation());*/



        Log.d(LOG_TAG, "Folder path: " + folderPath!!.absolutePath)
        try {
            val myFilePath = File(folderPath, fileName)
            if (!myFilePath.exists()) {
                myFilePath.createNewFile()
            }
            val gson = Gson()
            val json = gson.toJson(dataList)
            Log.d(LOG_TAG, json)
            val fileOutputStream = FileOutputStream(myFilePath)
            fileOutputStream.write(json.toByteArray())
            fileOutputStream.close()

//            Log.i(LOG_TAG,"The list of objects were successfully written to a file");
        } catch (ex: Exception) {
            Log.e(LOG_TAG, "Exception thrown in writeToFile: $ex")
            Log.d(LOG_TAG, "Could not write to file")
            ex.printStackTrace()
        }
    }
}

