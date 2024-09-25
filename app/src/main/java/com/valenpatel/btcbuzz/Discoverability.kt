package com.valenpatel.btcbuzz

import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class Discoverability : BroadcastReceiver() {
    override fun onReceive(p0: Context?, p1: Intent?) {
      val action = p1?.action

        if(action!=null){
            if(action==BluetoothAdapter.ACTION_STATE_CHANGED){
                when(p1.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE,BluetoothAdapter.ERROR)) {
                BluetoothAdapter.SCAN_MODE_CONNECTABLE->{
                    Log.d("Discoverability", "Connectable")
                }BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE->{
                    Log.d("Discoverability", "Discoverable")
                }
                BluetoothAdapter.SCAN_MODE_NONE->{
                    Log.d("Discoverability", "None")
                }

                }
            }
        }
    }
}