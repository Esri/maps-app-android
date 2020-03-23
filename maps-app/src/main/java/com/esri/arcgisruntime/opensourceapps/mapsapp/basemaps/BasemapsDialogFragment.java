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

package com.esri.arcgisruntime.opensourceapps.mapsapp.basemaps;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import android.app.DialogFragment;
import android.content.DialogInterface.OnCancelListener;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;

import com.esri.arcgisruntime.opensourceapps.mapsapp.R;
import com.esri.arcgisruntime.opensourceapps.mapsapp.account.AccountManager;
import com.esri.arcgisruntime.opensourceapps.mapsapp.basemaps.BasemapsAdapter.BasemapsAdapterClickListener;
import com.esri.arcgisruntime.opensourceapps.mapsapp.dialogs.ProgressDialogFragment;
import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.portal.*;

/**
 * Implements the dialog that provides a collection of basemaps to the user.
 */
public class BasemapsDialogFragment extends DialogFragment implements BasemapsAdapterClickListener, OnCancelListener {

	private static final String TAG = "BasemapsDialogFragment";
	private static final String TAG_BASEMAP_SEARCH_PROGRESS_DIALOG = "TAG_BASEMAP_SEARCH_PROGRESS_DIALOG";
	private static final int REQUEST_CODE_PROGRESS_DIALOG = 1;
	private final List<PortalItem> mPortalItems = new ArrayList<>();
	private ProgressDialogFragment mProgressDialog;
	private BasemapsDialogListener mBasemapsDialogListener;
	private BasemapsAdapter mBasemapsAdapter;
	private ArrayList<BasemapItem> mBasemapItemList;
	private static final String PUBLIC_BASEMAPS = "public_basemaps";
	private static final String PRIVATE_BASEMAPS = "private_basemaps";

	// Mandatory empty constructor for fragment manager to recreate fragment
	// after it's destroyed
	public BasemapsDialogFragment() {
	}

	/**
	 * Sets listener for selection of new basemap.
	 *
	 * @param listener
	 */
	public void setBasemapsDialogListener(BasemapsDialogListener listener) {
		mBasemapsDialogListener = listener;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setStyle(DialogFragment.STYLE_NORMAL,R.style.CustomDialog);

	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		getDialog().setTitle(R.string.title_basemaps_dialog);

		// Inflate basemaps grid layout and setup list and adapter to back it
		GridView view = (GridView) inflater.inflate(R.layout.grid_layout, container, false);
		mBasemapItemList = new ArrayList<>();
		mBasemapsAdapter = new BasemapsAdapter(getActivity(), mBasemapItemList, this);
		view.setAdapter(mBasemapsAdapter);

		// Show progress dialog
		mProgressDialog = ProgressDialogFragment.newInstance(getActivity().getString(R.string.fetching_basemaps));
		// set the target fragment to receive cancel notification
		mProgressDialog.setTargetFragment(this, REQUEST_CODE_PROGRESS_DIALOG);

		// If no basemaps yet, execute search for available basemaps and
		// populate the grid with them.
		if (mBasemapItemList.size() == 0) {

			fetchBasemapItems();
		}
		return view;
	}


	@Override
	public void onBasemapItemClicked(int position) {
		dismiss();

		String itemId = mBasemapItemList.get(position).item.getItemId();
		mBasemapsDialogListener.onBasemapChanged(itemId);
	}

	/**
	 * Fetch basemaps from local cache or portal
	 */
	private void fetchBasemapItems() {
		// Show a progress dialog
		mProgressDialog.show(getActivity().getFragmentManager(), TAG_BASEMAP_SEARCH_PROGRESS_DIALOG);
		// If user is signed in, check if we've already
		// downloaded their basemaps
		if (AccountManager.getInstance().isSignedIn()) {
			if (itemsLoadedFromCache(PRIVATE_BASEMAPS)) {
				mProgressDialog.dismiss();
			} else {
				getBasemapsFromUserPortal();
			}
		} else { // user is not signed in, but have
			// they previously retrieved public
			// basemaps from AGOL
			if (itemsLoadedFromCache(PUBLIC_BASEMAPS)) {
				mProgressDialog.dismiss();
			} else { // get basemaps from AGOL
				final Portal portal = AccountManager.getInstance().getAGOLPortal();
				if (portal.getPortalInfo() == null) {
					portal.addDoneLoadingListener(new Runnable() {
						@Override
						public void run() {
							loadBasemapsFromAGOL(portal);
						}
					});
					portal.loadAsync();
				} else {
					loadBasemapsFromAGOL(portal);
				}
			}
		}
	}

	private boolean itemsLoadedFromCache(String cacheName){

		if (PersistBasemaps.getInstance().storage.get(cacheName) != null){
			List<BasemapItem> cachedItems = PersistBasemaps.getInstance().storage.get(cacheName);
			Log.i(TAG, "Getting items " + cacheName + " " + cachedItems.size());
			mBasemapItemList.clear();
			mBasemapItemList.addAll(cachedItems);
			mBasemapsAdapter.notifyDataSetChanged();
			return true;
		}else{
			return false;
		}
	}

