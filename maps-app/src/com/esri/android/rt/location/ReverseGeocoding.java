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

package com.esri.android.rt.location;

import java.util.Map;

import android.app.Activity;
import android.os.AsyncTask;
import android.widget.Toast;

import com.esri.android.map.MapView;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.SpatialReference;
import com.esri.core.tasks.ags.geocode.Locator;
import com.esri.core.tasks.ags.geocode.LocatorReverseGeocodeResult;

public class ReverseGeocoding extends AsyncTask<Point, Void, LocatorReverseGeocodeResult> {
    private Activity currentActivity;
    private MapView mapView;

    public ReverseGeocoding(Activity activity, MapView map){
        this.currentActivity = activity;
        this.mapView = map;
    }

    @Override
    protected LocatorReverseGeocodeResult doInBackground(Point... params) {
        // create results object and set to null
        LocatorReverseGeocodeResult result = null;
        // set the geocode service
        Locator locator = new Locator();
        try {

            // Attempt to reverse geocode the point.
            // Our input and output spatial reference will be the same as the map.
            SpatialReference mapRef = mapView.getSpatialReference();
            result = locator.reverseGeocode(params[0], 50.0, mapRef, mapRef);
        } catch (Exception e) {
            e.printStackTrace();
        }
        // return the resulting point(s)
        return result;
    }

    protected void onPostExecute(LocatorReverseGeocodeResult result) {

        String resultAddress;

        // Construct a nicely formatted address from the results
        StringBuilder address = new StringBuilder();
        if (result != null && result.getAddressFields() != null) {
            Map<String, String> addressFields = result.getAddressFields();
            address.append(String.format("%s\n%s, %s %s",
                    addressFields.get("Address"), addressFields.get("City"),
                    addressFields.get("Region"), addressFields.get("Postal")));

            // Show the results of the reverse geocoding in a toast.
            resultAddress = address.toString();
            Toast.makeText(currentActivity, resultAddress, Toast.LENGTH_LONG).show();
        }
    }
}

