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
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import android.app.Fragment;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import com.esri.android.mapsapp.R.drawable;
import com.esri.android.mapsapp.R.id;
import com.esri.android.mapsapp.R.layout;
import com.esri.android.mapsapp.R.string;
import com.esri.android.mapsapp.account.AccountManager;
import com.esri.android.mapsapp.dialogs.ProgressDialogFragment;
import com.esri.android.mapsapp.util.TaskExecutor;
import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.portal.Portal;
import com.esri.arcgisruntime.portal.PortalItem;
import com.esri.arcgisruntime.portal.PortalItemType;
import com.esri.arcgisruntime.portal.PortalUser;
import com.esri.arcgisruntime.portal.PortalUserContent;

/**
 * Implements the view that shows the user's maps. Tapping on a map will open
 * it.
 */
public class ContentBrowserFragment extends Fragment implements View.OnClickListener {

	public final static String TAG = ContentBrowserFragment.class.getSimpleName();
	private static final String TAG_FETCH_MAPS_PROGRESS_DIALOG = "TAG_FETCH_MAPS_PROGRESS_DIALOG";
	private GridView mMapGrid;
	private View mNoMapsInfo;
	private List<PortalItem> mMaps;
	private ProgressDialogFragment mProgressDialog;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(layout.content_browser_fragment_layout, null);

		mMapGrid = (GridView) view.findViewById(id.content_browser_fragment_gridview);
		mMapGrid.setVisibility(View.GONE);

		mNoMapsInfo = view.findViewById(id.content_browser_fragment_no_maps_layout);
		mNoMapsInfo.setVisibility(View.GONE);

		View refreshButton = view.findViewById(id.content_browser_fragment_refresh_button);
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
			case id.map_item_linearlayout :
				// a map item has been clicked - open it
				ContentBrowserFragment.ViewHolder viewHolder = (ContentBrowserFragment.ViewHolder) view.getTag();
				((MapsAppActivity) getActivity()).showMap(viewHolder.portalItem.getId(), null);
				break;
			case id.content_browser_fragment_refresh_button :
				// re-fetch maps
				fetchMyMaps();
				break;
		}
	}

	/**
	 * Fetches the user's maps from the portal.
	 */
	private void fetchMyMaps() {
		mProgressDialog = ProgressDialogFragment.newInstance(getActivity().getString(string.fetching_maps));
		mProgressDialog.show(getActivity().getFragmentManager(), ContentBrowserFragment.TAG_FETCH_MAPS_PROGRESS_DIALOG);
		// new FetchMapsTask().execute();

		final List<PortalItem> webMapItems = new ArrayList<>();
		try {
			// fetch the user's maps from the portal
			Portal portal = AccountManager.getInstance().getPortal();
			if (portal != null) {
				PortalUser portalUser = portal.getPortalUser();
				final ListenableFuture<PortalUserContent> contentFuture = portalUser.fetchContentAsync();
				contentFuture.addDoneListener(new Runnable() {

					@Override
					public void run() {
						try {
							final PortalUserContent content = contentFuture.get();
							List<PortalItem> rootItems = content != null ? content.getItems() : null;
							if (rootItems != null) {
								// only select items of type WEBMAP
								for (PortalItem item : rootItems) {
									if (item.getType() == PortalItemType.WEBMAP) {
										webMapItems.add(item);
									}
								}
							}
							mMaps = webMapItems;
							refreshView();

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
			ContentBrowserFragment.ViewHolder viewHolder;
			if (view == null) {
				viewHolder = new ContentBrowserFragment.ViewHolder();
				view = getActivity().getLayoutInflater().inflate(layout.map_item_layout, null);
				view.setOnClickListener(ContentBrowserFragment.this);
				view.setTag(viewHolder);

				viewHolder.title = (TextView) view.findViewById(id.map_item_title_textView);
				viewHolder.thumbnailImageView = (ImageView) view.findViewById(id.map_item_thumbnail_imageView);
			} else {
				viewHolder = (ContentBrowserFragment.ViewHolder) view.getTag();
			}

			PortalItem portalItem = mMaps.get(position);

			viewHolder.title.setText(portalItem.getTitle());
			viewHolder.thumbnailImageView.setImageResource(drawable.ic_map_thumbnail); // use
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

		private final ContentBrowserFragment.ViewHolder mViewHolder;

		public FetchPortalItemThumbnailTask(ContentBrowserFragment.ViewHolder viewHolder) {
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
				Options options = new Options();
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
