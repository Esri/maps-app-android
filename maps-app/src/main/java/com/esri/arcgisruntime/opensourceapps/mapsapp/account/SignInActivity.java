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

package com.esri.arcgisruntime.opensourceapps.mapsapp.account;

import android.app.Activity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.Toast;

import com.esri.arcgisruntime.opensourceapps.mapsapp.R;
import com.esri.arcgisruntime.opensourceapps.mapsapp.dialogs.ProgressDialogFragment;
import com.esri.arcgisruntime.opensourceapps.mapsapp.util.StringUtils;
import com.esri.arcgisruntime.io.JsonEmbeddedException;
import com.esri.arcgisruntime.loadable.LoadStatus;
import com.esri.arcgisruntime.portal.Portal;
import com.esri.arcgisruntime.portal.PortalInfo;
import com.esri.arcgisruntime.security.AuthenticationManager;
import com.esri.arcgisruntime.security.DefaultAuthenticationChallengeHandler;
import com.esri.arcgisruntime.security.OAuthConfiguration;
import org.apache.http.client.HttpResponseException;

import java.net.MalformedURLException;

/**
 * Implements the sign in UX to ArcGIS portal accounts. Handles sign in to OAuth
 * and non-OAuth secured portals.
 */
public class SignInActivity extends Activity implements OnClickListener, TextWatcher {

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

		setContentView(R.layout.sign_in_activity_portal_url_layout);

		mPortalUrlEditText = findViewById(R.id.sign_in_activity_portal_url_edittext);
		mPortalUrlEditText.addTextChangedListener(this);

		mContinueButton = findViewById(R.id.sign_in_activity_continue_button);
		mContinueButton.setOnClickListener(this);
		mContinueButton.setEnabled(!mPortalUrlEditText.getText().toString().trim().isEmpty());

		View cancelButton = findViewById(R.id.sign_in_activity_cancel_button);
		cancelButton.setOnClickListener(this);
		mPortalUrl = mPortalUrlEditText.getText().toString().trim();

		// Set up an authentication handler
		// to be used when loading remote
		// resources or services.
		try {
			OAuthConfiguration oAuthConfiguration = new OAuthConfiguration("https://www.arcgis.com",getString( R.string.client_id), getString(
					R.string.redirect_uri));
			DefaultAuthenticationChallengeHandler authenticationChallengeHandler = new DefaultAuthenticationChallengeHandler(
					this);
			AuthenticationManager.setAuthenticationChallengeHandler(authenticationChallengeHandler);
			AuthenticationManager.addOAuthConfiguration(oAuthConfiguration);
		} catch (MalformedURLException e) {
			Log.i(TAG,"OAuth problem : " + e.getMessage());
			Toast.makeText(this, "The was a problem authenticating against the portal.", Toast.LENGTH_LONG).show();
		}

	}

	@Override
	public void onClick(View view) {
    final Activity activity = this;
		switch (view.getId()) {
			case R.id.sign_in_activity_continue_button :
				// determine what type of authentication is required to sign in
				// to the specified portal
				mPortalUrl = mPortalUrlEditText.getText().toString().trim();
        if (mPortalUrl.startsWith(HTTP)) {
          mPortalUrl = mPortalUrl.replace(HTTP, HTTPS);
        }else{
          mPortalUrl = HTTPS + mPortalUrl;
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
						}else{
              String errorMessage = portal.getLoadError().getMessage();
              String message = "Error accessing " + mPortalUrl + ". " + errorMessage +".";
              Integer errorCode = getErrorCode(portal.getLoadError());
              if (errorCode != null){
                message = message + " Error code " + errorCode.toString();
              }
              Toast.makeText(activity, message, Toast.LENGTH_LONG).show();
              Log.e(TAG, message);
            }
					}
				});
				portal.loadAsync();
				break;
			case R.id.sign_in_activity_cancel_button :
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

		if (mPortalUrl.startsWith(HTTP)) {
			mPortalUrl = mPortalUrl.replace(HTTP, HTTPS);
		}

		// Are we already signed in?
		if (AccountManager.getInstance().getPortal() != null) {
			Log.i(TAG, "Already signed into to Portal.");
			return;
		}
		final ProgressDialogFragment mProgressDialog;
		String clientId = getString(R.string.client_id);

		if (StringUtils.isEmpty(clientId)) {
			Toast.makeText(this, MSG_OBTAIN_CLIENT_ID, Toast.LENGTH_SHORT).show();
			return;
		}
		// default handler
		final Activity activity = this;
		final Portal portal = new Portal(mPortalUrl, true);
		mProgressDialog = ProgressDialogFragment.newInstance(getString(R.string.verifying_portal));
		mProgressDialog.show(getFragmentManager(), TAG_PROGRESS_DIALOG);
		portal.addDoneLoadingListener(new Runnable() {
			@Override
			public void run() {
				if (portal.getLoadStatus() == LoadStatus.LOADED) {
					mProgressDialog.dismiss();
					AccountManager.getInstance().setPortal(portal);
					finish();
				}else {
          mProgressDialog.dismiss();
          Throwable t = portal.getLoadError();
          Integer errorCode = getErrorCode(t);
          String errorMessage = portal.getLoadError().getMessage();
          String message = "Portal error: " + errorMessage;
          if (errorCode!=null){
            // Append error message with relevant error code.
            errorMessage = message + " Error code " + errorCode.toString();
            //403 thrown when user hits Cancel in Auth Popup window so we don't display a toast.
            if (errorCode !=403){
              Toast.makeText(activity, errorMessage, Toast.LENGTH_LONG).show();
            }
          }else{ // No error code
            Toast.makeText(activity, errorMessage, Toast.LENGTH_LONG).show();
          }
          Log.e(TAG, "Error signing in to portal: " + errorMessage );
					finish();
				}
			}
		});
		portal.loadAsync();

	}
  /**
   * Helper method that returns an error code from an http response
   * @param t Throwable
   * @return Integer representing error code
   */
  private Integer getErrorCode(Throwable t){
    Integer error = null;
    if (t instanceof JsonEmbeddedException){
      error = ((JsonEmbeddedException)t).getCode();
    } else if (t instanceof HttpResponseException){
      error= ((HttpResponseException) t).getStatusCode();
    }
    return error;
  }

}
