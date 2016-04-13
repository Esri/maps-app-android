/* Copyright 2016 Esri
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

package com.esri.android.mapsapp;

import android.Manifest;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.esri.android.mapsapp.account.AccountManager;
import com.esri.android.mapsapp.account.SignInActivity;
import com.esri.android.mapsapp.basemaps.BasemapsDialogFragment;
import com.esri.arcgisruntime.ArcGISRuntimeEnvironment;
import com.esri.arcgisruntime.mapping.Map;


import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * Entry point into the Maps App.
 */
public class MapsAppActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback  {

	public static DrawerLayout mDrawerLayout;
	
	ContentBrowserFragment mBrowseFragment;

	private final List<DrawerItem> mDrawerItems = new ArrayList<>();

	private static final int PERMISSION_REQUEST_LOCATION = 0;
	private View mLayout;

	private static final String TAG = MapsAppActivity.class.getSimpleName();
	/**
	 * The FrameLayout that hosts the main content of the activity, such as the
	 * MapView
	 */
    @InjectView(R.id.maps_app_activity_content_frame) FrameLayout mContentFrame;

	/**
	 * The list of menu items in the navigation drawer
	 */
	@InjectView(R.id.maps_app_activity_left_drawer) ListView mDrawerList;



	/**
	 * Helper component that ties the action bar to the navigation drawer.
	 */
//	private ActionBarDrawerToggle mDrawerToggle;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		/**
		 * Unlock basic license level by setting your client id here. The client
		 * id can be obtained on https://developers.arcgis.com
		 */
		ArcGISRuntimeEnvironment.setClientId(getString(R.string.client_id));

		setContentView(R.layout.maps_app_activity);
		mLayout = findViewById(R.id.maps_app_activity_content_frame);

		// We need a reference to the containing widget when
		// managing the Snackbar (used for notifying users about app permissions)
		mLayout = findViewById(R.id.maps_app_activity_content_frame);

		ButterKnife.inject(this);

		setupDrawer();


		// All devices running N and above require explicit permissions
		// checking when app is first run.

