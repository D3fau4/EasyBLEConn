package com.hato.bleconn;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import java.util.UUID;

public class BluetoothConfiguration {
    private static final String TAG = BluetoothConfiguration.class.getSimpleName();

    public Class<? extends BluetoothService> bluetoothServiceClass;
    public Context context;
    public String deviceName;
    public int bufferSize;
    public char characterDelimiter;
    public UUID uuid;
    public UUID uuidService;
    public UUID uuidCharacteristic;
    public int transport;
    public boolean callListenersInMainThread = true;
    public int connectionPriority;

    public BluetoothConfiguration() {
        setDefaultTransport();
    }

    private void setDefaultTransport() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            transport = BluetoothDevice.TRANSPORT_LE;
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // From Android LOLLIPOP (21) the transport types exists, but them are hide for use,
            // so is needed to use relfection to get the value
            try {
                transport = BluetoothDevice.class.getDeclaredField("TRANSPORT_LE").getInt(null);
            } catch (Exception ex) {
                Log.d(TAG, "Error on get BluetoothDevice.TRANSPORT_LE with reflection.", ex);
            }
        } else {
            transport = -1;
        }
    }
}
