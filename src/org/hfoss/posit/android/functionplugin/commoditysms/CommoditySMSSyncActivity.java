/*
 * File: BluetoothSyncActivity.java
 * 
 * Copyright (C) 2012 The Humanitarian FOSS Project (http://www.hfoss.org)
 * 
 * This file is part of POSIT, Portable Open Source Information Tool.
 *
 * POSIT is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License (LGPL) as published 
 * by the Free Software Foundation; either version 3.0 of the License, or (at
 * your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU LGPL along with this program; 
 * if not visit http://www.gnu.org/licenses/lgpl.html.
 * 
 */
package org.hfoss.posit.android.functionplugin.commoditysms;

import java.util.List;

import org.hfoss.posit.android.R;
import org.hfoss.posit.android.api.Find;
import org.hfoss.posit.android.api.database.DbHelper;
import org.hfoss.posit.android.api.database.DbManager;
import org.hfoss.posit.android.api.plugin.commodity.CommodityFind;
import org.hfoss.posit.android.api.plugin.commodity.CommoditySmsManager;
import org.hfoss.posit.android.sync.SyncCommoditySMS;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.j256.ormlite.android.apptools.OrmLiteBaseListActivity;

/**
 * This is the main Activity that displays the finds list to sync over Bluetooth
 * 
 * @author Elias Adum
 */
public class CommoditySMSSyncActivity extends OrmLiteBaseListActivity<DbManager> {

	// Debugging
	public static final String TAG = "BluetoothSyncActivity";
	
	// Message types sent from the BluetoothExplicitSyncService Handler
	public static final int MESSAGE_STATE_CHANGE = 1;
	public static final int MESSAGE_READ = 2;
	public static final int MESSAGE_WRITE = 3;
	public static final int MESSAGE_DEVICE_NAME = 4;
	public static final int MESSAGE_TOAST = 5;
	
	// Key names received from the BluetoothExplicitSyncService Handler
	public static final String DEVICE_NAME = "device_name";
	public static final String TOAST = "toast";
	
	// Intent request codes
	private static final int REQUEST_CONNECT_DEVICE = 1;
	private static final int REQUEST_ENABLE_BT = 2;
	
	// Layout Views
	private TextView mTitle;
	
	// Name of the connected device
	private String mConnectedDeviceName = null;
	// Local Bluetooth adapter
	private BluetoothAdapter mBluetoothAdapter = null;
	// Sync service
	private SyncCommoditySMS mSyncService = null;

	private SelectFindListAdapter mSFLAdapter;
	
	
//	public void sendFinds(Find find){
//		CommodityFind oiFind = (CommodityFind)find;
//		String smsPref = PreferenceManager.getDefaultSharedPreferences(this).getString(this.getString(R.string.smsPhoneKey), "");
////		CommoditySmsManager.sendSMS(smsPref, d+","+
////				cspinner.getItemAtPosition(posc)+","+
////				((EditText)findViewById(R.id.editText1)).getText().toString()+","+
////				((EditText)findViewById(R.id.editText3)).getText().toString()+","+
////				((EditText)findViewById(R.id.editText4)).getText().toString());
//		String s = oiFind.getMarket() + ","+ oiFind.getCommodity() + ","+
//		oiFind.getPrice1()+","+ oiFind.getPrice2()+","+ oiFind.getPrice3();
//		CommoditySmsManager.sendSMS(smsPref, s);
//	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.i(TAG, "onCreate()");
		super.onCreate(savedInstanceState);
		
