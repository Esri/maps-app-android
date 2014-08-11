package com.esri.android.mapsapp;

import com.esri.android.map.MapView;
import com.esri.android.map.event.OnPinchListener;

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
import android.util.AttributeSet;
import android.view.View;

public class Compass extends View implements SensorEventListener {

	float mAngle = 0;
	Paint mPaint;
	Bitmap mBitmap;
	Matrix mMatrix;
	MapView mMapView;

	public SensorManager sensorManager;
	public Sensor gsensor;
	public Sensor msensor;
	private float[] mGravity = new float[3];
	private float[] mGeomagnetic = new float[3];
	private float azimuth = 0f;
	public SensorEventListener sel;

	public Compass(Context context, AttributeSet attrs) {
		super(context, attrs);

		mPaint = new Paint();
		mMatrix = new Matrix();
		mBitmap = BitmapFactory.decodeResource(getResources(),
				R.drawable.ic_compass);
		sensorManager = (SensorManager) context
				.getSystemService(Context.SENSOR_SERVICE);
		gsensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		msensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
	}

	/**
	 * Overloaded constructor that takes a MapView, from which the compass
	 * rotation angle will be set.
	 */
	public Compass(Context context, AttributeSet attrs, MapView mapView) {
		this(context, attrs);

		mMapView = mapView;
		if (mMapView != null) {
			mMapView.setOnPinchListener(new OnPinchListener() {

				private static final long serialVersionUID = 1L;

				@Override
				public void prePointersUp(float arg0, float arg1, float arg2,
						float arg3, double arg4) {
				}

				@Override
				public void prePointersMove(float arg0, float arg1, float arg2,
						float arg3, double arg4) {
				}

				@Override
				public void prePointersDown(float arg0, float arg1, float arg2,
						float arg3, double arg4) {
				}

				@Override
				public void postPointersUp(float arg0, float arg1, float arg2,
						float arg3, double arg4) {
				}

				@Override
				public void postPointersMove(float arg0, float arg1,
						float arg2, float arg3, double arg4) {
					// Update the compass angle from the map rotation angle (the
					// arguments passed in to the method are not
					// relevant in this case).
					setRotationAngle(mMapView.getRotationAngle());
				}

				@Override
				public void postPointersDown(float arg0, float arg1,
						float arg2, float arg3, double arg4) {
				}
			});
		}
	}

	public void start() {
		sel = this;
		sensorManager.registerListener(this, gsensor,
				SensorManager.SENSOR_DELAY_NORMAL);
		sensorManager.registerListener(this, msensor,
				SensorManager.SENSOR_DELAY_NORMAL);
	}

	public void stop() {
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

	/** Draws the compass image at the current angle of rotation on the canvas. */
	@Override
	protected void onDraw(Canvas canvas) {

		// Reset the matrix to default values.
		mMatrix.reset();

		// Pass the current rotation angle to the matrix. The center of rotation
		// is set to be the center of the bitmap.
		mMatrix.postRotate(-this.mAngle, mBitmap.getHeight() / 2,
				mBitmap.getWidth() / 2);

		// Use the matrix to draw the bitmap image of the compass.
		canvas.drawBitmap(mBitmap, mMatrix, mPaint);

		super.onDraw(canvas);

	}

	public void onSensorChanged(SensorEvent event) {
		final float alpha = 0.97f;

		synchronized (this) {
			if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {

				mGravity[0] = alpha * mGravity[0] + (1 - alpha)
						* event.values[0];
				mGravity[1] = alpha * mGravity[1] + (1 - alpha)
						* event.values[1];
				mGravity[2] = alpha * mGravity[2] + (1 - alpha)
						* event.values[2];
			}

			if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {

				mGeomagnetic[0] = alpha * mGeomagnetic[0] + (1 - alpha)
						* event.values[0];
				mGeomagnetic[1] = alpha * mGeomagnetic[1] + (1 - alpha)
						* event.values[1];
				mGeomagnetic[2] = alpha * mGeomagnetic[2] + (1 - alpha)
						* event.values[2];

			}

			float R[] = new float[9];
			float I[] = new float[9];
			boolean success = SensorManager.getRotationMatrix(R, I, mGravity,
					mGeomagnetic);
			if (success) {
				float orientation[] = new float[3];
				SensorManager.getOrientation(R, orientation);
				azimuth = (float) Math.toDegrees(orientation[0]); // orientation
				azimuth = (azimuth + 360) % 360;

				setRotationAngle(-azimuth);
			}
		}
	}

	public void onAccuracyChanged(Sensor sensor, int accuracy) {
	}
}
