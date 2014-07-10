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

import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;

import com.esri.core.portal.PortalItem;

/**
 * Implements a PortalItem wrapper that supports parcelling and thumbnail caching.
 */
public class PortalItemParcelable implements Parcelable {

  private final PortalItem mPortalItem;

  private Bitmap mThumbnail;

  public PortalItemParcelable(PortalItem portalItem) {

    if (portalItem == null) {
      throw new IllegalStateException("portalItem cannot be null");
    }
    mPortalItem = portalItem;
  }

  public PortalItem getPortalItem() {
    return mPortalItem;
  }

  public Bitmap getThumbnail() {
    return mThumbnail;
  }

  public void setThumbnail(Bitmap thumbnail) {
    mThumbnail = thumbnail;
  }

  @Override
  public int describeContents() {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    // TODO Auto-generated method stub

  }
}
