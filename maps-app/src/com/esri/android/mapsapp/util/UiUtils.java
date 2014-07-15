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

package com.esri.android.mapsapp.util;

import android.content.res.Resources;

public class UiUtils {
  /**
   * Converts dp units to pixel units. Code taken from <a
   * href="http://developer.android.com/guide/practices/screens_support.html#dips-pels">
   * http://developer.android.com/guide/practices/screens_support.html#dips-pels</a>
   * 
   * @param dips The number in dp (dip) that you wish to convert to px (pixels)
   * @return The number of pixels that corresponds to the number in dips on this device
   */
  public static int dipsToPixels(int dips) {
    if (dips == 0) {
      return 0;
    }

    final float scale = Resources.getSystem().getDisplayMetrics().density;
    return (int) (dips * scale + 0.5f);
  }

}
