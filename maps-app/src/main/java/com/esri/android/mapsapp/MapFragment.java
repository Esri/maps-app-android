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

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.database.MatrixCursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.DrawerLayout;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewGroup.MarginLayoutParams;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.SearchView.OnQueryTextListener;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.esri.android.mapsapp.account.AccountManager;
import com.esri.android.mapsapp.dialogs.ProgressDialogFragment;
import com.esri.android.mapsapp.location.DirectionsDialogFragment;
import com.esri.android.mapsapp.location.DirectionsDialogFragment.DirectionsDialogListener;
import com.esri.android.mapsapp.location.RoutingDialogFragment;
import com.esri.android.mapsapp.location.RoutingDialogFragment.RoutingDialogListener;
import com.esri.android.mapsapp.tools.Compass;
import com.esri.android.mapsapp.util.TaskExecutor;
import com.esri.arcgisruntime.ArcGISRuntimeEnvironment;
import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.geometry.Envelope;
import com.esri.arcgisruntime.geometry.Geometry;
import com.esri.arcgisruntime.geometry.GeometryEngine;
import com.esri.arcgisruntime.geometry.LinearUnit;
import com.esri.arcgisruntime.geometry.LinearUnitId;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.Polyline;
import com.esri.arcgisruntime.geometry.SpatialReference;
import com.esri.arcgisruntime.loadable.LoadStatus;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.Viewpoint;
import com.esri.arcgisruntime.mapping.view.DefaultMapViewOnTouchListener;
import com.esri.arcgisruntime.mapping.view.Graphic;
import com.esri.arcgisruntime.mapping.view.GraphicsOverlay;
import com.esri.arcgisruntime.mapping.view.LocationDisplay;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.esri.arcgisruntime.mapping.view.WrapAroundMode;
import com.esri.arcgisruntime.portal.Portal;
import com.esri.arcgisruntime.portal.PortalItem;
import com.esri.arcgisruntime.security.AuthenticationManager;
import com.esri.arcgisruntime.security.DefaultAuthenticationChallengeHandler;
import com.esri.arcgisruntime.symbology.PictureMarkerSymbol;
import com.esri.arcgisruntime.symbology.SimpleLineSymbol;
import com.esri.arcgisruntime.tasks.geocode.GeocodeParameters;
import com.esri.arcgisruntime.tasks.geocode.GeocodeResult;
import com.esri.arcgisruntime.tasks.geocode.LocatorTask;
import com.esri.arcgisruntime.tasks.geocode.ReverseGeocodeParameters;
import com.esri.arcgisruntime.tasks.geocode.SuggestParameters;
import com.esri.arcgisruntime.tasks.geocode.SuggestResult;
import com.esri.arcgisruntime.tasks.route.DirectionDistanceTextUnits;
import com.esri.arcgisruntime.tasks.route.DirectionManeuver;
import com.esri.arcgisruntime.tasks.route.Route;
import com.esri.arcgisruntime.tasks.route.RouteParameters;
import com.esri.arcgisruntime.tasks.route.RouteResult;
import com.esri.arcgisruntime.tasks.route.RouteTask;
import com.esri.arcgisruntime.tasks.route.RouteTaskInfo;
import com.esri.arcgisruntime.tasks.route.Stop;

import android.view.ViewTreeObserver.OnGlobalLayoutListener;


/**
 * Implements the view that shows the map.
 */
/*public class MapFragment extends Fragment implements BasemapsDialogListener,
		RoutingDialogListener, OnCancelListener {*/
public class MapFragment extends Fragment  {

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

	private static final String SEARCH_HINT = "Search";

	private static final String FIND_PLACE = "Find";

	private static final String SUGGEST_PLACE = "Suggest";

	private static final String ROUTE = "Route";

	private static final String REVERSE_GECODE = "Reverse Geocode";

	private static FrameLayout.LayoutParams mlayoutParams;

	// Margins parameters for search view
	private static int TOP_MARGIN_SEARCH = 55;

	// The circle area specified by search_radius and input lat/lon serves
	// searching purpose.
	// It is also used to construct the extent which map zooms to after the
	// first
	// GPS fix is retrieved.
	private final static double SEARCH_RADIUS = 10;

	private String mPortalItemId;
	private String mBasemapPortalItemId;
	private PortalItem mPortalItem;
	private FrameLayout mMapContainer;
	public static MapView mMapView;
	private String mMapViewState;
	private SearchView mSearchview;

	// GPS location tracking
	private LocationDisplay mLocationDisplay;
	private boolean mIsLocationTracking;
	private Point mLocation = null;

	// Graphics layer to show geocode and reverse geocode results
	private GraphicsOverlay mLocationLayer;
	private Point mLocationLayerPoint;
	private String mLocationLayerPointString;

	// Graphics layer to show routes
	private GraphicsOverlay mRouteLayer;
	private List<DirectionManeuver> mRoutingDirections;

	// Spatial references used for projecting points
	private final SpatialReference mWm = SpatialReference.create(102100);
	private final SpatialReference mEgs = SpatialReference.create(4326);
	private MatrixCursor mSuggestionCursor;

	Compass mCompass;
	LayoutParams compassFrameParams;
	private MotionEvent mLongPressEvent;
	private ProgressDialogFragment mProgressDialog;

	@SuppressWarnings("rawtypes")
	// - using this only to cancel pending tasks in a generic way
	private AsyncTask mPendingTask;
	private View mSearchBox;
	private LocatorTask mLocator ;
	private View mSearchResult;
	private LayoutInflater mInflater;
	private String mStartLocation, mEndLocation;
	private SuggestParameters suggestParams;
	private GeocodeParameters mGeocodeParams;
	private ReverseGeocodeParameters mReverseGeocodeParams;
	private RouteTask mRouteTask;


	private final java.util.Map<String,Point> suggestMap = new TreeMap<>();
	private static List<SuggestResult> mSuggestionsList;

	private SpatialReference mapSpatialReference;
	private boolean suggestionClickFlag = false;
	private Point resultEndPoint;
	int width, height;

	LayoutParams gpsFrameParams;

	ImageButton navButton;
	DrawerLayout mDrawerLayout;
	ListView mDrawerList;

	public static MapFragment newInstance(String portalItemId,
										  String basemapPortalItemId) {
		MapFragment mapFragment = new MapFragment();

		Bundle args = new Bundle();
		args.putString(KEY_PORTAL_ITEM_ID, portalItemId);
		args.putString(KEY_BASEMAP_ITEM_ID, basemapPortalItemId);

		mapFragment.setArguments(args);
		return mapFragment;
	}

