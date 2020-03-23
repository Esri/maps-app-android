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

package com.esri.arcgisruntime.opensourceapps.mapsapp;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import android.app.Fragment;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.*;

import com.esri.arcgisruntime.opensourceapps.mapsapp.account.AccountManager;
import com.esri.arcgisruntime.opensourceapps.mapsapp.dialogs.ProgressDialogFragment;
import com.esri.arcgisruntime.opensourceapps.mapsapp.util.TaskExecutor;
import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.portal.Portal;
import com.esri.arcgisruntime.portal.PortalItem;
import com.esri.arcgisruntime.portal.PortalUser;
import com.esri.arcgisruntime.portal.PortalUserContent;

/**
 * Implements the view that shows the user's maps. Tapping on a map will open
 * it.
 */
public class ContentBrowserFragment extends Fragment implements OnClickListener {

	public final static String TAG = ContentBrowserFragment.class.getSimpleName();
	private static final String TAG_FETCH_MAPS_PROGRESS_DIALOG = "TAG_FETCH_MAPS_PROGRESS_DIALOG";
	private GridView mMapGrid;
	private View mNoMapsInfo;
	private List<PortalItem> mMaps;
	private ProgressDialogFragment mProgressDialog;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.content_browser_fragment_layout, null);

		mMapGrid = view.findViewById(R.id.content_browser_fragment_gridview);
		mMapGrid.setVisibility(View.GONE);

		mNoMapsInfo = view.findViewById(R.id.content_browser_fragment_no_maps_layout);
		mNoMapsInfo.setVisibility(View.GONE);

		View refreshButton = view.findViewById(R.id.content_browser_fragment_refresh_button);
		refreshButton.setOnClickListener(this);

		if (mMaps == null || mMaps.isEmpty()) {
			// fetch the user's maps
			fetchMyMaps();
		} else {
			refreshView();
		}

		return view;
	}

	@Override
	public void onClick(View view) {
		switch (view.getId()) {
			case R.id.map_item_linearlayout :
				// a map item has been clicked - open it
				ViewHolder viewHolder = (ViewHolder) view.getTag();
				((MapsAppActivity) getActivity()).showMap(viewHolder.portalItem.getItemId(), null);
				break;
			case R.id.content_browser_fragment_refresh_button :
				// re-fetch maps
				fetchMyMaps();
				break;
		}
	}

	/**
	 * Fetches the user's maps from the portal.
	 */
	private void fetchMyMaps() {
		// Show a progress dialog while we retrieve web maps...
		mProgressDialog = ProgressDialogFragment.newInstance(getActivity().getString(R.string.fetching_maps));
		mProgressDialog.show(getActivity().getFragmentManager(), TAG_FETCH_MAPS_PROGRESS_DIALOG);

		final List<PortalItem> webMapItems = new ArrayList<>();
		try {
			// Get the Portal the user is signed in to.
			Portal portal = AccountManager.getInstance().getPortal();
			if (portal != null) {
				// Get user content from the portal
				PortalUser portalUser = portal.getUser();

				final ListenableFuture<PortalUserContent> contentFuture = portalUser.fetchContentAsync();
				// Filter returned content for WEBMAP items
				contentFuture.addDoneListener(new Runnable() {

					@Override
					public void run() {
						try {
							final PortalUserContent content = contentFuture.get();
							List<PortalItem> rootItems = content != null ? content.getItems() : null;
							if (rootItems != null) {
								// only select items of type WEBMAP
								for (PortalItem item : rootItems) {
									if (item.getType() == PortalItem.Type.WEBMAP) {
										webMapItems.add(item);
									}
								}
							}
							mMaps = webMapItems;
							// Show the web maps in the grid view
							refreshView();

							// Dismiss the progress dialog
							mProgressDialog.dismiss();
						} catch (Exception e) {
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
	 * Refreshes the GridView that shows the maps.
	 */
	private void refreshView() {
		if (mMaps == null || mMaps.isEmpty()) {
			mMapGrid.setVisibility(View.GONE);
			mNoMapsInfo.setVisibility(View.VISIBLE);
		} else {
			mMapGrid.setVisibility(View.VISIBLE);
			mNoMapsInfo.setVisibility(View.GONE);

			BaseAdapter mapGridAdapter = (BaseAdapter) mMapGrid.getAdapter();
			if (mapGridAdapter == null) {
				mapGridAdapter = new MapGridAdapter();
				mMapGrid.setAdapter(mapGridAdapter);
			} else {
				mapGridAdapter.notifyDataSetChanged();
			}
		}
	}
	@Override
	public void onResume() {
		super.onResume();

		getView().setFocusableInTouchMode(true);
		getView().requestFocus();
		getView().setOnKeyListener(new View.OnKeyListener() {
			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if (event.getAction() == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_BACK) {
					// handle back button's click listener
					dismiss();
					return true;
				}
				return false;
			}
		});

	}
	private void dismiss(){
		getActivity().getFragmentManager().popBackStack();
	}
	/**
	 * Populates the ContentBrowserFragment's GridView with the user's maps.
	 */
	private class MapGridAdapter extends BaseAdapter {

		@Override
		public int getCount() {
			return mMaps.size();
		}

		@Override
		public Object getItem(int position) {
			return mMaps.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View view = convertView;
			ViewHolder viewHolder;
			if (view == null) {
				viewHolder = new ViewHolder();
				view = getActivity().getLayoutInflater().inflate(R.layout.map_item_layout, null);
				view.setOnClickListener(ContentBrowserFragment.this);
				view.setTag(viewHolder);

				viewHolder.title = view.findViewById(R.id.map_item_title_textView);
				viewHolder.thumbnailImageView = view.findViewById(R.id.map_item_thumbnail_imageView);
			} else {
				viewHolder = (ViewHolder) view.getTag();
			}

			PortalItem portalItem = mMaps.get(position);

			viewHolder.title.setText(portalItem.getTitle());
			viewHolder.thumbnailImageView.setImageResource(R.drawable.ic_map_thumbnail); // use
																							// default
																							// thumbnail
																							// temporarily
			viewHolder.portalItem = portalItem;
			viewHolder.fetchTumbnail();

			return view;
		}
	}

	/**
	 * View holder for a PortalItem. Also supports fetching the thumbnail for
	 * the PortalItem.
	 */
	private class ViewHolder {
		TextView title;

		ImageView thumbnailImageView;

		PortalItem portalItem;

		Future<Void> thumbnailFetchTask;

		/**
		 * Cancels any pending thumbnail fetch task and fetches a new thumbnail
		 * for the corresponding PortalItem, unless a cached thumbnail already
		 * exists.
		 */
		void fetchTumbnail() {
			if (thumbnailFetchTask != null) {
				thumbnailFetchTask.cancel(true);
			}

			thumbnailFetchTask = TaskExecutor.getInstance().getThreadPool()
					.submit(new FetchPortalItemThumbnailTask(this));
		}
	}

	/**
	 * Fetches the thumbnail of a PortalItem and sets it into the corresponding
	 * ImageView. Handles task cancellation by checking for the thread's
	 * interrupted state.
	 */
	private class FetchPortalItemThumbnailTask implements Callable<Void> {

		private final ViewHolder mViewHolder;

		public FetchPortalItemThumbnailTask(ViewHolder viewHolder) {
			mViewHolder = viewHolder;
		}

		@Override
		public Void call() throws Exception {
			byte[] thumbnailBytes = null;

			// check if task has been cancelled
			if (Thread.currentThread().isInterrupted()) {
				throw new InterruptedException();
			}

			if (mViewHolder != null) {
				thumbnailBytes = mViewHolder.portalItem.getThumbnailData();
			}

			// check if task has been cancelled
			if (Thread.currentThread().isInterrupted()) {
				throw new InterruptedException();
			}

			if (thumbnailBytes != null && thumbnailBytes.length > 0) {
				BitmapFactory.Options options = new BitmapFactory.Options();
				options.inPurgeable = true;
				final Bitmap bmp = BitmapFactory.decodeByteArray(thumbnailBytes, 0, thumbnailBytes.length, options);

				getActivity().runOnUiThread(new Runnable() {

					@Override
					public void run() {
						mViewHolder.thumbnailImageView.setImageBitmap(bmp);
					}
				});
			}

			return null;
		}
	}
}
