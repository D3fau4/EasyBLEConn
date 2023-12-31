package com.hato.bleconn;

import android.bluetooth.BluetoothDevice;
import android.os.Handler;
import android.util.Log;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public abstract class BluetoothService {
    protected static final boolean D = true;
    // Debugging
    private static final String TAG = BluetoothService.class.getSimpleName();
    protected static BluetoothService mDefaultServiceInstance;
    private static BluetoothConfiguration mDefaultConfiguration;
    private final Handler handler;
    protected BluetoothConfiguration mConfig;
    protected BluetoothStatus mStatus;
    protected OnBluetoothEventCallback onEventCallback;
    protected OnBluetoothScanCallback onScanCallback;

    protected BluetoothService(BluetoothConfiguration config) {
        this.mConfig = config;
        this.mStatus = BluetoothStatus.NONE;
        this.handler = new Handler();
    }

    /**
     * Use {@link BluetoothService#init(BluetoothConfiguration)} instead.
     *
     * @param config
     */
    @Deprecated
    public static void setDefaultConfiguration(BluetoothConfiguration config) {
        init(config);
    }

    /**
     * Configures and initialize the BluetoothService singleton instance.
     *
     * @param config
     */
    public static void init(BluetoothConfiguration config) {
        mDefaultConfiguration = config;
        if (mDefaultServiceInstance != null) {
            mDefaultServiceInstance.stopService();
            mDefaultServiceInstance = null;
        }
        try {
            Constructor<? extends BluetoothService> constructor =
                    (Constructor<? extends BluetoothService>) mDefaultConfiguration.bluetoothServiceClass.getDeclaredConstructors()[0];
            constructor.setAccessible(true);
            BluetoothService bluetoothService = constructor.newInstance(mDefaultConfiguration);
            mDefaultServiceInstance = bluetoothService;
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Get the BluetoothService singleton instance.
     *
     * @return
     */
    public synchronized static BluetoothService getDefaultInstance() {
        if (mDefaultServiceInstance == null) {
            throw new IllegalStateException("BluetoothService is not initialized. Call BluetoothService.init(config).");
        }
        return mDefaultServiceInstance;
    }

    public void setOnEventCallback(OnBluetoothEventCallback onEventCallback) {
        this.onEventCallback = onEventCallback;
    }

    public void setOnScanCallback(OnBluetoothScanCallback onScanCallback) {
        this.onScanCallback = onScanCallback;
    }

    public BluetoothConfiguration getConfiguration() {
        return mConfig;
    }

    protected synchronized void updateState(final BluetoothStatus status) {
        Log.v(TAG, "updateStatus() " + mStatus + " -> " + status);
        mStatus = status;

        // Give the new state to the Handler so the UI Activity can update
        if (onEventCallback != null)
            runOnMainThread(new Runnable() {
                @Override
                public void run() {
                    onEventCallback.onStatusChange(status);
                }
            });
    }

    protected void runOnMainThread(final Runnable runnable, final long delayMillis) {
        if (mConfig.callListenersInMainThread) {
            if (delayMillis > 0) {
                handler.postDelayed(runnable, delayMillis);
            } else {
                handler.post(runnable);
            }
        } else {
            if (delayMillis > 0) {
                new Thread() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(delayMillis);
                            runnable.run();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }.start();
            } else {
                runnable.run();
            }
        }
    }

    protected void runOnMainThread(Runnable runnable) {
        runOnMainThread(runnable, 0);
    }

    protected void removeRunnableFromHandler(Runnable runnable) {
        if (handler != null) {
            handler.removeCallbacks(runnable);
        }
    }

    /**
     * Current BluetoothService status.
     *
     * @return
     */
    public synchronized BluetoothStatus getStatus() {
        return mStatus;
    }

    /**
     * Start scan process and call the {@link OnBluetoothScanCallback}
     */
    public abstract void startScan();

    /**
     * Stop scan process and call the {@link OnBluetoothScanCallback}
     */
    public abstract void stopScan();

    /**
     * Try to connect to the device and call the {@link OnBluetoothEventCallback}
     */
    public abstract void connect(BluetoothDevice device);

    /**
     * Try to disconnect to the device and call the {@link OnBluetoothEventCallback}
     */
    public abstract void disconnect();

    /* ====================================
                STATICS METHODS
     ====================================== */

    /**
     * Write a array of bytes to the connected device.
     */
    public abstract void write(byte[] bytes);

    /**
     * Stops the BluetoothService and turn it unusable.
     */
    public abstract void stopService();

    /**
     * Request the connection priority.
     */
    public abstract void requestConnectionPriority(int connectionPriority);

    /* ====================================
                    CALLBACKS
     ====================================== */

    public interface OnBluetoothEventCallback {
        void onDataRead(byte[] buffer, int length);

        void onStatusChange(BluetoothStatus status);

        void onDeviceName(String deviceName);

        void onToast(String message);

        void onDataWrite(byte[] buffer);
    }

    public interface OnBluetoothScanCallback {
        void onDeviceDiscovered(BluetoothDevice device, int rssi);

        void onStartScan();

        void onStopScan();
    }
}
