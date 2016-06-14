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

package com.esri.android.mapsapp.tools;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.view.View;
import com.esri.android.mapsapp.R;

/**
 * The implementation of compass. There are two modes of operation for rotating
 * the compass, one is using device motion sensors and latter using the pinch
 * listeners.
 * 
 */
public class Compass extends View implements SensorEventListener {

	// Handles the sensors
	public final SensorManager sensorManager;
	private final Paint mPaint;
	private final Bitmap mBitmap;
	private final Matrix mMatrix;
	// Sensors for accelerometer and magnetometer
	private final Sensor gSensor;
	private final Sensor mSensor;
	// Used for orientation of the compass
	private final float[] mGravity = new float[3];
	private final float[] mGeomagnetic = new float[3];
	// To send and receive notification from the sensors.
	public SensorEventListener sensorEventListener;
	private float mAngle = 0;

	public Compass(Context context) {
		super(context);

		mPaint = new Paint();
		mMatrix = new Matrix();

		mBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.eaf_compass);

		sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
		gSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		mSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
	}

	public void start() {
		// A copy of instance which is used to restart the sensors
		sensorEventListener = this;

		// Enable the sensors
		sensorManager.registerListener(this, gSensor, SensorManager.SENSOR_DELAY_GAME);
		sensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_GAME);
	}

	public void stop() {
		// Disable the sensors
		sensorManager.unregisterListener(this);
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

	@Override
	public void onSensorChanged(SensorEvent event) {
		final float alpha = 0.97f;

		synchronized (this) {
			if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {

				mGravity[0] = alpha * mGravity[0] + (1 - alpha) * event.values[0];
				mGravity[1] = alpha * mGravity[1] + (1 - alpha) * event.values[1];
				mGravity[2] = alpha * mGravity[2] + (1 - alpha) * event.values[2];
			}

			if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {

				mGeomagnetic[0] = alpha * mGeomagnetic[0] + (1 - alpha) * event.values[0];
				mGeomagnetic[1] = alpha * mGeomagnetic[1] + (1 - alpha) * event.values[1];
				mGeomagnetic[2] = alpha * mGeomagnetic[2] + (1 - alpha) * event.values[2];

			}

			float R[] = new float[9];
			float I[] = new float[9];
			boolean success = SensorManager.getRotationMatrix(R, I, mGravity, mGeomagnetic);

			if (success) {
				float orientation[] = new float[3];
				SensorManager.getOrientation(R, orientation);
				float azimuth = (float) Math.toDegrees(orientation[0]);
				azimuth = (azimuth + 360) % 360;

				setRotationAngle(-azimuth);
			}
		}
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
	}
}
