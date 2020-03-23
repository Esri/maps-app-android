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

package com.esri.arcgisruntime.opensourceapps.mapsapp.location;

import java.util.List;

import android.app.Activity;
import android.app.DialogFragment;
import android.content.Context;
import android.content.res.Resources.NotFoundException;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.esri.arcgisruntime.opensourceapps.mapsapp.R;
import com.esri.arcgisruntime.tasks.networkanalysis.DirectionManeuver;
import com.esri.arcgisruntime.tasks.networkanalysis.DirectionManeuverType;

public class DirectionsDialogFragment extends DialogFragment {

	private static final String TAG = "DirectionsDialogFrag";
	DirectionsDialogListener mDirectionsDialogListener;
	List<DirectionManeuver> mRoutingDirections;

	/**
	 * Mandatory empty constructor for the fragment manager to instantiate the
	 * fragment.
	 */
	public DirectionsDialogFragment() {
	}

	public void setRoutingDirections(List<DirectionManeuver> routingDirections, DirectionsDialogListener listener) {
		mRoutingDirections = routingDirections;
		mDirectionsDialogListener = listener;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setStyle(DialogFragment.STYLE_NORMAL, 0);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.directions_list, container, false);
		getDialog().setTitle(R.string.title_directions_dialog);

		// Setup list adapter
		ListView listView = view.findViewById(R.id.directions_list_view);
		listView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
				mDirectionsDialogListener.onDirectionSelected(position);
			}

		});
		listView.setAdapter(new DirectionsListAdapter(mRoutingDirections));
		return view;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
	}

	@Override
	public void onDetach() {
		super.onDetach();
	}

	/**
	 * A callback interface that all activities containing this fragment must
	 * implement, to be informed when a particular direction is selected from
	 * the list.
	 */
	public interface DirectionsDialogListener {
		/**
		 * Callback for when a particular direction is selected from the list.
		 *
		 * @param position
		 *            Position in Routing Directions list of the selected
		 *            direction.
		 */
		void onDirectionSelected(int position);
	}

	/**
	 * List adapter for the list of route directions.
	 */
	private class DirectionsListAdapter extends ArrayAdapter<DirectionManeuver> {
		public DirectionsListAdapter(List<DirectionManeuver> directions) {
			super(getActivity(), 0, directions);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			// Inflate view if we haven't been given one to reuse
			View v = convertView;
			if (convertView == null) {
				v = getActivity().getLayoutInflater().inflate(R.layout.directions_list_item, parent, false);
			}

			// Configure the view for this item
			DirectionManeuver direction = getItem(position);
			ImageView imageView = v.findViewById(R.id.directions_maneuver_imageview);
			Drawable drawable = getRoutingIcon(direction.getManeuverType());
			if (drawable != null) {
				imageView.setImageDrawable(drawable);
			}
			TextView textView = v.findViewById(R.id.directions_text_textview);
			textView.setText(direction.getDirectionText());
			textView = v.findViewById(R.id.directions_length_textview);
			String lengthString = String.format("%.1f meters", direction.getLength());
			textView.setText(lengthString);
			return v;
		}

		private Drawable getRoutingIcon(DirectionManeuverType maneuver) {
			Context context = getActivity();
			int id;
			switch (maneuver) {
				case STRAIGHT :
					id = R.drawable.ic_routing_straight_arrow;
					break;
				case BEAR_LEFT :
					id = R.drawable.ic_routing_bear_left;
					break;
				case BEAR_RIGHT :
					id = R.drawable.ic_routing_bear_right;
					break;
				case TURN_LEFT :
					id = R.drawable.ic_routing_turn_left;
					break;
				case TURN_RIGHT :
					id = R.drawable.ic_routing_turn_right;
					break;
				case SHARP_LEFT :
					id = R.drawable.ic_routing_turn_sharp_left;
					break;
				case SHARP_RIGHT :
					id = R.drawable.ic_routing_turn_sharp_right;
					break;
				case U_TURN :
					id = R.drawable.ic_routing_u_turn;
					break;
				case FERRY :
					id = R.drawable.ic_routing_take_ferry;
					break;
				case ROUNDABOUT :
					id = R.drawable.ic_routing_get_on_roundabout;
					break;
				case HIGHWAY_MERGE :
					id = R.drawable.ic_routing_merge_onto_highway;
					break;
				case HIGHWAY_CHANGE :
					id = R.drawable.ic_routing_highway_change;
					break;
				case FORK_CENTER :
					id = R.drawable.ic_routing_take_center_fork;
					break;
				case FORK_LEFT :
					id = R.drawable.ic_routing_take_fork_left;
					break;
				case FORK_RIGHT :
					id = R.drawable.ic_routing_take_fork_right;
					break;
				case END_OF_FERRY :
					id = R.drawable.ic_routing_get_off_ferry;
					break;
				case RAMP_RIGHT :
					id = R.drawable.ic_routing_take_ramp_right;
					break;
				case RAMP_LEFT :
					id = R.drawable.ic_routing_take_ramp_left;
					break;
				case TURN_LEFT_RIGHT :
					id = R.drawable.ic_routing_left_right;
					break;
				case TURN_RIGHT_LEFT :
					id = R.drawable.ic_routing_right_left;
					break;
				case TURN_RIGHT_RIGHT :
					id = R.drawable.ic_routing_right_right;
					break;
				case TURN_LEFT_LEFT :
					id = R.drawable.ic_routing_left_left;
					break;
				case STOP :
					id = R.drawable.pin_circle_blue;
					break;
				case HIGHWAY_EXIT :
				case DEPART :
				case TRIP_ITEM :
				case PEDESTRIAN_RAMP :
				case ELEVATOR :
				case ESCALATOR :
				case STAIRS :
				case DOOR_PASSAGE :
				default :
					Log.w(TAG, maneuver.name() + "not supported");
					return null;
			}
			try {
				return context.getResources().getDrawable(id);
			} catch (NotFoundException e) {
				Log.w(TAG, "No drawable found for" + maneuver.name());
				return null;
			}
		}

	}

}
