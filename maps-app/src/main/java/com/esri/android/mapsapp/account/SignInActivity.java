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
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.Toast;

import com.esri.android.mapsapp.R;
import com.esri.android.mapsapp.dialogs.ProgressDialogFragment;
import com.esri.android.mapsapp.util.StringUtils;
import com.esri.android.oauth.OAuthView;
import com.esri.android.runtime.ArcGISRuntime;
import com.esri.core.io.EsriSecurityException;
import com.esri.core.io.UserCredentials;
import com.esri.core.map.CallbackListener;
import com.esri.core.portal.Portal;
import com.esri.core.portal.PortalInfo;

/**
 * Implements the sign in UX to ArcGIS portal accounts. Handles sign in to OAuth and non-OAuth secured portals.
 */
public class SignInActivity extends Activity implements OnClickListener, TextWatcher {

  public static final String TAG = SignInActivity.class.getSimpleName();
  
  private static final String MSG_OBTAIN_CLIENT_ID = "You have to provide a client id in order to do OAuth sign in. You can obtain a client id by registering the application on https://developers.arcgis.com.";

  private static final String HTTPS = "https://";

  private static final String HTTP = "http://";

  private static final int OAUTH_EXPIRATION_NEVER = -1;

  private EditText mPortalUrlEditText;

  private View mContinueButton;

  private String mPortalUrl;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.sign_in_activity_portal_url_layout);

    mPortalUrlEditText = (EditText) findViewById(R.id.sign_in_activity_portal_url_edittext);
    mPortalUrlEditText.addTextChangedListener(this);

    mContinueButton = findViewById(R.id.sign_in_activity_continue_button);
    mContinueButton.setOnClickListener(this);
    mContinueButton.setEnabled(!mPortalUrlEditText.getText().toString().trim().isEmpty());

    View cancelButton = findViewById(R.id.sign_in_activity_cancel_button);
    cancelButton.setOnClickListener(this);
  }

  @Override
  public void onClick(View view) {
    switch (view.getId()) {
      case R.id.sign_in_activity_continue_button:
        // determine what type of authentication is required to sign in to the specified portal
        new FetchAuthenticationTypeTask().execute();
        break;
      case R.id.sign_in_activity_cancel_button:
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
   * Signs into the portal using the generateToken REST endpoint.
   */
  private void signInWithGenerateToken() {
    Toast.makeText(this, "GenerateToken-based sign in not implemented yet", Toast.LENGTH_SHORT).show();
  }

  /**
   * Signs into the portal using OAuth2.
   */
  private void signInWithOAuth() {

    if (mPortalUrl.startsWith(HTTP)) {
      mPortalUrl = mPortalUrl.replace(HTTP, HTTPS);
    }

    String clientId = getString(R.string.client_id);
    if (StringUtils.isEmpty(clientId)) {
      Toast.makeText(this, MSG_OBTAIN_CLIENT_ID, Toast.LENGTH_SHORT).show();
      return;
    }

    // create an OAuthView and show it
    OAuthView oAuthView = new OAuthView(this, mPortalUrl, clientId, OAUTH_EXPIRATION_NEVER,
        new CallbackListener<UserCredentials>() {

          @Override
          public void onCallback(final UserCredentials credentials) {
            if (credentials != null) {
              Portal portal = new Portal(mPortalUrl, credentials);
              PortalInfo portalInfo = null;

              try {
                // fetch the portal info and user details, they will be cached in the Portal instance
                portalInfo = portal.fetchPortalInfo();
                portal.fetchUser();
              } catch (Exception e) {
                onError(e);
              }

              // hold on to the initialized portal for later use
              AccountManager.getInstance().setPortal(portal);

              // enable standard license level
              if (portalInfo != null) {
                ArcGISRuntime.License.setLicense(portalInfo.getLicenseInfo());
              }

              // we are done signing in
              finish();
            }
          }

          @Override
          public void onError(Throwable e) {
            Toast.makeText(SignInActivity.this, getString(R.string.failed_sign_in), Toast.LENGTH_SHORT).show();
            finish();
          }
        });

    setContentView(oAuthView);
  }

  /**
   * Fetches the PortalInfo asynchronously and determines the portal's authentication type.
   */
  private class FetchAuthenticationTypeTask extends AsyncTask<Void, Void, Integer> {

    private static final String TAG_PROGRESS_DIALOG = "TAG_PROGRESS_DIALOG";

    private static final int TYPE_UNDEFINED = -1;

    private static final int TYPE_OAUTH = 0;

    private static final int TYPE_GENERATE_TOKEN = 1;

    private ProgressDialogFragment mProgressDialog;

    @Override
    protected void onPreExecute() {
      super.onPreExecute();
      mProgressDialog = ProgressDialogFragment.newInstance(getString(R.string.verifying_portal));
      mProgressDialog.show(getFragmentManager(), TAG_PROGRESS_DIALOG);
    }

    @Override
    protected Integer doInBackground(Void... params) {
      int authType = TYPE_UNDEFINED;
      try {
        mPortalUrl = mPortalUrlEditText.getText().toString().trim();
        if (!mPortalUrl.startsWith(HTTP)) {
          mPortalUrl = HTTP + mPortalUrl;
        }

        Log.d(TAG, mPortalUrl);

        Portal portal = new Portal(mPortalUrl, null);
        PortalInfo portalInfo = portal.fetchPortalInfo();

        if (portalInfo != null) {
          authType = portalInfo.isSupportsOAuth() ? TYPE_OAUTH : TYPE_GENERATE_TOKEN;
        }
      } catch (EsriSecurityException ese) {
        // Enterprise Windows auth throws this exception - assume it's not OAuth.
        if (ese.getCode() == EsriSecurityException.AUTHENTICATION_FAILED) {
          authType = TYPE_GENERATE_TOKEN;
        }
      } catch (Exception e) {
        authType = TYPE_UNDEFINED;
      }

      return authType;
    }

    @Override
    protected void onPostExecute(Integer result) {
      super.onPostExecute(result);

      mProgressDialog.dismiss();

      switch (result) {
        case TYPE_OAUTH:
          signInWithOAuth();
          break;
        case TYPE_GENERATE_TOKEN:
          signInWithGenerateToken();
          break;
        default:
          // auth type could not be determined - just abort sign in
          finish();
      }
    }
  }
}
