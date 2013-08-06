
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
package com.esri.android.rt.map;

import com.arcgis.android.app.map.R;

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
	
	public void sendDirections(View view){
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
