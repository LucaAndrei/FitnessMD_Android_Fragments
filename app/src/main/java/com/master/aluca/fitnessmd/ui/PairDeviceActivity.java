/*********************************************************
 *
 * Copyright (c) 2017 Andrei Luca
 * All rights reserved. You may not copy, distribute, publicly display,
 * create derivative works from or otherwise use or modify this
 * software without first obtaining a license from Andrei Luca
 *
 *********************************************************/

package com.master.aluca.fitnessmd.ui;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.master.aluca.fitnessmd.R;
import com.master.aluca.fitnessmd.common.Constants;
import com.master.aluca.fitnessmd.common.webserver.WebserverManager;
import com.master.aluca.fitnessmd.service.FitnessMDService;

import java.util.Set;

public class PairDeviceActivity extends Activity {

    public static final String LOG_TAG = "Fitness_PairDevice";
    private ArrayAdapter<String> mPairedDevicesArrayAdapter;
    private ArrayAdapter<String> mNewDevicesArrayAdapter;
    BluetoothAdapter mBluetoothAdapter;
    Button mScanButton;
    private FitnessMDService mService;
    private Context mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pair_device);
        Log.d(LOG_TAG, "onCreate");
        mContext = getApplicationContext();

        // Get the local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        initializeElements();
    }

    private void initializeElements() {
        // Initialize the button to perform device discovery
        mScanButton = (Button) findViewById(R.id.button_scan);
        //mScanButton.setVisibility(View.GONE);
        mScanButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                Log.d(LOG_TAG, "mScanButton onClick");
                mNewDevicesArrayAdapter.clear();
                if (!mService.isBluetoothEnabled()) {
                    Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableIntent, Constants.REQUEST_ENABLE_BT);
                } else {
                    doDiscovery();
                }
            }
        });

        // Initialize array adapters. One for already paired devices and
        // one for newly discovered devices
        mPairedDevicesArrayAdapter = new ArrayAdapter<String>(this, R.layout.list_view_layout_text_color);
        mNewDevicesArrayAdapter = new ArrayAdapter<String>(this, R.layout.list_view_layout_text_color);

        // Find and set up the ListView for paired devices
        ListView pairedListView = (ListView) findViewById(R.id.paired_devices);
        pairedListView.setAdapter(mPairedDevicesArrayAdapter);
        pairedListView.setOnItemClickListener(mDeviceClickListener);

        // Find and set up the ListView for newly discovered devices
        ListView newDevicesListView = (ListView) findViewById(R.id.new_devices);
        newDevicesListView.setAdapter(mNewDevicesArrayAdapter);
        newDevicesListView.setOnItemClickListener(mDeviceClickListener);

        // Get a set of currently paired devices
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();

        // If there are paired devices, add each one to the ArrayAdapter
        if (pairedDevices.size() > 0) {
            Log.d(LOG_TAG, "pairedDevices.size() > 0");
            findViewById(R.id.title_paired_devices).setVisibility(View.VISIBLE);
            for (BluetoothDevice device : pairedDevices) {
                mPairedDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
            }
        } else {
            Log.d(LOG_TAG, "pairedDevices.size() <<< 0");
            String noDevices = getResources().getText(R.string.none_paired).toString();
            mPairedDevicesArrayAdapter.add(noDevices);
        }
    }


    private ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            Log.d(LOG_TAG, "on service connected");
            FitnessMDService.FitnessMD_Binder binder = (FitnessMDService.FitnessMD_Binder) iBinder;
            mService = binder.getService();

            if (mService == null) {
                Log.e(LOG_TAG, "unable to connect to service");
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.d(LOG_TAG, "on service disconnected");
            mService = null;
        }
    };

    @Override
    public void onStart() {
        super.onStart();
        Log.d(LOG_TAG, "onStart()");

        Intent intent = new Intent(mContext, FitnessMDService.class);
        if (!FitnessMDService.isServiceRunning()) {
            mContext.startService(intent);
        }

        if (!mContext.bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE)) {
            Log.e(LOG_TAG, "Unable to bind to fitnessmd service");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Register for broadcasts when a device is discovered
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        intentFilter.addAction(Constants.FINISH_ACTIVITY_INTENT);
        this.registerReceiver(mBroadcastReceiver, intentFilter);
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(mBroadcastReceiver);
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(LOG_TAG, "onStop()");

        if (mService != null) {
            mContext.unbindService(mServiceConnection);
        }
    }

    /**
     * Start device discover with the BluetoothAdapter
     */
    private void doDiscovery() {
        Log.d(LOG_TAG, "doDiscovery()");
        mScanButton.setText("Scanning ...");
        mScanButton.setEnabled(false);
        mScanButton.setBackgroundColor(Color.LTGRAY);
        // Indicate scanning in the title
        setProgressBarIndeterminateVisibility(true);
        setTitle(R.string.scanning);

        // Turn on sub-title for new devices
        findViewById(R.id.title_new_devices).setVisibility(View.VISIBLE);

        // If we're already discovering, stop it
        if (mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
        }

        // Request discover from BluetoothAdapter
        mBluetoothAdapter.startDiscovery();
    }

    // The on-click listener for all devices in the ListViews
    private OnItemClickListener mDeviceClickListener = new OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {

            // Cancel discovery because it's costly and we're about to connect
            mBluetoothAdapter.cancelDiscovery();
            mScanButton.setText("Scan for devices");
            mScanButton.setEnabled(true);
            mScanButton.setBackgroundColor(Color.parseColor("#52B3D9"));

            // Get the device MAC address, which is the last 17 chars in the View
            String info = ((TextView) v).getText().toString();
            Log.d(LOG_TAG, "OnItemClickListener : " + info);
            if (info != null && info.length() > 16) {
                Log.d(LOG_TAG, "info != null && info.length() > 16");
                String address = info.substring(info.length() - 17);
                Log.d(LOG_TAG, "User selected device : " + address);
                if (address != null && mService != null) {
                    mService.connectDevice(address);
                }
            } else {
                Log.d(LOG_TAG, "OnItemClickListener else");
            }
        }
    };

    // The BroadcastReceiver that listens for discovered devices and
    // changes the title when discovery is finished
    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(LOG_TAG, "onReceive : " + action);
            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // If it's already paired, skip it, because it's been listed already
                Log.d(LOG_TAG, "device getName: " + device.getName() + " >>> address : " + device.getAddress());
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    mNewDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                }
                // When discovery is finished, change the Activity title
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                setProgressBarIndeterminateVisibility(false);
                setTitle(R.string.select_device);
                if (mNewDevicesArrayAdapter.getCount() == 0) {
                    String noDevices = getResources().getText(R.string.none_found).toString();
                    mNewDevicesArrayAdapter.add(noDevices);
                }
                //mScanButton.setVisibility(View.VISIBLE);
                mScanButton.setText("Scan for devices");
                mScanButton.setEnabled(true);
                mScanButton.setBackgroundColor(Color.parseColor("#52B3D9"));
            } else if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                if (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1)
                        == BluetoothAdapter.STATE_OFF) {
                    Log.d(LOG_TAG, "Service - Bluetooth turned off");
                    mBluetoothAdapter.cancelDiscovery();
                }
            } else if (intent.getAction() == Constants.FINISH_ACTIVITY_INTENT) {
                Log.d(LOG_TAG, "FINISH_ACTIVITY_INTENT received");
                boolean shouldFinish = intent.getBooleanExtra(Constants.FINISH_ACTIVITY_BUNDLE_KEY,false);
                if (shouldFinish) {
                    Intent intentMainActiv = new Intent(getApplicationContext(), NoMeteorConnectionActivity.class);
                    startActivity(intentMainActiv);
                    finish();
                    overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
                    WebserverManager mWebserverManager = WebserverManager.getInstance(getApplicationContext());
                    mWebserverManager.destroyMeteor();
                } else {
                    finish();
                }
            }
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Constants.REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_CANCELED) {
                Log.d(LOG_TAG, "onActivityResult requestCode: " + "Enable Bluetooth"
                        + " >> resultCode : " + " CANCELED ");
                Toast.makeText(getApplicationContext(), "Some functions will not work unless you turn on bluetooth", Toast.LENGTH_SHORT).show();
                //finish();
            } else if (resultCode == RESULT_OK) {
                Log.d(LOG_TAG, "onActivityResult requestCode: " + "Enable Bluetooth"
                        + " >> resultCode : " + " ALLOWED ");
                // Bluetooth is now enabled, so set up a BT session
                doDiscovery();
                mService.initializeBluetoothManager();
            }

        }
    }

    @Override
    protected void onDestroy() {
        Log.d(LOG_TAG, "onDestroy");
        super.onDestroy();
        finish();
    }
}
