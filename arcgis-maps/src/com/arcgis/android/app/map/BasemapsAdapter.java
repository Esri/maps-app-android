/* Copyright 2013 ESRI
 *
 * All rights reserved under the copyright laws of the United States
 * and applicable international laws, treaties, and conventions.
 *
 * You may freely redistribute and use this sample code, with or
 * without modification, provided you include the original copyright
 * notice and use restrictions.
 *
 * See the Sample code usage restrictions document for further information.
 *
 */

package com.arcgis.android.app.map;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.os.StrictMode;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.esri.android.map.MapView;
import com.esri.core.portal.BaseMap;
import com.esri.core.portal.Portal;
import com.esri.core.portal.WebMap;

public class BasemapsAdapter extends BaseAdapter {

	// need context to use it to construct view
	private Context mContext;
	// hold onto a copy of all basemap items
	private List<BasemapItem> items;

	MapView updateMapView;
	Portal mPortal;

	// recreation web map
	WebMap recWebmap;
	// base webmap service
	WebMap baseWebmap;
	// basemap selected from baseWebmap
	BaseMap selectedBasemap;

	String basemapID;

	/**
	 * @param mContext
	 */
	public BasemapsAdapter(Context c) {
		mContext = c;
	}

	public BasemapsAdapter(Context c, ArrayList<BasemapItem> portalItems,
			MapView aMapView) {
		mContext = c;
		this.items = portalItems;
		updateMapView = aMapView;
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

	/**
	 * @param convertView
	 *            The old view to overwrite if one is passed
	 * @returns an ImageView that contains Basemaps and title descriptions
	 */
	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
		// TODO move off UI thread
		StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
		StrictMode.setThreadPolicy(policy); 
		
		if (convertView == null) {
			LayoutInflater inflator = (LayoutInflater) mContext
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			convertView = inflator.inflate(R.layout.basemap_image, null);
		}

		ImageView image = (ImageView) convertView
				.findViewById(R.id.listImageView);
		image.setImageBitmap(items.get((position)).itemThumbnail);

		image.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				
				
				String url = "http://www.arcgis.com";
				mPortal = new Portal(url, null);
				
				basemapID = items.get(position).item.getItemId();
				try {
					// recreation webmap item id to create a @WebMap
					String itemID = mContext.getString(R.string.rec_webmap_id);
					// create recreation Webmap
					recWebmap = WebMap.newInstance(itemID, mPortal);
					// create a new WebMap of selected basemap from default
					// portal
					baseWebmap = WebMap.newInstance(basemapID, mPortal);

				} catch (Exception e) {
					e.printStackTrace();
				}
				// Get the WebMaps basemap
				selectedBasemap = baseWebmap.getBaseMap();
				// switch basemaps on the recreation webmap
				updateMapView = new MapView(mContext, recWebmap,
						selectedBasemap, null, null);
				// reset the content view for the updated MapView
				MapsApp basemapsActivity = (MapsApp) mContext;
				basemapsActivity.setMapView(updateMapView);
			}
		});

		TextView text = (TextView) convertView.findViewById(R.id.listTextView);
		text.setText(items.get((position)).item.getTitle());

		return convertView;

	}

}
