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

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.esri.android.mapsapp.R.id;
import com.esri.android.mapsapp.R.layout;

public class BasemapsAdapter extends BaseAdapter {

	BasemapsAdapter.BasemapsAdapterClickListener mOnClickListener;
	// need context to use it to construct view
	Context mContext;
	// hold onto a copy of all basemap items
	List<BasemapItem> items;

	public BasemapsAdapter(Context c) {
		mContext = c;
	}

	public BasemapsAdapter(Context c, ArrayList<BasemapItem> portalItems, BasemapsAdapter.BasemapsAdapterClickListener listener) {
		mContext = c;
		items = portalItems;
		mOnClickListener = listener;
	}

	@Override
	public void notifyDataSetChanged() {
		super.notifyDataSetChanged();
	}

	@Override
	public int getCount() {
		return items == null ? 0 : items.size();
	}

	@Override
	public Object getItem(int position) {
		return items.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {

		// Inflate view unless we have an old one to reuse
		View newView = convertView;
		if (convertView == null) {
			LayoutInflater inflator = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			newView = inflator.inflate(layout.basemap_image, null);
		}

		// Create view for the thumbnail
		ImageView image = (ImageView) newView.findViewById(id.basemap_grid_item_thumbnail_imageview);
		image.setImageBitmap(items.get(position).itemThumbnail);

		// Register listener for clicks on the thumbnail
		image.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(final View view) {
				mOnClickListener.onBasemapItemClicked(position);
			}
		});

		// Set the title and return the view we've created
		TextView text = (TextView) newView.findViewById(id.basemap_grid_item_title_textview);
		text.setText(items.get(position).item.getTitle());
		return newView;
	}

	/**
	 * A callback interface that indicates when a basemap in the list has been
	 * clicked.
	 */
	public interface BasemapsAdapterClickListener {
		/**
		 * Callback for when a basemap list item is clicked.
		 *
		 * @param listPosition
		 *            Position within the list of an item that has been clicked.
		 */
		void onBasemapItemClicked(int listPosition);
	}

}