	public MapFragment() {
		// make MapFragment ctor private - use newInstance() instead
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setHasOptionsMenu(true);

		Bundle args = savedInstanceState != null ? savedInstanceState
				: getArguments();
		if (args != null) {
			mIsLocationTracking = args.getBoolean(KEY_IS_LOCATION_TRACKING);
			mPortalItemId = args.getString(KEY_PORTAL_ITEM_ID);
			mBasemapPortalItemId = args.getString(KEY_BASEMAP_ITEM_ID);
		}

		ArcGISRuntimeEnvironment.License.setLicense(getString(R.string.license));
		mLocator =  new LocatorTask("http://geocode.arcgis.com/arcgis/rest/services/World/GeocodeServer");

	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		// inflate MapView from layout

		mMapContainer = (FrameLayout) inflater.inflate(
				R.layout.map_fragment_layout, null);

		if (mPortalItemId != null) {
			// load the WebMap
			loadWebMapIntoMapView(mPortalItemId, mBasemapPortalItemId,
					AccountManager.getInstance().getPortal());
		} else {
			if (mBasemapPortalItemId != null) {
				// show a map with the basemap represented by
				// mBasemapPortalItemId
				loadWebMapIntoMapView(mBasemapPortalItemId, null,
						AccountManager.getInstance().getAGOLPortal());
			} else {
				// show the default map
				String defaultBaseMapURL = getString(R.string.default_basemap_url);
				Basemap basemap = new Basemap(defaultBaseMapURL);
				com.esri.arcgisruntime.mapping.Map map = new com.esri.arcgisruntime.mapping.Map(basemap);

				final MapView mapView =  (MapView) mMapContainer.findViewById(R.id.map);
				mapView.setMap(map);
				mapView.setMagnifierEnabled(true);
				// Set the MapView to allow the user to rotate the map when as
				// part of a pinch gesture.
				//TODO: Modified for Quartz
				// mapView.setAllowRotationByPinch(true);

				setMapView(mapView);


				// TODO: Is this needed in Quartz?
				// mapView.zoomin();

				// TODO: Does this need to run on runOnUiThread?

				// Set up click listener on floating action button
				setClickListenerForFloatingActionButton(mapView);

				// Get an initial location on start up
				mLocationDisplay = mapView.getLocationDisplay();
				mLocationDisplay.addLocationChangedListener(new LocationDisplay.LocationChangedListener() {
					@Override
					public void onLocationChanged(LocationDisplay.LocationChangedEvent locationChangedEvent) {

						Point point = locationChangedEvent.getLocation().getPosition();
						if (point !=null){
							mLocationDisplay.removeLocationChangedListener(this);
							Log.i(TAG, "I have a location " + point.getX() + " , " + point.getY());
							showMyLocation(point);
						}


					}
				});
				mLocationDisplay.startAsync();

				//add graphics layer
				addGraphicLayers();

				Log.i(TAG, "Map is magnifier enabled " + mapView.isMagnifierEnabled());
			}
		}

		return mMapContainer;
	}

