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

package com.esri.android.mapsapp.location;

import android.app.DialogFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SearchView;

import com.esri.android.mapsapp.R;

public class RoutingDialogFragment extends DialogFragment {
	public static final String ARG_END_POINT_DEFAULT = "EndPointDefault";
	
	public static final String MY_LOCATION = "My Location";
	
	private static final String SEARCH_FROM = "From";
	
	private static final String SEARCH_TO = "To";


	String mEndPointDefault;

	SearchView mStartText;

	SearchView mEndText;

	RoutingDialogListener mRoutingDialogListener;

	ImageView mSwap;

	Button mButton;

	/**
	 * A callback interface that all activities containing this fragment must
	 * implement, to receive a routing request from this fragment.
	 */
	public interface RoutingDialogListener {
		/**
		 * Callback for when the Get Route button is pressed.
		 * 
		 * @param startPoint
		 *            String entered by user to define start point.
		 * @param endPoint
		 *            String entered by user to define end point.
		 * @return true if routing task executed, false if parameters rejected.
		 *         If this method rejects the parameters it must display an
		 *         explanatory Toast to the user before returning.
		 */
		public boolean onGetRoute(String startPoint, String endPoint);
	}

	// Mandatory empty constructor for fragment manager to recreate fragment
	// after it's destroyed.
	public RoutingDialogFragment() {
	}

	/**
	 * Sets listener for click on Get Route button.
	 * 
	 * @param listener
	 */
	public void setRoutingDialogListener(RoutingDialogListener listener) {
		mRoutingDialogListener = listener;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setStyle(DialogFragment.STYLE_NORMAL, 0);

		if (getArguments().containsKey(ARG_END_POINT_DEFAULT)) {
			mEndPointDefault = getArguments().getString(ARG_END_POINT_DEFAULT);
		} else {
			mEndPointDefault = null;
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.routing_layout, container, false);
		getDialog().setTitle(R.string.title_routing_dialog);
		//Initialize searchviews
		mStartText = (SearchView) view.findViewById(R.id.startPoint);
		mEndText = (SearchView) view.findViewById(R.id.endPoint);

		mStartText.setIconifiedByDefault(false);
		mEndText.setIconifiedByDefault(false);
		
		//Set hint for searchviews
		mStartText.setQueryHint(SEARCH_FROM);
		mEndText.setQueryHint(SEARCH_TO);

		
		//Change default search icons for the search view
		int startIconId = mStartText.getContext().getResources().
                getIdentifier("android:id/search_mag_icon", null, null);
		ImageView start_icon = (ImageView)mStartText.findViewById(startIconId);
		start_icon.setImageResource(R.drawable.pin_circle_red);

		int endIconId = mEndText.getContext().getResources().
                getIdentifier("android:id/search_mag_icon", null, null);
		ImageView end_icon = (ImageView)mEndText.findViewById(endIconId);
		end_icon.setImageResource(R.drawable.pin_circle_blue);

		
		mStartText.setQuery(MY_LOCATION, false);
		mStartText.clearFocus();
		mEndText.requestFocus();
		if (mEndPointDefault != null) {
			mEndText.setQuery(mEndPointDefault, false);
		}
		mSwap = (ImageView) view.findViewById(R.id.iv_interchange);

		mButton = (Button) view.findViewById(R.id.getRouteButton);
		mButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				String startPoint = mStartText.getQuery().toString();
				String endPoint = mEndText.getQuery().toString();
				if (mRoutingDialogListener.onGetRoute(startPoint, endPoint)) {
					dismiss();
				}
			}

		});
		
		mSwap.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				//Swap the places
				String temp = mStartText.getQuery().toString();
				mStartText.setQuery(mEndText.getQuery().toString(),false);
				mEndText.setQuery(temp,false);
				
			}
		});
		
		return view;
	}

}
