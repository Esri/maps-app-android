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
import java.util.Map;
import java.util.concurrent.Callable;

import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import com.esri.android.map.Callout;
import com.esri.android.map.CalloutStyle;
import com.esri.android.map.GraphicsLayer;
import com.esri.android.map.LocationDisplayManager;
import com.esri.android.map.LocationDisplayManager.AutoPanMode;
import com.esri.android.map.MapOnTouchListener;
import com.esri.android.map.MapView;
import com.esri.android.map.event.OnStatusChangedListener;
import com.esri.android.mapsapp.account.AccountManager;
import com.esri.android.mapsapp.basemaps.BasemapsDialogFragment;
import com.esri.android.mapsapp.basemaps.BasemapsDialogFragment.BasemapsDialogListener;
import com.esri.android.mapsapp.dialogs.ProgressDialogFragment;
import com.esri.android.mapsapp.location.DirectionsDialogFragment;
import com.esri.android.mapsapp.location.DirectionsDialogFragment.DirectionsDialogListener;
import com.esri.android.mapsapp.location.RoutingDialogFragment;
import com.esri.android.mapsapp.location.RoutingDialogFragment.RoutingDialogListener;
import com.esri.android.mapsapp.tools.MeasuringTool;
import com.esri.android.mapsapp.util.TaskExecutor;
import com.esri.android.mapsapp.util.UiUtils;
import com.esri.core.geometry.Envelope;
import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.LinearUnit;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.Polyline;
import com.esri.core.geometry.SpatialReference;
import com.esri.core.geometry.Unit;
import com.esri.core.map.Graphic;
import com.esri.core.portal.BaseMap;
import com.esri.core.portal.Portal;
import com.esri.core.portal.WebMap;
import com.esri.core.symbol.PictureMarkerSymbol;
import com.esri.core.symbol.SimpleFillSymbol;
import com.esri.core.symbol.SimpleLineSymbol;
import com.esri.core.symbol.SimpleLineSymbol.STYLE;
import com.esri.core.symbol.SimpleMarkerSymbol;
import com.esri.core.tasks.geocode.Locator;
import com.esri.core.tasks.geocode.LocatorFindParameters;
import com.esri.core.tasks.geocode.LocatorGeocodeResult;
import com.esri.core.tasks.geocode.LocatorReverseGeocodeResult;
import com.esri.core.tasks.na.NAFeaturesAsFeature;
import com.esri.core.tasks.na.Route;
import com.esri.core.tasks.na.RouteDirection;
import com.esri.core.tasks.na.RouteParameters;
import com.esri.core.tasks.na.RouteResult;
import com.esri.core.tasks.na.RouteTask;
import com.esri.core.tasks.na.StopGraphic;

/**
 * Implements the view that shows the map.
 */
