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
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.SearchView.OnSuggestionListener;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.esri.android.mapsapp.R.drawable;
import com.esri.android.mapsapp.R.id;
import com.esri.android.mapsapp.R.layout;
import com.esri.android.mapsapp.R.string;
import com.esri.android.mapsapp.account.AccountManager;
import com.esri.android.mapsapp.dialogs.ProgressDialogFragment;
import com.esri.android.mapsapp.location.DirectionsDialogFragment;
import com.esri.android.mapsapp.location.RoutingDialogFragment;
import com.esri.android.mapsapp.tools.Compass;
import com.esri.android.mapsapp.util.TaskExecutor;
import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.geometry.Envelope;
import com.esri.arcgisruntime.geometry.Geometry;
import com.esri.arcgisruntime.geometry.GeometryEngine;
import com.esri.arcgisruntime.geometry.LinearUnit;
import com.esri.arcgisruntime.geometry.LinearUnitId;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.SpatialReference;
import com.esri.arcgisruntime.loadable.LoadStatus;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.Map;
import com.esri.arcgisruntime.mapping.Viewpoint;
import com.esri.arcgisruntime.mapping.Viewpoint.Type;
import com.esri.arcgisruntime.mapping.view.DefaultMapViewOnTouchListener;
import com.esri.arcgisruntime.mapping.view.Graphic;
import com.esri.arcgisruntime.mapping.view.GraphicsOverlay;
import com.esri.arcgisruntime.mapping.view.LocationDisplay;
import com.esri.arcgisruntime.mapping.view.LocationDisplay.AutoPanMode;
import com.esri.arcgisruntime.mapping.view.LocationDisplay.LocationChangedEvent;
import com.esri.arcgisruntime.mapping.view.LocationDisplay.LocationChangedListener;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.esri.arcgisruntime.mapping.view.WrapAroundMode;
import com.esri.arcgisruntime.portal.Portal;
import com.esri.arcgisruntime.portal.PortalItem;
import com.esri.arcgisruntime.symbology.PictureMarkerSymbol;
import com.esri.arcgisruntime.symbology.SimpleLineSymbol;
import com.esri.arcgisruntime.symbology.SimpleLineSymbol.Style;
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
import com.esri.arcgisruntime.tasks.route.Stop;

/**
 * Implements the view that shows the map.
 */

public class MapFragment extends Fragment {

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

	private static final String REVERSE_GECODE = "Reverse Geocode";
	// The circle area specified by search_radius and input lat/lon serves
	// searching purpose.
	// It is also used to construct the extent which map zooms to after the
	// first
	// GPS fix is retrieved.
	private final static double SEARCH_RADIUS = 10;
	public static MapView mMapView;
	private static FrameLayout.LayoutParams mlayoutParams;
	// Margins parameters for search view
	private static final int TOP_MARGIN_SEARCH = 55;
	private static List<SuggestResult> mSuggestionsList;
	// Spatial references used for projecting points
	private final SpatialReference mWm = SpatialReference.create(102100);
	private final SpatialReference mEgs = SpatialReference.create(4326);
	private final java.util.Map<String, Point> suggestMap = new TreeMap<>();
	Compass mCompass;
	ViewGroup.LayoutParams compassFrameParams;
	int width, height;
	ViewGroup.LayoutParams gpsFrameParams;
	ImageButton navButton;
	DrawerLayout mDrawerLayout;
	ListView mDrawerList;
	private String mPortalItemId;
	private String mBasemapPortalItemId;
	private PortalItem mPortalItem;
	private FrameLayout mMapContainer;
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
	private MatrixCursor mSuggestionCursor;
	private MotionEvent mLongPressEvent;
	private ProgressDialogFragment mProgressDialog;
	@SuppressWarnings("rawtypes")
	// - using this only to cancel pending tasks in a generic way
	private AsyncTask mPendingTask;
	private View mSearchBox;
	private LocatorTask mLocator;
	private View mSearchResult;
	private LayoutInflater mInflater;
	private String mStartLocation, mEndLocation;
	private SuggestParameters suggestParams;
	private GeocodeParameters mGeocodeParams;
	private ReverseGeocodeParameters mReverseGeocodeParams;
	private RouteTask mRouteTask;
	private SpatialReference mapSpatialReference;
	private boolean suggestionClickFlag = false;
	private Point resultEndPoint;

	public MapFragment() {
		// make MapFragment ctor private - use newInstance() instead
	}

