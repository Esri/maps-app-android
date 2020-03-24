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

package com.esri.arcgisruntime.opensourceapps.mapsapp.tools;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.view.View;
import com.esri.arcgisruntime.opensourceapps.mapsapp.R;

/**
 * The implementation of a compass showing the relative rotation of the map.
 */
public class Compass extends View {

	private final Paint mPaint;
	private final Bitmap mBitmap;
	private final Matrix mMatrix;
	// Used for orientation of the compass
	private float mAngle = 0;

	public Compass(Context context) {
		super(context);

		mPaint = new Paint();
		mMatrix = new Matrix();

		mBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.eaf_compass);
	}

	/**
	 * Updates the angle, in degrees, at which the compass is draw within this
	 * view.
	 */
	public void setRotationAngle(double angle) {
		// Save the new rotation angle.
		mAngle = (float) angle;
		// Force the compass to re-paint itself.
		postInvalidate();
	}

	/**
	 * Draws the compass image at the current angle of rotation on the canvas.
	 */
	@Override
	protected void onDraw(Canvas canvas) {
		// Reset the matrix to default values.
		mMatrix.reset();

		// Pass the current rotation angle to the matrix. The center of rotation
		// is set to be the center of the bitmap.
		mMatrix.postRotate(-mAngle, mBitmap.getHeight() / 2, mBitmap.getWidth() / 2);

		// Use the matrix to draw the bitmap image of the compass.
		canvas.drawBitmap(mBitmap, mMatrix, mPaint);

		super.onDraw(canvas);
	}
}
