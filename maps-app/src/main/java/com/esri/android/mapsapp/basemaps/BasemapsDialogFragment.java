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

package com.esri.android.mapsapp.basemaps;

import java.util.ArrayList;
import java.util.List;

import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;

import com.esri.android.mapsapp.R;
import com.esri.android.mapsapp.account.AccountManager;
import com.esri.android.mapsapp.basemaps.BasemapsAdapter.BasemapsAdapterClickListener;
import com.esri.android.mapsapp.dialogs.ProgressDialogFragment;
import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.portal.Portal;
import com.esri.arcgisruntime.portal.PortalGroup;
import com.esri.arcgisruntime.portal.PortalInfo;
import com.esri.arcgisruntime.portal.PortalItem;
import com.esri.arcgisruntime.portal.PortalQueryParams;
import com.esri.arcgisruntime.portal.PortalQueryResultSet;

/**
 * Implements the dialog that provides a collection of basemaps to the user.
 */
public class BasemapsDialogFragment extends DialogFragment implements BasemapsAdapterClickListener, OnCancelListener {

	private static final String TAG = "BasemapsDialogFragment";
	private static final String TAG_BASEMAP_SEARCH_PROGRESS_DIALOG = "TAG_BASEMAP_SEARCH_PROGRESS_DIALOG";
	private static final int REQUEST_CODE_PROGRESS_DIALOG = 1;
	private final List<PortalItem> mBasemapResult = new ArrayList<PortalItem>();
	private ProgressDialogFragment mProgressDialog;
	private BasemapsDialogListener mBasemapsDialogListener;
	private BasemapsAdapter mBasemapsAdapter;
	private ArrayList<BasemapItem> mBasemapItemList;

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
		setStyle(DialogFragment.STYLE_NORMAL, 0);

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
		mProgressDialog.setTargetFragment(BasemapsDialogFragment.this, REQUEST_CODE_PROGRESS_DIALOG);

		// If no basemaps yet, execute search for available basemaps and
		// populate the grid with them.
		if (mBasemapItemList.size() == 0) {

			fetchBasemapItems();
		}
		return view;
	}

	@Override
	public void onResume() {
		super.onResume();
	}

	@Override
	public void onBasemapItemClicked(int position) {
		dismiss();

		String itemId = mBasemapItemList.get(position).item.getId();
		mBasemapsDialogListener.onBasemapChanged(itemId);
	}

	@Override
	public void onCancel(DialogInterface dialog) {
		// the progress dialog has been canceled - cancel pending basemap search
		// task
		// TODO: Anything to cancel?

	}

	/**
	 * Fetch basemaps using ListenableFuture pattern
	 */
	private void fetchBasemapItems() {
		// Show a progress dialog
		mProgressDialog.show(getActivity().getFragmentManager(), TAG_BASEMAP_SEARCH_PROGRESS_DIALOG);

		if (AccountManager.getInstance().isSignedIn()) {
			getBasemapsFromUserPortal();
		} else {
			getBasemapsFromAGOL();
		}
	}

	/**
	 * Creates a query string to fetch basemap portal items from arcgis.com.
	 */
	private String createDefaultQueryString() {
		String query;

		String[] mBasemapIds = {"d5e02a0c1f2b4ec399823fdd3c2fdebd", // topographic
				"716b600dbbac433faa4bec9220c76b3a", // imagery with labels
				"b834a68d7a484c5fb473d4ba90d35e71", // open street map
				"8bf7167d20924cbf8e25e7b11c7c502c", // streets
				"2adf08a4a1a84834a773805a6e86f69e", // Oceans
				"149a9bb14d604bd18f4597b21c19fac7" // Gray
		};

		StringBuilder str = new StringBuilder();
		for (int i = 0; i < mBasemapIds.length; i++) {
			str.append("id:").append(mBasemapIds[i]);
			if (i < mBasemapIds.length - 1) {
				str.append(" OR ");
			}
		}
		query = str.toString();

		return query;
	}

	/*
	 * Get basemap content from user's portal
	 */
	private void getBasemapsFromUserPortal() {
		// we are signed in - fetch the basemaps of the user's portal
		final Portal portal = AccountManager.getInstance().getPortal();
		PortalInfo portalInfo = AccountManager.getInstance().getPortalInfo();

		PortalQueryParams queryParams = new PortalQueryParams();

		// get the query string to fetch the portal group that defines the
		// portal's basemaps
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

						PortalQueryParams basemapQueryParams = new PortalQueryParams();
						basemapQueryParams.setQueryForItemsInGroup(group.getId());

						final ListenableFuture<PortalQueryResultSet<PortalItem>> contentFuture = portal
								.findItemsAsync(basemapQueryParams);
						contentFuture.addDoneListener(new Runnable() {
							@Override
							public void run() {
								try {
									PortalQueryResultSet<PortalItem> items = contentFuture.get();
									mBasemapResult.addAll(items.getResults());
									getBasemapThumbnails();

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

	/*
	 * Get basemap content from ArcGISOnline and assemble basemap items for the
	 * view
	 */
	private void getBasemapsFromAGOL() {
		Portal portal = AccountManager.getInstance().getAGOLPortal();

		// Create a PortalQueryParams to query for items in basemap group
		PortalQueryParams queryParams = new PortalQueryParams();
		queryParams.setSortField("name").setSortOrder(PortalQueryParams.SortOrder.ASCENDING);
		queryParams.setQuery(createDefaultQueryString());

		// Find items that match the query
		final ListenableFuture<PortalQueryResultSet<PortalItem>> itemFuture = portal.findItemsAsync(queryParams);
		itemFuture.addDoneListener(new Runnable() {
			@Override
			public void run() {
				try {
					PortalQueryResultSet<PortalItem> baseMapItems = itemFuture.get();
					mBasemapResult.addAll(baseMapItems.getResults());
					getBasemapThumbnails();
				} catch (Exception itemE) {
					mProgressDialog.dismiss();
					itemE.printStackTrace();
				}
			}
		});
	}

	/**
	 * Get thumbnails asycnchronously and build the adapter item for the view
	 */
	private void getBasemapThumbnails() {
		for (final PortalItem item : mBasemapResult) {
			if (item.getThumbnailFileName() != null) {
				final ListenableFuture<byte[]> futureThumbnail = item.fetchThumbnailAsync();
				futureThumbnail.addDoneListener(new Runnable() {

					@Override
					public void run() {
						try {
							byte[] itemThumbnailData = futureThumbnail.get();
							if ((itemThumbnailData != null) && (itemThumbnailData.length > 0)) {
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
