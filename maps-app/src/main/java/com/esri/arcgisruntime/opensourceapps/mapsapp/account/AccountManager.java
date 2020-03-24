/* Copyright 1995-2014 Esri
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

package com.esri.arcgisruntime.opensourceapps.mapsapp.account;

import com.esri.arcgisruntime.portal.Portal;
import com.esri.arcgisruntime.portal.PortalInfo;
import com.esri.arcgisruntime.portal.PortalUser;

/**
 * Singleton used to provide access to, and helper
 * methods around, a Portal object.
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
	 * Gets the Portal instance the app is currently signed into. Returns null
	 * if the user hasn't signed in to a portal.
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
			mPortalUser = mPortal != null ? mPortal.getUser() : null;
			mPortalInfo = mPortal != null ? mPortal.getPortalInfo() : null;
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
			mAGOLPortal = new Portal(AGOL_PORTAL_URL);
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
