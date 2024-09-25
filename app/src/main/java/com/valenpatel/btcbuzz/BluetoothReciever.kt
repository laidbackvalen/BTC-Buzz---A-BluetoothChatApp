package com.valenpatel.btcbuzz

import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BluetoothReciever : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val action = intent?.action
        if (action == BluetoothAdapter.ACTION_STATE_CHANGED) {
            when (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1)) {
                BluetoothAdapter.STATE_OFF -> {
                    // Bluetooth is off
                    Log.d("BluetoothReceiver", "State OFF")
                }

                BluetoothAdapter.STATE_ON -> {
                    // Bluetooth is on
                    Log.d("BluetoothReceiver", "State ON")
                }

                BluetoothAdapter.STATE_TURNING_OFF -> {
                    // Bluetooth is turning off
                    Log.d("BluetoothReceiver", "State TURNING OFF")
                }

                BluetoothAdapter.STATE_TURNING_ON -> {
                    // Bluetooth is turning on
                    Log.d("BluetoothReceiver", "State TURNING ON")
                }
            }
        }
    }
}