public class MapFragment extends Fragment implements BasemapsDialogListener,
		RoutingDialogListener, OnCancelListener {
	public static final String TAG = MapFragment.class.getSimpleName();

	private static final String KEY_PORTAL_ITEM_ID = "KEY_PORTAL_ITEM_ID";

	private static final String KEY_BASEMAP_ITEM_ID = "KEY_BASEMAP_ITEM_ID";

	private static final String KEY_IS_LOCATION_TRACKING = "IsLocationTracking";

	private static final int REQUEST_CODE_PROGRESS_DIALOG = 1;

	// The circle area specified by search_radius and input lat/lon serves
	// searching purpose.
	// It is also used to construct the extent which map zooms to after the
	// first
	// GPS fix is retrieved.
	private final static double SEARCH_RADIUS = 10;

	private String mPortalItemId;

	private String mBasemapPortalItemId;

	private FrameLayout mMapContainer;

	private MapView mMapView;

	private CalloutStyle mCalloutStyle;

	private int mMaxCalloutWidth;

	private int mMaxCalloutHeight;

	private String mMapViewState;

	// GPS location tracking
	private boolean mIsLocationTracking;

	private Point mLocation = null;

	// Graphics layer to show geocode and reverse geocode results
	private GraphicsLayer mLocationLayer;

	private Point mLocationLayerPoint;

	private String mLocationLayerPointString;

	// Graphics layer to show routes
	private GraphicsLayer mRouteLayer;

	private List<RouteDirection> mRoutingDirections;

	private MenuItem mActionItemDirections;

	// Spatial references used for projecting points
	private final SpatialReference mWm = SpatialReference.create(102100);

	private final SpatialReference mEgs = SpatialReference.create(4326);

	private EditText mSearchEditText;

	private MotionEvent mLongPressEvent;

	@SuppressWarnings("rawtypes")
	// - using this only to cancel pending tasks in a generic way
	private AsyncTask mPendingTask;

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

		mCalloutStyle = new CalloutStyle();
		mCalloutStyle.setCornerCurve(getActivity().getResources()
				.getDimensionPixelSize(R.dimen.place_callout_corner_curve));
		mCalloutStyle.setFrameColor(getActivity().getResources().getColor(
				R.color.place_callout_frame_color));
		mCalloutStyle.setAnchor(Callout.ANCHOR_POSITION_LOWER_MIDDLE);

		mMaxCalloutWidth = getActivity().getResources().getDimensionPixelSize(
				R.dimen.place_callout_max_width);
		mMaxCalloutHeight = getActivity().getResources().getDimensionPixelSize(
				R.dimen.place_callout_max_height);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
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
				MapView mapView = new MapView(getActivity(), defaultBaseMapURL,
						"", "");

				setMapView(mapView);
			}
		}
		return mMapContainer;
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);

		// Inflate the menu items for use in the action bar
		inflater.inflate(R.menu.actions, menu);

		// Get a reference to the EditText widget for the search option
		View searchRef = menu.findItem(R.id.menu_search).getActionView();
		mSearchEditText = (EditText) searchRef.findViewById(R.id.searchText);

		// Set key listener to start search if Enter key pressed
		mSearchEditText.setOnKeyListener(new OnKeyListener() {
			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if (keyCode == KeyEvent.KEYCODE_ENTER) {
					onSearchButtonClicked(mSearchEditText);
					return true;
				}
				return false;
			}
		});

		// Save a reference to the Directions button
		mActionItemDirections = menu.findItem(R.id.directions);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.route:
			showRoutingDialogFragemnt();			
			return true;

		case R.id.basemaps:
			// Show BasemapsDialogFragment to offer a choice if basemaps.
			// This calls back to onBasemapChanged() if one is selected.
			BasemapsDialogFragment basemapsFrag = new BasemapsDialogFragment();
			basemapsFrag.setBasemapsDialogListener(this);
			basemapsFrag.show(getFragmentManager(), null);
			return true;

		case R.id.location:
			// Toggle location tracking on or off
			if (mIsLocationTracking) {
				mMapView.getLocationDisplayManager().stop();
				mIsLocationTracking = false;
			} else {
				startLocationTracking();
			}
			return true;

		case R.id.directions:
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
							RouteDirection direction = mRoutingDirections
									.get(position);
							mMapView.setExtent(direction.getGeometry());
						}

					});
			getFragmentManager().beginTransaction().add(frag, null).commit();
			return true;

		case R.id.action_measure:
			// initialize some resources for the measure tool, optional.
			Unit[] linearUnits = new Unit[] {
					Unit.create(LinearUnit.Code.CENTIMETER),
					Unit.create(LinearUnit.Code.METER),
					Unit.create(LinearUnit.Code.KILOMETER),
					Unit.create(LinearUnit.Code.INCH),
					Unit.create(LinearUnit.Code.FOOT),
					Unit.create(LinearUnit.Code.YARD),
					Unit.create(LinearUnit.Code.MILE_STATUTE) };
			SimpleMarkerSymbol markerSymbol = new SimpleMarkerSymbol(
					Color.BLUE, 10,
					com.esri.core.symbol.SimpleMarkerSymbol.STYLE.DIAMOND);
			SimpleLineSymbol lineSymbol = new SimpleLineSymbol(Color.YELLOW, 3);
			SimpleFillSymbol fillSymbol = new SimpleFillSymbol(Color.argb(100,
					0, 225, 255));
			fillSymbol.setOutline(new SimpleLineSymbol(Color.TRANSPARENT, 0));

			// create the tool, required.
			MeasuringTool measuringTool = new MeasuringTool(mMapView);
			// customize the tool, optional.
			measuringTool.setLinearUnits(linearUnits);
			measuringTool.setMarkerSymbol(markerSymbol);
			measuringTool.setLineSymbol(lineSymbol);
			measuringTool.setFillSymbol(fillSymbol);

			// fire up the tool, required.
			getActivity().startActionMode(measuringTool);
			return true;

		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onPause() {
		super.onPause();

		// Pause the MapView and stop the LocationDisplayManager to save battery
		if (mMapView != null) {
			if (mIsLocationTracking) {
				mMapView.getLocationDisplayManager().stop();
			}
			mMapViewState = mMapView.retainState();
			mMapView.pause();
		}
	}

	@Override
	public void onResume() {
		super.onResume();

		// Start the MapView and LocationDisplayManager running again
		if (mMapView != null) {
			mMapView.unpause();
			if (mMapViewState != null) {
				mMapView.restoreState(mMapViewState);
			}
			if (mIsLocationTracking) {
				mMapView.getLocationDisplayManager().start();
			}
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
				final WebMap webmap = WebMap.newInstance(portalItemId, portal);

				// load the WebMap that represents the basemap if one was
				// specified
				WebMap basemapWebMap = null;
				if (basemapPortalItemId != null
						&& !basemapPortalItemId.isEmpty()) {
					basemapWebMap = WebMap.newInstance(basemapPortalItemId,
							portal);
				}
				final BaseMap basemap = basemapWebMap != null ? basemapWebMap
						.getBaseMap() : null;

				if (webmap != null) {
					getActivity().runOnUiThread(new Runnable() {
						@Override
						public void run() {
							MapView mapView = new MapView(getActivity(),
									webmap, basemap, null, null);
							setMapView(mapView);
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
	private void setMapView(MapView mapView) {
		mMapView = mapView;
		mMapView.setEsriLogoVisible(true);
		mMapView.enableWrapAround(true);

		View view;
		LayoutInflater inflater = (LayoutInflater) getActivity()
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		view = inflater.inflate(R.layout.searchview, null);
		LinearLayout item = (LinearLayout) view
				.findViewById(R.id.linearLayout1);

		FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
				LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		lp.setMargins(25, 55, 25, 0);
		item.setLayoutParams(lp);

		SearchView sv = (SearchView) item.findViewById(R.id.searchView1);
		sv.setIconifiedByDefault(false);
		ImageView iv = (ImageView) item.findViewById(R.id.imageView1);

		// set MapView into the activity layout
		mMapContainer.addView(mMapView);
		mMapContainer.addView(item);

		iv.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				showRoutingDialogFragemnt();

			}
		});
		// Setup listener for map initialized
		mMapView.setOnStatusChangedListener(new OnStatusChangedListener() {

			private static final long serialVersionUID = 1L;

			@Override
			public void onStatusChanged(Object source, STATUS status) {
				Log.i(TAG, "MapView.setOnStatusChangedListener() status="
						+ status.toString());
				if (source == mMapView && status == STATUS.INITIALIZED) {
					if (mMapViewState == null) {
						// Starting location tracking will cause zoom to My
						// Location
						startLocationTracking();
					} else {
						mMapView.restoreState(mMapViewState);
					}
					// add search and routing layers
					addGraphicLayers();
				}
			}
		});

		// Setup use of magnifier on a long press on the map
		mMapView.setShowMagnifierOnLongPress(true);
		mLongPressEvent = null;

		// Setup OnTouchListener to detect and act on long-press
		mMapView.setOnTouchListener(new MapOnTouchListener(getActivity(),
				mMapView) {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
					// Start of a new gesture. Make sure mLongPressEvent is
					// cleared.
					mLongPressEvent = null;
				}
				return super.onTouch(v, event);
			}

			@Override
			public void onLongPress(MotionEvent point) {
				// Set mLongPressEvent to indicate we are processing a
				// long-press
				mLongPressEvent = point;
				super.onLongPress(point);
			}

			@Override
			public boolean onDragPointerUp(MotionEvent from,
					final MotionEvent to) {
				if (mLongPressEvent != null) {
					// This is the end of a long-press that will have displayed
					// the
					// magnifier.
					// Perform reverse-geocoding of the point that was pressed
					Point mapPoint = mMapView.toMapPoint(to.getX(), to.getY());
					ReverseGeocodingAsyncTask reverseGeocodeTask = new ReverseGeocodingAsyncTask();
					reverseGeocodeTask.execute(mapPoint);
					mPendingTask = reverseGeocodeTask;

					mLongPressEvent = null;
					// Remove any previous graphics
					resetGraphicsLayers();
					mSearchEditText.setText("");
				}
				return super.onDragPointerUp(from, to);
			}

		});

	}

	private void showRoutingDialogFragemnt() {
		// Show RoutingDialogFragment to get routing start and end points.
		// This calls back to onGetRoute() to do the routing.
		RoutingDialogFragment routingFrag = new RoutingDialogFragment();
		routingFrag.setRoutingDialogListener(this);
		Bundle arguments = new Bundle();
		if (mLocationLayerPoint != null) {
			arguments.putString(RoutingDialogFragment.ARG_END_POINT_DEFAULT,
					mLocationLayerPointString);
		}
		routingFrag.setArguments(arguments);
		routingFrag.show(getFragmentManager(), null);

	}

	/**
	 * Shows a mini callout on the map.
	 * 
	 * @param location
	 *            the location of the callout
	 * @param message
	 *            the content of the callout
	 * @param yOffsetDips
	 *            the y offset of the callout in dips
	 */
	private void showCallout(Point location, String message, int yOffsetDips) {

		View view = getActivity().getLayoutInflater().inflate(
				R.layout.simple_callout_layout, null);

		TextView textView = (TextView) view
				.findViewById(R.id.simple_callout_textview);
		textView.setText(message);

		int yOffset = UiUtils.dipsToPixels(yOffsetDips);

		Callout callout = mMapView.getCallout();
		callout.setStyle(mCalloutStyle);
		callout.setMaxWidth(mMaxCalloutWidth);
		callout.setMaxHeight(mMaxCalloutHeight);
		callout.setOffset(0, yOffset);
		callout.animatedShow(location, view);
	}

	/**
	 * Hides the callout.
	 */
	private void hideCallout() {
		Callout callout = mMapView.getCallout();
		callout.hide();
	}

	/**
	 * Clears all graphics out of the location layer and the route layer.
	 */
	void resetGraphicsLayers() {
		mLocationLayer.removeAll();
		mRouteLayer.removeAll();
		mLocationLayerPoint = null;
		mLocationLayerPointString = null;
		mRoutingDirections = null;
		mActionItemDirections.setVisible(false);

		hideCallout();
	}

	/**
	 * Adds location layer and the route layer to the MapView.
	 */
	void addGraphicLayers() {
		// Add location layer
		if (mLocationLayer == null) {
			mLocationLayer = new GraphicsLayer();
		}
		mMapView.addLayer(mLocationLayer);

		// Add the route graphic layer
		if (mRouteLayer == null) {
			mRouteLayer = new GraphicsLayer();
		}
		mMapView.addLayer(mRouteLayer);
	}

	/**
	 * Starts tracking GPS location.
	 */
	void startLocationTracking() {
		LocationDisplayManager locDispMgr = mMapView
				.getLocationDisplayManager();
		locDispMgr.setAutoPanMode(AutoPanMode.OFF);
		locDispMgr.setAllowNetworkLocation(true);
		locDispMgr.setLocationListener(new LocationListener() {

			boolean locationChanged = false;

			// Zooms to the current location when first GPS fix arrives
			@Override
			public void onLocationChanged(Location loc) {
				double locy = loc.getLatitude();
				double locx = loc.getLongitude();
				Point wgspoint = new Point(locx, locy);
				mLocation = (Point) GeometryEngine.project(wgspoint,
						SpatialReference.create(4326),
						mMapView.getSpatialReference());
				if (!locationChanged) {
					locationChanged = true;
					Unit mapUnit = mMapView.getSpatialReference().getUnit();
					double zoomWidth = Unit.convertUnits(SEARCH_RADIUS,
							Unit.create(LinearUnit.Code.MILE_US), mapUnit);
					Envelope zoomExtent = new Envelope(mLocation, zoomWidth,
							zoomWidth);
					mMapView.setExtent(zoomExtent);
				}
			}

			@Override
			public void onProviderDisabled(String arg0) {
			}

			@Override
			public void onProviderEnabled(String arg0) {
			}

			@Override
			public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
			}
		});
		locDispMgr.start();
		mIsLocationTracking = true;
	}

	@Override
	public void onBasemapChanged(String basemapPortalItemId) {
		((MapsAppActivity) getActivity()).showMap(mPortalItemId,
				basemapPortalItemId);
	}

	/**
	 * Called from search_layout.xml when user presses Search button.
	 * 
	 * @param view
	 */
	public void onSearchButtonClicked(View view) {

		// Hide virtual keyboard
		InputMethodManager inputManager = (InputMethodManager) getActivity()
				.getSystemService(Context.INPUT_METHOD_SERVICE);
		inputManager.hideSoftInputFromWindow(getActivity().getCurrentFocus()
				.getWindowToken(), 0);

		// Remove any previous graphics and routes
		resetGraphicsLayers();

		// Obtain address and execute locator task
		String address = mSearchEditText.getText().toString();
		executeLocatorTask(address);
	}

	/**
	 * Set up the search parameters and execute the Locator task.
	 * 
	 * @param address
	 */
	private void executeLocatorTask(String address) {
		// Create Locator parameters from single line address string
		LocatorFindParameters findParams = new LocatorFindParameters(address);

		// Use the centre of the current map extent as the find location point
		findParams.setLocation(mMapView.getCenter(),
				mMapView.getSpatialReference());

		// Calculate distance for find operation
		Envelope mapExtent = new Envelope();
		mMapView.getExtent().queryEnvelope(mapExtent);
		// assume map is in metres, other units wont work, double current
		// envelope
		double distance = (mapExtent != null && mapExtent.getWidth() > 0) ? mapExtent
				.getWidth() * 2 : 10000;
		findParams.setDistance(distance);
		findParams.setMaxLocations(2);

		// Set address spatial reference to match map
		findParams.setOutSR(mMapView.getSpatialReference());

		// Execute async task to find the address
		LocatorAsyncTask locatorTask = new LocatorAsyncTask();
		locatorTask.execute(findParams);
		mPendingTask = locatorTask;

		mLocationLayerPointString = address;
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
	@Override
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
		mSearchEditText.setText("");

		// Do the routing
		executeRoutingTask(startPoint, endPoint);
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
		// Create a list of start end point params
		LocatorFindParameters routeStartParams = new LocatorFindParameters(
				start);
		LocatorFindParameters routeEndParams = new LocatorFindParameters(end);
		List<LocatorFindParameters> routeParams = new ArrayList<LocatorFindParameters>();

		// Add params to list
		routeParams.add(routeStartParams);
		routeParams.add(routeEndParams);

		// Execute async task to do the routing
		RouteAsyncTask routeTask = new RouteAsyncTask();
		routeTask.execute(routeParams);
		mPendingTask = routeTask;
	}

	@Override
	public void onCancel(DialogInterface dialog) {
		// a pending task needs to be canceled
		if (mPendingTask != null) {
			mPendingTask.cancel(true);
		}
	}

	/*
	 * This class provides an AsyncTask that performs a geolocation request on a
	 * background thread and displays the first result on the map on the UI
	 * thread.
	 */
	private class LocatorAsyncTask extends
			AsyncTask<LocatorFindParameters, Void, List<LocatorGeocodeResult>> {
		private static final String TAG_LOCATOR_PROGRESS_DIALOG = "TAG_LOCATOR_PROGRESS_DIALOG";

		private Exception mException;

		private ProgressDialogFragment mProgressDialog;

		public LocatorAsyncTask() {
		}

		@Override
		protected void onPreExecute() {
			mProgressDialog = ProgressDialogFragment.newInstance(getActivity()
					.getString(R.string.address_search));
			// set the target fragment to receive cancel notification
			mProgressDialog.setTargetFragment(MapFragment.this,
					REQUEST_CODE_PROGRESS_DIALOG);
			mProgressDialog.show(getActivity().getFragmentManager(),
					TAG_LOCATOR_PROGRESS_DIALOG);
		}

		@Override
		protected List<LocatorGeocodeResult> doInBackground(
				LocatorFindParameters... params) {
			// Perform routing request on background thread
			mException = null;
			List<LocatorGeocodeResult> results = null;

			// Create locator using default online geocoding service and tell it
			// to
			// find the given address
			Locator locator = Locator.createOnlineLocator();
			try {
				results = locator.find(params[0]);
			} catch (Exception e) {
				mException = e;
			}
			return results;
		}

		@Override
		protected void onPostExecute(List<LocatorGeocodeResult> result) {
			// Display results on UI thread
			mProgressDialog.dismiss();
			if (mException != null) {
				Log.w(TAG, "LocatorSyncTask failed with:");
				mException.printStackTrace();
				Toast.makeText(getActivity(),
						getString(R.string.addressSearchFailed),
						Toast.LENGTH_LONG).show();
				return;
			}

			if (result.size() == 0) {
				Toast.makeText(getActivity(),
						getString(R.string.noResultsFound), Toast.LENGTH_LONG)
						.show();
			} else {
				// Use first result in the list
				LocatorGeocodeResult geocodeResult = result.get(0);

				// get return geometry from geocode result
				Point resultPoint = geocodeResult.getLocation();
				// create marker symbol to represent location
				Drawable drawable = getActivity().getResources().getDrawable(
						R.drawable.pin_circle_red);
				PictureMarkerSymbol resultSymbol = new PictureMarkerSymbol(
						getActivity(), drawable);
				// create graphic object for resulting location
				Graphic resultLocGraphic = new Graphic(resultPoint,
						resultSymbol);
				// add graphic to location layer
				mLocationLayer.addGraphic(resultLocGraphic);

				// show a callout for return address
				String address = geocodeResult.getAddress();
				showCallout(resultPoint, address, 10);

				mLocationLayerPoint = resultPoint;

				// Zoom map to geocode result location
				mMapView.zoomToResolution(geocodeResult.getLocation(), 2);
			}
		}

	}

	/**
	 * This class provides an AsyncTask that performs a routing request on a
	 * background thread and displays the resultant route on the map on the UI
	 * thread.
	 */
	private class RouteAsyncTask extends
			AsyncTask<List<LocatorFindParameters>, Void, RouteResult> {
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
		protected RouteResult doInBackground(
				List<LocatorFindParameters>... params) {
			// Perform routing request on background thread
			mException = null;

			// Define route objects
			List<LocatorGeocodeResult> geocodeStartResult = null;
			List<LocatorGeocodeResult> geocodeEndResult = null;
			Point startPoint = null;
			Point endPoint = null;

			// Create a new locator to geocode start/end points;
			// by default uses ArcGIS online world geocoding service
			Locator locator = Locator.createOnlineLocator();

			try {
				// Geocode start position, or use My Location (from GPS)
				LocatorFindParameters startParam = params[0].get(0);
				if (startParam.getText()
						.equals(getString(R.string.my_location))) {
					startPoint = (Point) GeometryEngine.project(mLocation, mWm,
							mEgs);
				} else {
					geocodeStartResult = locator.find(startParam);
					startPoint = geocodeStartResult.get(0).getLocation();
					if (isCancelled()) {
						return null;
					}
				}

				// Geocode the destination
				LocatorFindParameters endParam = params[0].get(1);
				geocodeEndResult = locator.find(endParam);
				endPoint = geocodeEndResult.get(0).getLocation();
			} catch (Exception e) {
				mException = e;
				return null;
			}
			if (isCancelled()) {
				return null;
			}

			// Create a new routing task pointing to an ArcGIS Network Analysis
			// Service
			RouteTask routeTask;
			RouteParameters routeParams = null;
			try {
				routeTask = RouteTask.createOnlineRouteTask(
						getString(R.string.routingservice_url), null);
				// Retrieve default routing parameters
				routeParams = routeTask.retrieveDefaultRouteTaskParameters();
			} catch (Exception e) {
				mException = e;
				return null;
			}
			if (isCancelled()) {
				return null;
			}

			// Customize the route parameters
			NAFeaturesAsFeature routeFAF = new NAFeaturesAsFeature();
			StopGraphic sgStart = new StopGraphic(startPoint);
			StopGraphic sgEnd = new StopGraphic(endPoint);
			routeFAF.setFeatures(new Graphic[] { sgStart, sgEnd });
			routeFAF.setCompressedRequest(true);
			routeParams.setStops(routeFAF);
			routeParams.setOutSpatialReference(mMapView.getSpatialReference());

			// Solve the route
			RouteResult routeResult;
			try {
				routeResult = routeTask.solve(routeParams);
			} catch (Exception e) {
				mException = e;
				return null;
			}
			if (isCancelled()) {
				return null;
			}
			return routeResult;
		}

		@Override
		protected void onPostExecute(RouteResult result) {
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
			Route route = result.getRoutes().get(0);

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
			mRouteLayer.addGraphics(new Graphic[] { routeGraphic, startGraphic,
					endGraphic });

			// Zoom to the extent of the entire route with a padding
			mMapView.setExtent(route.getEnvelope(), 100);

			// Save routing directions so user can display them later
			mRoutingDirections = route.getRoutingDirections();
			mActionItemDirections.setVisible(true);
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
					marker.getBounds().bottom);
			destinationSymbol.setOffsetY(offsetY);
			return new Graphic(point, destinationSymbol);
		}
	}

	/**
	 * This class provides an AsyncTask that performs a reverse geocoding
	 * request on a background thread and displays the resultant point on the
	 * map on the UI thread.
	 */
	public class ReverseGeocodingAsyncTask extends
			AsyncTask<Point, Void, LocatorReverseGeocodeResult> {
		private static final String TAG_REVERSE_GEOCODING_PROGRESS_DIALOG = "TAG_REVERSE_GEOCODING_PROGRESS_DIALOG";

		private Exception mException;

		private ProgressDialogFragment mProgressDialog;

		private Point mPoint;

		@Override
		protected void onPreExecute() {
			mProgressDialog = ProgressDialogFragment.newInstance(getActivity()
					.getString(R.string.reverse_geocoding));
			// set the target fragment to receive cancel notification
			mProgressDialog.setTargetFragment(MapFragment.this,
					REQUEST_CODE_PROGRESS_DIALOG);
			mProgressDialog.show(getActivity().getFragmentManager(),
					TAG_REVERSE_GEOCODING_PROGRESS_DIALOG);
		}

		@Override
		protected LocatorReverseGeocodeResult doInBackground(Point... params) {
			// Perform reverse geocoding request on background thread
			mException = null;
			LocatorReverseGeocodeResult result = null;
			mPoint = params[0];

			// Create locator using default online geocoding service and tell it
			// to
			// find the given point
			Locator locator = Locator.createOnlineLocator();
			try {
				// Our input and output spatial reference will be the same as
				// the map
				SpatialReference mapRef = mMapView.getSpatialReference();
				result = locator.reverseGeocode(mPoint, 100.0, mapRef, mapRef);
				mLocationLayerPoint = mPoint;
			} catch (Exception e) {
				mException = e;
			}
			// return the resulting point(s)
			return result;
		}

		@Override
		protected void onPostExecute(LocatorReverseGeocodeResult result) {
			// Display results on UI thread
			mProgressDialog.dismiss();
			if (mException != null) {
				Log.w(TAG, "LocatorSyncTask failed with:");
				mException.printStackTrace();
				Toast.makeText(getActivity(),
						getString(R.string.addressSearchFailed),
						Toast.LENGTH_LONG).show();
				return;
			}

			// Construct a nicely formatted address from the results
			StringBuilder address = new StringBuilder();
			if (result != null && result.getAddressFields() != null) {
				Map<String, String> addressFields = result.getAddressFields();
				address.append(String.format("%s\n%s, %s %s",
						addressFields.get("Address"),
						addressFields.get("City"), addressFields.get("Region"),
						addressFields.get("Postal")));

				// Draw marker on map.
				// create marker symbol to represent location
				Drawable drawable = getActivity().getResources().getDrawable(
						R.drawable.pin_circle_red);
				PictureMarkerSymbol symbol = new PictureMarkerSymbol(
						getActivity(), drawable);
				mLocationLayer.addGraphic(new Graphic(mPoint, symbol));

				// Address string is saved for use in routing
				mLocationLayerPointString = address.toString();
				// show results in callout
				showCallout(mPoint, mLocationLayerPointString, 10);
				// center the map to result location
				mMapView.centerAt(mPoint, true);
			}
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
		float dp = px / (metrics.densityDpi / 160f);
		return dp;
	}
}
