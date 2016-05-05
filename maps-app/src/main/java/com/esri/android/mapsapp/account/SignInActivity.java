/* Copyright 1995-2014 Esri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * For additional information, contact:
 * Environmental Systems Research Institute, Inc.
 * Attn: Contracts Dept
 * 380 New York Street
 * Redlands, California, USA 92373
 *
 * email: contracts@esri.com
 *
 */

package com.esri.android.mapsapp.account;

import android.app.Activity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.esri.android.mapsapp.R.id;
import com.esri.android.mapsapp.R.layout;
import com.esri.android.mapsapp.R.string;
import com.esri.android.mapsapp.dialogs.ProgressDialogFragment;
import com.esri.android.mapsapp.util.StringUtils;
import com.esri.arcgisruntime.loadable.LoadStatus;
import com.esri.arcgisruntime.portal.Portal;
import com.esri.arcgisruntime.portal.PortalInfo;
import com.esri.arcgisruntime.portal.PortalUser;
import com.esri.arcgisruntime.security.AuthenticationManager;
import com.esri.arcgisruntime.security.DefaultAuthenticationChallengeHandler;

/**
 * Implements the sign in UX to ArcGIS portal accounts. Handles sign in to OAuth
 * and non-OAuth secured portals.
 */
public class SignInActivity extends Activity implements View.OnClickListener, TextWatcher {

	public static final String TAG = SignInActivity.class.getSimpleName();

	private static final String MSG_OBTAIN_CLIENT_ID = "You have to provide a client id in order to do OAuth sign in. You can obtain a client id by registering the application on https://developers.arcgis.com.";

	private static final String HTTPS = "https://";

	private static final String HTTP = "http://";
	private static final String TAG_PROGRESS_DIALOG = "TAG_PROGRESS_DIALOG";
	private EditText mPortalUrlEditText;
	private View mContinueButton;
	private String mPortalUrl;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(layout.sign_in_activity_portal_url_layout);

		mPortalUrlEditText = (EditText) findViewById(id.sign_in_activity_portal_url_edittext);
		mPortalUrlEditText.addTextChangedListener(this);

		mContinueButton = findViewById(id.sign_in_activity_continue_button);
		mContinueButton.setOnClickListener(this);
		mContinueButton.setEnabled(!mPortalUrlEditText.getText().toString().trim().isEmpty());

		View cancelButton = findViewById(id.sign_in_activity_cancel_button);
		cancelButton.setOnClickListener(this);
		mPortalUrl = mPortalUrlEditText.getText().toString().trim();

		// Set up an authentication handler
		// to be used when loading remote
		// resources or services.
		// TODO: Explain more about how this works.
		DefaultAuthenticationChallengeHandler authenticationChallengeHandler = new DefaultAuthenticationChallengeHandler(
				this);
		AuthenticationManager.setAuthenticationChallengeHandler(authenticationChallengeHandler);

	}

	@Override
	public void onClick(View view) {
		switch (view.getId()) {
			case id.sign_in_activity_continue_button :
				// determine what type of authentication is required to sign in
				// to the specified portal
				mPortalUrl = mPortalUrlEditText.getText().toString().trim();
				if (!mPortalUrl.startsWith(SignInActivity.HTTP)) {
					mPortalUrl = SignInActivity.HTTP + mPortalUrl;
				}
				final Portal portal = new Portal(mPortalUrl);
				portal.addDoneLoadingListener(new Runnable() {
					@Override
					public void run() {
						if (portal.getLoadStatus() == LoadStatus.LOADED) {
							PortalInfo portalInformation = portal.getPortalInfo();
							if (portalInformation.isSupportsOAuth()) {
								signInWithOAuth();
							}
						}
					}
				});
				portal.loadAsync();
				Log.i(SignInActivity.TAG, "Finished handling CONTINUE click in SignIn Activity");
				break;
			case id.sign_in_activity_cancel_button :
				finish();
				break;
		}
	}

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count, int after) {
	}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {
	}

	@Override
	public void afterTextChanged(Editable s) {
		if (s == null) {
			return;
		}

		// update the enabled state of the Continue button
		String url = s.toString().trim();
		mContinueButton.setEnabled(StringUtils.isNotEmpty(url));
	}

	/**
	 * Signs into the portal using OAuth2.
	 */
	private void signInWithOAuth() {

		if (mPortalUrl.startsWith(SignInActivity.HTTP)) {
			mPortalUrl = mPortalUrl.replace(SignInActivity.HTTP, SignInActivity.HTTPS);
		}

		// Are we already signed in?
		if (AccountManager.getInstance().getPortal() != null) {
			Log.i(SignInActivity.TAG, "Already signed into to Portal.");
			return;
		}
		Log.i(SignInActivity.TAG, "Signing in with OAuth...");
		final ProgressDialogFragment mProgressDialog;
		String clientId = getString(string.client_id);

		if (StringUtils.isEmpty(clientId)) {
			Toast.makeText(this, SignInActivity.MSG_OBTAIN_CLIENT_ID, Toast.LENGTH_SHORT).show();
			return;
		}
		// default handler

		final Portal portal = new Portal(mPortalUrl, true);
		mProgressDialog = ProgressDialogFragment.newInstance(getString(string.verifying_portal));
		mProgressDialog.show(getFragmentManager(), SignInActivity.TAG_PROGRESS_DIALOG);
		portal.addDoneLoadingListener(new Runnable() {
			@Override
			public void run() {
				if (portal.getLoadStatus() == LoadStatus.LOADED) {
					PortalInfo portalInformation = portal.getPortalInfo();
					String portalName = portalInformation.getPortalName(); // Returns
																			// 'ArcGIS
																			// Online'
					PortalUser user = portal.getPortalUser();
					Log.i(SignInActivity.TAG, portalName + " , user = " + user.getUserName());
					mProgressDialog.dismiss();
					AccountManager.getInstance().setPortal(portal);
					Log.i(SignInActivity.TAG, "Portal has been set in 'SignInActivity'");
					finish();
				}
			}
		});
		portal.loadAsync();

	}

}
