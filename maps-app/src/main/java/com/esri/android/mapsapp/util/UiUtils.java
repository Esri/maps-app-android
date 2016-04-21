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

package com.esri.android.mapsapp.util;

import android.content.res.Resources;

public class UiUtils {
	/**
	 * Converts dp units to pixel units. Code taken from <a href=
	 * "http://developer.android.com/guide/practices/screens_support.html#dips-pels">
	 * http://developer.android.com/guide/practices/screens_support.html#dips-
	 * pels</a>
	 * 
	 * @param dips
	 *            The number in dp (dip) that you wish to convert to px (pixels)
	 * @return The number of pixels that corresponds to the number in dips on
	 *         this device
	 */
	public static int dipsToPixels(int dips) {
		if (dips == 0) {
			return 0;
		}

		final float scale = Resources.getSystem().getDisplayMetrics().density;
		return (int) (dips * scale + 0.5f);
	}

}
