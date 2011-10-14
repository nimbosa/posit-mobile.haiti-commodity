package org.hfoss.posit.android.experimental.sync;

/*
 * Copyright (C) 2010 The Android Open Source Project
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SyncResult;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import org.apache.http.ParseException;
import org.apache.http.auth.AuthenticationException;
import org.hfoss.posit.android.experimental.R;
import org.hfoss.posit.android.experimental.api.Find;
import org.hfoss.posit.android.experimental.api.FindHistory;
import org.hfoss.posit.android.experimental.api.SyncHistory;
//import org.hfoss.posit.android.experimental.api.authentication.NetworkUtilities;
import org.hfoss.posit.android.experimental.api.database.DbHelper;
import org.json.JSONException;

import java.io.IOException;
import java.util.Date;
import java.util.List;

/**
 * SyncAdapter implementation for syncing sample SyncAdapter contacts to the
 * platform ContactOperations provider.
 */
public class SyncAdapter extends AbstractThreadedSyncAdapter {

	private static final String TAG = "SyncAdapter";

	private final AccountManager mAccountManager;

	private final Context mContext;

	private Communicator communicator;

	private Date mLastUpdated;

	/**
	 * Account type string.
	 */
	public static final String ACCOUNT_TYPE = "org.hfoss.posit.account";

	/**
	 * Authtoken type string.
	 */
	public static final String AUTHTOKEN_TYPE = "org.hfoss.posit.account";

	public SyncAdapter(Context context, boolean autoInitialize) {
		super(context, autoInitialize);
		mContext = context;
		mAccountManager = AccountManager.get(context);
	}

	@Override
	public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider,
			SyncResult syncResult) {

		List<Find> finds;
		Log.i(TAG, "In onPerformSync()");
		String authToken = null;
		
		try {
			// use the account manager to request the credentials
			authToken = mAccountManager.blockingGetAuthToken(account, AUTHTOKEN_TYPE, true /* notifyAuthFailure */);
			Log.i(TAG, "auth token: " + authToken);

			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
			int projectId = prefs.getInt(mContext.getString(R.string.projectPref), 0);
			
			finds = DbHelper.getDbManager(mContext).getChangedFinds(projectId);
			Communicator.sendFindsToServer(finds, mContext, authToken);
			DbHelper.getDbManager(mContext).recordSync(new SyncHistory("idkwhatthisissupposedtobe"));
			
			Log.i(TAG, "histories: " +finds);
//			
//			boolean success = false;
//			//mdbh = new PositDbHelper(mContext);
//
//			//Log.i(TAG, "server=" + server + " key=" + authKey + " pid="
//			//		+ mProjectId + " imei=" + imei);
//
//			// Wait here to make sure there is a WIFI connection
//			//waitHere();
//			
//			// Check that project exists
//			if(!comm.projectExists(""+mProjectId, server))
//				mHandler.sendEmptyMessage(PROJECTERROR);
//			
//			// Get finds from the server since last sync with this device
//			// (NEEDED: should be made project specific)
//
//			String serverFindGuIds = Communicator.getServerFindsNeedingSync(mContext, authToken);
//
//			// Get finds from the client
//
//			String phoneFindGuIds = mdbh.getDeltaFindsIds(mProjectId);
//			Log.i(TAG, "phoneFindsNeedingSync = " + phoneFindGuIds);
//
//			// Send finds to the server
//
//			success = sendFindsToServer(phoneFindGuIds);
//
//			// Get finds from the server and store in the DB
//
//			success = getFindsFromServer(serverFindGuIds);
//
//			// Record the synchronization in the client's sync_history table
//
//			ContentValues values = new ContentValues();
//			values.put(PositDbHelper.SYNC_COLUMN_SERVER, server);
//
//			success = mdbh.recordSync(values);
//			if (!success) {
//				Log.i(TAG, "Error recording sync stamp");
//				mHandler.sendEmptyMessage(SYNCERROR);
//			}
//
//			// Record the synchronization in the server's sync_history table
//
//			String url = server + "/api/recordSync?authKey=" + authKey + "&imei="
//					+ imei;
//			Log.i(TAG, "recordSyncDone URL=" + url);
//			String responseString = "";
//
//			try {
//				responseString = comm.doHTTPGET(url);
//			} catch (Exception e) {
//				Log.i(TAG, e.getMessage());
//				e.printStackTrace();
//				mHandler.sendEmptyMessage(NETWORKERROR);
//			}
//			Log.i(TAG, "HTTPGet recordSync response = " + responseString);
//
//			mHandler.sendEmptyMessage(DONE);
//			return;
			
			
		} catch (final AuthenticatorException e) {
			syncResult.stats.numParseExceptions++;
			Log.e(TAG, "AuthenticatorException", e);
		} catch (final OperationCanceledException e) {
			Log.e(TAG, "OperationCanceledExcetpion", e);
		} catch (final IOException e) {
			Log.e(TAG, "IOException", e);
			syncResult.stats.numIoExceptions++;
		} catch (final ParseException e) {
			syncResult.stats.numParseExceptions++;
			Log.e(TAG, "ParseException", e);
		}
	}
}