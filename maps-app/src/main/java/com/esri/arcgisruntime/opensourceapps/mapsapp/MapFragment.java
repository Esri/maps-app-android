/* Copyright 1995-2016 Esri
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

package com.esri.arcgisruntime.opensourceapps.mapsapp;

import java.net.MalformedURLException;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.database.MatrixCursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.provider.BaseColumns;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.MarginLayoutParams;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.SearchView.OnQueryTextListener;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.esri.arcgisruntime.opensourceapps.mapsapp.account.AccountManager;
import com.esri.arcgisruntime.opensourceapps.mapsapp.basemaps.BasemapsDialogFragment.BasemapsDialogListener;
import com.esri.arcgisruntime.opensourceapps.mapsapp.dialogs.ProgressDialogFragment;
import com.esri.arcgisruntime.opensourceapps.mapsapp.location.DirectionsDialogFragment;
import com.esri.arcgisruntime.opensourceapps.mapsapp.location.DirectionsDialogFragment.DirectionsDialogListener;
import com.esri.arcgisruntime.opensourceapps.mapsapp.location.RoutingDialogFragment;
import com.esri.arcgisruntime.opensourceapps.mapsapp.location.RoutingDialogFragment.RoutingDialogListener;
import com.esri.arcgisruntime.opensourceapps.mapsapp.tools.Compass;
import com.esri.arcgisruntime.opensourceapps.mapsapp.util.TaskExecutor;

import com.esri.arcgisruntime.UnitSystem;
import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.geometry.Envelope;
import com.esri.arcgisruntime.geometry.Geometry;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.SpatialReference;
import com.esri.arcgisruntime.loadable.LoadStatus;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.Viewpoint;
import com.esri.arcgisruntime.mapping.view.DefaultMapViewOnTouchListener;
import com.esri.arcgisruntime.mapping.view.Graphic;
import com.esri.arcgisruntime.mapping.view.GraphicsOverlay;
import com.esri.arcgisruntime.mapping.view.LocationDisplay;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.esri.arcgisruntime.mapping.view.ViewpointChangedEvent;
import com.esri.arcgisruntime.mapping.view.ViewpointChangedListener;
import com.esri.arcgisruntime.mapping.view.WrapAroundMode;
import com.esri.arcgisruntime.portal.Portal;
import com.esri.arcgisruntime.portal.PortalItem;
import com.esri.arcgisruntime.security.AuthenticationManager;
import com.esri.arcgisruntime.security.DefaultAuthenticationChallengeHandler;
import com.esri.arcgisruntime.security.OAuthConfiguration;
import com.esri.arcgisruntime.symbology.PictureMarkerSymbol;
import com.esri.arcgisruntime.symbology.SimpleLineSymbol;
import com.esri.arcgisruntime.tasks.geocode.GeocodeParameters;
import com.esri.arcgisruntime.tasks.geocode.GeocodeResult;
import com.esri.arcgisruntime.tasks.geocode.LocatorTask;
import com.esri.arcgisruntime.tasks.geocode.ReverseGeocodeParameters;
import com.esri.arcgisruntime.tasks.geocode.SuggestParameters;
import com.esri.arcgisruntime.tasks.geocode.SuggestResult;
import com.esri.arcgisruntime.tasks.networkanalysis.*;

/**
 * Implements the view that shows the map.
 */

