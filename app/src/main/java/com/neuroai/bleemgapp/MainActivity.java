package com.neuroai.bleemgapp;
import android.Manifest;
import android.bluetooth.*;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresPermission;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.UUID;


public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_CODE_BLUETOOTH_SCAN = 1;

    private static final String TAG = "BLE_EMG";
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothGatt bluetoothGatt;

    // Replace these UUIDs with your BLE UUIDs from Arduino code
    private static final UUID SERVICE_UUID = UUID.fromString("19b10000-e8f2-537e-4f6c-d104768a1214");
    private static final UUID CHARACTERISTIC_UUID = UUID.fromString("19b10001-e8f2-537e-4f6c-d104768a1214");

    private TextView emgValueText;
    private Button connectButton;

    private static final String[] ALL_BLE_PERMISSIONS = new String[]{
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION
    };

    private boolean appLacksBlePermissions() {
        for (String permission : ALL_BLE_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (appLacksBlePermissions()) {
            ActivityCompat.requestPermissions(MainActivity.this, ALL_BLE_PERMISSIONS, 2);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        emgValueText = findViewById(R.id.emgValue);
        connectButton = findViewById(R.id.connectButton);

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "BLE not supported!", Toast.LENGTH_SHORT).show();
            finish();
        }

        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        connectButton.setOnClickListener(v -> startScan());
    }

//    @SuppressLint("MissingPermission")
    private void startScan() {
//        if (appLacksBlePermissions()) {
//            ActivityCompat.requestPermissions(MainActivity.this, ALL_BLE_PERMISSIONS, 2);
//            return;
//        }

        bluetoothAdapter.getBluetoothLeScanner().startScan(leScanCallback);
        Toast.makeText(this, "Scanning for BLE Device...", Toast.LENGTH_SHORT).show();
    }

    private final ScanCallback leScanCallback = new ScanCallback() {
        @Override
        @RequiresPermission(allOf = {Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN})
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            Log.d(TAG,"result device name: "+ result.getDevice().getName());
            if (result.getDevice().getName() != null && result.getDevice().getName().equals("Aadit")) {
                bluetoothAdapter.getBluetoothLeScanner().stopScan(leScanCallback);
                connectDevice(result.getDevice());
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode); // very sad days
            Toast.makeText(MainActivity.this, "we are going through sad times, we failed to connect", Toast.LENGTH_SHORT).show();
        }
    };

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private void connectDevice(BluetoothDevice device) {
        Toast.makeText(this, "Connecting to " + device.getName(), Toast.LENGTH_SHORT).show();
        bluetoothGatt = device.connectGatt(this, false, bluetoothGattCallback);
    }

    private final BluetoothGattCallback bluetoothGattCallback = new BluetoothGattCallback() {
        @Override
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d(TAG, "Connected to GATT server.");
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Connected!", Toast.LENGTH_SHORT).show());
                gatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d(TAG, "Disconnected from GATT server.");
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Disconnected!", Toast.LENGTH_SHORT).show());
            }
        }

        @Override
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        public void onServicesDiscovered(@NonNull BluetoothGatt gatt, int status) {
            BluetoothGattCharacteristic characteristic = gatt.getService(SERVICE_UUID).getCharacteristic(CHARACTERISTIC_UUID);
            gatt.setCharacteristicNotification(characteristic, true);

            BluetoothGattDescriptor descriptor = characteristic.getDescriptors().get(0);
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            gatt.writeDescriptor(descriptor);
        }

        @Override
        public void onCharacteristicChanged(@NonNull BluetoothGatt gatt, @NonNull BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            final byte[] emg = characteristic.getValue();
            int emgValue = Byte.toUnsignedInt(emg[0]);
            runOnUiThread(() -> emgValueText.setText("EMG Data: " + emgValue));
        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_CODE_BLUETOOTH_SCAN) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startScan();
            } else {
                Toast.makeText(this, "Well this app doesn't work without bluetooth... kind of stupid move", Toast.LENGTH_SHORT).show();
            }
        }
    }

//    @SuppressLint("MissingPermission")
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bluetoothGatt != null) {
//            if (appLacksBlePermissions()) {
//                ActivityCompat.requestPermissions(MainActivity.this, ALL_BLE_PERMISSIONS, 2);
//                return;
//            }

            bluetoothGatt.close();
            bluetoothGatt = null;
        }
    }
}