	/**
	 * The floating action button toggles location tracking.  When location
	 * tracking is on, the compass is shown in the upper right of the map view.
	 * When location tracking is off, the compass is shown if the map is not oriented
	 * north (0 degrees).
	 * @param mapView
	 */
	private void setClickListenerForFloatingActionButton(final MapView mapView){
		final FloatingActionButton fab = (FloatingActionButton) mMapContainer.findViewById(R.id.fab);
		fab.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mLocationDisplay = mapView.getLocationDisplay();

				// Toggle location tracking on or off
				if (mIsLocationTracking) {
					fab.setImageResource(R.drawable.ic_action_compass_mode);
					mLocationDisplay.setAutoPanMode(LocationDisplay.AutoPanMode.COMPASS);
					mCompass.start();
					mCompass.setVisibility(View.VISIBLE);
					Log.i(TAG, "Location tracking on, compass should be visible");
					mIsLocationTracking = false;
				} else {
					fab.setImageResource(android.R.drawable.ic_menu_mylocation);
					mLocationDisplay.startAsync();
					mLocationDisplay.addLocationChangedListener(new LocationDisplay.LocationChangedListener() {
						@Override
						public void onLocationChanged(LocationDisplay.LocationChangedEvent locationChangedEvent) {
							startLocationTracking(locationChangedEvent);
						}
					});
					if (mMapView.getMapRotation() != 0) {
						mCompass.setVisibility(View.VISIBLE);
						mCompass.setRotationAngle(mMapView.getMapRotation());
						Log.i(TAG, "No location tracking, map not pointed north, compass should be visible");
					} else {
						mCompass.setVisibility(View.GONE);
						Log.i(TAG, "No location tracking, map is pointed north, compass should not be visible");
					}
					mIsLocationTracking = true;
				}
			}
		});
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);

		// Inflate the menu items for use in the action bar
		inflater.inflate(R.menu.action, menu);

	}

	public boolean onOptionsItemSelected(MenuItem item) {

		switch (item.getItemId()) {

			default:
				return super.onOptionsItemSelected(item);
		}
	}


	@Override
	public void onPause() {
		super.onPause();

		// Pause the MapView and stop the LocationDisplayManager to save battery
		if (mMapView != null) {
			//TODO: Needed for Quartz?
			/*
			if (mIsLocationTracking) {
				mMapView.getLocationDisplay().
				mCompass.stop();
			}
			mMapViewState = mMapView.retainState();
			*/
			mMapView.pause();
		}
	}

	@Override
	public void onResume() {
		super.onResume();

		// Start the MapView and LocationDisplayManager running again
		if (mMapView != null) {
			// mCompass.start();
			mMapView.resume();
			// TODO: Need for Quartz?
			/*
			if (mMapViewState != null) {
				mMapView.restoreState(mMapViewState);
			}
			if (mIsLocationTracking) {
				mMapView.getLocationDisplayManager().start();
			}
			*/
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		outState.putBoolean(KEY_IS_LOCATION_TRACKING, mIsLocationTracking);
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
	private void loadWebMapIntoMapView(final String portalItemId,
									   final String basemapPortalItemId, final Portal portal) {

		TaskExecutor.getInstance().getThreadPool().submit(new Callable<Void>() {

			@Override
			public Void call() throws Exception {

				// load a WebMap instance from the portal item
				PortalItem portalItem = new PortalItem(portal, portalItemId);
				final com.esri.arcgisruntime.mapping.Map webmap = new com.esri.arcgisruntime.mapping.Map(portalItem);

				// load the WebMap that represents the basemap if one was
				// specified

				if (basemapPortalItemId != null
						&& !basemapPortalItemId.isEmpty()) {
					PortalItem webPortalItem = new PortalItem(portal, basemapPortalItemId);
					Basemap basemapWebMap = new Basemap(webPortalItem);
					webmap.setBasemap(basemapWebMap);
				}

				if (webmap != null) {
					// TODO: DO WE NEED to run this on RunOnUiThread IN QUARTZ?
					getActivity().runOnUiThread(new Runnable() {
						@Override
						public void run() {
							final MapView mapView =  (MapView) mMapContainer.findViewById(R.id.map);
							mapView.setMap(webmap);
							setMapView(mapView);
							setClickListenerForFloatingActionButton(mapView);
							addGraphicLayers();
						}
					});

				} else {
					throw new Exception("Failed to load web map.");
				}
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
		mMapView.setLogoVisible(true);
		mMapView.setWrapAroundMode(WrapAroundMode.ENABLE_WHEN_SUPPORTED);

		//TODO: Is this needed in Quartz?
		//mapView.setAllowRotationByPinch(true);

		// Creating an inflater
		mInflater = (LayoutInflater) getActivity().getSystemService(
				Context.LAYOUT_INFLATER_SERVICE);

		// Setting up the layout params for the searchview and searchresult
		// layout
		mlayoutParams = new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT,
				LayoutParams.WRAP_CONTENT, Gravity.LEFT | Gravity.TOP);
		int LEFT_MARGIN_SEARCH = 15;
		int RIGHT_MARGIN_SEARCH = 15;
		int BOTTOM_MARGIN_SEARCH = 0;
		mlayoutParams.setMargins(LEFT_MARGIN_SEARCH, TOP_MARGIN_SEARCH,
				RIGHT_MARGIN_SEARCH, BOTTOM_MARGIN_SEARCH);


		// Displaying the searchbox layout
		showSearchBoxLayout();


		// Set up location tracking
		mLocationDisplay = mapView.getLocationDisplay();
		mLocationDisplay.addLocationChangedListener(new LocationDisplay.LocationChangedListener() {
			@Override
			public void onLocationChanged(LocationDisplay.LocationChangedEvent locationChangedEvent) {
				startLocationTracking(locationChangedEvent);
			}
		});
		mLocationDisplay.startAsync();


		// TODO: Port to Quartz
		// Setup use of magnifier on a long press on the map
		mMapView.setMagnifierEnabled(true);
		mLongPressEvent = null;

		// Setup OnTouchListener to detect and act on long-press
		mMapView.setOnTouchListener(new MapTouchListener(getActivity().getApplicationContext(), mMapView));

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
		compassFrameParams = new FrameLayout.LayoutParams(WIDTH, HEIGHT,
				Gravity.RIGHT);

		int TOP_MARGIN_COMPASS = TOP_MARGIN_SEARCH + height + 45;

		int LEFT_MARGIN_COMPASS = 0;
		int BOTTOM_MARGIN_COMPASS = 0;
		int RIGHT_MARGIN_COMPASS = 0;
		((MarginLayoutParams) compassFrameParams).setMargins(
				LEFT_MARGIN_COMPASS, TOP_MARGIN_COMPASS, RIGHT_MARGIN_COMPASS,
				BOTTOM_MARGIN_COMPASS);

		mCompass.setLayoutParams(compassFrameParams);

		mCompass.setVisibility(View.GONE);

		mCompass.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				mCompass.setVisibility(View.GONE);
				mMapView.setRotation(0f);
				mMapView.setRotation(0f);
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

		suggestionClickFlag = false;
		// Show RoutingDialogFragment to get routing start and end points.
		// This calls back to onGetRoute() to do the routing.
		RoutingDialogFragment routingFrag = new RoutingDialogFragment();
		routingFrag.setRoutingDialogListener(new RoutingDialogListener() {
			@Override
			public boolean onGetRoute(String startPoint, String endPoint) {
				return false;
			}
		});
		Bundle arguments = new Bundle();
		if (mLocationLayerPoint != null) {
			arguments.putString(RoutingDialogFragment.ARG_END_POINT_DEFAULT,
					mLocationLayerPointString);
		}
		routingFrag.setArguments(arguments);
		routingFrag.show(getFragmentManager(), null);

	}

	/**
	 * Displays the Directions Dialog Fragment
	 */
	private void showDirectionsDialogFragment() {
		// Launch a DirectionsListFragment to display list of directions
		final DirectionsDialogFragment frag = new DirectionsDialogFragment();
		frag.setRoutingDirections(mRoutingDirections,
				new DirectionsDialogListener() {

					@Override
					public void onDirectionSelected(int position) {
						// User has selected a particular direction -
						// dismiss the dialog and
						// zoom to the selected direction
						frag.dismiss();
						DirectionManeuver direction = mRoutingDirections
								.get(position);

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
		navButton = (ImageButton) mSearchBox.findViewById(R.id.btn_nav_menu);
		// Get the navigation drawer from Activity
		mDrawerLayout = (DrawerLayout) getActivity().findViewById(R.id.maps_app_activity_drawer_layout);
		mDrawerList = (ListView) getActivity().findViewById(R.id.maps_app_activity_left_drawer);

		// Set click listener to open/close drawer
		navButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if(mDrawerLayout.isDrawerOpen(mDrawerList)){
					mDrawerLayout.closeDrawer(mDrawerList);
				}else{
					mDrawerLayout.openDrawer(mDrawerList);
				}

			}
		});

		// Setting the layout parameters to the layout
		mSearchBox.setLayoutParams(mlayoutParams);

		// Initializing the searchview and the image view
		mSearchview = (SearchView) mSearchBox
				.findViewById(R.id.searchView1);

		ImageView iv_route = (ImageView) mSearchBox
				.findViewById(R.id.imageView1);

		mSearchview.setIconifiedByDefault(false);
		mSearchview.setQueryHint(SEARCH_HINT);

		applySuggestionCursor();

//		navButton = (Button)mSearchBox.findViewById(R.id.navbutton);

		// Adding the layout to the map conatiner
		mMapContainer.addView(mSearchBox);

		// Setup the listener for the route onclick
		iv_route.setOnClickListener(new OnClickListener() {

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
				if(mLocator == null)
					return false;
				getSuggestions(newText);
				return true;
			}
		});

		// Add the compass after getting the height of the layout
		mSearchBox.getViewTreeObserver().addOnGlobalLayoutListener(
				new OnGlobalLayoutListener() {
					@Override
					public void onGlobalLayout() {
						addCompass(mSearchBox.getHeight());
						mSearchBox.getViewTreeObserver().removeGlobalOnLayoutListener(this);
					}

				});

		mSearchview.setOnSuggestionListener(new SearchView.OnSuggestionListener() {

			@Override
			public boolean onSuggestionSelect(int position) {
				return false;
			}

			@Override
			public boolean onSuggestionClick(int position) {
				// Obtain the content of the selected suggesting place via cursor
				MatrixCursor cursor = (MatrixCursor) mSearchview.getSuggestionsAdapter().getItem(position);
				int indexColumnSuggestion = cursor.getColumnIndex(COLUMN_NAME_ADDRESS);
				final String address = cursor.getString(indexColumnSuggestion);

				suggestionClickFlag = true;
				// Find the Location of the suggestion
				findLocation(address);

				cursor.close();

				return true;
			}
		});
	}

	/**
	 * Initialize Suggestion Cursor
	 */
	private void initSuggestionCursor() {
		String[] cols = new String[]{BaseColumns._ID, COLUMN_NAME_ADDRESS, COLUMN_NAME_X, COLUMN_NAME_Y};
		mSuggestionCursor = new MatrixCursor(cols);
	}

	/**
	 * Set the suggestion cursor to an Adapter then set it to the search view
	 */
	private void applySuggestionCursor() {
		String[] cols = new String[]{COLUMN_NAME_ADDRESS};
		int[] to = new int[]{R.id.suggestion_item_address};

		SimpleCursorAdapter mSuggestionAdapter = new SimpleCursorAdapter(mMapView.getContext(), R.layout.search_suggestion_item, mSuggestionCursor, cols, to, 0);
		mSearchview.setSuggestionsAdapter(mSuggestionAdapter);
		mSuggestionAdapter.notifyDataSetChanged();
	}


	/**
	 * Provide a character by character suggestions for the search string
	 *
	 * @param query String typed so far by the user to fetch the suggestions
	 */
	private void getSuggestions(String query) {
		if (query == null || query.isEmpty()){
			return;
		}
		// Initialize the locatorSugestion parameters
		locatorParams(SUGGEST_PLACE);

		final ListenableFuture<List<SuggestResult>> suggestionsFuture  = mLocator.suggestAsync(query, suggestParams );
		suggestionsFuture.addDoneListener(new Runnable() {
			@Override
			public void run() {
				try{
					// Get the suggestions returned from the locator task
					mSuggestionsList = suggestionsFuture.get();
					List<String> suggestedAddresses = new ArrayList<String>(mSuggestionsList.size());

					if (mSuggestionsList != null && mSuggestionsList.size() > 0) {
						initSuggestionCursor();
						int key = 0;
						for (SuggestResult result : mSuggestionsList){
							suggestedAddresses.add(result.getLabel());
							// Add the suggestion results to the cursor
							mSuggestionCursor.addRow(new Object[]{key++, result.getLabel(), "0", "0"});
						}
						applySuggestionCursor();
					}

				}catch (Exception e){
					Log.e(TAG, "No suggested places found");
					Log.e(TAG, "Get suggestions error " +  e.getMessage());
				}
			}
		});
	}

	/**
	 * Initialize SuggestionParameters or GeocodeParameters
	 *
	 * @param TYPE A String determining thr type of parameters to be initialized
	 */

	private void locatorParams(String TYPE) {
		if(TYPE.contentEquals(SUGGEST_PLACE)) {
			// Create suggestion parameters
			suggestParams = new SuggestParameters();
			suggestParams.setSearchArea(calculateSearchArea());
		}
		if (TYPE.contentEquals(FIND_PLACE)) {
			// Create find parameters
			mGeocodeParams = new GeocodeParameters();
			// Set max results and spatial reference
			mGeocodeParams.setMaxResults(2);
			mGeocodeParams.setOutputSpatialReference(mMapView.getSpatialReference());
			// Use the centre of the current map extent as the location
			mGeocodeParams.setSearchArea(calculateSearchArea());
		}
		if (TYPE.contentEquals(ROUTE)){
			// Create find parameters
			mGeocodeParams = new GeocodeParameters();
			// Set max results and spatial reference
			mGeocodeParams.setMaxResults(2);
			mGeocodeParams.setOutputSpatialReference(mMapView.getSpatialReference());
		}
		if (TYPE.contentEquals(REVERSE_GECODE)){
			mReverseGeocodeParams = new ReverseGeocodeParameters();
			mReverseGeocodeParams.setOutputSpatialReference(mMapView.getSpatialReference());

		}
	}

	private void findLocation(String address){

		final String TAG_LOCATOR_PROGRESS_DIALOG = "TAG_LOCATOR_PROGRESS_DIALOG";
		// Display progress dialog on UI thread
		final ProgressDialogFragment mProgressDialog = ProgressDialogFragment.newInstance(getActivity()
				.getString(R.string.address_search));
		// set the target fragment to receive cancel notification
		mProgressDialog.setTargetFragment(MapFragment.this,
				REQUEST_CODE_PROGRESS_DIALOG);
		mProgressDialog.show(getActivity().getFragmentManager(),
				TAG_LOCATOR_PROGRESS_DIALOG);

		// get the Location for the suggestion from the ArrayList
		for(SuggestResult result: mSuggestionsList) {
			if(address.matches(result.getLabel())) {
				// Prepare the GeocodeParams
				locatorParams(FIND_PLACE);
				final ListenableFuture<List<GeocodeResult>> locFuture = mLocator.geocodeAsync(result, mGeocodeParams);
				locFuture.addDoneListener(new Runnable() {
					@Override
					public void run() {
						try{

							List<GeocodeResult> locationResults = locFuture.get();
							GeocodeResult result = null;
							Point resultPoint = null;
							String resultAddress = null;
							if (locationResults != null && locationResults.size() > 0 ){
								// Get the first returned result
								result = locationResults.get(0);
								resultPoint = result.getDisplayLocation();
								resultAddress = result.getLabel();
							}

							// Dismiss progress dialog
							mProgressDialog.dismiss();
							if (resultPoint == null)
								return;

							// Display the result
							displaySearchResult(resultPoint,resultAddress);
							hideKeyboard();
						}catch (Exception e){
							Log.e(TAG, "Geocode error " + e.getMessage());
						}
					}
				});
			}
		}
	}


	protected void hideKeyboard() {
		// Hide soft keyboard
		mSearchview.clearFocus();
		InputMethodManager inputManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
		inputManager.hideSoftInputFromWindow(mSearchview.getWindowToken(), 0);
	}

	private void displaySearchResult(Point resultPoint, String address) {

		// create marker symbol to represent location
		Bitmap icon = BitmapFactory.decodeResource(getActivity().getResources(), R.drawable.pin_circle_red);
		BitmapDrawable drawable = new BitmapDrawable(getActivity().getResources(), icon);
		PictureMarkerSymbol resultSymbol = new PictureMarkerSymbol(drawable);
		// create graphic object for resulting location
		Graphic resultLocGraphic = new Graphic(resultPoint,
                    resultSymbol);
		// add graphic to location layer
		mLocationLayer.getGraphics().add(resultLocGraphic);

		mLocationLayerPoint = resultPoint;

		mLocationLayerPointString = address;

		// Zoom map to geocode result location

		//mMapView.zoomToResolution(resultPoint, 2);
		showSearchResultLayout(address);
	}
	/**
	 * Clears all graphics out of the location layer and the route layer.
	 */
	void resetGraphicsLayers() {
		mLocationLayer.getGraphics().clear();
		mRouteLayer.getGraphics().clear();
		mLocationLayerPoint = null;
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
			mRouteLayer =  new GraphicsOverlay();
		}
		mMapView.getGraphicsOverlays().add(mRouteLayer);
	}

	/**
	 * Starts tracking GPS location.
	 */
	void startLocationTracking(LocationDisplay.LocationChangedEvent locationChangedEvent) {

		mCompass.start();
		// Enabling the line below causes the map to not zoom in on my location
		//locDispMgr.setAutoPanMode(LocationDisplay.AutoPanMode.DEFAULT);
		//TODO: How to in Quartz?
		//locDispMgr.setAllowNetworkLocation(true);


		boolean locationChanged = false;
		Point wgsPoint = locationChangedEvent.getLocation().getPosition();

		if (!locationChanged) {
			locationChanged = true;
			showMyLocation(wgsPoint);
		}
		mIsLocationTracking = true;
	}

	private void showMyLocation(Point wgsPoint){
		if (mMapView.getSpatialReference() != null){
			mLocation = (Point) GeometryEngine.project(wgsPoint,
					mMapView.getSpatialReference());
			LinearUnit mapUnit = (LinearUnit) mMapView.getSpatialReference().getUnit();
			LinearUnit mile = new LinearUnit(LinearUnitId.MILES);

			double zoomWidth = mile.convertTo(mapUnit,SEARCH_RADIUS);
			double width = zoomWidth/10 ;
			double height = zoomWidth/10;

			Point envPoint = new Point(mLocation.getX()-width, mLocation.getY()-height, mMapView.getSpatialReference());
			Point envPointB = new Point(mLocation.getX()+width, mLocation.getY()+height, mMapView.getSpatialReference());

			Envelope zoomExtent = new Envelope(envPoint, envPointB);
			mMapView.setViewpointGeometryAsync(zoomExtent);
		}

	}

	/**
	 * Called from search_layout.xml when user presses Search button.
	 *
	 * @param address
	 */
	public void onSearchButtonClicked(final String address) {

		// Hide virtual keyboard
		hideKeyboard();

		// Remove any previous graphics and routes
		resetGraphicsLayers();
		// TODO: Un comment once Locator task is working
		executeLocatorTask(address);
	}

	/**
	 * Set up the search parameters and execute the Locator task.
	 *
	 * @param address
	 */
	private void executeLocatorTask(final String address) {

		// Create Locator parameters from single line address string
		final GeocodeParameters geoParameters = new GeocodeParameters();
		geoParameters.setMaxResults(2);

		// Use the centre of the current map extent as the find location point
		if (mLocation != null){
			geoParameters.setPreferredSearchLocation(mLocation);
		}

		// Set address spatial reference to match map
		SpatialReference sR = mMapView.getSpatialReference();
		geoParameters.setOutputSpatialReference(sR);

		geoParameters.setSearchArea(calculateSearchArea());

		// Execute async task to find the address
		mLocator.addDoneLoadingListener(new Runnable() {
			@Override
			public void run() {
				if (mLocator.getLoadStatus() == LoadStatus.LOADED){
					// Call geocodeAsync passing in an address
					final ListenableFuture<List<GeocodeResult>> geocodeFuture = mLocator.geocodeAsync(address, geoParameters);
					geocodeFuture.addDoneListener(new Runnable() {
						@Override
						public void run() {
							try {
								// Get the results of the async operation
								List<GeocodeResult> geocodeResults = geocodeFuture.get();

								if (geocodeResults.size() > 0) {
									// Use the first result - for example display on the map
									GeocodeResult topResult = geocodeResults.get(0);
									displaySearchResult(topResult.getDisplayLocation(),address);

									Log.i(TAG, topResult.getDisplayLocation().getX() + " " + topResult.getDisplayLocation().getY());

								}

							} catch (InterruptedException e) {
								// Deal with exception...
								e.printStackTrace();
							} catch (ExecutionException e) {
								// Deal with exception...
								e.printStackTrace();
							}
							// Done processing and can remove this listener.
							geocodeFuture.removeDoneListener(this);
						}
					});

				}else{
					Log.i(TAG, "Locator task error");
				}
			}
		});
		mLocator.loadAsync();
	}

	/**
	 * Calculate search geometry given current map extent
	 * @return Envelope representing an area double the
	 * size of the current map extent
	 *
	 */
	private Envelope calculateSearchArea(){
		SpatialReference sR = mMapView.getSpatialReference();

		// Get the current map space
		Geometry mapGeometry = mMapView.getCurrentViewpoint(Viewpoint.Type.BOUNDING_GEOMETRY).getTargetGeometry();
		Envelope mapExtent = mapGeometry.getExtent();
		// Calculate distance for find operation

		// assume map is in metres, other units wont work, double current
		// envelope
		double width = (mapExtent.getWidth() > 0) ? mapExtent
				.getWidth() * 2 : 10000;
		double height = (mapExtent.getHeight() > 0) ? mapExtent
				.getHeight() * 2 : 10000;
		double xMax = mapExtent.getXMax() + width;
		double xMin = mapExtent.getXMin() - width;
		double yMax = mapExtent.getYMax() + height;
		double yMin = mapExtent.getYMin() - height;

		return new Envelope(new Point(xMax,yMax, sR), new Point(xMin, yMin, sR));
	}



	/**
	 * Called by RoutingDialogFragment when user presses Get Route button.
	 *
	 * @param startPoint
	 *            String entered by user to define start point.
	 * @param endPoint
	 *            String entered by user to define end point.
	 * @return true if routing task executed, false if parameters rejected. If
	 *         this method rejects the parameters it must display an explanatory
	 *         Toast to the user before returning.
	 */
	//@Override
	public boolean onGetRoute(String startPoint, String endPoint) {
		// Check if we need a location fix
		if (startPoint.equals(getString(R.string.my_location))
				&& mLocation == null) {
			Toast.makeText(getActivity(),
					getString(R.string.need_location_fix), Toast.LENGTH_LONG)
					.show();
			return false;
		}
		// Remove any previous graphics and routes
		resetGraphicsLayers();

		// Do the routing
		// TODO: Uncomment when routing task is working
		getRoute(startPoint, endPoint);
		return true;
	}

	/**
	 * Set up Route Parameters to execute RouteTask
	 *
	 * @param start
	 * @param end
	 */

	@SuppressWarnings("unchecked")
	private void executeRoutingTask(String start, String end) {
		resetGraphicsLayers();
		// Create a list of start end point params
		List<String> routeParams = new ArrayList<>();

		// Add params to list
		routeParams.add(start);
		routeParams.add(end);

		// Execute async task to do the routing
		RouteAsyncTask routeTask = new RouteAsyncTask();
		routeTask.execute(routeParams);
		mPendingTask = routeTask;
	}

	//@Override
	public void onCancel(DialogInterface dialog) {
		// a pending task needs to be canceled
		if (mPendingTask != null) {
			mPendingTask.cancel(true);
		}
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
		TextView tv = (TextView) mSearchResult.findViewById(R.id.textView1);
		tv.setTypeface(null, Typeface.BOLD);
		tv.setText(address);

		// Adding the search result layout to the map container
		mMapContainer.addView(mSearchResult);

		// Setup the listener for the "cancel" icon
		ImageView iv_cancel = (ImageView) mSearchResult
				.findViewById(R.id.imageView3);
		iv_cancel.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// Remove the search result view
				mMapContainer.removeView(mSearchResult);

				suggestionClickFlag = false;
				// Add the search box view
				showSearchBoxLayout();

				// Remove all graphics from the map
				resetGraphicsLayers();

			}
		});

		// Set up the listener for the "Get Directions" icon
		ImageView iv_route = (ImageView) mSearchResult
				.findViewById(R.id.imageView2);
		iv_route.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				onGetRoute(getString(R.string.my_location),
						mLocationLayerPointString);
			}
		});

		// Add the compass after getting the height of the layout
		mSearchResult.getViewTreeObserver().addOnGlobalLayoutListener(
				new OnGlobalLayoutListener() {
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
		int index_from = mStartLocation.indexOf(",");
		int index_to = mEndLocation.indexOf(",");
		if (index_from != -1)
			mStartLocation = mStartLocation.substring(0, index_from);
		if (index_to != -1)
			mEndLocation = mEndLocation.substring(0, index_to);

		// Initialize the textvieww and display the text
		TextView tv_from = (TextView) mSearchResult.findViewById(R.id.tv_from);
		tv_from.setTypeface(null, Typeface.BOLD);
		tv_from.setText(" " + mStartLocation);

		TextView tv_to = (TextView) mSearchResult.findViewById(R.id.tv_to);
		tv_to.setTypeface(null, Typeface.BOLD);
		tv_to.setText(" " + mEndLocation);

		// Rounding off the values
		distance = Math.round(distance * 10.0) / 10.0;
		time = Math.round(time * 10.0) / 10.0;

		TextView tv_time = (TextView) mSearchResult.findViewById(R.id.tv_time);
		tv_time.setTypeface(null, Typeface.BOLD);
		tv_time.setText(time + " mins");

		TextView tv_dist = (TextView) mSearchResult.findViewById(R.id.tv_dist);
		tv_dist.setTypeface(null, Typeface.BOLD);
		tv_dist.setText(" (" + distance + " miles)");

		// Adding the layout
		mMapContainer.addView(mSearchResult);

		// Setup the listener for the "Cancel" icon
		ImageView iv_cancel = (ImageView) mSearchResult
				.findViewById(R.id.imageView3);
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
		ImageView iv_directions = (ImageView) mSearchResult
				.findViewById(R.id.imageView2);
		iv_directions.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				showDirectionsDialogFragment();
			}
		});

		// Add the compass after getting the height of the layout
		mSearchResult.getViewTreeObserver().addOnGlobalLayoutListener(
				new OnGlobalLayoutListener() {
					@Override
					public void onGlobalLayout() {
						addCompass(mSearchResult.getHeight());
						mSearchResult.getViewTreeObserver().removeGlobalOnLayoutListener(this);
					}

				});

	}
	private void getRoute(String origin, final String destination){
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
		Log.i(TAG, "Route task URL = " + routeTaskURL);

		mRouteTask = new RouteTask(routeTaskURL);

		try {
			// Geocode start position, or use My Location (from GPS)

			if (origin.equals(getString(R.string.my_location))) {
				mStartLocation = getString(R.string.my_location);

				// We have start location, now get the destination location
				Log.i(TAG, "Geocoding address: " + destination);
				final ListenableFuture<List<GeocodeResult>> geoFutureEnd = mLocator.geocodeAsync(destination, mGeocodeParams);
				geoFutureEnd.addDoneListener(new Runnable() {
					@Override
					public void run() {
						try {
							List<GeocodeResult> results = geoFutureEnd.get();
							if (results != null && results.size() > 0) {
								Point endPoint = results.get(0).getDisplayLocation();
								mEndLocation = destination;
								// Set spatial reference
								final SpatialReference ESPG_3857 = SpatialReference.create(102100);

								// We have the start and end, now get route

								//final Stop start = new Stop(new Point(-13041036.0527, 32.8725884));
								//final Stop end = new Stop(new Point(-117.252512, 32.8681446 ));

								final Stop start = new Stop(new Point(-1.3018598562659847E7,
										3863191.8817135547, ESPG_3857));
								final Stop end = new Stop(new Point(-1.3036911787723785E7, 3839935.706521739,
										ESPG_3857));

								SpatialReference sr = mMapView.getSpatialReference();
								Log.i(TAG, "Spatial reference = " + sr.getWKID());
								mRouteTask.addDoneLoadingListener(new Runnable() {
									@Override
									public void run() {
										LoadStatus status = mRouteTask.getLoadStatus();
										Log.i(TAG, status.name());
										if (status == LoadStatus.FAILED_TO_LOAD){

											Log.i(TAG,mRouteTask.getLoadError().getMessage());
											mRouteTask.retryLoadAsync();
										}else{
											final ListenableFuture<RouteParameters> routeTaskFuture = mRouteTask.generateDefaultParametersAsync();
											routeTaskFuture.addDoneListener(new Runnable() {

												@Override
												public void run() {
													try {
														Log.i(TAG, "Task loaded, getting route paramters");
														RouteParameters routeParameters = routeTaskFuture.get();
														routeParameters.getStops().add(start);
														routeParameters.getStops().add(end);
														routeParameters.setReturnDirections(true);
														routeParameters.setReturnRoutes(true);
														routeParameters.setDirectionsDistanceTextUnits(DirectionDistanceTextUnits.IMPERIAL);
														routeParameters.setOutputSpatialReference(ESPG_3857);

														final ListenableFuture<RouteResult> routeResFuture = mRouteTask.solveAsync(routeParameters);
														routeResFuture.addDoneListener(new Runnable() {
															@Override
															public void run() {
																try {
																	RouteResult routeResult = routeResFuture.get();
																	// Show route results
																	showRoute(routeResult);

																	// Dismiss progress dialog
																	mProgressDialog.dismiss();

																} catch (InterruptedException e) {
																	e.printStackTrace();
																} catch (ExecutionException e) {
																	e.printStackTrace();
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
								});
								mRouteTask.loadAsync();

							}else{
								Log.i(TAG, "Geocoding failed to return results for this address: " + destination);
							}
						} catch (InterruptedException e) {
							e.printStackTrace();
						} catch (ExecutionException e) {
							e.printStackTrace();
						}
					}
				});

			} else {
				// Get the start location
				final ListenableFuture<List<GeocodeResult>> geoFutureStart = mLocator.geocodeAsync(origin, mGeocodeParams);
				geoFutureStart.addDoneListener(new Runnable() {
					@Override
					public void run() {
						try {
							List<GeocodeResult> results = geoFutureStart.get();
							if (results != null && results.size() > 0) {
								Point origin = results.get(0).getDisplayLocation();
								String originLabel = results.get(0).getLabel();

								// Now we need to get the end location
								final ListenableFuture<List<GeocodeResult>> geoFutureEnd2 = mLocator.geocodeAsync(destination, mGeocodeParams);
								geoFutureEnd2.addDoneListener(new Runnable() {
									@Override
									public void run() {
										try {
											List<GeocodeResult> results = geoFutureEnd2.get();
											if (results != null && results.size() > 0) {
												Point end = results.get(0).getDisplayLocation();
												String endLabel = results.get(0).getLabel();

												// We have the start and end, now get route
											}
										} catch (InterruptedException e) {
											e.printStackTrace();
										} catch (ExecutionException e) {
											e.printStackTrace();
										}

									}
								});
							}
						} catch (InterruptedException e) {
							e.printStackTrace();
						} catch (ExecutionException e) {
							e.printStackTrace();
						}
					}
				});
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	/**
	 * Show progress dialog with the given message and name
	 * @param message String
	 * @param name String
     */
	private void showProgressDialog(String message, String name){
		mProgressDialog = ProgressDialogFragment.newInstance(message);
		// set the target fragment to receive cancel notification
		mProgressDialog.setTargetFragment(MapFragment.this,
				REQUEST_CODE_PROGRESS_DIALOG);
		mProgressDialog.show(getActivity().getFragmentManager(),
				name);
	}

	private void showRoute(RouteResult routeResult){
		// Get first item in list of routes provided by server
		Route route;
		try {
			route = routeResult.getRoutes().get(0);
			if( route.getTotalLength() == 0.0 ) {
				throw new Exception("Can not find the Route");
			}
		} catch (Exception e) {
			Toast.makeText(getActivity(), "We are sorry, we couldn't find the route. Please make " +
							"sure the Source and Destination are different or are connected by road",
					Toast.LENGTH_LONG).show();
			Log.e(TAG,e.getMessage());
			return;
		}


		// Create polyline graphic of the full route
		SimpleLineSymbol lineSymbol = new SimpleLineSymbol(SimpleLineSymbol.Style.SOLID,Color.RED, 2);
		Graphic routeGraphic = new Graphic(route.getRouteGeometry(), lineSymbol);

		// Create point graphic to mark start of route

		Geometry shape = routeGraphic.getGeometry();
		//Graphic startGraphic = createMarkerGraphic(startPoint, false);

		// Create point graphic to mark end of route
		//int endPointIndex = ((Polyline) routeGraphic.getGeometry())
		//		.getPointCount() - 1;
		//Point endPoint = ((Polyline) routeGraphic.getGeometry())
	//			.getPoint(endPointIndex);
		//Graphic endGraphic = createMarkerGraphic(endPoint, true);

		// Add these graphics to route layer
		mRouteLayer.getGraphics().add(routeGraphic);
		//mRouteLayer.getGraphics().add(startGraphic);



		// Zoom to the extent of the entire route with a padding
		mMapView.setViewpointGeometryWithPaddingAsync(shape, 100);

		// Save routing directions so user can display them later
		mRoutingDirections = route.getDirectionManeuvers();

		// Show Routing Result Layout
		showRoutingResultLayout(route.getTotalLength(),
				route.getTotalTime());
	}

	private Graphic createMarkerGraphic(Point point, boolean endPoint) {

		BitmapDrawable bitmapDrawable = (BitmapDrawable) ContextCompat.getDrawable(getActivity().getApplicationContext(),
				endPoint? R.drawable.pin_circle_red
						: R.drawable.pin_circle_red);
		PictureMarkerSymbol destinationSymbol = new PictureMarkerSymbol( bitmapDrawable);
		// NOTE: marker's bounds not set till marker is used to create
		// destinationSymbol
		float offsetY = convertPixelsToDp(getActivity(),
				bitmapDrawable != null ?  bitmapDrawable.getBounds().bottom : 0);
		destinationSymbol.setOffsetY(offsetY);
		return new Graphic(point, destinationSymbol);
	}

	/**
	 * This class provides an AsyncTask that performs a routing request on a
	 * background thread and displays the resultant route on the map on the UI
	 * thread.
	 */
	private class RouteAsyncTask extends
			AsyncTask<List<String>, Void, Void> {
		private static final String TAG_ROUTE_SEARCH_PROGRESS_DIALOG = "TAG_ROUTE_SEARCH_PROGRESS_DIALOG";

		private Exception mException;

		private ProgressDialogFragment mProgressDialog;

		public RouteAsyncTask() {
		}

		@Override
		protected void onPreExecute() {
			mProgressDialog = ProgressDialogFragment.newInstance(getActivity()
					.getString(R.string.route_search));
			// set the target fragment to receive cancel notification
			mProgressDialog.setTargetFragment(MapFragment.this,
					REQUEST_CODE_PROGRESS_DIALOG);
			mProgressDialog.show(getActivity().getFragmentManager(),
					TAG_ROUTE_SEARCH_PROGRESS_DIALOG);
		}


		@Override
		protected Void doInBackground(
				List<String>... params) {
			// Perform routing request on background thread
			mException = null;

			// Define route objects
			List<GeocodeResult> geocodeStartResult;
			List<GeocodeResult> geocodeEndResult;
			final Point startPoint;
			final Point endPoint;

			// Configure params
			locatorParams(ROUTE);
			String routeTaskURL = getString(R.string.routingservice_url);
			Log.i(TAG, "Route task URL = " + routeTaskURL);

			final RouteTask routeTask = new RouteTask(routeTaskURL);

			// Create a new locator to geocode start/end points;
			// by default uses ArcGIS online world geocoding service

			try {
				// Geocode start position, or use My Location (from GPS)
				String startParam = params[0].get(0);
				final String endParam = params[0].get(1);

				if (startParam.equals(getString(R.string.my_location))) {
					mStartLocation = getString(R.string.my_location);
					startPoint = (Point) GeometryEngine.project(mLocation, mWm);

					// We have start location, now get the destination location

					final ListenableFuture<List<GeocodeResult>> geoFutureEnd = mLocator.geocodeAsync(endParam, mGeocodeParams);
					geoFutureEnd.addDoneListener(new Runnable() {
						@Override
						public void run() {
							try {
								List<GeocodeResult> results = geoFutureEnd.get();
								if (results != null && results.size() > 0) {
									Point endPoint = results.get(0).getDisplayLocation();

									// We have the start and end, now get route

									final Stop start = new Stop(new Point(32.8725884,-117.2835913, mMapView.getSpatialReference() ));
									final Stop end = new Stop(new Point(32.8681446, -117.252512, mMapView.getSpatialReference() ));


									routeTask.addDoneLoadingListener(new Runnable() {
										@Override
										public void run() {
											LoadStatus status = routeTask.getLoadStatus();
											Log.i(TAG, status.name());
											if (status == LoadStatus.FAILED_TO_LOAD){
												Log.i(TAG,routeTask.getLoadError().getMessage());
												routeTask.retryLoadAsync();
											}else{
												final ListenableFuture<RouteParameters> routeTaskFuture = routeTask.generateDefaultParametersAsync();
												routeTaskFuture.addDoneListener(new Runnable() {

													@Override
													public void run() {
														try {

															RouteParameters routeParameters = routeTaskFuture.get();
															routeParameters.getStops().add(start);
															routeParameters.getStops().add(end);
															routeParameters.setReturnDirections(true);
															routeParameters.setReturnRoutes(true);
															routeParameters.setDirectionsDistanceTextUnits(DirectionDistanceTextUnits.IMPERIAL);
															routeParameters.setOutputSpatialReference(mMapView.getSpatialReference());

															final ListenableFuture<RouteResult> routeResFuture = routeTask.solveAsync(routeParameters);
															routeResFuture.addDoneListener(new Runnable() {
																@Override
																public void run() {
																	try {
																		RouteResult routeResult = routeResFuture.get();
																		if(routeResult.getRoutes()!= null && routeResult.getRoutes().size() > 0){
																			for ( Route route : routeResult.getRoutes()){
																				Log.i(TAG, route.getRouteName());
																			}
																		}

																	} catch (InterruptedException e) {
																		e.printStackTrace();
																	} catch (ExecutionException e) {
																		e.printStackTrace();
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
									});
									routeTask.loadAsync();

								}
							} catch (InterruptedException e) {
								e.printStackTrace();
							} catch (ExecutionException e) {
								e.printStackTrace();
							}
						}
					});

				} else {
					// Get the start location
					final ListenableFuture<List<GeocodeResult>> geoFutureStart = mLocator.geocodeAsync(startParam, mGeocodeParams);
					geoFutureStart.addDoneListener(new Runnable() {
						@Override
						public void run() {
							try {
								List<GeocodeResult> results = geoFutureStart.get();
								if (results != null && results.size() > 0) {
									Point origin = results.get(0).getDisplayLocation();
									String originLabel = results.get(0).getLabel();

									// Now we need to get the end location
									final ListenableFuture<List<GeocodeResult>> geoFutureEnd2 = mLocator.geocodeAsync(endParam, mGeocodeParams);
									geoFutureEnd2.addDoneListener(new Runnable() {
										@Override
										public void run() {
											try {
												List<GeocodeResult> results = geoFutureEnd2.get();
												if (results != null && results.size() > 0) {
													Point end = results.get(0).getDisplayLocation();
													String endLabel = results.get(0).getLabel();

													// We have the start and end, now get route
												}
											} catch (InterruptedException e) {
												e.printStackTrace();
											} catch (ExecutionException e) {
												e.printStackTrace();
											}

										}
									});
								}
							} catch (InterruptedException e) {
								e.printStackTrace();
							} catch (ExecutionException e) {
								e.printStackTrace();
							}
						}
					});

					if (isCancelled()) {
						return null;
					}
				}

			} catch (Exception e) {
				e.printStackTrace();
			}

			return null;
		}
		/*@Override
		protected void onPostExecute( ) {
			// Display results on UI thread
			mProgressDialog.dismiss();
			if (mException != null) {
				Log.w(TAG, "RouteSyncTask failed with:");
				mException.printStackTrace();
				Toast.makeText(getActivity(),
						getString(R.string.routingFailed), Toast.LENGTH_LONG)
						.show();
				return;
			}

			// Get first item in list of routes provided by server
			Route route;
			try {
				route = result.getRoutes().get(0);
				if( route.getTotalMiles() == 0.0 || route.getTotalKilometers() == 0.0 ) {
					throw new Exception("Can not find the Route");
				}
			} catch (Exception e) {
				Toast.makeText(getActivity(), "We are sorry, we couldn't find the route. Please make " +
								"sure the Source and Destination are different or are connected by road",
						Toast.LENGTH_LONG).show();
				Log.e(TAG,e.getMessage());
				return;
			}


			// Create polyline graphic of the full route
			SimpleLineSymbol lineSymbol = new SimpleLineSymbol(Color.RED, 2,
					STYLE.SOLID);
			Graphic routeGraphic = new Graphic(route.getRouteGraphic()
					.getGeometry(), lineSymbol);

			// Create point graphic to mark start of route
			Point startPoint = ((Polyline) routeGraphic.getGeometry())
					.getPoint(0);
			Graphic startGraphic = createMarkerGraphic(startPoint, false);

			// Create point graphic to mark end of route
			int endPointIndex = ((Polyline) routeGraphic.getGeometry())
					.getPointCount() - 1;
			Point endPoint = ((Polyline) routeGraphic.getGeometry())
					.getPoint(endPointIndex);
			Graphic endGraphic = createMarkerGraphic(endPoint, true);

			// Add these graphics to route layer
			mRouteLayer.addGraphics(new Graphic[]{routeGraphic, startGraphic,
					endGraphic});

			// Zoom to the extent of the entire route with a padding
			mMapView.setExtent(route.getEnvelope(), 100);

			// Save routing directions so user can display them later
			mRoutingDirections = route.getRoutingDirections();

			// Show Routing Result Layout
			showRoutingResultLayout(route.getTotalMiles(),
					route.getTotalMinutes());

		}

		Graphic createMarkerGraphic(Point point, boolean endPoint) {
			Drawable marker = getResources().getDrawable(
					endPoint ? R.drawable.pin_circle_blue
							: R.drawable.pin_circle_red);
			PictureMarkerSymbol destinationSymbol = new PictureMarkerSymbol(
					mMapView.getContext(), marker);
			// NOTE: marker's bounds not set till marker is used to create
			// destinationSymbol
			float offsetY = convertPixelsToDp(getActivity(),
					marker != null ? marker.getBounds().bottom : 0);
			destinationSymbol.setOffsetY(offsetY);
			return new Graphic(point, destinationSymbol);
		}*/
	}

	private void reverseGeocode(Point point){
		// Show progress dialog
		showProgressDialog(getString(R.string.reverse_geocoding), TAG_REVERSE_GEOCODING_PROGRESS_DIALOG);

		// Provision reverse geocode parameers
		locatorParams(REVERSE_GECODE);
		final ListenableFuture<List<GeocodeResult>> reverseFuture = mLocator.reverseGeocodeAsync(point, mReverseGeocodeParams);
		reverseFuture.addDoneListener(new Runnable() {
			@Override
			public void run() {
				try {
					mProgressDialog.dismiss();
					List<GeocodeResult> results = reverseFuture.get();
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
	 * Displays any gecoded results by add a
	 * PictureMarkerSymbol to the map's graphics layer.
	 * @param results A list of GeocodeResult items
     */
	private void showReverseGeocodeResult(List<GeocodeResult> results){

		if (results != null && results.size() > 0) {
			// Show the first item returned
			// Address string is saved for use in routing
			mLocationLayerPointString = results.get(0).getLabel();
			Point addressPoint = results.get(0).getDisplayLocation();
			// Draw marker on map.
			// create marker symbol to represent location

			BitmapDrawable bitmapDrawable = (BitmapDrawable) ContextCompat.getDrawable(getActivity().getApplicationContext(), R.drawable.pin_circle_red);
			PictureMarkerSymbol destinationSymbol = new PictureMarkerSymbol( bitmapDrawable);
			mLocationLayer.getGraphics().add(new Graphic(addressPoint, destinationSymbol));

			// center the map to result location
			mMapView.setViewpointCenterAsync(addressPoint);


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


	private class MapTouchListener extends DefaultMapViewOnTouchListener{
		/**
		 * Instantiates a new DrawingMapViewOnTouchListener with the specified context and MapView.
		 *
		 * @param context the application context from which to get the display metrics
		 * @param mapView the MapView on which to control touch events
		 */
		public MapTouchListener(Context context, MapView mapView) {
			super(context, mapView);
		}

		@Override
		public boolean onUp(MotionEvent motionEvent) {
			if (mLongPressEvent != null) {
				// This is the end of a long-press that will have displayed
				// the magnifier.
				// Perform reverse-geocoding of the point that was pressed

				android.graphics.Point mapPoint = new android.graphics.Point((int) motionEvent.getX(), (int) motionEvent.getY());
				Point point = mMapView.screenToLocation(mapPoint);
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
	}
}

