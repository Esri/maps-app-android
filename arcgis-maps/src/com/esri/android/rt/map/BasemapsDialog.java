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
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class BasemapsDialog extends DialogFragment {
	
	public BasemapsDialog(){
		// Empty constructor required for DialogFragment
	}
	
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		
	}
	
	public View onCreateView(LayoutInflater inflator, ViewGroup container, Bundle savedInstanceState){
		
		View view = inflator.inflate(R.layout.grid_layout, container);
		
		return view;
	}

}
