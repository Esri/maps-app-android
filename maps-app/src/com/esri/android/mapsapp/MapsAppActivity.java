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

    setContentView(R.layout.maps_app_activity);
    setupDrawer();
  }

  @Override
  protected void onResume() {
    super.onResume();

    setView();
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
      showMap(null);
    }
  }

  /**
   * Opens the content browser that shows the user's maps.
   */
  private void showContentBrowser() {
    if (mBrowseFragment == null) {
      mBrowseFragment = new ContentBrowserFragment();
    }

    FragmentTransaction transaction = getFragmentManager().beginTransaction();
    transaction.add(R.id.maps_app_activity_content_frame, mBrowseFragment, ContentBrowserFragment.TAG);
    transaction.commit();

    invalidateOptionsMenu(); // reload the options menu
  }

  /**
   * Opens the map represented by the specified portal item or if null, opens a default map.
   */
  public void showMap(PortalItemParcelable portalItem) {
    MapFragment mapFragment = MapFragment.newInstance(portalItem);

    FragmentTransaction transaction = getFragmentManager().beginTransaction();
    transaction.add(R.id.maps_app_activity_content_frame, mapFragment, MapFragment.TAG);
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
      item = new DrawerItem(getString(R.string.sign_out), new DrawerItem.OnClickListener() {

        @Override
        public void onClick() {
          signOut();
        }
      });
    } else {
      item = new DrawerItem(getString(R.string.sign_in), new DrawerItem.OnClickListener() {

        @Override
        public void onClick() {
          showSignInActivity();
        }
      });
    }
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
      View view = convertView;
      if (view == null) {
        view = getLayoutInflater().inflate(R.layout.drawer_item_layout, null);
      }

      DrawerItem drawerItem = (DrawerItem) getItem(position);
      TextView textView = (TextView) view;
      textView.setText(drawerItem.getTitle());

      return view;
    }
  }
}