		requestLocationPermission();
	}

	/**
	 * Requests the {@link android.Manifest.permission#ACCESS_COARSE_LOCATION} permission.
	 * If an additional rationale should be displayed, the user has to launch the request from
	 * a SnackBar that includes additional information.
	 */

	private void requestLocationPermission() {
		// Permission has not been granted and must be requested.
		if (ActivityCompat.shouldShowRequestPermissionRationale(this,
				Manifest.permission.ACCESS_FINE_LOCATION)) {

			// Provide an additional rationale to the user if the permission was not granted
			// and the user would benefit from additional context for the use of the permission.
			// Display a SnackBar with a button to request the missing permission.
			Snackbar.make(mLayout, "Location access is required to display the map.",
					Snackbar.LENGTH_INDEFINITE).setAction("OK", new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					// Request the permission
					ActivityCompat.requestPermissions(MapsAppActivity.this,
							new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
							PERMISSION_REQUEST_LOCATION);
				}
			}).show();

		} else {
			Snackbar.make(mLayout,
					"Permission is not available. Requesting location permission.",
					Snackbar.LENGTH_SHORT).show();
			// Request the permission. The result will be received in onRequestPermissionResult().
			ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
					PERMISSION_REQUEST_LOCATION);
		}
	}


	/**
	 * Once the app has prompted for permission to access location, the response
	 * from the user is handled here.
	 * @param requestCode int: The request code passed into requestPermissions
	 * @param permissions String: The requested permission(s).
	 * @param grantResults int: The grant results for the permission(s).  This will be
	 *                     either PERMISSION_GRANTED or PERMISSION_DENIED
	 */
	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions,
										   int[] grantResults) {

		if (requestCode == PERMISSION_REQUEST_LOCATION) {
			// Request for camera permission.
			if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
				// Permission has been granted, go ahead and show the map
				setView();
			} else {
				// Permission request was denied.
				Snackbar.make(mLayout, "Location permission request was denied.",
						Snackbar.LENGTH_SHORT)
						.show();
			}
		}

		// END_INCLUDE(onRequestPermissionsResult)

	}
	@Override
	protected void onResume() {
		super.onResume();
		updateDrawer();
	}

	/**
	 * Initializes the navigation drawer.
	 */
	private void setupDrawer() {
		mDrawerLayout = (DrawerLayout) findViewById(R.id.maps_app_activity_drawer_layout);

		// Set the list's click listener
		mDrawerList.setOnItemClickListener(new DrawerItemClickListener());

		// set a custom shadow that overlays the main content when the drawer
		// opens
		mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow,
				GravityCompat.START);

		updateDrawer();
	}

	private void setView() {
		if (AccountManager.getInstance().isSignedIn()) {
			// we are signed in to a portal - show the content browser to choose
			// a map
			showContentBrowser();
		} else {
			// show the default map
			showMap(null, null);
		}
	}

	/**
	 * Opens the content browser that shows the user's maps.
	 */
	private void showContentBrowser() {
		FragmentManager fragmentManager = getFragmentManager();
		Fragment browseFragment = fragmentManager
				.findFragmentByTag(ContentBrowserFragment.TAG);
		if (browseFragment == null) {
			browseFragment = new ContentBrowserFragment();
		}

		if (!browseFragment.isVisible()) {
			FragmentTransaction transaction = fragmentManager
					.beginTransaction();
			transaction.add(R.id.maps_app_activity_content_frame,
					browseFragment, ContentBrowserFragment.TAG);
			transaction.addToBackStack(null);
			transaction.commit();

			invalidateOptionsMenu(); // reload the options menu
		}

		mDrawerLayout.closeDrawers();
	}

	/**
	 * Opens the map represented by the specified portal item or if null, opens
	 * a default map.
	 */
	public void showMap(String portalItemId, String basemapPortalItemId) {
		Log.i(TAG, "Show map called...");
		// remove existing MapFragment explicitly, simply replacing it can cause
		// the app to freeze when switching basemaps
		FragmentTransaction transaction;
		FragmentManager fragmentManager = getFragmentManager();
		Fragment currentMapFragment = fragmentManager
				.findFragmentByTag(MapFragment.TAG);
		if (currentMapFragment != null) {
			transaction = fragmentManager.beginTransaction();
			transaction.remove(currentMapFragment);
			transaction.commit();
		}

		MapFragment mapFragment = MapFragment.newInstance(portalItemId,
				basemapPortalItemId);

		transaction = fragmentManager.beginTransaction();
		transaction.replace(R.id.maps_app_activity_content_frame, mapFragment,
				MapFragment.TAG);
		transaction.addToBackStack(null);
		transaction.commit();

		invalidateOptionsMenu(); // reload the options menu
	}

	private void showSignInActivity() {
		Intent intent = new Intent(this, SignInActivity.class);
		startActivity(intent);

		mDrawerLayout.closeDrawers();
	}

	/**
	 * Resets the app to "signed out" state.
	 */
	private void signOut() {
		AccountManager.getInstance().setPortal(null);

		setView();

		updateDrawer();
		mDrawerLayout.closeDrawers();
	}

	/**
	 * Updates the navigation drawer items.
	 */
	private void updateDrawer() {
		mDrawerItems.clear();


		DrawerItem item;
		if (AccountManager.getInstance().isSignedIn()) {

			// user info
			LinearLayout userInfoView = (LinearLayout) getLayoutInflater()
					.inflate(R.layout.drawer_item_user_layout, null);
			TextView textView = (TextView) userInfoView
					.findViewById(R.id.drawer_item_fullname_textview);
			textView.setText(AccountManager.getInstance().getPortalUser()
					.getFullName());

			textView = (TextView) userInfoView
					.findViewById(R.id.drawer_item_username_textview);
			textView.setText(AccountManager.getInstance().getPortalUser().getUserName());

			item = new DrawerItem(userInfoView, null);
			mDrawerItems.add(item);

			// Sign Out
			
			LinearLayout view_signOut = (LinearLayout) getLayoutInflater()
					.inflate(R.layout.drawer_item_layout, null);
			TextView text_drawer_signOut = (TextView) view_signOut
					.findViewById(R.id.drawer_item_textview);
			ImageView icon_drawer_signOut = (ImageView) view_signOut
					.findViewById(R.id.drawer_item_icon);

			text_drawer_signOut.setText(getString(R.string.sign_out));
			icon_drawer_signOut.setImageResource(R.drawable.ic_profile);
			item = new DrawerItem(view_signOut,
					new DrawerItem.OnClickListener() {

						@Override
						public void onClick() {
							signOut();
						}
					});
			mDrawerItems.add(item);

			// My Maps
			LinearLayout view_myMaps = (LinearLayout) getLayoutInflater()
					.inflate(R.layout.drawer_item_layout, null);
			TextView text_drawer_myMaps = (TextView) view_myMaps
					.findViewById(R.id.drawer_item_textview);
			ImageView icon_drawer_myMaps = (ImageView) view_myMaps
					.findViewById(R.id.drawer_item_icon);

			text_drawer_myMaps.setText(getString(R.string.my_maps));
			icon_drawer_myMaps.setImageResource(R.drawable.ic_map32);
			item = new DrawerItem(view_myMaps,
					new DrawerItem.OnClickListener() {

						@Override
						public void onClick() {
							showContentBrowser();
						}
					});
			mDrawerItems.add(item);
		} else {

			// Adding the Sign In item in the drawer
			LinearLayout view_signIn = (LinearLayout) getLayoutInflater()
					.inflate(R.layout.drawer_item_layout, null);
			TextView text_drawer_signIn = (TextView) view_signIn
					.findViewById(R.id.drawer_item_textview);
			ImageView icon_drawer_signIn = (ImageView) view_signIn
					.findViewById(R.id.drawer_item_icon);

			text_drawer_signIn.setText(getString(R.string.sign_in));
			icon_drawer_signIn.setImageResource(R.drawable.ic_profile);
			item = new DrawerItem(view_signIn,
					new DrawerItem.OnClickListener() {

						@Override
						public void onClick() {
							showSignInActivity();
						}
					});
			mDrawerItems.add(item);
		}

		// Adding the basemap item in the drawer
		LinearLayout view_basemap = (LinearLayout) getLayoutInflater().inflate(R.layout.drawer_item_layout, null);
		TextView text_drawer_basemap = (TextView) view_basemap.findViewById(R.id.drawer_item_textview);
		ImageView icon_drawer_basemap = (ImageView) view_basemap.findViewById(R.id.drawer_item_icon);
		text_drawer_basemap.setText(getString(R.string.menu_basemaps));
		icon_drawer_basemap.setImageResource(R.drawable.action_basemaps);
		item = new DrawerItem(view_basemap, new DrawerItem.OnClickListener() {

			@Override
			public void onClick() {
				// Show BasemapsDialogFragment to offer a choice if basemaps.
				// This calls back to onBasemapChanged() if one is selected.
				BasemapsDialogFragment basemapsFrag = new BasemapsDialogFragment();
				basemapsFrag.setBasemapsDialogListener(new BasemapsDialogFragment.BasemapsDialogListener() {

					@Override
					public void onBasemapChanged(String itemId) {
						Log.i(TAG, "Basemap changed and noted from DrawerItem.");
						showMap(null,itemId);
					}
				});
				basemapsFrag.show(getFragmentManager(), null);
				mDrawerLayout.closeDrawers();
			}

		});

		mDrawerItems.add(item);

		BaseAdapter adapter = (BaseAdapter) mDrawerList.getAdapter();
		if (adapter == null) {
			adapter = new DrawerItemListAdapter();
			mDrawerList.setAdapter(adapter);
		} else {
			adapter.notifyDataSetChanged();
		}
	}

	/**
	 * Handles selection of items in the navigation drawer.
	 */
	private class DrawerItemClickListener implements OnItemClickListener {

		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			mDrawerItems.get(position).onClicked();
		}
	}

	/**
	 * Populates the navigation drawer list with items.
	 */
	private class DrawerItemListAdapter extends BaseAdapter {

		@Override
		public int getCount() {
			return mDrawerItems.size();
		}

		@Override
		public Object getItem(int position) {
			return mDrawerItems.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			DrawerItem drawerItem = (DrawerItem) getItem(position);
			return drawerItem.getView();
		}
	}

}
