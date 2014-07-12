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

package com.esri.android.mapsapp.account;

import com.esri.core.portal.Portal;
import com.esri.core.portal.PortalInfo;
import com.esri.core.portal.PortalUser;


/**
 * Singleton used to provide access to, and helper methods around, a com.esri.core.portal.Portal object.
 */
public class AccountManager {

  private static final String AGOL_PORTAL_URL = "http://www.arcgis.com";

  private static AccountManager sAccountManager;

  private Portal mPortal;

  private Portal mAGOLPortal;

  private PortalUser mPortalUser;

  private PortalInfo mPortalInfo;

  private AccountManager() {
  }

  public static AccountManager getInstance() {
    if (sAccountManager == null) {
      sAccountManager = new AccountManager();
    }
    return sAccountManager;
  }

  /**
   * Gets the Portal instance the app is currently signed into. Returns null if the user hasn't signed in to a portal.
   */
  public Portal getPortal() {
    return mPortal;
  }

  /**
   * Sets the portal the app is currently signed in to.
   */
  public void setPortal(Portal portal) {
    mPortalUser = null;
    mPortalInfo = null;
    
    mPortal = portal;
    
    try {
      mPortalUser = mPortal != null ? mPortal.fetchUser() : null;
      mPortalInfo = mPortal != null ? mPortal.fetchPortalInfo() : null;
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Gets the ArcGIS Online portal instance (http://www.arcgis.com).
   * 
   * @return the ArcGIS Online portal instance
   */
  public Portal getAGOLPortal() {
    if (mAGOLPortal == null) {
      mAGOLPortal = new Portal(AGOL_PORTAL_URL, null);
    }
    return mAGOLPortal;
  }

  public boolean isSignedIn() {
    return mPortal != null;
  }

  public PortalUser getPortalUser() {
    return mPortalUser;
  }

  public PortalInfo getPortalInfo() {
    return mPortalInfo;
  }
}
