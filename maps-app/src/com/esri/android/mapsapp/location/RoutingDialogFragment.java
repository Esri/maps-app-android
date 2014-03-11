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

package com.esri.android.mapsapp.location;

import android.app.DialogFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.esri.android.mapsapp.R;

public class RoutingDialogFragment extends DialogFragment {
  public static final String ARG_END_POINT_DEFAULT = "EndPointDefault";
  
  String mEndPointDefault;

  EditText mStartText;

  EditText mEndText;

  RoutingDialogListener mRoutingDialogListener;

  /**
   * A callback interface that all activities containing this fragment must implement, to receive a routing request
   * from this fragment.
   */
  public interface RoutingDialogListener {
    /**
     * Callback for when the Get Route button is pressed.
     * 
     * @param startPoint String entered by user to define start point.
     * @param endPoint String entered by user to define end point.
     * @return true if routing task executed, false if parameters rejected. If this method rejects the parameters it
     *         must display an explanatory Toast to the user before returning.
     */
    public boolean onGetRoute(String startPoint, String endPoint);
  }

  // Mandatory empty constructor for fragment manager to recreate fragment after it's destroyed.
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
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.routing_layout, container, false);
    getDialog().setTitle(R.string.title_routing_dialog);
    mStartText = (EditText) view.findViewById(R.id.startPoint);
    mEndText = (EditText) view.findViewById(R.id.endPoint);
    if (mEndPointDefault != null) {
      mEndText.setText(mEndPointDefault);
    }
    Button button = (Button) view.findViewById(R.id.getRouteButton);
    button.setOnClickListener(new View.OnClickListener() {

      @Override
      public void onClick(View v) {
        String startPoint = mStartText.getText().toString();
        String endPoint = mEndText.getText().toString();
        if (mRoutingDialogListener.onGetRoute(startPoint, endPoint)) {
          dismiss();
        }
      }

    });
    return view;
  }

}
