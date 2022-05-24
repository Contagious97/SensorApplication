package com.example.sensorapplication.models


import java.io.Serializable

class AnglePoint(var elevationAngle: Double, var timeMillis: Long) : Serializable {
    fun setElevationAngle(elevationAngle: Float) {
        this.elevationAngle = elevationAngle.toDouble()
    }
}