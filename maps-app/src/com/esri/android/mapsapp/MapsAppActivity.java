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

package com.esri.android.mapsapp;

import java.util.ArrayList;
import java.util.List;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.esri.android.mapsapp.account.AccountManager;
import com.esri.android.mapsapp.account.SignInActivity;

/**
 * Entry point into the Maps App.
 */
public class MapsAppActivity extends Activity {

  private static final String TAG = MapsAppActivity.class.getSimpleName();

  DrawerLayout mDrawerLayout;

  ContentBrowserFragment mBrowseFragment;

  /**
   * The FrameLayout that hosts the main content of the activity, such as the MapView
   */
  FrameLayout mContentFrame;

  /**
   * The list of menu items in the navigation drawer
   */
  private ListView mDrawerList;

  private final List<DrawerItem> mDrawerItems = new ArrayList<DrawerItem>();

  /**
   * Helper component that ties the action bar to the navigation drawer.
   */
  private ActionBarDrawerToggle mDrawerToggle;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    /**
     * Unlock basic license level by setting your client id here. The client id can be obtained on
     * https://developers.arcgis.com
     */
    // ArcGISRuntime.setClientId(getString(R.string.client_id));

    setContentView(R.layout.maps_app_activity);
    setupDrawer();

    setView();
  }

  @Override
  protected void onResume() {
    super.onResume();

    updateDrawer();
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // first check if the drawer toggle button was selected
    boolean handled = mDrawerToggle.onOptionsItemSelected(item);
    if (!handled) {
      handled = super.onOptionsItemSelected(item);
    }
    return handled;
  }

  /**
   * Initializes the navigation drawer.
   */
  private void setupDrawer() {
    mDrawerLayout = (DrawerLayout) findViewById(R.id.maps_app_activity_drawer_layout);
    mContentFrame = (FrameLayout) findViewById(R.id.maps_app_activity_content_frame);
    mDrawerList = (ListView) findViewById(R.id.maps_app_activity_left_drawer);

    // Set the list's click listener
    mDrawerList.setOnItemClickListener(new DrawerItemClickListener());

    // set a custom shadow that overlays the main content when the drawer opens
    mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
    // set up the drawer's list view with items and click listener

    ActionBar actionBar = getActionBar();
    actionBar.setDisplayHomeAsUpEnabled(true);
    actionBar.setHomeButtonEnabled(true);

    // ActionBarDrawerToggle ties together the the proper interactions
    // between the navigation drawer and the action bar app icon.
    mDrawerToggle = new ActionBarDrawerToggle(this, /* host Activity */
    mDrawerLayout, /* DrawerLayout object */
    R.drawable.ic_drawer, /* nav drawer image to replace 'Up' caret */
    R.string.navigation_drawer_open, /* "open drawer" description for accessibility */
    R.string.navigation_drawer_close /* "close drawer" description for accessibility */
    ) {
      @Override
      public void onDrawerClosed(View drawerView) {
        super.onDrawerClosed(drawerView);

        invalidateOptionsMenu(); // calls onPrepareOptionsMenu()
      }

      @Override
      public void onDrawerOpened(View drawerView) {
        super.onDrawerOpened(drawerView);

        invalidateOptionsMenu(); // calls onPrepareOptionsMenu()
      }
    };

    // Defer code dependent on restoration of previous instance state.
    mDrawerLayout.post(new Runnable() {
      @Override
      public void run() {
        mDrawerToggle.syncState();
      }
    });

    mDrawerLayout.setDrawerListener(mDrawerToggle);

    updateDrawer();
  }

  private void setView() {
    if (AccountManager.getInstance().isSignedIn()) {
      // we are signed in to a portal - show the content browser to choose a map
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
    Fragment browseFragment = fragmentManager.findFragmentByTag(ContentBrowserFragment.TAG);
    if (browseFragment == null) {
      browseFragment = new ContentBrowserFragment();
    }

    if (!browseFragment.isVisible()) {
      FragmentTransaction transaction = fragmentManager.beginTransaction();
      transaction.add(R.id.maps_app_activity_content_frame, browseFragment, ContentBrowserFragment.TAG);
      transaction.addToBackStack(null);
      transaction.commit();

      invalidateOptionsMenu(); // reload the options menu
    }

    mDrawerLayout.closeDrawers();
  }

  /**
   * Opens the map represented by the specified portal item or if null, opens a default map.
   */
  public void showMap(String portalItemId, String basemapPortalItemId) {

    // remove existing MapFragment explicitly, simply replacing it can cause the app to freeze when switching basemaps
    FragmentTransaction transaction = null;
    FragmentManager fragmentManager = getFragmentManager();
    Fragment currentMapFragment = fragmentManager.findFragmentByTag(MapFragment.TAG);
    if (currentMapFragment != null) {
      transaction = fragmentManager.beginTransaction();
      transaction.remove(currentMapFragment);
      transaction.commit();
    }

    MapFragment mapFragment = MapFragment.newInstance(portalItemId, basemapPortalItemId);

    transaction = fragmentManager.beginTransaction();
    transaction.replace(R.id.maps_app_activity_content_frame, mapFragment, MapFragment.TAG);
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

    DrawerItem item = null;
    if (AccountManager.getInstance().isSignedIn()) {

      // user info
      View userInfoView = getLayoutInflater().inflate(R.layout.drawer_item_user_layout, null);
      TextView textView = (TextView) userInfoView.findViewById(R.id.drawer_item_fullname_textview);
      textView.setText(AccountManager.getInstance().getPortalUser().getFullName());

      textView = (TextView) userInfoView.findViewById(R.id.drawer_item_username_textview);
      textView.setText(AccountManager.getInstance().getPortalUser().getUsername());

      item = new DrawerItem(userInfoView, null);
      mDrawerItems.add(item);

      // Sign Out
      TextView view = (TextView) getLayoutInflater().inflate(R.layout.drawer_item_layout, null);
      view.setText(getString(R.string.sign_out));

      item = new DrawerItem(view, new DrawerItem.OnClickListener() {

        @Override
        public void onClick() {
          signOut();
        }
      });
      mDrawerItems.add(item);

      // My Maps
      view = (TextView) getLayoutInflater().inflate(R.layout.drawer_item_layout, null);
      view.setText(getString(R.string.my_maps));

      item = new DrawerItem(view, new DrawerItem.OnClickListener() {

        @Override
        public void onClick() {
          showContentBrowser();
        }
      });
      mDrawerItems.add(item);
    } else {
      TextView view = (TextView) getLayoutInflater().inflate(R.layout.drawer_item_layout, null);
      view.setText(getString(R.string.sign_in));

      item = new DrawerItem(view, new DrawerItem.OnClickListener() {

        @Override
        public void onClick() {
          showSignInActivity();
        }
      });
      mDrawerItems.add(item);
    }

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
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
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
