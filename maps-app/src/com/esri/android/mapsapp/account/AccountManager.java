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


/**
 * Singleton used to provide access to, and helper methods around, a com.esri.core.portal.Portal object.
 */
public class AccountManager {

  private static AccountManager sAccountManager;

  private Portal mPortal;

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
    mPortal = portal;
  }

  public boolean isSignedIn() {
    return mPortal != null;
  }
}
