/*
 * COPYRIGHT 1995-2014 ESRI
 *
 * TRADE SECRETS: ESRI PROPRIETARY AND CONFIDENTIAL
 * Unpublished material - all rights reserved under the
 * Copyright Laws of the United States.
 *
 * For additional information, contact:
 * Environmental Systems Research Institute, Inc.
 * Attn: Contracts Dept
 * 380 New York Street
 * Redlands, California, USA 92373
 *
 * email: contracts@esri.com
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
    mContinueButton.setEnabled(false);

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
  
  private void signInWithForm() {
    // TODO Auto-generated method stub

  }

  private void signInWithOAuth() {

    if (mPortalUrl.startsWith(HTTP)) {
      mPortalUrl = mPortalUrl.replace(HTTP, HTTPS);
    }

    // create an OAuthView and show it
    OAuthView oAuthView = new OAuthView(this, mPortalUrl,
        getString(R.string.app_oauth_id),
        OAUTH_EXPIRATION_NEVER, new CallbackListener<UserCredentials>() {

          @Override
          public void onCallback(final UserCredentials credentials) {
            if (credentials != null) {
              Portal portal = new Portal(mPortalUrl, credentials);

              try {
                // fetch the portal info and user details, they will be cached in the Portal instance
                portal.fetchPortalInfo();
                portal.fetchUser();
              } catch (Exception e) {
                onError(e);
              }

              // hold on to the initialized portal for later use
              AccountManager.getInstance().setPortal(portal);

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

    private static final int TYPE_FORM = 1;

    private ProgressDialogFragment mProgressDialog;

    private boolean mIsOAuth;

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
        mPortalUrl = mPortalUrlEditText.getText().toString();
        if (!mPortalUrl.startsWith(HTTP)) {
          mPortalUrl = new StringBuilder(HTTP).append(mPortalUrl).toString();
        }
        
        Log.d(TAG, mPortalUrl);

        Portal portal = new Portal(mPortalUrl, null);
        PortalInfo portalInfo = portal.fetchPortalInfo();

        if (portalInfo != null) {
          authType = portalInfo.isSupportsOAuth() ? TYPE_OAUTH : TYPE_FORM;
        }
      } catch (EsriSecurityException ese){
        // Enterprise Windows auth throws this exception - assume it's not OAuth.
        if (ese.getCode() == EsriSecurityException.AUTHENTICATION_FAILED) {
          authType = TYPE_FORM;
        }
      } catch (Exception e) {
        authType = TYPE_UNDEFINED;
      }

      return Integer.valueOf(authType);
    }

    @Override
    protected void onPostExecute(Integer result) {
      super.onPostExecute(result);

      mProgressDialog.dismiss();

      switch (result.intValue()) {
        case TYPE_OAUTH:
          signInWithOAuth();
          break;
        case TYPE_FORM:
          signInWithForm();
          break;
        default:
          // auth type could not be determined - just abort sign in
          finish();
      }
    }
  }
}