	public static MapFragment newInstance(String portalItemId, String basemapPortalItemId) {
		MapFragment mapFragment = new MapFragment();

		Bundle args = new Bundle();
		args.putString(MapFragment.KEY_PORTAL_ITEM_ID, portalItemId);
		args.putString(MapFragment.KEY_BASEMAP_ITEM_ID, basemapPortalItemId);

		mapFragment.setArguments(args);
		return mapFragment;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setHasOptionsMenu(true);

		// Restore any previous state
		Bundle args = savedInstanceState != null ? savedInstanceState : getArguments();
		if (args != null) {
			mIsLocationTracking = args.getBoolean(MapFragment.KEY_IS_LOCATION_TRACKING);
			mPortalItemId = args.getString(MapFragment.KEY_PORTAL_ITEM_ID);
			mBasemapPortalItemId = args.getString(MapFragment.KEY_BASEMAP_ITEM_ID);
		}


	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		mMapContainer = (FrameLayout) inflater.inflate(layout.map_fragment_layout, null);

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
				String defaultBaseMapURL = getString(string.default_basemap_url);
				Basemap basemap = new Basemap(defaultBaseMapURL);
				Map map = new Map(basemap);

				final MapView mapView = (MapView) mMapContainer.findViewById(id.map);
				mapView.setMap(map);

				setMapView(mapView);

				// Set up click listener on floating action button
				setClickListenerForFloatingActionButton(mapView);

				// Get an initial location on start up
				mLocationDisplay = mapView.getLocationDisplay();
				mLocationDisplay.addLocationChangedListener(new LocationChangedListener() {
					@Override
					public void onLocationChanged(LocationChangedEvent locationChangedEvent) {

						Point point = locationChangedEvent.getLocation().getPosition();

						if (point != null) {
							mLocation = point;
							showMyLocation(point);
							mLocationDisplay.removeLocationChangedListener(this);
						}

					}
				});
				mLocationDisplay.startAsync();

				// add graphics layer
				addGraphicLayers();

			}
		}

		return mMapContainer;
	}

	/**
	 * The floating action button toggles location tracking. When location
	 * tracking is on, the compass is shown in the upper right of the map view.
	 * When location tracking is off, the compass is shown if the map is not
	 * oriented north (0 degrees).
	 *
	 * @param mapView
	 */
	private void setClickListenerForFloatingActionButton(final MapView mapView) {
		final FloatingActionButton fab = (FloatingActionButton) mMapContainer.findViewById(id.fab);
		fab.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mLocationDisplay = mapView.getLocationDisplay();

				// Toggle location tracking on or off
				if (mIsLocationTracking) {
					fab.setImageResource(drawable.ic_action_compass_mode);
					mLocationDisplay.setAutoPanMode(AutoPanMode.DEFAULT);
					mCompass.start();
					mCompass.setVisibility(View.VISIBLE);
					mIsLocationTracking = false;
				} else {
					fab.setImageResource(android.R.drawable.ic_menu_mylocation);
					mLocationDisplay.startAsync();
					mLocationDisplay.addLocationChangedListener(new LocationChangedListener() {
						@Override
						public void onLocationChanged(LocationChangedEvent locationChangedEvent) {
							startLocationTracking(locationChangedEvent);

						}
					});
					if (MapFragment.mMapView.getMapRotation() != 0) {
						mCompass.setVisibility(View.VISIBLE);
						mCompass.setRotationAngle(MapFragment.mMapView.getMapRotation());
					} else {
						mCompass.setVisibility(View.GONE);
					}
					mIsLocationTracking = true;
					mLocationDisplay.setAutoPanMode(AutoPanMode.COMPASS);
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

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		switch (item.getItemId()) {

			default :
				return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onPause() {
		super.onPause();

		// Pause the MapView and stop the LocationDisplayManager to save battery
		if (MapFragment.mMapView != null) {
			Log.i(MapFragment.TAG, "In OnPause not null");
			 if (mIsLocationTracking) {
				 MapFragment.mMapView.getLocationDisplay().stop();
				 Log.i(MapFragment.TAG, "On Pause stopping location display");
			 }
			//mMapViewState = mMapView.


			MapFragment.mMapView.pause();
		}
	}

	@Override
	public void onResume() {
		super.onResume();

		// Start the MapView and LocationDisplayManager running again
		if (MapFragment.mMapView != null) {
			Log.i(MapFragment.TAG, "In OnResume and map view not null");
			MapFragment.mMapView.resume();
			// TODO: Anything else to do here?
			 if (mIsLocationTracking) {
				 MapFragment.mMapView.getLocationDisplay().startAsync();
				 Log.i(MapFragment.TAG, "On Resume started async for location display");
			 }
			if (mCompass != null){
				mCompass.start();
				Log.i(MapFragment.TAG,"On Resume started compass");
			}

		}
	}
	@Override
	public void onDestroyView(){
		super.onDestroyView();

		MapFragment.mMapView.dispose();
		MapFragment.mMapView = null;
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		outState.putBoolean(MapFragment.KEY_IS_LOCATION_TRACKING, mIsLocationTracking);
		outState.putString(MapFragment.KEY_PORTAL_ITEM_ID, mPortalItemId);
		outState.putString(MapFragment.KEY_BASEMAP_ITEM_ID, mBasemapPortalItemId);
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
				final Map webmap = new Map(portalItem);

				// load the WebMap that represents the basemap if one was
				// specified

				if (basemapPortalItemId != null && !basemapPortalItemId.isEmpty()) {
					PortalItem webPortalItem = new PortalItem(portal, basemapPortalItemId);
					Basemap basemapWebMap = new Basemap(webPortalItem);
					webmap.setBasemap(basemapWebMap);
				}

				if (webmap != null) {
					getActivity().runOnUiThread(new Runnable() {
						@Override
						public void run() {
							final MapView mapView = (MapView) mMapContainer.findViewById(id.map);
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

		MapFragment.mMapView = mapView;
		MapFragment.mMapView.setLogoVisible(true);
		MapFragment.mMapView.setWrapAroundMode(WrapAroundMode.ENABLE_WHEN_SUPPORTED);

		// Creating an inflater
		mInflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		// Setting up the layout params for the searchview and searchresult
		// layout
		MapFragment.mlayoutParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT,
				Gravity.LEFT | Gravity.TOP);
		int LEFT_MARGIN_SEARCH = 15;
		int RIGHT_MARGIN_SEARCH = 15;
		int BOTTOM_MARGIN_SEARCH = 0;

		MapFragment.mlayoutParams.setMargins(LEFT_MARGIN_SEARCH, MapFragment.TOP_MARGIN_SEARCH, RIGHT_MARGIN_SEARCH, BOTTOM_MARGIN_SEARCH);

		// Displaying the searchbox layout
		showSearchBoxLayout();

		// Set up location tracking
		mLocationDisplay = mapView.getLocationDisplay();
		mLocationDisplay.addLocationChangedListener(new LocationChangedListener() {
			@Override
			public void onLocationChanged(LocationChangedEvent locationChangedEvent) {
				startLocationTracking(locationChangedEvent);
			}
		});
		mLocationDisplay.startAsync();

		// Setup use of magnifier on a long press on the map
		MapFragment.mMapView.setMagnifierEnabled(true);
		mLongPressEvent = null;

		// Setup OnTouchListener to detect and act on long-press
		MapFragment.mMapView.setOnTouchListener(new MapTouchListener(getActivity().getApplicationContext(), MapFragment.mMapView));

		mLocator = new LocatorTask(getString(string.geocode_url));

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
		mCompass = new Compass(MapFragment.mMapView.getContext());
		mCompass.setAlpha(1f);
		mCompass.setRotationAngle(45);
		int HEIGHT = 240;
		int WIDTH = 240;
		compassFrameParams = new FrameLayout.LayoutParams(WIDTH, HEIGHT, Gravity.RIGHT);

		int TOP_MARGIN_COMPASS = MapFragment.TOP_MARGIN_SEARCH + height + 45;

		int LEFT_MARGIN_COMPASS = 0;
		int BOTTOM_MARGIN_COMPASS = 0;
		int RIGHT_MARGIN_COMPASS = 0;
		((ViewGroup.MarginLayoutParams) compassFrameParams).setMargins(LEFT_MARGIN_COMPASS, TOP_MARGIN_COMPASS,
				RIGHT_MARGIN_COMPASS, BOTTOM_MARGIN_COMPASS);

		mCompass.setLayoutParams(compassFrameParams);

		mCompass.setVisibility(View.GONE);

		mCompass.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				mCompass.setVisibility(View.GONE);
				MapFragment.mMapView.setRotation(0f);
				MapFragment.mMapView.setRotation(0f);
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
		routingFrag.setRoutingDialogListener(new RoutingDialogFragment.RoutingDialogListener() {
			@Override
			public boolean onGetRoute(String startPoint, String endPoint) {
				return false;
			}
		});
		Bundle arguments = new Bundle();
		if (mLocationLayerPoint != null) {
			arguments.putString(RoutingDialogFragment.ARG_END_POINT_DEFAULT, mLocationLayerPointString);
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
		frag.setRoutingDirections(mRoutingDirections, new DirectionsDialogFragment.DirectionsDialogListener() {

			@Override
			public void onDirectionSelected(int position) {
				// User has selected a particular direction -
				// dismiss the dialog and
				// zoom to the selected direction
				frag.dismiss();
				DirectionManeuver direction = mRoutingDirections.get(position);

				// create a viewpoint from envelope
				Viewpoint vp = new Viewpoint(direction.getGeometry().getExtent());
				MapFragment.mMapView.setViewpoint(vp);
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
		mSearchBox = mInflater.inflate(layout.searchview, null);
		// Inflate navigation drawer button on SearchView
		navButton = (ImageButton) mSearchBox.findViewById(id.btn_nav_menu);
		// Get the navigation drawer from Activity
		mDrawerLayout = (DrawerLayout) getActivity().findViewById(id.maps_app_activity_drawer_layout);
		mDrawerList = (ListView) getActivity().findViewById(id.maps_app_activity_left_drawer);

		// Set click listener to open/close drawer
		navButton.setOnClickListener(new View.OnClickListener() {
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
		mSearchBox.setLayoutParams(MapFragment.mlayoutParams);

		// Initializing the searchview and the image view
		mSearchview = (SearchView) mSearchBox.findViewById(id.searchView1);

		ImageView iv_route = (ImageView) mSearchBox.findViewById(id.imageView1);

		mSearchview.setIconifiedByDefault(false);
		mSearchview.setQueryHint(MapFragment.SEARCH_HINT);

		applySuggestionCursor();

		// navButton = (Button)mSearchBox.findViewById(R.id.navbutton);

		// Adding the layout to the map conatiner
		mMapContainer.addView(mSearchBox);

		// Setup the listener for the route onclick
		iv_route.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				showRoutingDialogFragment();

			}
		});

		// Setup the listener when the search button is pressed on the keyboard
		mSearchview.setOnQueryTextListener(new SearchView.OnQueryTextListener() {

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
		mSearchBox.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
			@Override
			public void onGlobalLayout() {
				addCompass(mSearchBox.getHeight());
				mSearchBox.getViewTreeObserver().removeOnGlobalLayoutListener(this);
			}

		});

		mSearchview.setOnSuggestionListener(new OnSuggestionListener() {

			@Override
			public boolean onSuggestionSelect(int position) {
				return false;
			}

			@Override
			public boolean onSuggestionClick(int position) {
				// Obtain the content of the selected suggesting place via
				// cursor
				MatrixCursor cursor = (MatrixCursor) mSearchview.getSuggestionsAdapter().getItem(position);
				int indexColumnSuggestion = cursor.getColumnIndex(MapFragment.COLUMN_NAME_ADDRESS);
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
		String[] cols = {BaseColumns._ID, MapFragment.COLUMN_NAME_ADDRESS, MapFragment.COLUMN_NAME_X, MapFragment.COLUMN_NAME_Y};
		mSuggestionCursor = new MatrixCursor(cols);
	}

	/**
	 * Set the suggestion cursor to an Adapter then set it to the search view
	 */
	private void applySuggestionCursor() {
		String[] cols = {MapFragment.COLUMN_NAME_ADDRESS};
		int[] to = {id.suggestion_item_address};

		SimpleCursorAdapter mSuggestionAdapter = new SimpleCursorAdapter(MapFragment.mMapView.getContext(),
				layout.search_suggestion_item, mSuggestionCursor, cols, to, 0);
		mSearchview.setSuggestionsAdapter(mSuggestionAdapter);
		mSuggestionAdapter.notifyDataSetChanged();
	}

	/**
	 * Provide a character by character suggestions for the search string
	 *
	 * @param query
	 *            String typed so far by the user to fetch the suggestions
	 */
	private void getSuggestions(String query) {
		if (query == null || query.isEmpty()) {
			return;
		}
		// Initialize the locatorSugestion parameters
		locatorParams(MapFragment.SUGGEST_PLACE);

		final ListenableFuture<List<SuggestResult>> suggestionsFuture = mLocator.suggestAsync(query, suggestParams);
		suggestionsFuture.addDoneListener(new Runnable() {
			@Override
			public void run() {
				try {
					// Get the suggestions returned from the locator task
					MapFragment.mSuggestionsList = suggestionsFuture.get();
					List<String> suggestedAddresses = new ArrayList<>(MapFragment.mSuggestionsList.size());

					if (MapFragment.mSuggestionsList != null && MapFragment.mSuggestionsList.size() > 0) {
						initSuggestionCursor();
						int key = 0;
						for (SuggestResult result : MapFragment.mSuggestionsList) {
							suggestedAddresses.add(result.getLabel());
							// Add the suggestion results to the cursor
							mSuggestionCursor.addRow(new Object[]{key++, result.getLabel(), "0", "0"});
						}
						applySuggestionCursor();
					}

				} catch (Exception e) {
					Log.e(MapFragment.TAG, "No suggested places found");
					Log.e(MapFragment.TAG, "Get suggestions error " + e.getMessage());
				}
			}
		});
	}

	/**
	 * Initialize SuggestionParameters or GeocodeParameters
	 *
	 * @param TYPE
	 *            A String determining thr type of parameters to be initialized
	 */

	private void locatorParams(String TYPE) {
		if (TYPE.contentEquals(MapFragment.SUGGEST_PLACE)) {
			// Create suggestion parameters
			suggestParams = new SuggestParameters();
			suggestParams.setSearchArea(calculateSearchArea());
		}
		if (TYPE.contentEquals(MapFragment.FIND_PLACE)) {
			// Create find parameters
			mGeocodeParams = new GeocodeParameters();
			// Set max results and spatial reference
			mGeocodeParams.setMaxResults(2);
			mGeocodeParams.setOutputSpatialReference(MapFragment.mMapView.getSpatialReference());
			// Use the centre of the current map extent as the location
			mGeocodeParams.setSearchArea(calculateSearchArea());
		}
		if (TYPE.contentEquals(MapFragment.ROUTE)) {
			// Create find parameters
			mGeocodeParams = new GeocodeParameters();
			// Set max results and spatial reference
			mGeocodeParams.setMaxResults(2);
			mGeocodeParams.setOutputSpatialReference(MapFragment.mMapView.getSpatialReference());
		}
		if (TYPE.contentEquals(MapFragment.REVERSE_GECODE)) {
			mReverseGeocodeParams = new ReverseGeocodeParameters();
			mReverseGeocodeParams.setOutputSpatialReference(MapFragment.mMapView.getSpatialReference());

		}
	}

	private void findLocation(String address) {

		final String TAG_LOCATOR_PROGRESS_DIALOG = "TAG_LOCATOR_PROGRESS_DIALOG";
		// Display progress dialog on UI thread
		final ProgressDialogFragment mProgressDialog = ProgressDialogFragment
				.newInstance(getActivity().getString(string.address_search));
		// set the target fragment to receive cancel notification
		mProgressDialog.setTargetFragment(this, MapFragment.REQUEST_CODE_PROGRESS_DIALOG);
		mProgressDialog.show(getActivity().getFragmentManager(), TAG_LOCATOR_PROGRESS_DIALOG);

		// get the Location for the suggestion from the ArrayList
		for (SuggestResult result : MapFragment.mSuggestionsList) {
			if (address.matches(result.getLabel())) {
				// Prepare the GeocodeParams
				locatorParams(MapFragment.FIND_PLACE);
				final ListenableFuture<List<GeocodeResult>> locFuture = mLocator.geocodeAsync(result, mGeocodeParams);
				locFuture.addDoneListener(new Runnable() {
					@Override
					public void run() {
						try {

							List<GeocodeResult> locationResults = locFuture.get();
							GeocodeResult result = null;
							Point resultPoint = null;
							String resultAddress = null;
							if (locationResults != null && locationResults.size() > 0) {
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
							displaySearchResult(resultPoint, resultAddress);
							hideKeyboard();
						} catch (Exception e) {
							Log.e(MapFragment.TAG, "Geocode error " + e.getMessage());
						}
					}
				});
			}
		}
	}

	protected void hideKeyboard() {
		// Hide soft keyboard
		mSearchview.clearFocus();
		InputMethodManager inputManager = (InputMethodManager) getActivity()
				.getSystemService(Context.INPUT_METHOD_SERVICE);
		inputManager.hideSoftInputFromWindow(mSearchview.getWindowToken(), 0);
	}

	private void displaySearchResult(Point resultPoint, String address) {

		// create marker symbol to represent location
		Bitmap icon = BitmapFactory.decodeResource(getActivity().getResources(), drawable.pin_circle_red);
		BitmapDrawable drawable = new BitmapDrawable(getActivity().getResources(), icon);
		PictureMarkerSymbol resultSymbol = new PictureMarkerSymbol(drawable);
		// create graphic object for resulting location
		Graphic resultLocGraphic = new Graphic(resultPoint, resultSymbol);
		// add graphic to location layer
		mLocationLayer.getGraphics().add(resultLocGraphic);

		mLocationLayerPoint = resultPoint;

		mLocationLayerPointString = address;

		// Zoom map to geocode result location

		// mMapView.zoomToResolution(resultPoint, 2);
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

		MapFragment.mMapView.getGraphicsOverlays().add(mLocationLayer);

		// Add the route graphic layer
		if (mRouteLayer == null) {
			mRouteLayer = new GraphicsOverlay();
		}
		MapFragment.mMapView.getGraphicsOverlays().add(mRouteLayer);
	}

	/**
	 * Starts tracking GPS location.
	 */
	void startLocationTracking(LocationChangedEvent locationChangedEvent) {

		mCompass.start();
		// Enabling the line below causes the map to not zoom in on my location
		// locDispMgr.setAutoPanMode(LocationDisplay.AutoPanMode.DEFAULT);
		// TODO: How to in Quartz?
		// locDispMgr.setAllowNetworkLocation(true);

		boolean locationChanged = false;
		Point wgsPoint = locationChangedEvent.getLocation().getPosition();

		if (!locationChanged) {
			locationChanged = true;
			showMyLocation(wgsPoint);
		}
		mIsLocationTracking = true;
	}

	/**
	 * Zoom the map to the current location, if set.
	 *
	 * @param wgsPoint
	 *            - Point representing current location
	 */
	private void showMyLocation(Point wgsPoint) {
		if (MapFragment.mMapView.getSpatialReference() != null && mLocation != null) {
			mLocation = (Point) GeometryEngine.project(wgsPoint, MapFragment.mMapView.getSpatialReference());
			LinearUnit mapUnit = (LinearUnit) MapFragment.mMapView.getSpatialReference().getUnit();
			LinearUnit mile = new LinearUnit(LinearUnitId.MILES);

			double zoomWidth = mile.convertTo(mapUnit, MapFragment.SEARCH_RADIUS);
			double width = zoomWidth / 10;
			double height = zoomWidth / 10;

			Point envPoint = new Point(mLocation.getX() - width, mLocation.getY() - height,
					MapFragment.mMapView.getSpatialReference());
			Point envPointB = new Point(mLocation.getX() + width, mLocation.getY() + height,
					MapFragment.mMapView.getSpatialReference());

			Envelope zoomExtent = new Envelope(envPoint, envPointB);
			MapFragment.mMapView.setViewpointGeometryAsync(zoomExtent);
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
		if (mLocation != null) {
			geoParameters.setPreferredSearchLocation(mLocation);
		}

		// Set address spatial reference to match map
		SpatialReference sR = MapFragment.mMapView.getSpatialReference();
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
									GeocodeResult topResult = geocodeResults.get(0);
									displaySearchResult(topResult.getDisplayLocation(), address);

									Log.i(MapFragment.TAG, topResult.getDisplayLocation().getX() + " "
											+ topResult.getDisplayLocation().getY());

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

				} else {
					Log.i(MapFragment.TAG, "Locator task error");
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
		SpatialReference sR = MapFragment.mMapView.getSpatialReference();

		// Get the current map space
		Geometry mapGeometry = MapFragment.mMapView.getCurrentViewpoint(Type.BOUNDING_GEOMETRY).getTargetGeometry();
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
	// @Override
	public void onGetRoute(String startPoint, String endPoint) {
		// Check if we need a location fix
		if (startPoint.equals(getString(string.my_location)) && mLocation == null) {
			Toast.makeText(getActivity(), getString(string.need_location_fix), Toast.LENGTH_LONG).show();

		}
		// Remove any previous graphics and routes
		resetGraphicsLayers();

		// Do the routing
		getRoute(startPoint, endPoint);
	}


	// @Override
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
		mSearchResult = mInflater.inflate(layout.search_result, null);

		// Set layout parameters
		mSearchResult.setLayoutParams(MapFragment.mlayoutParams);

		// Initialize the textview and set its text
		TextView tv = (TextView) mSearchResult.findViewById(id.textView1);
		tv.setTypeface(null, Typeface.BOLD);
		tv.setText(address);

		// Adding the search result layout to the map container
		mMapContainer.addView(mSearchResult);

		// Setup the listener for the "cancel" icon
		ImageView iv_cancel = (ImageView) mSearchResult.findViewById(id.imageView3);
		iv_cancel.setOnClickListener(new View.OnClickListener() {

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
		ImageView iv_route = (ImageView) mSearchResult.findViewById(id.imageView2);
		iv_route.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				onGetRoute(getString(string.my_location), mLocationLayerPointString);
			}
		});

		// Add the compass after getting the height of the layout
		mSearchResult.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
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
		mSearchResult = mInflater.inflate(layout.routing_result, null);

		mSearchResult.setLayoutParams(MapFragment.mlayoutParams);

		// Shorten the start and end location by finding the first comma if
		// present
		int index_from = mStartLocation.indexOf(",");
		int index_to = mEndLocation.indexOf(",");
		if (index_from != -1)
			mStartLocation = mStartLocation.substring(0, index_from);
		if (index_to != -1)
			mEndLocation = mEndLocation.substring(0, index_to);

		// Initialize the textvieww and display the text
		TextView tv_from = (TextView) mSearchResult.findViewById(id.tv_from);
		tv_from.setTypeface(null, Typeface.BOLD);
		tv_from.setText(" " + mStartLocation);

		TextView tv_to = (TextView) mSearchResult.findViewById(id.tv_to);
		tv_to.setTypeface(null, Typeface.BOLD);
		tv_to.setText(" " + mEndLocation);

		// Rounding off the values
		distance = Math.round(distance * 10.0) / 10.0;
		time = Math.round(time * 10.0) / 10.0;

		TextView tv_time = (TextView) mSearchResult.findViewById(id.tv_time);
		tv_time.setTypeface(null, Typeface.BOLD);
		tv_time.setText(time + " mins");

		TextView tv_dist = (TextView) mSearchResult.findViewById(id.tv_dist);
		tv_dist.setTypeface(null, Typeface.BOLD);
		tv_dist.setText(" (" + distance + " miles)");

		// Adding the layout
		mMapContainer.addView(mSearchResult);

		// Setup the listener for the "Cancel" icon
		ImageView iv_cancel = (ImageView) mSearchResult.findViewById(id.imageView3);
		iv_cancel.setOnClickListener(new View.OnClickListener() {

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
		ImageView iv_directions = (ImageView) mSearchResult.findViewById(id.imageView2);
		iv_directions.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				showDirectionsDialogFragment();
			}
		});

		// Add the compass after getting the height of the layout
		mSearchResult.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
			@Override
			public void onGlobalLayout() {
				addCompass(mSearchResult.getHeight());
				mSearchResult.getViewTreeObserver().removeGlobalOnLayoutListener(this);
			}

		});

	}
	private void getRoute(String origin, final String destination) {
		// Show the progress dialog while getting route info
		showProgressDialog(getString(string.route_search), MapFragment.TAG_ROUTE_SEARCH_PROGRESS_DIALOG);

		// Configure geocode params specific for routing
		locatorParams(MapFragment.ROUTE);

		// Assign the appropriate routing url.
		// Note that the version (e.g. 10.0, 10.3)
		// of the server publishing the service does
		// make a difference.
		// Read more about routing services (here)
		String routeTaskURL = getString(string.routingservice_url);
		Log.i(MapFragment.TAG, "Route task URL = " + routeTaskURL);

		mRouteTask = new RouteTask(routeTaskURL);

		try {
			// Geocode start position, or use My Location (from GPS)

			if (origin.equals(getString(string.my_location))) {
				mStartLocation = getString(string.my_location);

				// We have start location, now get the destination location
				Log.i(MapFragment.TAG, "Geocoding address: " + destination);
				final ListenableFuture<List<GeocodeResult>> geoFutureEnd = mLocator.geocodeAsync(destination,
						mGeocodeParams);
				geoFutureEnd.addDoneListener(new Runnable() {
					@Override
					public void run() {
						try {
							List<GeocodeResult> results = geoFutureEnd.get();
							if (results != null && results.size() > 0) {
								//Point endPoint = results.get(0).getDisplayLocation();
								mEndLocation = destination;
								// Set spatial reference
								final SpatialReference ESPG_3857 = SpatialReference.create(102100);

								// We have the start and end, now get route
								mLocation = new Point(-1.3018598562659847E7, 3863191.8817135547, ESPG_3857);
								final Point endPoint = new Point(-1.3036911787723785E7, 3839935.706521739, ESPG_3857);
								final Stop start = new Stop(mLocation);
								final Stop end = new Stop(endPoint);

								SpatialReference sr = MapFragment.mMapView.getSpatialReference();
								mRouteTask.addDoneLoadingListener(new Runnable() {
									@Override
									public void run() {
										LoadStatus status = mRouteTask.getLoadStatus();
										Log.i(MapFragment.TAG, status.name());
										if (status == LoadStatus.FAILED_TO_LOAD) {

											Log.i(MapFragment.TAG, mRouteTask.getLoadError().getMessage());
											mRouteTask.retryLoadAsync();
										} else {
											final ListenableFuture<RouteParameters> routeTaskFuture = mRouteTask
													.generateDefaultParametersAsync();
											routeTaskFuture.addDoneListener(new Runnable() {

												@Override
												public void run() {
													try {
														Log.i(MapFragment.TAG, "Task loaded, getting route paramters");
														RouteParameters routeParameters = routeTaskFuture.get();
														routeParameters.getStops().add(start);
														routeParameters.getStops().add(end);
														routeParameters.setReturnDirections(true);
														routeParameters.setReturnRoutes(true);
														routeParameters.setDirectionsDistanceTextUnits(
																DirectionDistanceTextUnits.IMPERIAL);
														routeParameters.setOutputSpatialReference(ESPG_3857);

														final ListenableFuture<RouteResult> routeResFuture = mRouteTask
																.solveAsync(routeParameters);
														routeResFuture.addDoneListener(new Runnable() {
															@Override
															public void run() {
																try {
																	RouteResult routeResult = routeResFuture.get();
																	// Show
																	// route
																	// results
																	showRoute(routeResult, mLocation, endPoint);

																	// Dismiss
																	// progress
																	// dialog
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

							} else {
								Log.i(MapFragment.TAG, "Geocoding failed to return results for this address: " + destination);
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
				final ListenableFuture<List<GeocodeResult>> geoFutureStart = mLocator.geocodeAsync(origin,
						mGeocodeParams);
				geoFutureStart.addDoneListener(new Runnable() {
					@Override
					public void run() {
						try {
							List<GeocodeResult> results = geoFutureStart.get();
							if (results != null && results.size() > 0) {
								Point origin = results.get(0).getDisplayLocation();
								String originLabel = results.get(0).getLabel();

								// Now we need to get the end location
								final ListenableFuture<List<GeocodeResult>> geoFutureEnd2 = mLocator
										.geocodeAsync(destination, mGeocodeParams);
								geoFutureEnd2.addDoneListener(new Runnable() {
									@Override
									public void run() {
										try {
											List<GeocodeResult> results = geoFutureEnd2.get();
											if (results != null && results.size() > 0) {
												Point end = results.get(0).getDisplayLocation();
												String endLabel = results.get(0).getLabel();

												// We have the start and end,
												// now get route
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
	 * 
	 * @param message
	 *            String
	 * @param name
	 *            String
	 */
	private void showProgressDialog(String message, String name) {
		mProgressDialog = ProgressDialogFragment.newInstance(message);
		// set the target fragment to receive cancel notification
		mProgressDialog.setTargetFragment(this, MapFragment.REQUEST_CODE_PROGRESS_DIALOG);
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
			Log.e(MapFragment.TAG, e.getMessage());
			return;
		}

		// Create polyline graphic of the full route
		SimpleLineSymbol lineSymbol = new SimpleLineSymbol(Style.SOLID, Color.RED, 2);
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
		MapFragment.mMapView.setViewpointGeometryWithPaddingAsync(shape, 100);

		// Save routing directions so user can display them later
		mRoutingDirections = route.getDirectionManeuvers();

		// Show Routing Result Layout
		showRoutingResultLayout(route.getTotalLength(), route.getTotalTime());
	}

	private Graphic createMarkerGraphic(Point point, boolean endPoint) {

		BitmapDrawable bitmapDrawable = (BitmapDrawable) ContextCompat.getDrawable(
				getActivity().getApplicationContext(),
				endPoint ? drawable.pin_circle_blue : drawable.pin_circle_red);
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
		showProgressDialog(getString(string.reverse_geocoding), MapFragment.TAG_REVERSE_GEOCODING_PROGRESS_DIALOG);

		// Provision reverse geocode parameers
		locatorParams(MapFragment.REVERSE_GECODE);
		final ListenableFuture<List<GeocodeResult>> reverseFuture = mLocator.reverseGeocodeAsync(point,
				mReverseGeocodeParams);
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
			Point addressPoint = results.get(0).getDisplayLocation();
			// Draw marker on map.
			// create marker symbol to represent location

			BitmapDrawable bitmapDrawable = (BitmapDrawable) ContextCompat
					.getDrawable(getActivity().getApplicationContext(), drawable.pin_circle_red);
			PictureMarkerSymbol destinationSymbol = new PictureMarkerSymbol(bitmapDrawable);
			mLocationLayer.getGraphics().add(new Graphic(addressPoint, destinationSymbol));

			// center the map to result location
			MapFragment.mMapView.setViewpointCenterAsync(addressPoint);

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
				// the magnifier.
				// Perform reverse-geocoding of the point that was pressed

				android.graphics.Point mapPoint = new android.graphics.Point((int) motionEvent.getX(),
						(int) motionEvent.getY());
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