public class MapFragment extends Fragment implements BasemapsDialogListener,
    RoutingDialogListener, OnCancelListener
{

	public static final String TAG = MapFragment.class.getSimpleName();

	private static final String KEY_PORTAL_ITEM_ID = "KEY_PORTAL_ITEM_ID";

	private static final String KEY_BASEMAP_ITEM_ID = "KEY_BASEMAP_ITEM_ID";

	private static final String KEY_IS_LOCATION_TRACKING = "IsLocationTracking";

	private static final String TAG_ROUTE_SEARCH_PROGRESS_DIALOG = "TAG_ROUTE_SEARCH_PROGRESS_DIALOG";

	private static final String TAG_REVERSE_GEOCODING_PROGRESS_DIALOG = "TAG_REVERSE_GEOCODING_PROGRESS_DIALOG";

	private static final String COLUMN_NAME_ADDRESS = "address";

	private static final String COLUMN_NAME_X = "x";

	private static final String COLUMN_NAME_Y = "y";

	private static final int REQUEST_CODE_PROGRESS_DIALOG = 1;

	public static final String SEARCH_HINT = "Search";

	private static final String FIND_PLACE = "Find";

	private static final String SUGGEST_PLACE = "Suggest";

	private static final String ROUTE = "Route";

	private static final String REVERSE_GEOCODE = "Reverse Geocode";

    private static final int TOP_MARGIN_SEARCH = 55;

  // The circle area specified by search_radius and input lat/lon serves
	// searching purpose.
	// It is also used to construct the extent which map zooms to after the
	// first
	// GPS fix is retrieved.
	public MapView mMapView;
	private LayoutParams mlayoutParams;
	// Margins parameters for search view
	private static List<SuggestResult> mSuggestionsList;
	Compass mCompass;
	ViewGroup.LayoutParams mCompassFrameParams;
	ImageButton mNavButton;
	DrawerLayout mDrawerLayout;
	ListView mDrawerList;
	private String mPortalItemId;
	private String mBasemapPortalItemId;
	private FrameLayout mMapContainer;
	private SearchView mSearchview;
	// GPS location tracking
	private LocationDisplay mLocationDisplay;
	private boolean mIsInCompassMode;
	private Point mLocation = null;
	// Graphics layer to show geocode and reverse geocode results
	private GraphicsOverlay mLocationLayer;
	private Point mFoundLocation;
	private String mLocationLayerPointString;
	// Graphics layer to show routes
	private GraphicsOverlay mRouteLayer;
	private List<DirectionManeuver> mRoutingDirections;
	private MatrixCursor mSuggestionCursor;
	private MotionEvent mLongPressEvent;
	private ProgressDialogFragment mProgressDialog;
	private View mSearchBox;
	private LocatorTask mLocator;
	private View mSearchResult;
	private LayoutInflater mInflater;
	private String mStartLocationName, mEndLocationName;
	private SuggestParameters suggestParams;
	private GeocodeParameters mGeocodeParams;
	private ReverseGeocodeParameters mReverseGeocodeParams;
	private RouteTask mRouteTask;
	private ArcGISMap mMap;
	private Basemap mBasemap;
	private GeocodeResult mGeocodedLocation;

	public MapFragment() {
		// make MapFragment ctor private - use newInstance() instead
	}

	public static MapFragment newInstance(String portalItemId, String basemapPortalItemId) {
		MapFragment mapFragment = new MapFragment();

		Bundle args = new Bundle();
		args.putString(KEY_PORTAL_ITEM_ID, portalItemId);
		args.putString(KEY_BASEMAP_ITEM_ID, basemapPortalItemId);

		mapFragment.setArguments(args);
		return mapFragment;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Restore any previous state
		Bundle args = savedInstanceState != null ? savedInstanceState : getArguments();
		if (args != null) {
			mIsInCompassMode = args.getBoolean(KEY_IS_LOCATION_TRACKING);
			mPortalItemId = args.getString(KEY_PORTAL_ITEM_ID);
			mBasemapPortalItemId = args.getString(KEY_BASEMAP_ITEM_ID);
		}

		try {
			OAuthConfiguration oAuthConfiguration = new OAuthConfiguration("https://www.arcgis.com",getString( R.string.client_id), getString(
					R.string.redirect_uri));
			DefaultAuthenticationChallengeHandler authenticationChallengeHandler = new DefaultAuthenticationChallengeHandler(
					getActivity());
			AuthenticationManager.setAuthenticationChallengeHandler(authenticationChallengeHandler);
			AuthenticationManager.addOAuthConfiguration(oAuthConfiguration);
		} catch (MalformedURLException e) {
			Log.i(TAG,"OAuth problem : " + e.getMessage());
			Toast.makeText(getActivity(), "The was a problem authenticating against the portal.", Toast.LENGTH_LONG).show();
		}

	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		mMapContainer = (FrameLayout) inflater.inflate(R.layout.map_fragment_layout, null);

		if (mPortalItemId != null) {
			// load the WebMap
			loadWebMapIntoMapView(mPortalItemId, mBasemapPortalItemId, AccountManager.getInstance().getPortal());
		} else {
			if (mBasemapPortalItemId != null) {
				// show a map with the basemap represented by
				// mBasemapPortalItemId

				loadWebMapIntoMapView(mBasemapPortalItemId, null,
						AccountManager.getInstance().getAGOLPortal());

			} else {
				String defaultBaseMapURL = getString(R.string.default_basemap_url);
				mBasemap = new Basemap(defaultBaseMapURL);
				mMap = new ArcGISMap(mBasemap);

				mMapView = mMapContainer.findViewById(R.id.map);

				mMapView.setMap(mMap);

				setMapView(mMapView);

				// Set up click listener on floating action button
				setClickListenerForFloatingActionButton(mMapView);

				// add graphics layer
				addGraphicLayers();

				// synchronize the compass icon as the map changes
				mMapView.addViewpointChangedListener(new ViewpointChangedListener() {
					@Override
					public void viewpointChanged(ViewpointChangedEvent visibleAreaChangedEvent) {
						mCompass.setRotationAngle(((MapView) visibleAreaChangedEvent.getSource()).getMapRotation());
					}
				});
			}
		}
		return mMapContainer;
	}

	/**
	 * The compass and Location FAB should behave as described below.
	 *
	 * Compass:
	 *
	 * Whenever the map is not orientated North (non-zero bearing) the compass appears
	 * When the compass is clicked, the map orients back to north (zero bearing),
	 * the default orientation and the compass fades away, or after a short duration disappears.
	 *
	 * Location FAB:
	 *
	 * Tapping on location button should switch between NAVIGATION & OFF (default)
	 * When in NAVIGATION mode orientation should be with respect to device. (In COMPASS mode).  It
	 * follows the direction the device travels in.
	 * When in 'OFF' mode orientation should return to North.
	 * @param mapView
	 */
	private void setClickListenerForFloatingActionButton(final MapView mapView) {
		final FloatingActionButton fab = mMapContainer.findViewById(R.id.fab);
		fab.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {

				mLocationDisplay = mapView.getLocationDisplay();
				Log.i("AUTOPAN", mLocationDisplay.getAutoPanMode().name());
				// Toggle AutoPanMode
				if (!mIsInCompassMode) {
					fab.setImageResource(R.drawable.ic_action_compass_mode);
					mLocationDisplay.setAutoPanMode(LocationDisplay.AutoPanMode.NAVIGATION);
					mIsInCompassMode = true;
					mCompass.setVisibility(View.GONE);
				} else { // Turn pan mode off 
					fab.setImageResource(android.R.drawable.ic_menu_mylocation);
					mLocationDisplay.setAutoPanMode(LocationDisplay.AutoPanMode.RECENTER);
					mCompass.setVisibility(View.GONE);
					mIsInCompassMode = false;
				}
				Log.i(MapFragment.TAG, "Auto pan mode is " + mLocationDisplay.getAutoPanMode().name());
				Log.i(MapFragment.TAG,"Compass rotation is " + mCompass.getRotation());
				Log.i(MapFragment.TAG, "Map rotation is " + mMapView.getMapRotation());
			}
    });
  }

	@Override
	public void onPause() {
		super.onPause();

		// Pause the MapView and stop the LocationDisplayManager to save battery
		if (mMapView != null) {
			 if (mIsInCompassMode) {
				 mMapView.getLocationDisplay().stop();
			 }

			mMapView.pause();
		}

	}

	@Override
	public void onResume() {
		super.onResume();
		// Start the MapView and LocationDisplayManager running again
		if (mMapView != null) {
			mMapView.resume();
			 if (mIsInCompassMode) {
				 mMapView.getLocationDisplay().startAsync();
			 }
		}
	}
	@Override
	public void onDestroyView(){
		super.onDestroyView();

		mMapView.dispose();
		mMapView = null;
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		outState.putBoolean(KEY_IS_LOCATION_TRACKING, mIsInCompassMode);
		outState.putString(KEY_PORTAL_ITEM_ID, mPortalItemId);
		outState.putString(KEY_BASEMAP_ITEM_ID, mBasemapPortalItemId);
	}
	/**
	 * Loads a WebMap and creates a MapView from it which is set into the
	 * fragment's layout.
	 *
	 * @param portalItemId
	 *            The portal item id that represents the web map.
	 * @param basemapPortalItemId
	 *            The portal item id that represents the basemap.
	 * @throws Exception
	 *             if WebMap loading failed.
	 */
	private void loadWebMapIntoMapView(final String portalItemId, final String basemapPortalItemId,
			final Portal portal) {

		TaskExecutor.getInstance().getThreadPool().submit(new Callable<Void>() {

			@Override
			public Void call() throws Exception {

				// load a WebMap instance from the portal item
				PortalItem portalItem = new PortalItem(portal, portalItemId);
				final ArcGISMap webmap = new ArcGISMap(portalItem);

				// load the WebMap that represents the basemap if one was
				// specified

				if (basemapPortalItemId != null && !basemapPortalItemId.isEmpty()) {
					PortalItem webPortalItem = new PortalItem(portal, basemapPortalItemId);
					Basemap basemapWebMap = new Basemap(webPortalItem);
					webmap.setBasemap(basemapWebMap);
				}

				getActivity().runOnUiThread(new Runnable() {
					@Override
					public void run() {
						final MapView mapView = mMapContainer.findViewById(R.id.map);
						mapView.setMap(webmap);
						setMapView(mapView);
						setClickListenerForFloatingActionButton(mapView);
						addGraphicLayers();

					}
				});

				return null;
			}
		});
	}

	/**
	 * Takes a MapView that has already been instantiated to show a WebMap,
	 * completes its setup by setting various listeners and attributes, and sets
	 * it as the activity's content view.
	 *
	 * @param mapView
	 */
	private void setMapView(final MapView mapView) {

		mMapView = mapView;
		mMapView.setWrapAroundMode(WrapAroundMode.ENABLE_WHEN_SUPPORTED);

		// Creating an inflater
		mInflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		// Setting up the layout params for the searchview and searchresult
		// layout
		mlayoutParams = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT,
				Gravity.LEFT | Gravity.TOP);
		int LEFT_MARGIN_SEARCH = 15;
		int RIGHT_MARGIN_SEARCH = 15;
		int BOTTOM_MARGIN_SEARCH = 0;

		mlayoutParams.setMargins(LEFT_MARGIN_SEARCH, TOP_MARGIN_SEARCH, RIGHT_MARGIN_SEARCH, BOTTOM_MARGIN_SEARCH);

		// Displaying the searchbox layout
		showSearchBoxLayout();

		// Show current location
		mLocationDisplay = mapView.getLocationDisplay();
		mLocationDisplay.setAutoPanMode(LocationDisplay.AutoPanMode.RECENTER);
		mLocationDisplay.startAsync();

		mLocationDisplay.setInitialZoomScale(50000);

		// Handle any location changes
		mLocationDisplay.addLocationChangedListener(new LocationListener());


		// Setup use of magnifier on a long press on the map
		mMapView.setMagnifierEnabled(true);
		mLongPressEvent = null;

		// Setup OnTouchListener to detect and act on long-press
		mMapView.setOnTouchListener(new MapTouchListener(getActivity().getApplicationContext(), mMapView));

		mLocator = new LocatorTask(getString(R.string.geocode_url));
	}

	/**
	 * Adds the compass as per the height of the layout
	 *
	 * @param height
	 */
	private void addCompass(int height) {

		mMapContainer.removeView(mCompass);

		// Create the Compass custom view, and add it onto
		// the MapView.
		mCompass = new Compass(mMapView.getContext());
		mCompass.setAlpha(1f);
		mCompass.setRotationAngle(45);
		int HEIGHT = 240;
		int WIDTH = 240;
		mCompassFrameParams = new LayoutParams(WIDTH, HEIGHT, Gravity.RIGHT);

		int TOP_MARGIN_COMPASS = TOP_MARGIN_SEARCH + height + 45;

		int LEFT_MARGIN_COMPASS = 0;
		int BOTTOM_MARGIN_COMPASS = 0;
		int RIGHT_MARGIN_COMPASS = 0;
		((MarginLayoutParams) mCompassFrameParams).setMargins(LEFT_MARGIN_COMPASS, TOP_MARGIN_COMPASS,
				RIGHT_MARGIN_COMPASS, BOTTOM_MARGIN_COMPASS);

		mCompass.setLayoutParams(mCompassFrameParams);

		mCompass.setVisibility(View.GONE);

		mCompass.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				mCompass.setVisibility(View.GONE);
				mMapView.setViewpointRotationAsync(0);
			}
		});

		// Add the compass on the map
		mMapContainer.addView(mCompass);

	}

	/**
	 *
	 * Displays the Dialog Fragment which allows users to route
	 */
	private void showRoutingDialogFragment() {

		// Show RoutingDialogFragment to get routing start and end points.
		// This calls back to onGetRoute() to do the routing.
		RoutingDialogFragment routingFrag = new RoutingDialogFragment();
		routingFrag.setRoutingDialogListener(new RoutingDialogListener() {
			@Override
			public boolean onGetRoute(String startPoint, String endPoint) {
				getRoute(startPoint,endPoint);
				return true;
			}
		});
		Bundle arguments = new Bundle();
		if (mFoundLocation != null) {
			arguments.putString(RoutingDialogFragment.ARG_END_POINT_DEFAULT, mLocationLayerPointString);
		}
    FragmentTransaction ft = getFragmentManager().beginTransaction();
    Fragment prev = getFragmentManager().findFragmentByTag("routingdialog");
    if (prev != null) {
      ft.remove(prev);
    }
    ft.addToBackStack(null);
		routingFrag.setArguments(arguments);
		routingFrag.show(ft, "routingdialog");

	}


	/**
	 * Displays the Directions Dialog Fragment
	 */
	private void showDirectionsDialogFragment() {
		// Launch a DirectionsListFragment to display list of directions
		final DirectionsDialogFragment frag = new DirectionsDialogFragment();
		frag.setRoutingDirections(mRoutingDirections, new DirectionsDialogListener() {

			@Override
			public void onDirectionSelected(int position) {
				// User has selected a particular direction -
				// dismiss the dialog and
				// zoom to the selected direction
				frag.dismiss();
				DirectionManeuver direction = mRoutingDirections.get(position);

				// create a viewpoint from envelope
				Viewpoint vp = new Viewpoint(direction.getGeometry().getExtent());
				mMapView.setViewpoint(vp);
			}

		});
		getFragmentManager().beginTransaction().add(frag, null).commit();

	}

	/**
	 * Displays the search view layout
	 *
	 */
	private void showSearchBoxLayout() {

		// Inflating the layout from the xml file
		mSearchBox = mInflater.inflate(R.layout.searchview, null);
		// Inflate navigation drawer button on SearchView
		mNavButton = mSearchBox.findViewById(R.id.btn_nav_menu);
		// Get the navigation drawer from Activity
		mDrawerLayout = getActivity().findViewById(R.id.maps_app_activity_drawer_layout);
		mDrawerList = getActivity().findViewById(R.id.maps_app_activity_left_drawer);

		// Set click listener to open/close drawer
		mNavButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (mDrawerLayout.isDrawerOpen(mDrawerList)) {
					mDrawerLayout.closeDrawer(mDrawerList);
				} else {
					mDrawerLayout.openDrawer(mDrawerList);
				}

			}
		});

		// Setting the layout parameters to the layout
		mSearchBox.setLayoutParams(mlayoutParams);

		// Initializing the searchview and the image view
		mSearchview = mSearchBox.findViewById(R.id.searchView1);

		ImageView ivRoute = mSearchBox.findViewById(R.id.imageView1);

		mSearchview.setIconifiedByDefault(false);
		mSearchview.setQueryHint(SEARCH_HINT);

		applySuggestionCursor();


		// Adding the layout to the map conatiner
		mMapContainer.addView(mSearchBox);

		// Setup the listener for the route onclick
		ivRoute.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				showRoutingDialogFragment();

			}
		});

		// Setup the listener when the search button is pressed on the keyboard
		mSearchview.setOnQueryTextListener(new OnQueryTextListener() {

			@Override
			public boolean onQueryTextSubmit(String query) {
				onSearchButtonClicked(query);
				mSearchview.clearFocus();
				return true;
			}

			@Override
			public boolean onQueryTextChange(String newText) {
				if (mLocator == null)
					return false;
				getSuggestions(newText);
				return true;
			}
		});

		// Add the compass after getting the height of the layout
		mSearchBox.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
			@Override
			public void onGlobalLayout() {
				addCompass(mSearchBox.getHeight());
				mSearchBox.getViewTreeObserver().removeOnGlobalLayoutListener(this);
			}

		});

		mSearchview.setOnSuggestionListener(new SearchView.OnSuggestionListener() {

			@Override
			public boolean onSuggestionSelect(int position) {
				return false;
			}

			@Override
			public boolean onSuggestionClick(int position) {
				// Obtain the content of the selected suggesting place via
				// cursor
				MatrixCursor cursor = (MatrixCursor) mSearchview.getSuggestionsAdapter().getItem(position);
				int indexColumnSuggestion = cursor.getColumnIndex(COLUMN_NAME_ADDRESS);
				final String address = cursor.getString(indexColumnSuggestion);

				// Find the Location of the suggestion
				geoCodeSuggestedLocation(address);

				cursor.close();

				return true;
			}
		});
	}

	/**
	 * Initialize Suggestion Cursor
	 */
	private void initSuggestionCursor() {
		String[] cols = {BaseColumns._ID, COLUMN_NAME_ADDRESS, COLUMN_NAME_X, COLUMN_NAME_Y};
		mSuggestionCursor = new MatrixCursor(cols);
	}

	/**
	 * Set the suggestion cursor to an Adapter then set it to the search view
	 */
	private void applySuggestionCursor() {
		String[] cols = { COLUMN_NAME_ADDRESS};
		int[] to = {R.id.suggestion_item_address};
		SimpleCursorAdapter mSuggestionAdapter = new SimpleCursorAdapter(mMapView.getContext(),
				R.layout.search_suggestion_item, mSuggestionCursor, cols, to, 0);
		mSearchview.setSuggestionsAdapter(mSuggestionAdapter);
		mSuggestionAdapter.notifyDataSetChanged();
	}

	/**
	 * Provide a character by character suggestions for the search string
	 *
	 * @param query
	 *            String typed so far by the user to fetch the suggestions
	 */
	private void getSuggestions(final String query) {
		if (query == null || query.isEmpty()) {
			return;
		}

		// Initialize the locatorSugestion parameters
		locatorParams(SUGGEST_PLACE);

		// Attach a listener to the locator task since
		// the LocatorTask may or may not be loaded the
		// the very first time a user types text into the search box.
		// If the Locator is already loaded, the following listener
		// is invoked immediately.

		mLocator.addDoneLoadingListener(new Runnable() {
			@Override public void run() {
				// Quick exit if location doesn't support suggestions
				if (!mLocator.getLocatorInfo().isSupportsSuggestions()){
					return;
				}
				final ListenableFuture<List<SuggestResult>> suggestionsFuture = mLocator.suggestAsync(query, suggestParams);
				// Attach a done listener that executes upon completion of the async call
				suggestionsFuture.addDoneListener(new Runnable() {
					@Override
					public void run() {
						try {
							// Get the suggestions returned from the locator task.
							// Store retrieved suggestions for future use (e.g. if the user
							// selects a retrieved suggestion, it can easily be
							// geocoded).
							mSuggestionsList = suggestionsFuture.get();

							showSuggestedPlaceNames(mSuggestionsList);

						} catch (Exception e) {
							Log.e(TAG, "Error on getting suggestions " + e.getMessage());
						}
					}
				});
			}
		});
		// Initiate the asynchronous call
		mLocator.loadAsync();
	}

	private void showSuggestedPlaceNames(List<SuggestResult> suggestions){
		if (suggestions == null || suggestions.isEmpty()){
			return;
		}
		initSuggestionCursor();
		int key = 0;
		for (SuggestResult result : suggestions) {
			// Add the suggestion results to the cursor
			mSuggestionCursor.addRow(new Object[]{key++, result.getLabel(), "0", "0"});
		}
		applySuggestionCursor();
	}


	/**
	 * Initialize SuggestionParameters or GeocodeParameters
	 *
	 * @param TYPE
	 *            A String determining thr type of parameters to be initialized
	 */

	private void locatorParams(String TYPE) {
		if (TYPE.contentEquals(SUGGEST_PLACE)) {
			// Create suggestion parameters
			suggestParams = new SuggestParameters();
			suggestParams.setSearchArea(calculateSearchArea());
			suggestParams.setPreferredSearchLocation(mLocation);

		}
		if (TYPE.contentEquals(FIND_PLACE)) {
			// Create find parameters
			mGeocodeParams = new GeocodeParameters();
			// Set max results and spatial reference
			mGeocodeParams.setMaxResults(2);
			mGeocodeParams.setOutputSpatialReference(mMapView.getSpatialReference());
			// Use the centre of the current map extent as the location
			mGeocodeParams.setSearchArea(calculateSearchArea());
			mGeocodeParams.setPreferredSearchLocation(mLocation);
		}
		if (TYPE.contentEquals(ROUTE)) {
			// Create find parameters
			mGeocodeParams = new GeocodeParameters();
			// Set max results and spatial reference
			mGeocodeParams.setMaxResults(2);
			mGeocodeParams.setOutputSpatialReference(mMapView.getSpatialReference());
		}
		if (TYPE.contentEquals(REVERSE_GEOCODE)) {
			mReverseGeocodeParams = new ReverseGeocodeParameters();
			mReverseGeocodeParams.setOutputSpatialReference(mMapView.getSpatialReference());

		}
	}

  /**
   *
   * Retrieves location for selected suggestion
   * @param address Suggested address user clicked on
   */
	private void geoCodeSuggestedLocation(final String address) {

    final String TAG_LOCATOR_PROGRESS_DIALOG = "TAG_LOCATOR_PROGRESS_DIALOG";
    // Display progress dialog on UI thread
    final ProgressDialogFragment mProgressDialog = ProgressDialogFragment
        .newInstance(getActivity().getString(R.string.address_search));
    // set the target fragment to receive cancel notification
    mProgressDialog.setTargetFragment(this, REQUEST_CODE_PROGRESS_DIALOG);
    mProgressDialog.show(getActivity().getFragmentManager(), TAG_LOCATOR_PROGRESS_DIALOG);

		// Null out any previously located result
		mGeocodedLocation = null;

    SuggestResult matchedSuggestion = null;
    // get the Location for the suggestion from the ArrayList
    for (SuggestResult result : mSuggestionsList) {
			// changed from address.matches because addresses with parentheses were throwing off REGEX.
      if (address.equalsIgnoreCase(result.getLabel())) {
        matchedSuggestion = result;
        break;
      }
    }
    if (matchedSuggestion != null) {
		// Prepare the GeocodeParameters for geocoding the address
      locatorParams(FIND_PLACE);

      final ListenableFuture<List<GeocodeResult>> locFuture = mLocator.geocodeAsync(matchedSuggestion, mGeocodeParams);
			// Attach a done listener that executes upon completion of the async call
      locFuture.addDoneListener(new Runnable() {
        @Override
        public void run() {
          try {
            List<GeocodeResult> locationResults = locFuture.get();
						showSuggestedPlace(locationResults, address);
						mProgressDialog.dismiss();
          } catch (Exception e) {
						// Notify that there was a problem with geocoding
            Log.e(TAG, "Geocode error " + e.getMessage());
						mProgressDialog.dismiss();
						Toast.makeText(getActivity(),
								getString(R.string.geo_locate_error),
								Toast.LENGTH_LONG).show();
          }
        }
      });
    }else{
			// Notify that no matched suggestion was found
			mProgressDialog.dismiss();
			Toast.makeText(getActivity(),
					getString(R.string.location_not_foud) + " " + address,
					Toast.LENGTH_LONG).show();
		}

	}

	/**
	 * Given an address and the geocode results, dismiss
	 * progress dialog and keyboard and show the geocoded location.
	 * @param locationResults - List of GeocodeResult
   */
	private void showSuggestedPlace(final List<GeocodeResult> locationResults, final String address){

		Point resultPoint = null;
		String resultAddress = null;
		if (locationResults != null && locationResults.size() > 0) {
			// Get the first returned result
			mGeocodedLocation = locationResults.get(0);
			resultPoint = mGeocodedLocation.getDisplayLocation();
			resultAddress = mGeocodedLocation.getLabel();
		}else{
			Log.i(MapFragment.TAG, "No geocode results found for suggestion");
		}

		if (resultPoint == null){
			Toast.makeText(getActivity(),
					getString(R.string.location_not_foud) + resultAddress,
					Toast.LENGTH_LONG).show();
			return;
		}
		// Display the result
		displaySearchResult(resultPoint, address);
	}


	private void displaySearchResult(Point resultPoint, String address) {

		// create marker symbol to represent location
		Bitmap icon = BitmapFactory.decodeResource(getActivity().getResources(), R.drawable.pin_circle_red);
		BitmapDrawable drawable = new BitmapDrawable(getActivity().getResources(), icon);
		PictureMarkerSymbol resultSymbol = new PictureMarkerSymbol(drawable);
		// create graphic object for resulting location
		Graphic resultLocGraphic = new Graphic(resultPoint, resultSymbol);
		// add graphic to location layer
		mLocationLayer.getGraphics().add(resultLocGraphic);

		mFoundLocation = resultPoint;

		mLocationLayerPointString = address;

		// Zoom map to geocode result location
    mMapView.setViewpointCenterAsync(resultPoint);
		showSearchResultLayout(address);
	}
	/**
	 * Clears all graphics out of the location layer and the route layer.
	 */
	void resetGraphicsLayers() {
		mLocationLayer.getGraphics().clear();
		mRouteLayer.getGraphics().clear();
		mFoundLocation = null;
		mLocationLayerPointString = null;
		mRoutingDirections = null;
	}

	/**
	 * Adds location layer and the route layer to the MapView.
	 */
	void addGraphicLayers() {
		// Add location layer
		if (mLocationLayer == null) {
			mLocationLayer = new GraphicsOverlay();
		}

		mMapView.getGraphicsOverlays().add(mLocationLayer);

		// Add the route graphic layer
		if (mRouteLayer == null) {
			mRouteLayer = new GraphicsOverlay();
		}
		mMapView.getGraphicsOverlays().add(mRouteLayer);
	}



	/**
	 * Called from search_layout.xml when user presses Search button.
	 *
	 * @param address
	 */
	public void onSearchButtonClicked(final String address) {

		Log.i(TAG, " #### Submitted address " + address);

		// Remove any previous graphics and routes
		resetGraphicsLayers();

		geoCodeTypedAddress(address);
	}

	/**
	 * Geocode an address typed in by user
	 *
	 * @param address
	 */
	private void geoCodeTypedAddress(final String address) {
		// Create Locator parameters from single line address string
		final GeocodeParameters geoParameters = new GeocodeParameters();
		geoParameters.setMaxResults(2);

		// Use the centre of the current map extent as the find location point
		if (mLocation != null) {
			geoParameters.setPreferredSearchLocation(mLocation);
		}
		// Null out any previously located result
		mGeocodedLocation = null;

		// Set address spatial reference to match map
		SpatialReference sR = mMapView.getSpatialReference();
		geoParameters.setOutputSpatialReference(sR);

		geoParameters.setSearchArea(calculateSearchArea());

		// Execute async task to find the address
		mLocator.addDoneLoadingListener(new Runnable() {
			@Override
			public void run() {
				if (mLocator.getLoadStatus() == LoadStatus.LOADED) {
					// Call geocodeAsync passing in an address
					final ListenableFuture<List<GeocodeResult>> geocodeFuture = mLocator.geocodeAsync(address,
							geoParameters);
					geocodeFuture.addDoneListener(new Runnable() {
						@Override
						public void run() {
							try {
								// Get the results of the async operation
								List<GeocodeResult> geocodeResults = geocodeFuture.get();

								if (geocodeResults.size() > 0) {
									// Use the first result - for example
									// display on the map
									mGeocodedLocation = geocodeResults.get(0);
									displaySearchResult(mGeocodedLocation.getDisplayLocation(), mGeocodedLocation.getLabel());

								}else{
                  Toast.makeText(getActivity(),
                      getString(R.string.location_not_foud) + address,
                      Toast.LENGTH_LONG).show();

								}

							} catch (InterruptedException e) {
								// Deal with exception...
								e.printStackTrace();
								Toast.makeText(getActivity(),
										getString(R.string.geo_locate_error),
										Toast.LENGTH_LONG);

							} catch (ExecutionException e) {
								// Deal with exception...
								e.printStackTrace();
								Toast.makeText(getActivity(),
										getString(R.string.geo_locate_error),
										Toast.LENGTH_LONG);
							}
							// Done processing and can remove this listener.
							geocodeFuture.removeDoneListener(this);
						}
					});

				} else {
					Log.i(TAG, "Trying to reload locator task");
					mLocator.retryLoadAsync();
				}
			}
		});
		mLocator.loadAsync();
	}

	/**
	 * Calculate search geometry given current map extent
	 *
	 * @return Envelope representing an area double the size of the current map
	 *         extent
	 *
	 */
	private Envelope calculateSearchArea() {
		SpatialReference sR = mMapView.getSpatialReference();

		// Get the current map space
		Geometry mapGeometry = mMapView.getCurrentViewpoint(Viewpoint.Type.BOUNDING_GEOMETRY).getTargetGeometry();
		Envelope mapExtent = mapGeometry.getExtent();
		// Calculate distance for find operation

		// assume map is in metres, other units wont work, double current
		// envelope
		double width = mapExtent.getWidth() > 0 ? mapExtent.getWidth() * 2 : 10000;
		double height = mapExtent.getHeight() > 0 ? mapExtent.getHeight() * 2 : 10000;
		double xMax = mapExtent.getXMax() + width;
		double xMin = mapExtent.getXMin() - width;
		double yMax = mapExtent.getYMax() + height;
		double yMin = mapExtent.getYMin() - height;
		return new Envelope(new Point(xMax, yMax, sR), new Point(xMin, yMin, sR));
	}

	/**
	 * Called by RoutingDialogFragment when user presses Get Route button.
	 *
	 * @param startPoint
	 *            String entered by user to define start point.
	 * @param endPoint
	 *            String entered by user to define end point.
	 */

	@Override public boolean onGetRoute(String startPoint, String endPoint) {
		// Check if we need a location fix
		if (startPoint.equals(getString(R.string.my_location)) && mLocation == null) {
			Toast.makeText(getActivity(), getString(R.string.need_location_fix), Toast.LENGTH_LONG).show();

		}
		// Remove any previous graphics and routes
		resetGraphicsLayers();

		// Do the routing
		getRoute(startPoint, endPoint);
		return true;

	}

	/**
	 * Shows the search result in the layout after successful geocoding and
	 * reverse geocoding
	 *
	 */

	private void showSearchResultLayout(String address) {
		// Remove the layouts
		mMapContainer.removeView(mSearchBox);
		mMapContainer.removeView(mSearchResult);

		// Inflate the new layout from the xml file
		mSearchResult = mInflater.inflate(R.layout.search_result, null);

		// Set layout parameters
		mSearchResult.setLayoutParams(mlayoutParams);

		// Initialize the textview and set its text
		TextView tv = mSearchResult.findViewById(R.id.textView1);
		tv.setTypeface(null, Typeface.BOLD);
		tv.setText(address);

		// Adding the search result layout to the map container
		mMapContainer.addView(mSearchResult);

		// Setup the listener for the "cancel" icon
		ImageView iv_cancel = mSearchResult.findViewById(R.id.imageView3);
		iv_cancel.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// Remove the search result view
				mMapContainer.removeView(mSearchResult);

				// Add the search box view
				showSearchBoxLayout();

				// Remove all graphics from the map
				resetGraphicsLayers();

			}
		});

		// Set up the listener for the "Get Directions" icon
		ImageView iv_route = mSearchResult.findViewById(R.id.imageView2);
		iv_route.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				onGetRoute(getString(R.string.my_location), mLocationLayerPointString);
			}
		});

		// Add the compass after getting the height of the layout
		mSearchResult.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
			@Override
			public void onGlobalLayout() {
				addCompass(mSearchResult.getHeight());
				mSearchResult.getViewTreeObserver().removeGlobalOnLayoutListener(this);
			}

		});

	}

	/**
	 * Shows the Routing result layout after successful routing
	 *
	 * @param time
	 * @param distance
	 *
	 */

	private void showRoutingResultLayout(double distance, double time) {

		// Remove the layours
		mMapContainer.removeView(mSearchResult);
		mMapContainer.removeView(mSearchBox);

		// Inflate the new layout from the xml file
		mSearchResult = mInflater.inflate(R.layout.routing_result, null);

		mSearchResult.setLayoutParams(mlayoutParams);

		// Shorten the start and end location by finding the first comma if
		// present
		int index_from = mStartLocationName.indexOf(",");
		int index_to = mEndLocationName.indexOf(",");
		if (index_from != -1)
			mStartLocationName = mStartLocationName.substring(0, index_from);
		if (index_to != -1)
			mEndLocationName = mEndLocationName.substring(0, index_to);

		// Initialize the textvieww and display the text
		TextView tv_from = mSearchResult.findViewById(R.id.tv_from);
		tv_from.setTypeface(null, Typeface.BOLD);
		tv_from.setText(" " + mStartLocationName);

		TextView tv_to = mSearchResult.findViewById(R.id.tv_to);
		tv_to.setTypeface(null, Typeface.BOLD);
		tv_to.setText(" " + mEndLocationName);

    // Convert meters to miles

		// Rounding off the values
		distance = Math.round(distance * 10.0) / 10.0;
		time = Math.round(time * 10.0) / 10.0;

		TextView tv_time = mSearchResult.findViewById(R.id.tv_time);
		tv_time.setTypeface(null, Typeface.BOLD);
		tv_time.setText(time + " mins");

		TextView tv_dist = mSearchResult.findViewById(R.id.tv_dist);
		tv_dist.setTypeface(null, Typeface.BOLD);
		tv_dist.setText(" (" + distance + " meters)");

		// Adding the layout
		mMapContainer.addView(mSearchResult);

		// Setup the listener for the "Cancel" icon
		ImageView iv_cancel = mSearchResult.findViewById(R.id.imageView3);
		iv_cancel.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// Remove the search result view
				mMapContainer.removeView(mSearchResult);
				// Add the default search box view
				showSearchBoxLayout();
				// Remove all graphics from the map
				resetGraphicsLayers();

			}
		});

		// Set up the listener for the "Show Directions" icon
		ImageView iv_directions = mSearchResult.findViewById(R.id.imageView2);
		iv_directions.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				showDirectionsDialogFragment();
			}
		});

		// Add the compass after getting the height of the layout
		mSearchResult.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
			@Override
			public void onGlobalLayout() {
				addCompass(mSearchResult.getHeight());
				mSearchResult.getViewTreeObserver().removeGlobalOnLayoutListener(this);
			}

		});


	}

	/**
	 * Given an origin and destination, execute a route solving task
	 * @param originName - string representing a starting location
	 * @param destinationName - string representing an ending location
   */
	private void getRoute(String originName, final String destinationName) {
		// Show the progress dialog while getting route info
		showProgressDialog(getString(R.string.route_search), TAG_ROUTE_SEARCH_PROGRESS_DIALOG);

		// Configure geocode params specific for routing
		locatorParams(ROUTE);

		// Assign the appropriate routing url.
		// Note that the version (e.g. 10.0, 10.3)
		// of the server publishing the service does
		// make a difference.
		// Read more about routing services (here)
		String routeTaskURL = getString(R.string.routingservice_url);


		mRouteTask = new RouteTask(getActivity(),routeTaskURL);
		mEndLocationName = destinationName;
		Log.i(TAG, mRouteTask.getUri());
		Point endPoint;

		try {
			// Geocode start position or use My Location (from GPS)
			if (originName.equals(getString(R.string.my_location))) {
				mStartLocationName = getString(R.string.my_location);

				// We're using my current location as the
				// start location, do we need to geocode the destination?
				// A previous GeocodeResult may be available...
				if (mGeocodedLocation != null){
					endPoint = mGeocodedLocation.getDisplayLocation();
					// We have the start and end, now get route
					mRouteTask.addDoneLoadingListener(new RouteSolver(mLocation,endPoint));
					mRouteTask.loadAsync();

				}else{
					Log.i(TAG, "Geocoding destination address: " + destinationName);
					final ListenableFuture<List<GeocodeResult>> geoFutureEnd = mLocator.geocodeAsync(destinationName,
							mGeocodeParams);
					geoFutureEnd.addDoneListener(new Runnable() {
						@Override
						public void run() {
							try {
								List<GeocodeResult> results = geoFutureEnd.get();
								if (results != null && results.size() > 0) {
									final Point endPoint = results.get(0).getDisplayLocation();

									// We have the start and end, now get route
									mRouteTask.addDoneLoadingListener(new RouteSolver(mLocation,endPoint));
									mRouteTask.loadAsync();

								} else {
									Log.i(TAG, "Geocoding failed to return results for this address: " + destinationName);
								}
							} catch (Exception ie) {
								mProgressDialog.dismiss();
								ie.printStackTrace();
								Toast.makeText(getActivity(),
										getString(R.string.geo_locate_error),
										Toast.LENGTH_LONG);
							}
						}
					});
				}
			} else {
				// Get the start location
				final ListenableFuture<List<GeocodeResult>> geoFutureStart = mLocator.geocodeAsync(originName,
						mGeocodeParams);
				geoFutureStart.addDoneListener(new Runnable() {
					@Override
					public void run() {
						try {
							List<GeocodeResult> results = geoFutureStart.get();
							if (results != null && results.size() > 0) {
								final Point origin = results.get(0).getDisplayLocation();
                mStartLocationName = results.get(0).getLabel();

								// Now we need to get the end location
								final ListenableFuture<List<GeocodeResult>> geoFutureEnd2 = mLocator
										.geocodeAsync(destinationName, mGeocodeParams);
								geoFutureEnd2.addDoneListener(new Runnable() {
									@Override
									public void run() {
										try {
											List<GeocodeResult> results = geoFutureEnd2.get();
											if (results != null && results.size() > 0) {
												Point end = results.get(0).getDisplayLocation();
                        mEndLocationName = results.get(0).getLabel();
												// We have the start and end,
												// now get route
												mRouteTask.addDoneLoadingListener(new RouteSolver(origin,end));
												mRouteTask.loadAsync();
											}
										} catch (Exception e) {
											e.printStackTrace();
											mProgressDialog.dismiss();
											Toast.makeText(getActivity(),
													getString(R.string.geo_locate_error),
													Toast.LENGTH_LONG);
										}

									}
								});
							}
						} catch (Exception e) {
							e.printStackTrace();
							mProgressDialog.dismiss();
							Toast.makeText(getActivity(),
									getString(R.string.geo_locate_error),
									Toast.LENGTH_LONG);
						}
					}
				});
			}

		} catch (Exception e) {
			e.printStackTrace();
			mProgressDialog.dismiss();
			Toast.makeText(getActivity(),
					getString(R.string.routingFailed),
					Toast.LENGTH_LONG);
		}

	}

	/**
	 * Show progress dialog with the given message and name
	 *
	 * @param message
	 *            String
	 * @param name
	 *            String
	 */
	private void showProgressDialog(String message, String name) {
		mProgressDialog = ProgressDialogFragment.newInstance(message);
		// set the target fragment to receive cancel notification
		mProgressDialog.setTargetFragment(this, REQUEST_CODE_PROGRESS_DIALOG);
		mProgressDialog.show(getActivity().getFragmentManager(), name);
	}

	private void showRoute(RouteResult routeResult, Point startPoint, Point endPoint) {
		// Get first item in list of routes provided by server
		Route route;
		try {
			route = routeResult.getRoutes().get(0);
			if (route.getTotalLength() == 0.0) {
				throw new Exception("Can not find the Route");
			}
		} catch (Exception e) {
			Toast.makeText(getActivity(),
					"We are sorry, we couldn't find the route. Please make "
							+ "sure the Source and Destination are different or are connected by road",
					Toast.LENGTH_LONG).show();
			Log.e(TAG, e.getMessage());
			return;
		}

		// Create polyline graphic of the full route
		SimpleLineSymbol lineSymbol = new SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, Color.RED, 2);
		Graphic routeGraphic = new Graphic(route.getRouteGeometry(), lineSymbol);

		// Create point graphic to mark start of route

		Geometry shape = routeGraphic.getGeometry();
		Graphic startGraphic = createMarkerGraphic(startPoint, false);

		// Create point graphic to mark end of route
		Graphic endGraphic = createMarkerGraphic(endPoint, true);

		// Add these graphics to route layer
		mRouteLayer.getGraphics().add(routeGraphic);
		mRouteLayer.getGraphics().add(startGraphic);
		mRouteLayer.getGraphics().add(endGraphic);

		// Zoom to the extent of the entire route with a padding
		mMapView.setViewpointGeometryAsync(shape,400);

		// Save routing directions so user can display them later
		mRoutingDirections = route.getDirectionManeuvers();

		// Show Routing Result Layout
		showRoutingResultLayout(route.getTotalLength(), route.getTotalTime());
	}

	private Graphic createMarkerGraphic(Point point, boolean endPoint) {

		BitmapDrawable bitmapDrawable = (BitmapDrawable) ContextCompat.getDrawable(
				getActivity().getApplicationContext(),
				endPoint ? R.drawable.pin_circle_blue : R.drawable.pin_circle_red);
		PictureMarkerSymbol destinationSymbol = new PictureMarkerSymbol(bitmapDrawable);
		// NOTE: marker's bounds not set till marker is used to create
		// destinationSymbol
		float offsetY = convertPixelsToDp(getActivity(),
				bitmapDrawable.getBounds().bottom);
		destinationSymbol.setOffsetY(offsetY);
		return new Graphic(point, destinationSymbol);
	}

	private void reverseGeocode(Point point) {
		if (point == null){
			return;
		}
		// Show progress dialog
		showProgressDialog(getString(R.string.reverse_geocoding), TAG_REVERSE_GEOCODING_PROGRESS_DIALOG);

		// Provision reverse geocode parameers
		locatorParams(REVERSE_GEOCODE);
		// Pass in the point and geocode parameters to the reverse geocode method
		final ListenableFuture<List<GeocodeResult>> reverseFuture = mLocator.reverseGeocodeAsync(point,
				mReverseGeocodeParams);
		// Attach a done listener that shows the results upon completion of the async call
		reverseFuture.addDoneListener(new Runnable() {
			@Override
			public void run() {
				try {
					if (mProgressDialog !=null){
						mProgressDialog.dismiss();
					}

					List<GeocodeResult> results = reverseFuture.get();
					// Process and display results
					showReverseGeocodeResult(results);
				} catch (InterruptedException e) {
					e.printStackTrace();
				} catch (ExecutionException e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Displays any gecoded results by add a PictureMarkerSymbol to the map's
	 * graphics layer.
	 *
	 * @param results
	 *            A list of GeocodeResult items
	 */
	private void showReverseGeocodeResult(List<GeocodeResult> results) {

		if (results != null && results.size() > 0) {
			// Show the first item returned
			// Address string is saved for use in routing
			mLocationLayerPointString = results.get(0).getLabel();
			mFoundLocation = results.get(0).getDisplayLocation();
			// Draw marker on map.
			// create marker symbol to represent location

			BitmapDrawable bitmapDrawable = (BitmapDrawable) ContextCompat
					.getDrawable(getActivity().getApplicationContext(), R.drawable.pin_circle_red);
			PictureMarkerSymbol destinationSymbol = new PictureMarkerSymbol(bitmapDrawable);
			mLocationLayer.getGraphics().add(new Graphic(mFoundLocation, destinationSymbol));

			// center the map to result location
			mMapView.setViewpointCenterAsync(mFoundLocation);

			// Show the result on the search result layout
			showSearchResultLayout(mLocationLayerPointString);
		}
	}

	/**
	 * Converts device specific pixels to density independent pixels.
	 *
	 * @param context
	 * @param px
	 *            number of device specific pixels
	 * @return number of density independent pixels
	 */
	private float convertPixelsToDp(Context context, float px) {
		DisplayMetrics metrics = context.getResources().getDisplayMetrics();
		return px / (metrics.densityDpi / 160f);
	}

  @Override public void onBasemapChanged(String itemId) {
    ((MapsAppActivity) getActivity()).showMap(mPortalItemId,
        itemId);
  }

  @Override public void onCancel(DialogInterface dialog) {

  }

  private class MapTouchListener extends DefaultMapViewOnTouchListener {
		/**
		 * Instantiates a new DrawingMapViewOnTouchListener with the specified
		 * context and MapView.
		 *
		 * @param context
		 *            the application context from which to get the display
		 *            metrics
		 * @param mapView
		 *            the MapView on which to control touch events
		 */
		public MapTouchListener(Context context, MapView mapView) {
			super(context, mapView);
		}

		@Override
		public boolean onUp(MotionEvent motionEvent) {
			if (mLongPressEvent != null) {
				// This is the end of a long-press that will have displayed
				// the magnifier.  Get the graphic
				// coordinates for the motion event.
				android.graphics.Point mapPoint = new android.graphics.Point((int) motionEvent.getX(),
						(int) motionEvent.getY());
				Point point = mMapView.screenToLocation(mapPoint);
				// Set any previously located point to null
				mFoundLocation = null;
				reverseGeocode(point);

				mLongPressEvent = null;

				// Remove any previous graphics
				resetGraphicsLayers();
			}
			return true;
		}

		@Override
		public void onLongPress(MotionEvent e) {
			super.onLongPress(e);
			// Set mLongPressEvent to indicate
			// we are processing a long-press
			mLongPressEvent = e;
		}
		@Override
		public boolean onRotate (MotionEvent e, double angle){
			super.onRotate(e,angle);
			// Show the compass if map isn't oriented towards north
			if (mMapView.getMapRotation() != 0){
				mCompass.setVisibility(View.VISIBLE);
				mCompass.setRotationAngle(mMapView.getMapRotation());
			}else{
				mCompass.setVisibility(View.GONE);
			}
			return true;
		}

	}
	/**
	 * Listen for location changes and update my location
	 */
	private class LocationListener implements LocationDisplay.LocationChangedListener {

		@Override public void onLocationChanged(LocationDisplay.LocationChangedEvent locationChangedEvent) {
			if (locationChangedEvent.getLocation().getPosition() != null){
				mLocation = locationChangedEvent.getLocation().getPosition();
			}
		}
	}
	/**
	 * A helper class for solving routes
	 */
	private class RouteSolver implements Runnable{
		private final Stop origin;

    private final Stop destination;

		public RouteSolver(Point start, Point end){
			origin = new Stop(start);
			destination = new Stop(end);
		}
		@Override
		public void run (){
			LoadStatus status = mRouteTask.getLoadStatus();
			Log.i(TAG, "Route task is " + status.name());

			// Has the route task loaded successfully?
			if (status == LoadStatus.FAILED_TO_LOAD) {
				Log.i(TAG, mRouteTask.getLoadError().getMessage());
				mProgressDialog.dismiss();
				Toast.makeText(getActivity(),
						"There was a problem loading the route task.",
						Toast.LENGTH_LONG).show();
				// We may want to try reloading it  --> mRouteTask.retryLoadAsync();

			} else {
				final ListenableFuture<RouteParameters> routeTaskFuture = mRouteTask
						.createDefaultParametersAsync();
				// Add a done listener that uses the returned route parameters
				// to build up a specific request for the route we need
				routeTaskFuture.addDoneListener(new Runnable() {

					@Override
					public void run() {
						try {
							RouteParameters routeParameters = routeTaskFuture.get();
							// Add a stop for origin and destination
							routeParameters.getStops().add(origin);
							routeParameters.getStops().add(destination);
							// We want the task to return driving directions and routes
							routeParameters.setReturnDirections(true);
							routeParameters.setOutputSpatialReference(mMapView.getSpatialReference());
							routeParameters.setDirectionsDistanceUnits(UnitSystem.METRIC);
							final ListenableFuture<RouteResult> routeResFuture = mRouteTask
									.solveRouteAsync(routeParameters);
							routeResFuture.addDoneListener(new Runnable() {
								@Override
								public void run() {
									try {
										RouteResult routeResult = routeResFuture.get();
										// Show route results
										showRoute(routeResult, origin.getGeometry(), destination.getGeometry());
										// Dismiss progress dialog
										mProgressDialog.dismiss();


									} catch (InterruptedException e) {
										e.printStackTrace();
										mProgressDialog.dismiss();
										Toast.makeText(getActivity(),
												getString(R.string.routingFailed),
												Toast.LENGTH_LONG).show();
									} catch (ExecutionException e) {
										e.printStackTrace();
										mProgressDialog.dismiss();
										Toast.makeText(getActivity(),
												"We're sorry, we couldn't find a route",
												Toast.LENGTH_LONG).show();
									}
								}
							});
						} catch (InterruptedException e) {
							e.printStackTrace();
						} catch (ExecutionException e) {
							e.printStackTrace();
						}
					}
				});
			}
		}
	}
}