		// Set up the window layout
		requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		setContentView(R.layout.bluetooth_main);
		getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE,
				R.layout.custom_title);

		// Set up the custom title
		mTitle = (TextView) findViewById(R.id.title_left_text);
		mTitle.setText(R.string.app_name);
		mTitle = (TextView) findViewById(R.id.title_right_text);
		
		// Get local Bluetooth adapter
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		
		// If adapter is null then Bluetooth is not supported
		if (mBluetoothAdapter == null) {
			Toast.makeText(this, getString(R.string.bt_toast_not_available), 
					Toast.LENGTH_LONG).show();
			finish();
			return;
		}
	}
	
	@Override
	public void onStart() {
		super.onStart();
		
		// If Bluetooth is not on, request it to be enabled
		if (!mBluetoothAdapter.isEnabled()) {
			Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
			
		// Otherwise set up the sync list
		} else {
			if (mSyncService == null) setupSync();
		}
	}
	
	@Override
	public synchronized void onResume() {
		super.onResume();
		
		// Performing this check in onResume() covers the case in which BT was
		// not enabled during onStart(), so we were paused to enable it...
		// onResume() will be called when ACTION_REQUEST_ENABLE activity
		// returns.
		if (mSyncService != null) {
			if (mSyncService.getState() == SyncCommoditySMS.STATE_NONE) {
				// Start the Bluetooth Service
				mSyncService.start();
			}
		}
	}
	
	@Override
	public synchronized void onPause() {
		super.onPause();
	}

	@Override
	public void onStop() {
		super.onStop();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		
		// Stop the Bluetooth services
		if (mSyncService != null) {
			mSyncService.stop();
		}
	}
	
	/**
	 * Toggle the checkbox of the item clicked
	 */
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		mSFLAdapter.toggleState(position);
	}
	
	/**
	 * Select all the items in the list
	 * 
	 * @param view
	 */
	public void selectAll(View view) {
		mSFLAdapter.selectAll();
	}

	/**
	 * Deselect all the items in the list
	 * 
	 * @param view
	 */
	public void selectNone(View view) {
		mSFLAdapter.deselectAll();
	}
	
	/**
	 * Start the sync over Bluetooth
	 * 
	 * @param view
	 */
	public void sendSelected(View view) {
		// Check that we are connected before trying to send
//		if (mSyncService.getState() != SyncBluetooth.STATE_CONNECTED) {
//			Toast.makeText(this, R.string.bt_not_connected, Toast.LENGTH_SHORT).show();
//			return;
//		}
		
		String[] guids = mSFLAdapter.getSelectedGuids();
		
		if (guids.length > 0) {
			Toast.makeText(this, R.string.bt_synching, Toast.LENGTH_SHORT).show();
//			mSyncService.sendFinds(guids);
			
			String smsPref = PreferenceManager.getDefaultSharedPreferences(this).getString(this.getString(R.string.smsPhoneKey), "");
			mSyncService.sendFinds(smsPref, guids);
			Toast.makeText(this, R.string.bt_synching_complete, Toast.LENGTH_SHORT).show();
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.bluetooth_main_menu, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.bt_connect:
			// Launch the DeviceListActivity to see devices and perform a scan
			Intent serverIntent = new Intent(this, CommoditySMSListActivity.class);
			startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
			return true;
		case R.id.bt_discoverable:
			// Ensure this device is discoverable by others
			ensureDiscoverable();
			return true;
		}
		
		return false;
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case REQUEST_CONNECT_DEVICE:
			// When DeviceListActivity returns a device to connect
			if (resultCode == Activity.RESULT_OK) {
				// Get the device MAC address
				String address = data.getExtras().getString(CommoditySMSListActivity.EXTRA_DEVICE_ADDRESS);
				// Get the Bluetooth Device object
				BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
				// Attempt to connect to the device
				mSyncService.connect(device);
			}
			break;

		case REQUEST_ENABLE_BT:
			// When the request to enable Bluetooth returns
			if (resultCode == Activity.RESULT_OK) {
				// Bluetooth is now enabled, so set up the sync list
				setupSync();
			} else {
				// User did not enable Bluetooth or error occurred
				Log.d(TAG, "BT not enabled");
				Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
				finish();
			}
			break;
		}
	}
	
	/**
	 * Ensure that we can be detected by other Bluetooth devices during a scan.
	 */
	private void ensureDiscoverable() {
		if (mBluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
			Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
			discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
			startActivity(discoverableIntent);
		}
	}

	/**
	 * Setup the adapter, view, and service
	 */
	private void setupSync() {
		
		mSFLAdapter = new SelectFindListAdapter(this);
		mSyncService = new SyncCommoditySMS(this, mHandler);
		
		List<String> finds = mSyncService.getFindsNeedingSync();
		
		DbManager dbHelper = this.getHelper();
		
		for (String findGuid : finds) {
			CommodityFind f = (CommodityFind)dbHelper.getFindByGuid(findGuid);
			if (0 == f.getSMSStatus()){
				Log.i(TAG, "SMS status = " + f.getSMSStatus());
				mSFLAdapter.addItem(new SelectFind(findGuid, false, f.getId(), 
					f.getCommodity(), f.getMarket()));
				
				}
		}
		
		setListAdapter(mSFLAdapter);
	}
	
	
	/************************ UI Handler *************************************/
	private final Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MESSAGE_STATE_CHANGE:
				switch (msg.arg1) {
				case SyncCommoditySMS.STATE_CONNECTED:
					mTitle.setText(R.string.bt_title_connected_to);
					mTitle.append(mConnectedDeviceName);
					break;

				case SyncCommoditySMS.STATE_CONNECTING:
					mTitle.setText(R.string.bt_title_connecting);
					break;
					
				case SyncCommoditySMS.STATE_LISTEN:
				case SyncCommoditySMS.STATE_NONE:
					mTitle.setText(R.string.bt_title_not_connected);
					break;
				}
				break;

			case MESSAGE_WRITE:
				// Send find successfully
				if ((Boolean) msg.obj) {
					Toast.makeText(getApplicationContext(), 
						R.string.bt_successful_send, Toast.LENGTH_SHORT).show();
				} else {
					Toast.makeText(getApplicationContext(), 
						R.string.bt_unsuccessful_send, Toast.LENGTH_SHORT).show();
				} 
				break;
				
			case MESSAGE_READ:
				// Received find successfully
				if ((Boolean) msg.obj) {
					Toast.makeText(getApplicationContext(), 
						R.string.bt_successful_recv, Toast.LENGTH_SHORT).show();
				} else {
					Toast.makeText(getApplicationContext(), 
						R.string.bt_unsuccessful_recv, Toast.LENGTH_SHORT).show();
				}
				break;
				
			case MESSAGE_DEVICE_NAME:
				// Save the connected device's name
				mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
				Toast.makeText(getApplicationContext(), 
					"Connected to " + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
				break;
				
			case MESSAGE_TOAST:
				Toast.makeText(getApplicationContext(), 
					msg.getData().getString(TOAST), Toast.LENGTH_SHORT).show();
				break;
			}
		}
	};

}