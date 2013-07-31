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

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.esri.android.map.Layer;
import com.esri.android.map.MapView;
import com.esri.android.map.ags.ArcGISFeatureLayer;
import com.esri.android.map.ags.ArcGISPopupInfo;
import com.esri.android.map.popup.Popup;
import com.esri.android.map.popup.PopupContainer;
import com.esri.android.map.popup.PopupContainerView;
import com.esri.core.map.popup.PopupInfo;

public class PopupFragment extends Fragment {
	
	private PopupContainer mPopupContainer;
	private ArrayList<Popup> mPopups;
	private MapView mMapView;
	private boolean isInitialize, isDisplayed;
	private OnEditListener mEditListener;

	public PopupFragment() {
		mPopups = new ArrayList<Popup>();
		isInitialize = false;
		isDisplayed = false;
	}
	
	public PopupFragment(MapView mapView) {
		this.mMapView = mapView;
		mPopups = new ArrayList<Popup>();
		isInitialize = false;
		isDisplayed = false;
	}
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		
		// Set listener to handle editing events
		mEditListener = (OnEditListener) activity;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// Create popupcontainer and add popups to popupcontainer
		mPopupContainer = new PopupContainer(mMapView);
		if (mPopups != null && mPopups.size() > 0) {
			for (Popup popup : mPopups) {
				mPopupContainer.addPopup(popup);
			}
			isInitialize = true;
		}
		
		// Fragment wants to add menu to action bar
		setHasOptionsMenu(true);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		PopupContainerView view = null;
		
		if (mPopupContainer != null) {
			view = mPopupContainer.getPopupContainerView();
			view.setOnPageChangelistener(new OnPageChangeListener() {
				
				@Override
				public void onPageSelected(int arg0) {
					
				}
				
				@Override
				public void onPageScrolled(int arg0, float arg1, int arg2) {
					// Refresh menu item while swipping popups
					Activity activity = (Activity)mMapView.getContext();
					activity.invalidateOptionsMenu();
				}
				
				@Override
				public void onPageScrollStateChanged(int arg0) {
					
				}
			});
		}
		return view;
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		// Inflate the menu; this adds items to the action bar if it is present.
		inflater.inflate(R.menu.popup_activity, menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (mPopupContainer == null || mPopupContainer.getPopupCount() <= 0)
			return true;
		
		Popup popup = mPopupContainer.getCurrentPopup();
	    switch(item.getItemId()){
		    case R.id.menu_camera:
	      	startActivityForResult(new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI), 1);
	       break;
		    case R.id.menu_delete:
	      	deleteFeature(popup);
	       break;
	      case R.id.menu_edit:
	      	editFeature(popup);
	        break;
	      case R.id.menu_save:
	      	saveFeature(popup);
	       break;
	    }
	    
		return true;
	}
	
	@Override
	public void onPrepareOptionsMenu(Menu menu) {
	
		// Turn on/off menu items based on popup's edit capabilities
		for (int i = 0; i < menu.size(); i++) {
			MenuItem item = menu.getItem(i);
			if (mPopupContainer != null) {
				Popup popup = mPopupContainer.getCurrentPopup();
				if (popup != null) {
					if (popup.isEditMode() ) {
						if (item.getItemId() == R.id.menu_save || item.getItemId() == R.id.menu_camera) {
							item.setVisible(true);
							item.setEnabled(true);
						}
						else {
							item.setVisible(false);
							item.setEnabled(false);
						}
					}
					else {
						if (((item.getItemId() == R.id.menu_edit) && (popup.isEditable()))
								|| ((item.getItemId() == R.id.menu_delete) && (popup.isDeletable()))) {
							item.setVisible(true);
							item.setEnabled(true);
						} else {
							item.setVisible(false);
							item.setEnabled(false);
						}
					}
				} else {
					item.setVisible(false);
					item.setEnabled(false);
				}
			} else {
				item.setVisible(false);
				item.setEnabled(false);
			}
		}
	}
	
	public void addPopup(Popup popup) {
		// Add popup to the list
		if (mPopups == null) 
			mPopups = new ArrayList<Popup>();
		mPopups.add(popup);
		// Add popup to popupcontainer if it has been created
		if (mPopupContainer != null) {
			mPopupContainer.addPopup(popup);
		}
	}

	// Indicate if popupcontainer has been created
	public boolean isInitialize() {
		return isInitialize;
	}

	public void setInitialize(boolean isInitialize) {
		this.isInitialize = isInitialize;
	}

	// Indicate if fragment is displayed
	public boolean isDisplayed() {
		return isDisplayed;
	}

	public void setDisplayed(boolean isDisplayed) {
		this.isDisplayed = isDisplayed;
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		 if (resultCode == Activity.RESULT_OK && data != null && mPopupContainer != null) {
	    	// Add the selected media as attachment.
	      Uri selectedImage = data.getData();
	      mPopupContainer.getCurrentPopup().addAttachment(selectedImage);
	    }
	}
	
	// When "delete" menu item is clicked
	private void deleteFeature(Popup popup) {
		ArcGISFeatureLayer fl = getFeatureLayer(popup);
		mEditListener.onDelete(fl, popup);
	}
	
	// When "edit" menu item is clicked
	private void editFeature(Popup popup) {
		ArcGISFeatureLayer fl = getFeatureLayer(popup);
		mEditListener.onEdit(fl, popup);
	}
	
	// When "save" menu item is clicked
	private void saveFeature(Popup popup) {
		ArcGISFeatureLayer fl = getFeatureLayer(popup);
		mEditListener.onSave(fl, popup);
	}
	
	// Get the feature layer which is associated with the current popup
	private ArcGISFeatureLayer getFeatureLayer(Popup popup) {
		ArcGISFeatureLayer fl = null;
		
		if (mMapView == null || popup == null)
			return null;
		PopupInfo popupInfo = popup.getPopupInfo();
		if (popupInfo instanceof ArcGISPopupInfo) {
			ArcGISPopupInfo agsPopupInfo = (ArcGISPopupInfo) popupInfo;
			Layer[] layers = mMapView.getLayers();
			for (Layer layer : layers) {
				if ((layer instanceof ArcGISFeatureLayer) 
						&& (layer.getUrl().compareToIgnoreCase(agsPopupInfo.getLayerUrl()) == 0)) {
					fl = (ArcGISFeatureLayer) layer;
					return fl;
				}
			}
		}
		
		return fl;
	}
	
	// Listener to handle editing events
	public interface OnEditListener {
		public void onDelete(ArcGISFeatureLayer fl, Popup popup);
		public void onEdit(ArcGISFeatureLayer fl, Popup popup);
		public void onSave(ArcGISFeatureLayer fl, Popup popup);
	}
}
