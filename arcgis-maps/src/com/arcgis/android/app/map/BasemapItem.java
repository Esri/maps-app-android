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

import android.graphics.Bitmap;

import com.esri.core.portal.PortalItem;

public class BasemapItem {

	public PortalItem item;
	public Bitmap itemThumbnail;
	
	public BasemapItem(PortalItem item, Bitmap bt) {
		this.item = item;
		this.itemThumbnail = bt;
	}
}