	/*
	 * Get basemap content from user's portal
	 */
	private void getBasemapsFromUserPortal() {
		// we are signed in - fetch the basemaps of the user's portal
		final Portal portal = AccountManager.getInstance().getPortal();
		PortalInfo portalInfo = AccountManager.getInstance().getPortalInfo();

		PortalQueryParameters queryParams = new PortalQueryParameters();

		// get the query string to fetch the portal
		// group that defines the portal's basemaps
		queryParams.setQuery(portalInfo.getBasemapGalleryGroupQuery());

		// Use a listenable future for retrieving search results from portal
		final ListenableFuture<PortalQueryResultSet<PortalGroup>> groupFuture = portal.findGroupsAsync(queryParams);
		groupFuture.addDoneListener(new Runnable() {
			@Override

			public void run() {

				try {
					PortalQueryResultSet<PortalGroup> basemapGroupResult = groupFuture.get();
					if (basemapGroupResult != null && basemapGroupResult.getResults() != null
							&& !basemapGroupResult.getResults().isEmpty()) {

						PortalGroup group = basemapGroupResult.getResults().get(0);

						PortalQueryParameters basemapQueryParams = new PortalQueryParameters();
						basemapQueryParams.setQueryForItemsInGroup(group.getGroupId());

						final ListenableFuture<PortalQueryResultSet<PortalItem>> contentFuture = portal
								.findItemsAsync(basemapQueryParams);
						contentFuture.addDoneListener(new Runnable() {
							@Override
							public void run() {
								try {
									PortalQueryResultSet<PortalItem> items = contentFuture.get();
									mPortalItems.addAll(items.getResults());
									getBasemapThumbnails();
									PersistBasemaps.getInstance().storage.put(PRIVATE_BASEMAPS,mBasemapItemList);
									Log.i(TAG, "Persisting " + PRIVATE_BASEMAPS + " wtih " + mBasemapItemList.size() + " items");
								} catch (Exception e) {
									mProgressDialog.dismiss();
									e.printStackTrace();
								}

							}
						});
					}
				} catch (Exception ie) {
					ie.printStackTrace();
				}
			}
		});
	}

	private void loadBasemapsFromAGOL(final Portal portal) {
		//Provides information about a portal as seen by the current user, anonymous or logged in. 
		PortalInfo portalInfo = portal.getPortalInfo();
		// Get the query string for items in basemap group 
		String baseMapQueryString = portalInfo.getBasemapGalleryGroupQuery();
		//Create query parameters suitable for finding content or groups contained in a portal 
		PortalQueryParameters queryParams = new PortalQueryParameters(baseMapQueryString);
		// Limit query to publicly available items
		queryParams.setCanSearchPublic(true);
		final ListenableFuture<PortalQueryResultSet<PortalGroup>> groupFuture = portal.findGroupsAsync(queryParams);
		// Listen for completion
		groupFuture.addDoneListener(new Runnable() {
			@Override public void run() {
				try {
					PortalQueryResultSet<PortalGroup> groupResults = groupFuture.get();
					if (!groupResults.getResults().isEmpty()) {
						PortalGroup basemapGroup = groupResults.getResults().get(0);
						String groupId = basemapGroup.getGroupId();
						// Build a new query param object to retrieve basemaps for given group
						PortalQueryParameters basemapQuery = new PortalQueryParameters();
						basemapQuery.setQuery(PortalItem.Type.WEBMAP, groupId, null);
						basemapQuery.setCanSearchPublic(true);
						// Set sort order on basemap name 
						basemapQuery.setSortField("name").setSortOrder(PortalQueryParameters.SortOrder.ASCENDING);
						// Find items that match the query
						final ListenableFuture<PortalQueryResultSet<PortalItem>> itemFuture = portal.findItemsAsync(basemapQuery);
						itemFuture.addDoneListener(new Runnable() {
							@Override
							public void run() {
								try {
									PortalQueryResultSet<PortalItem> items = itemFuture.get();
									mPortalItems.addAll(items.getResults());
									getBasemapThumbnails();
									PersistBasemaps.getInstance().storage.put(PUBLIC_BASEMAPS,mBasemapItemList);
									Log.i(TAG, "Persisting " + PUBLIC_BASEMAPS + " wtih " + mBasemapItemList.size() + " items");
								} catch (Exception e) {
									mProgressDialog.dismiss();
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

	/**
	 * Get thumbnails asycnchronously and populate the dialog
	 * adapter with basemap items as thumbnails are retrieved.
	 */
	private void getBasemapThumbnails() {
		for (final PortalItem item : mPortalItems) {
			if (item.getThumbnailFileName() != null) {
				final ListenableFuture<byte[]> futureThumbnail = item.fetchThumbnailAsync();
				futureThumbnail.addDoneListener(new Runnable() {

					@Override
					public void run() {
						try {
							byte[] itemThumbnailData = futureThumbnail.get();
							if (itemThumbnailData != null && itemThumbnailData.length > 0) {
								// Decode thumbnail and add this item to list
								// for display
								Bitmap bitmap = BitmapFactory.decodeByteArray(itemThumbnailData, 0,
										itemThumbnailData.length);
								BasemapItem portalItemData = new BasemapItem(item, bitmap);
								mBasemapItemList.add(portalItemData);
								// Update grid with results
								mBasemapsAdapter.notifyDataSetChanged();
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				});
			}
		}
		mProgressDialog.dismiss();
	}
	// If basemap item is persisted do not go out to service to fetch them again

	/**
	 * A callback interface that all activities containing this fragment must
	 * implement, to receive a new basemap from this fragment.
	 */
	public interface BasemapsDialogListener {
		/**
		 * Called when a basemap is selected.
		 *
		 * @param itemId
		 *            portal item id of the selected basemap
		 */
		void onBasemapChanged(String itemId);
	}
}