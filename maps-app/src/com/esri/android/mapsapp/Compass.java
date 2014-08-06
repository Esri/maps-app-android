package com.esri.android.mapsapp;

import com.esri.android.map.MapView;
import com.esri.android.map.event.OnPinchListener;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class Compass extends View {

	float mAngle = 0;

	Paint mPaint;

	Bitmap mBitmap;

	Matrix mMatrix;

	MapView mMapView;

	public Compass(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub

		mPaint = new Paint();
		mMatrix = new Matrix();
		mBitmap = BitmapFactory.decodeResource(getResources(),
				R.drawable.cn2);
		
	}

	/**
	 * Overloaded constructor that takes a MapView, from which the compass
	 * rotation angle will be set.
	 */
	public Compass(Context context, AttributeSet attrs, MapView mapView) {
		this(context, attrs);

		// Save reference to the MapView passed in to this compass.
		mMapView = mapView;
		if (mMapView != null) {

			// Set an OnPinchListener on the map to listen for the pinch gesture
			// which may change the map rotation.
			mMapView.setOnPinchListener(new OnPinchListener() {

				private static final long serialVersionUID = 1L;

				public void prePointersUp(float arg0, float arg1, float arg2,
						float arg3, double arg4) {
				}

				public void prePointersMove(float arg0, float arg1, float arg2,
						float arg3, double arg4) {
				}

				public void prePointersDown(float arg0, float arg1, float arg2,
						float arg3, double arg4) {
				}

				public void postPointersUp(float arg0, float arg1, float arg2,
						float arg3, double arg4) {
				}

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

}
