/*
 * COPYRIGHT 1995-2014 ESRI
 *
 * TRADE SECRETS: ESRI PROPRIETARY AND CONFIDENTIAL
 * Unpublished material - all rights reserved under the
 * Copyright Laws of the United States.
 *
 * For additional information, contact:
 * Environmental Systems Research Institute, Inc.
 * Attn: Contracts Dept
 * 380 New York Street
 * Redlands, California, USA 92373
 *
 * email: contracts@esri.com
 */

package com.esri.android.mapsapp;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

/**
 * Implements the view that shows a map.
 */
public class MapFragment extends Fragment {
  private static final String KEY_PORTAL_ITEM = "KEY_PORTAL_ITEM";

  private FrameLayout mMapContainer;

  private PortalItemParcelable mPortalItem;

  public static MapFragment newInstance(PortalItemParcelable portalItem) {
    MapFragment mapFragment = new MapFragment();

    Bundle args = new Bundle();
    args.putParcelable(KEY_PORTAL_ITEM, portalItem);

    mapFragment.setArguments(args);
    return mapFragment;
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    Bundle args = savedInstanceState;
    if (args == null) {
      args = getArguments();
    }

    if (args == null) {
      throw new IllegalStateException("arguments cannot be null");
    }

    mPortalItem = args.getParcelable(KEY_PORTAL_ITEM);
    if (mPortalItem == null) {
      throw new IllegalStateException("portal item cannot be null");
    }
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    mMapContainer = (FrameLayout) inflater.inflate(R.layout.map_fragment_layout, null);

    // TODO: fetch webmap etc..

    return mMapContainer;
  }
}
