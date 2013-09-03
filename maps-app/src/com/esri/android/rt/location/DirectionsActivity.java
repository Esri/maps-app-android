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

import com.arcgis.android.app.map.R;
import com.esri.android.rt.map.MapsApp;

import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.Menu;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

public class DirectionsActivity extends Activity {

	// UI definitions
	EditText startText;
	EditText endText;

	int basemap;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.directions_layout);

		startText = (EditText) findViewById(R.id.myLocation);
		endText = (EditText) findViewById(R.id.endPoint);
		Bundle extras = getIntent().getExtras();
		basemap = extras.getInt("basemap");

	}

	public void sendDirections(View view) {
		// hide virtual keyboard
		InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		inputManager.hideSoftInputFromWindow(
				getCurrentFocus().getWindowToken(), 0);
		// obtain start and end points
		String startPoint = startText.getText().toString();
		String endPoint = endText.getText().toString();
		// send to BasemapsActivity for routing
		Intent intent = new Intent(DirectionsActivity.this, MapsApp.class);
		intent.putExtra("start", startPoint);
		intent.putExtra("end", endPoint);
		intent.putExtra("basemap", basemap);
		startActivity(intent);

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.directions, menu);
		return true;
	}

}
