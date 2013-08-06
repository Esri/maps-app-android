/* Copyright 1995-2013 Esri
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

package com.esri.android.rt.map;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.Toast;

import com.arcgis.android.app.map.R;
import com.esri.android.map.GraphicsLayer;
import com.esri.android.map.LocationService;
import com.esri.android.map.MapView;
import com.esri.android.map.ags.ArcGISFeatureLayer;
import com.esri.android.map.event.OnLongPressListener;
import com.esri.android.map.event.OnStatusChangedListener;
import com.esri.android.map.popup.Popup;
import com.esri.android.rt.location.ReverseGeocoding;
import com.esri.android.rt.map.PopupFragment.OnEditListener;
import com.esri.core.geometry.Envelope;
import com.esri.core.geometry.Geometry;
import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.LinearUnit;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.Polyline;
import com.esri.core.geometry.SpatialReference;
import com.esri.core.geometry.Unit;
import com.esri.core.map.CallbackListener;
import com.esri.core.map.FeatureEditResult;
import com.esri.core.map.Graphic;
import com.esri.core.portal.Portal;
import com.esri.core.portal.PortalGroup;
import com.esri.core.portal.PortalInfo;
import com.esri.core.portal.PortalItem;
import com.esri.core.portal.PortalItemType;
import com.esri.core.portal.PortalQueryParams;
import com.esri.core.portal.PortalQueryParams.PortalQuerySortOrder;
import com.esri.core.portal.PortalQueryResultSet;
import com.esri.core.symbol.PictureMarkerSymbol;
import com.esri.core.symbol.SimpleLineSymbol;
import com.esri.core.symbol.SimpleMarkerSymbol;
import com.esri.core.symbol.TextSymbol;
import com.esri.core.tasks.ags.geocode.Locator;
import com.esri.core.tasks.ags.geocode.LocatorFindParameters;
import com.esri.core.tasks.ags.geocode.LocatorGeocodeResult;
import com.esri.core.tasks.ags.na.NAFeaturesAsFeature;
import com.esri.core.tasks.ags.na.Route;
import com.esri.core.tasks.ags.na.RoutingParameters;
import com.esri.core.tasks.ags.na.RoutingResult;
import com.esri.core.tasks.ags.na.RoutingTask;
import com.esri.core.tasks.ags.na.StopGraphic;

public class MapsApp extends FragmentActivity implements
		OnEditListener {
	// map definitions
	static MapView mMapView = null;

	// Recreation webmap URL
	String recWebMapURL;

	int basemap;

	// Geocoding definitions
	Locator locator;
	LocatorGeocodeResult geocodeResult;
	GeocoderTask mGeocode;
	// graphics layer to show geocode result
	GraphicsLayer locationLayer;

	// GPS Location definitions
	Point mLocation = null;
	// The circle area specified by search_radius and input lat/lon serves
	// searching purpose. It is also used to construct the extent which
	// map zooms to after the first GPS fix is retrieved.
	final static double SEARCH_RADIUS = 10;

	// Spatial references used for projecting points
	final SpatialReference wm = SpatialReference.create(102100);
	final SpatialReference egs = SpatialReference.create(4326);

	// create UI components
	static ProgressDialog dialog;
	// Edit text box for entering search items
	EditText searchText;

	GridView gridView;
	BasemapsAdapter bAdapter;
	ArrayList<BasemapItem> itemDataList;
	Portal portal;
	PortalQueryResultSet<PortalItem> queryResultSet;

	// Strings for routing
	String startText;
	String endText;
	Point routePnt;
	// routing result definition
	RoutingResult routeResult;
	// route definition
	Route route;
	String routeSummary;
	// graphics layer to show routes
	GraphicsLayer routeLayer;

	// bundle to get routing parameters back to UI
	Bundle extras;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// mGeocode = (GeocoderTask) getLastNonConfigurationInstance();
		// if (mGeocode != null) {
		// mGeocode.mActivity = new WeakReference<BasemapsActivity>(this);
		// }

		// Retrieve the map and initial extent from recreation webmap
		recWebMapURL = getString(R.string.rec_webmap_url);
		mMapView = new MapView(this, recWebMapURL, "", "");

		// set the content view to the map
		// setContentView(mMapView);
		setMapView(mMapView);

		// attribute app and pan across dateline
		addAttributes();
		
		// Zoom to device location and accept intent from route layout
		mMapView.setOnStatusChangedListener(new OnStatusChangedListener() {

			private static final long serialVersionUID = 1L;

			@Override
			public void onStatusChanged(Object source, STATUS status) {
				if (source == mMapView && status == STATUS.INITIALIZED) {
					// add search and routing layers
					addGraphicLayers();
					// start location service
					LocationService ls = mMapView.getLocationService();
					ls.setAutoPan(false);
					ls.setLocationListener(new LocationListener() {

						boolean locationChanged = false;

						// Zooms to the current location when first GPS fix
						// arrives.
						@Override
						public void onLocationChanged(Location loc) {
							if (!locationChanged) {
								locationChanged = true;
								double locy = loc.getLatitude();
								double locx = loc.getLongitude();
								Point wgspoint = new Point(locx, locy);
								mLocation = (Point) GeometryEngine.project(
										wgspoint,
										SpatialReference.create(4326),
										mMapView.getSpatialReference());

								Unit mapUnit = mMapView.getSpatialReference()
										.getUnit();
								double zoomWidth = Unit.convertUnits(
										SEARCH_RADIUS,
										Unit.create(LinearUnit.Code.MILE_US),
										mapUnit);
								Envelope zoomExtent = new Envelope(mLocation,
										zoomWidth, zoomWidth);
								mMapView.setExtent(zoomExtent);

								extras = getIntent().getExtras();
								if (extras != null) {
									startText = extras.getString("start");
									endText = extras.getString("end");
									basemap = extras.getInt("basemap");

									// route start and end points
									route(startText, endText);
								}
							}

						}

						@Override
						public void onProviderDisabled(String arg0) {

						}

						@Override
						public void onProviderEnabled(String arg0) {
						}

						@Override
						public void onStatusChanged(String arg0, int arg1,
								Bundle arg2) {

						}
					});
					ls.start();

				}

			}
		});

        mMapView.setOnLongPressListener(new OnLongPressListener() {

            private static final long serialVersionUID = 1L;

            @Override
            public void onLongPress(float x, float y) {
                Point mapPoint = mMapView.toMapPoint(x, y);

                new ReverseGeocoding(MapsApp.this, mMapView).execute(mapPoint);

            }
        });
		
	}

	public void setMapView(MapView mapView) {
		mMapView = mapView;
		mMapView.setOnSingleTapListener(new SingleTapListener(mMapView));
		setContentView(mMapView);
	}

	private void addAttributes() {
		// attribute ESRI logo to map
		mMapView.setEsriLogoVisible(true);
		// enable map to wrap around date line
		mMapView.enableWrapAround(true);

	}

	private void addGraphicLayers() {
		// Add location layer
		locationLayer = new GraphicsLayer();
		mMapView.addLayer(locationLayer);

		// Add the route graphic layer (shows the full route)
		routeLayer = new GraphicsLayer();
		mMapView.addLayer(routeLayer);

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// inflate basemaps action bar menu
		getMenuInflater().inflate(R.menu.basemap_menu, menu);
		// create action view from basemap menu
		View searchRef = menu.findItem(R.id.menu_search).getActionView();
		// get a reference to EditText field
		searchText = (EditText) searchRef.findViewById(R.id.searchText);
		// return
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// handle basemap item selection
		switch (item.getItemId()) {
		case R.id.route:

			Intent directionsIntent = new Intent(MapsApp.this,
					DirectionsActivity.class);
			directionsIntent.putExtra("basemap", basemap);
			startActivity(directionsIntent);

			return true;
		case R.id.basemaps:
			// inflate grid layout with basemap options
			LayoutInflater inflator = LayoutInflater
					.from(getApplicationContext());
			gridView = (GridView) inflator.inflate(R.layout.grid_layout, null);
			// array list to hold basemap items
			itemDataList = new ArrayList<BasemapItem>();
			// create the custom basemap adapter and send basemap items and 
			// mapview to switch basemaps
			bAdapter = new BasemapsAdapter(MapsApp.this, itemDataList,
					mMapView);
			// set the adapter to the gridview
			gridView.setAdapter(bAdapter);
			// pull up the gridview
			setContentView(gridView);
			// populate the gridview with available basemaps
			new GroupTask().execute();

			return true;

		default:
			return super.onOptionsItemSelected(item);
		}
	}

	// public Object onRetainNonConfigurationInstance() {
	// return mGeocode;
	// }

	/**
	 * Submit address for place search
	 * 
	 * @param view
	 */
	public void locate(View view) {
		// hide virtual keyboard
		InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		inputManager.hideSoftInputFromWindow(
				getCurrentFocus().getWindowToken(), 0);
		// remove any previous graphics and callouts
		locationLayer.removeAll();
		// remove any previous routes
		routeLayer.removeAll();
		// obtain address from text box
		String address = searchText.getText().toString();
		// set parameters to support the find operation for a geocoding service
		setSearchParams(address);
	}

	/**
	 * Set up the search parameters to execute the Geocoder task
	 * 
	 * @param address
	 */
	private void setSearchParams(String address) {
		// create Locator parameters from single line address string
		LocatorFindParameters findParams = new LocatorFindParameters(address);
		// set the search extent to extent of map
		// SpatialReference inSR = mMapView.getSpatialReference();
		// Envelope searchExtent = mMapView.getMapBoundaryExtent();
		// Use the centre of the current map extent as the find location point
		findParams.setLocation(mMapView.getCenter(),
				mMapView.getSpatialReference());
		// calculate distance for find operation
		Envelope mapExtent = new Envelope();
		mMapView.getExtent().queryEnvelope(mapExtent);
		// assume map is in metres, other units wont work
		// double current envelope
		double distance = (mapExtent != null && mapExtent.getWidth() > 0) ? mapExtent
				.getWidth() * 2 : 10000;
		findParams.setDistance(distance);
		findParams.setMaxLocations(2);
		// set address spatial reference to match map
		findParams.setOutSR(mMapView.getSpatialReference());
		// execute async task to geocode address
		mGeocode = new GeocoderTask(this);
		mGeocode.execute(findParams);
	}

	/**
	 * Submit start and end point for routing
	 * 
	 * @param start
	 * @param end
	 */
	public void route(String start, String end) {
		// remove any previous graphics and callouts
		locationLayer.removeAll();
		// remove any previous routes
		routeLayer.removeAll();
		// set parameters to geocode address for points
		setRouteParams(start, end);
	}

	/**
	 * Set up Route Parameters to execute RouteTask
	 * 
	 * @param start
	 * @param end
	 */
	private void setRouteParams(String start, String end) {
		// create a list of start end point params
		LocatorFindParameters routeStartParams = new LocatorFindParameters(
				start);
		LocatorFindParameters routeEndParams = new LocatorFindParameters(end);
		List<LocatorFindParameters> routeParams = new ArrayList<LocatorFindParameters>();
		// add params to list
		routeParams.add(routeStartParams);
		routeParams.add(routeEndParams);
		// run asych route task
		new RouteTask().execute(routeParams);

	}

	@Override
	public void onDelete(ArcGISFeatureLayer fl, Popup popup) {
		// Commit deletion to server
		Graphic gr = popup.getGraphic();
		if (gr == null)
			return;
		fl.applyEdits(null, new Graphic[] { gr }, null,
				new EditCallbackListener(this, fl, popup, true,
						"Deleting feature"));

		// Dismiss popup
		this.getSupportFragmentManager().popBackStack();
	}

	@Override
	public void onEdit(ArcGISFeatureLayer fl, Popup popup) {
		// Set popup into editing mode
		popup.setEditMode(true);
		// refresh menu items
		this.invalidateOptionsMenu();
	}

	@Override
	public void onSave(ArcGISFeatureLayer fl, Popup popup) {
		// Commit edits to server
		Graphic gr = popup.getGraphic();
		if (gr != null) {
			Map<String, Object> attributes = gr.getAttributes();
			Map<String, Object> updatedAttrs = popup.getUpdatedAttributes();
			for (Entry<String, Object> entry : updatedAttrs.entrySet()) {
				attributes.put(entry.getKey(), entry.getValue());
			}
			Graphic newgr = new Graphic(gr.getGeometry(), null, attributes,
					null);
			fl.applyEdits(null, null, new Graphic[] { newgr },
					new EditCallbackListener(this, fl, popup, true,
							"Saving feature"));
		}

		// Dismiss popup
		this.getSupportFragmentManager().popBackStack();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (mGeocode != null) {
			mGeocode.mActivity = null;
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		mMapView.pause();
	}

	@Override
	protected void onResume() {
		super.onResume();
		mMapView.unpause();
	}

	/*
	 * AsyncTask to geocode an address to a point location Draw resulting point
	 * location on the map with matching address
	 */
	private class GeocoderTask extends
			AsyncTask<LocatorFindParameters, Void, List<LocatorGeocodeResult>> {

		WeakReference<MapsApp> mActivity;

		GeocoderTask(MapsApp activity) {
			mActivity = new WeakReference<MapsApp>(activity);
		}

		@Override
		protected void onPreExecute() {
			// show progress dialog while geocoding address
			dialog = ProgressDialog.show(mMapView.getContext(), "Geocoder",
					"Searching for address ...");
		}

		// The result of geocode task is passed as a parameter to map the
		// results
		@Override
		protected void onPostExecute(List<LocatorGeocodeResult> result) {

			if (dialog.isShowing()) {
				dialog.dismiss();
			}

			// The result of geocode task is passed as a parameter to map the
			// results
			if (result == null || result.size() == 0) {
				// update UI with notice that no results were found
				Toast toast = Toast.makeText(MapsApp.this,
						"No result found.", Toast.LENGTH_LONG);
				toast.show();
			} else {
				// get first result in the list
				// update global result
				geocodeResult = result.get(0);

				// get return geometry from geocode result
				Geometry resultLocGeom = geocodeResult.getLocation();
				// create marker symbol to represent location
				SimpleMarkerSymbol resultSymbol = new SimpleMarkerSymbol(
						Color.BLACK, 20, SimpleMarkerSymbol.STYLE.SQUARE);
				// create graphic object for resulting location
				Graphic resultLocation = new Graphic(resultLocGeom,
						resultSymbol);
				// add graphic to location layer
				locationLayer.addGraphic(resultLocation);
				// create text symbol for return address
				TextSymbol resultAddress = new TextSymbol(12,
						geocodeResult.getAddress(), Color.BLACK);
				// create offset for text
				resultAddress.setOffsetX(10);
				resultAddress.setOffsetY(50);
				// create a graphic object for address text
				Graphic resultText = new Graphic(resultLocGeom, resultAddress);
				// add address text graphic to location graphics layer
				locationLayer.addGraphic(resultText);
				// zoom to geocode result

				mMapView.zoomToResolution(geocodeResult.getLocation(), 2);
				// create a runnable to be added to message queue
				// handler.post(new MyRunnable());
			}
		}

		@Override
		protected List<LocatorGeocodeResult> doInBackground(
				LocatorFindParameters... params) {
			// create results object and set to null
			List<LocatorGeocodeResult> results = null;
			// set the geocode service
			locator = new Locator();

			try {

				// pass address to find method to return point representing
				// address
				results = locator.find(params[0]);
			} catch (Exception e) {
				e.printStackTrace();
			}
			// return the resulting point(s)
			return results;
		}

	}

	private class RouteTask extends
			AsyncTask<List<LocatorFindParameters>, Void, RoutingResult> {

		@Override
		protected void onPreExecute() {
			// show progress dialog while geocoding address
			dialog = ProgressDialog.show(mMapView.getContext(), "Routing",
					"Searching for route ...");
		}

		@Override
		protected void onPostExecute(RoutingResult result) {
			if (dialog.isShowing()) {
				dialog.dismiss();
			}

			// The result of geocode task is passed as a parameter to map the
			// results
			if (result == null) {
				// update UI with notice that no results were found
				Toast toast = Toast.makeText(MapsApp.this,
						"No result found.", Toast.LENGTH_LONG);
				toast.show();
			} else {
				route = result.getRoutes().get(0);
				// Symbols for the route and the destination
				SimpleLineSymbol routeSymbol = new SimpleLineSymbol(Color.BLUE,
						3);
				PictureMarkerSymbol destinationSymbol = new PictureMarkerSymbol(
						getResources().getDrawable(R.drawable.stat_finish));
				// graphic to mark route
				Graphic routeGraphic = new Graphic(route.getRoute()
						.getGeometry(), routeSymbol);
				Graphic endGraphic = new Graphic(
						((Polyline) routeGraphic.getGeometry()).getPoint(((Polyline) routeGraphic
								.getGeometry()).getPointCount() - 1),
						destinationSymbol);

				// Get the full route summary and set it as our current label
				routeLayer
						.addGraphics(new Graphic[] { routeGraphic, endGraphic });
				// Zoom to the extent of the entire route with a padding
				mMapView.setExtent(route.getEnvelope(), 100);

			}
		}

		@Override
		protected RoutingResult doInBackground(
				List<LocatorFindParameters>... params) {
			
			// define route objects
			List<LocatorGeocodeResult> geocodeStartResult = null;
			List<LocatorGeocodeResult> geocodeEndResult = null;
			Point startPoint = null;
			Point endPoint = null;

			// parse LocatorFindParameters
			LocatorFindParameters startParam = params[0].get(0);
			LocatorFindParameters endParam = params[0].get(1);
			// create a new locator to geocode start/end points
			Locator locator = new Locator();

			try {
				// if GPS then location known and can be reprojected
				if (startParam.getText().equals("My Location")) {
					// startPoint = mLocation;
					startPoint = (Point) GeometryEngine.project(mLocation, wm,
							egs);
				} else {
					// if not GPS than we need to geocode and get location
					geocodeStartResult = locator.find(startParam);
					startPoint = geocodeStartResult.get(0).getLocation();

				}
				// geocode the destination
				geocodeEndResult = locator.find(endParam);
				endPoint = geocodeEndResult.get(0).getLocation();
			} catch (Exception e) {
				e.printStackTrace();
			}

			// build routing parameters
			RoutingParameters routeParams = new RoutingParameters();
			NAFeaturesAsFeature routeFAF = new NAFeaturesAsFeature();
			// Create the stop points
			StopGraphic sgStart = new StopGraphic(startPoint);
			StopGraphic sgEnd = new StopGraphic(endPoint);
			routeFAF.setFeatures(new Graphic[] { sgStart, sgEnd });
			routeFAF.setCompressedRequest(true);
			routeParams.setStops(routeFAF);
			routeParams.setOutSpatialReference(mMapView.getSpatialReference());
			// Create a new routing task pointing to an
			// NAService
			RoutingTask routingTask = new RoutingTask(
					getString(R.string.routingservice_url));
			try {
				// Solve the route
				routeResult = routingTask.solve(routeParams);
			} catch (Exception e) {
				e.printStackTrace();
			}
			return routeResult;
		}
	}

	private class GroupTask extends AsyncTask<Void, Void, Boolean> {

		@Override
		protected void onPreExecute() {
			// show progress dialog while searching basemaps
			dialog = ProgressDialog.show(mMapView.getContext(), "Basemaps View",
					"Searching Basemaps ...");

		}

		@Override
		protected Boolean doInBackground(Void... params) {
			try {
				fetchBasemapsItems();
			} catch (Exception e) {
				e.printStackTrace();
			}
			return true;
		}

		@Override
		protected void onPostExecute(Boolean update) {
			// dismiss dialog
			if (dialog.isShowing()) {
				dialog.dismiss();
			}

			if (update == true) {
				// update the adapter
				bAdapter.notifyDataSetChanged();
			}

		}

		private void fetchBasemapsItems() throws Exception {
			// GIST > https://gist.github.com/doneill/5499642
			// Open default portal
			String url = "http://www.arcgis.com";
			portal = new Portal(url, null);

			// get the information provided by portal
			PortalInfo portalInfo = portal.fetchPortalInfo();
			// get query to determine which basemap gallery group should be used
			// in client
			String basemapGalleryGroupQuery = portalInfo
					.getBasemapGalleryGroupQuery();
			// create a PortalQueryParams from the basemap query
			PortalQueryParams portalQueryParams = new PortalQueryParams(
					basemapGalleryGroupQuery);
			// allow public search for basemaps
			portalQueryParams.setCanSearchPublic(true);
			// find groups for basemaps
			PortalQueryResultSet<PortalGroup> results = portal
					.findGroups(portalQueryParams);

			// get the basemap group
			List<PortalGroup> groupResults = results.getResults();
			// Get the portal items
			if (groupResults != null && groupResults.size() > 0) {
				PortalQueryParams queryParams = new PortalQueryParams();
				queryParams.setCanSearchPublic(true);
				queryParams.setLimit(15);

				String groupID = groupResults.get(0).getGroupId();
				queryParams.setQuery(PortalItemType.WEBMAP, groupID, null);
				queryParams.setSortField("name").setSortOrder(
						PortalQuerySortOrder.ASCENDING);

				// can't run on UI thread
				queryResultSet = portal.findItems(queryParams);

				for (PortalItem item : queryResultSet.getResults()) {
					byte[] data = item.fetchThumbnail();
					if (data != null) {
						Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0,
								data.length);
						BasemapItem portalItemData = new BasemapItem(item,
								bitmap);
						Log.i("TAG", "Item id = " + item.getTitle());
						itemDataList.add(portalItemData);
					}
				}

			} else {
				Log.i("TAG", "portal group empty");
			}
		}
	}

	// Handle callback on committing edits to server
	private class EditCallbackListener implements
			CallbackListener<FeatureEditResult[][]> {
		private String operation = "Operation ";
		private ArcGISFeatureLayer featureLayer = null;
		private boolean existingFeature = true;
		private Popup popup = null;
		private Context context;

		public EditCallbackListener(Context context,
				ArcGISFeatureLayer featureLayer, Popup popup,
				boolean existingFeature, String msg) {
			this.operation = msg;
			this.featureLayer = featureLayer;
			this.existingFeature = existingFeature;
			this.popup = popup;
			this.context = context;
		}

		@Override
		public void onCallback(FeatureEditResult[][] objs) {
			if (featureLayer == null || !featureLayer.isInitialized()
					|| !featureLayer.isEditable())
				return;

			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					Toast.makeText(context, operation + " succeeded!",
							Toast.LENGTH_SHORT).show();
				}
			});

			if (objs[1] == null || objs[1].length <= 0) {
				// Save attachments to the server if newly added attachments
				// exist.
				// Retrieve object id of the feature
				int oid;
				if (existingFeature) {
					oid = objs[2][0].getObjectId();
				} else {
					oid = objs[0][0].getObjectId();
				}
				// Get newly added attachments
				List<File> attachments = popup.getAddedAttachments();
				if (attachments != null && attachments.size() > 0) {
					for (File attachment : attachments) {
						// Save newly added attachment based on the object id of
						// the feature.
						featureLayer.addAttachment(oid, attachment,
								new CallbackListener<FeatureEditResult>() {
									@Override
									public void onError(Throwable e) {
										// Failed to save new attachments.
										runOnUiThread(new Runnable() {
											@Override
											public void run() {
												Toast.makeText(
														context,
														"Adding attachment failed!",
														Toast.LENGTH_SHORT)
														.show();
											}
										});
									}

									@Override
									public void onCallback(
											FeatureEditResult arg0) {
										// New attachments have been saved.
										runOnUiThread(new Runnable() {
											@Override
											public void run() {
												Toast.makeText(
														context,
														"Adding attachment succeeded!.",
														Toast.LENGTH_SHORT)
														.show();
											}
										});
									}
								});
					}
				}

				// Delete attachments if some attachments have been mark as
				// delete.
				// Get ids of attachments which are marked as delete.
				List<Integer> attachmentIDs = popup.getDeletedAttachmentIDs();
				if (attachmentIDs != null && attachmentIDs.size() > 0) {
					int[] ids = new int[attachmentIDs.size()];
					for (int i = 0; i < attachmentIDs.size(); i++) {
						ids[i] = attachmentIDs.get(i);
					}
					// Delete attachments
					featureLayer.deleteAttachments(oid, ids,
							new CallbackListener<FeatureEditResult[]>() {
								@Override
								public void onError(Throwable e) {
									// Failed to delete attachments
									runOnUiThread(new Runnable() {
										@Override
										public void run() {
											Toast.makeText(
													context,
													"Deleting attachment failed!",
													Toast.LENGTH_SHORT).show();
										}
									});
								}

								@Override
								public void onCallback(FeatureEditResult[] objs) {
									// Attachments have been removed.
									runOnUiThread(new Runnable() {
										@Override
										public void run() {
											Toast.makeText(
													context,
													"Deleting attachment succeeded!",
													Toast.LENGTH_SHORT).show();
										}
									});
								}
							});
				}

			}
		}

		@Override
		public void onError(Throwable e) {
			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					Toast.makeText(context, operation + " failed!",
							Toast.LENGTH_SHORT).show();
				}
			});
		}

	}
}
