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

import java.util.ArrayList;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.graphics.Color;
import android.view.ActionMode;
import android.view.ActionMode.Callback;
import android.view.ActionProvider;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.Spinner;
import android.widget.TextView;

import com.esri.android.map.CalloutPopupWindow;
import com.esri.android.map.GraphicsLayer;
import com.esri.android.map.MapView;
import com.esri.android.map.event.OnSingleTapListener;
import com.esri.core.geometry.AreaUnit;
import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.LinearUnit;
import com.esri.core.geometry.MultiPath;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.Polygon;
import com.esri.core.geometry.Polyline;
import com.esri.core.geometry.Unit;
import com.esri.core.map.Graphic;
import com.esri.core.symbol.FillSymbol;
import com.esri.core.symbol.LineSymbol;
import com.esri.core.symbol.MarkerSymbol;
import com.esri.core.symbol.SimpleFillSymbol;
import com.esri.core.symbol.SimpleLineSymbol;
import com.esri.core.symbol.SimpleMarkerSymbol;
import com.esri.core.symbol.SimpleMarkerSymbol.STYLE;

/**
 * The Measuring Tool follows Android's
 * <a href="http://developer.android.com/design/patterns/actionbar.html#contextual">
 * Contextual Action Bars</a> design pattern to provide basic measuring and
 * sketching capabilities in a self-contained tool. The tool can be readily plugged
 * into your existing app provided the following two requirements are met:
 * <ul><li>
 * An active {@link MapView} in your app and,
 * <li>
 * an <a href="http://developer.android.com/design/patterns/actionbar.html">Action Bar</a>.
 * </ul>
 *
 * To wire the measure tool to your Action Bar, first copy the Measuring tool java class
 * to your application project's src folder. Next, you need to create a new action button,
 * which can usually be achieved by adding a {@link MenuItem} to the menu xml that populates
 * the Action Bar.
 *
 * <pre><code>
 *     &lt;item
 *       android:id="@+id/action_measure"
 *       android:icon="@android:drawable/ic_menu_edit"
 *       android:orderInCategory="100"
 *       android:showAsAction="ifRoom"
 *       android:title="@string/measure"/&gt;
 * </code></pre>
 *
 * Then add the following code snippet to your {@link Activity} or {@link Fragment} that
 * contains the {@link MapView} to start the Measuring tool when the Action Button is clicked.
 *
 * <pre><code>
 *   @Override
 * public boolean onOptionsItemSelected(MenuItem item) {
 *  switch (item.getItemId()) {
 *    case R.id.action_measure:
 *      startActionMode(new MeasuringTool(mapView));
 *      break;
 *    default:
 *      break;
 *  }
 *  return super.onOptionsItemSelected(item);
 * }
 * </code></pre>
 *
 * The symbols used to draw lines and polygons can be customized by calling:
 * <ul>
 * <li>{@link #setMarkerSymbol(MarkerSymbol)}
 * <li>{@link #setLineSymbol(LineSymbol)}
 * <li>{@link #setFillSymbol(FillSymbol)}
 * </ul>
 *
 * The linear and area units in the drop down list can also be customized by calling:
 * <ul>
 * <li>{@link #setLinearUnits(Unit[])}
 * <li>{@link #setAreaUnits(Unit[])}
 */
public class MeasuringTool implements Callback, OnSingleTapListener {

  private static final long serialVersionUID = 1L;

  public enum MeasureType {
    LINEAR, AREA;

    static public MeasureType getType(int i) {
      switch (i) {
        case 0:
          return LINEAR;
        case 1:
          return AREA;
        default:
          return LINEAR;
      }
    }
  }

  private static final int MENU_DELETE = 0;
  private static final int MENU_PREF = 1;
  private static final int MENU_UNDO = 2;
  private MapView mMap;
  private GraphicsLayer mLayer;
  private OnSingleTapListener mOldOnSingleTapListener;
  private MarkerSymbol mMarkerSymbol;
  private LineSymbol mLineSymbol;
  private double mResult;
  private TextView mText;
  private MeasureType mMeasureMode = MeasureType.LINEAR;
  private int mCurrentLinearUnit;
  private Unit[] mLinearUnits;
  private Unit[] mDefaultLinearUnits;
  private int mCurrentAreaUnit;
  private Unit[] mAreaUnits;
  private Unit[] mDefaultAreaUnits;
  private Context mContext;
  private ArrayList<Point> mPoints;
  private FillSymbol mFillSymbol;
  private CalloutPopupWindow mCallout;
  private ActionMode mMode;
  private Polyline mLine;
  private Polygon mPolygon;

  public MeasuringTool(MapView map) {
    this.mMap = map;
    mContext = mMap.getContext();
    mMarkerSymbol = new SimpleMarkerSymbol(Color.RED, 10, STYLE.CIRCLE);
    mLineSymbol = new SimpleLineSymbol(Color.BLACK, 3);
    mDefaultLinearUnits = new Unit[] {
        Unit.create(LinearUnit.Code.METER),
        Unit.create(LinearUnit.Code.KILOMETER),
        Unit.create(LinearUnit.Code.FOOT),
        Unit.create(LinearUnit.Code.MILE_STATUTE) };
    mDefaultAreaUnits = new Unit[] {
        Unit.create(AreaUnit.Code.SQUARE_METER),
        Unit.create(AreaUnit.Code.SQUARE_KILOMETER),
        Unit.create(AreaUnit.Code.SQUARE_FOOT),
        Unit.create(AreaUnit.Code.SQUARE_MILE_STATUTE)};
    mFillSymbol = new SimpleFillSymbol(Color.argb(100, 225, 225, 0));
    mFillSymbol.setOutline(new SimpleLineSymbol(Color.TRANSPARENT, 0));
  }

  private void init() {
    mOldOnSingleTapListener = mMap.getOnSingleTapListener();
    mMap.setOnSingleTapListener(this);
    mLayer = new GraphicsLayer();
    mMap.addLayer(mLayer);
    mPoints = new ArrayList<Point>();
  }

  //Called when the user selects a contextual menu item
  @Override
  public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
    switch (item.getItemId()) {
      case MENU_DELETE:
        deleteAll();
        break;
      case MENU_UNDO:
        undo();
        break;
      default:
        break;
    }
    return false;
  }

  private void deleteAll() {
    mLayer.removeAll();
    mResult = 0;
    mPoints.clear();
    showResult();
    updateMenu();
  }

  //Called when the action mode is created; startActionMode() was called
  @Override
  public boolean onCreateActionMode(ActionMode mode, Menu menu) {
    mMode = mode;
    init();
    MenuItem item;
    item = menu.add(Menu.NONE, MENU_UNDO, 1, "undo");
    item.setIcon(android.R.drawable.ic_menu_revert);
    item.setVisible(false);

    item = menu.add(Menu.NONE, MENU_DELETE, 2, "clear");
    item.setIcon(android.R.drawable.ic_menu_delete);
    item.setVisible(false);

    item = menu.add(Menu.NONE, MENU_PREF, 3, "preferences");
    item.setIcon(android.R.drawable.ic_menu_manage);
    item.setActionProvider(new Preferences(mContext));
    return true;
  }

  //Called when the user exits the action mode
  @Override
  public void onDestroyActionMode(ActionMode mode) {
    hideCallout();
    mMap.removeLayer(mLayer);
    mLayer = null;
    mMap.setOnSingleTapListener(mOldOnSingleTapListener);
    mPoints = null;
  }

  private void hideCallout() {
    if (mCallout != null && mCallout.isShowing())
      mCallout.hide();
  }

  //Called each time the action mode is shown. Always called after onCreateActionMode, but
  // may be called multiple times if the mode is invalidated.
  @Override
  public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
    return false; // Return false if nothing is done
  }

  @Override
  public void onSingleTap(float x, float y) {
    addPoint(x, y);
  }

  private void addPoint(float x, float y) {
    Point point = mMap.toMapPoint(x, y);
    mPoints.add(point);
    clearAndDraw();
  }

  private void undo() {
    mPoints.remove(mPoints.size()-1);
    clearAndDraw();
  }

  private void clearAndDraw() {
    int[] oldGraphics = mLayer.getGraphicIDs();
    draw();
    mLayer.removeGraphics(oldGraphics);
    updateMenu();
  }

  private void draw() {
    if (mPoints.size() == 0) {
      return;
    }
    int index = 0;
    mResult = 0;
    mLine = new Polyline();
    mPolygon = new Polygon();
    for (Point point : mPoints) {
      mLayer.addGraphic(new Graphic(point, mMarkerSymbol, 100));
      if (index == 0) {
        mLine.startPath(point);
        if (mMeasureMode == MeasureType.AREA) {
          mPolygon.startPath(point);
        }
      } else {
        mLine.lineTo(point);
        if (mMeasureMode == MeasureType.AREA) {
          mPolygon.lineTo(point);
        }
      }
      mLayer.addGraphic(new Graphic(mLine, mLineSymbol));
      index++;
    }
    if (mMeasureMode == MeasureType.LINEAR) {
      mResult += GeometryEngine.geodesicLength(mLine, mMap.getSpatialReference(), (LinearUnit) getLinearUnit(mCurrentLinearUnit));
      Point screenPoint = mMap.toScreenPoint(mPoints.get(index-1));
      showResult((float) screenPoint.getX(), (float) screenPoint.getY());
    } else if (mMeasureMode == MeasureType.AREA) {
      mLine.lineTo(mPoints.get(0));
      mLayer.addGraphic(new Graphic(mLine, mLineSymbol));
      mPolygon.lineTo(mPoints.get(0));
      mLayer.addGraphic(new Graphic(mPolygon, mFillSymbol));
      mResult = GeometryEngine.geodesicArea(mPolygon, mMap.getSpatialReference(), (AreaUnit) getAreaUnit(mCurrentAreaUnit));
      Point labelPointForPolygon = GeometryEngine.getLabelPointForPolygon(mPolygon, mMap.getSpatialReference());
      Point screenPoint = mMap.toScreenPoint(labelPointForPolygon);
      showResult((float) screenPoint.getX(), (float) screenPoint.getY());
    }
  }

  private void updateMenu() {
    mMode.getMenu().findItem(MENU_DELETE).setVisible(mPoints.size() > 0);
    mMode.getMenu().findItem(MENU_UNDO).setVisible(mPoints.size() > 0);
  }

  private void showResult(float x, float y) {
    if (mResult > 0) {
      if (mCallout == null) {
        mText = new TextView(mContext);
        mCallout = new CalloutPopupWindow(mText);
      }
      mText.setText(getResultString());
      mCallout.showCallout(mMap, mMap.toMapPoint(x, y), 0, 0);
    } else if (mCallout != null && mCallout.isShowing()) {
      mCallout.hide();
    }
  }

  private void showResult() {
    if (mResult > 0) {
      mText.setText(getResultString());
      mCallout.showCallout(mMap);
    } else if (mCallout.isShowing()) {
      mCallout.hide();
    }
  }

  /**
   * Customize linear units
   *
   * @param linearUnits Array of Unit for measurement of dimensions
   */
  public void setLinearUnits(Unit[] linearUnits) {
    mLinearUnits = linearUnits;
  }

  Unit getLinearUnit(int position) {
    return mLinearUnits == null ? mDefaultLinearUnits[position] : mLinearUnits[position];
  }

  int getAreaUnitSize() {
    return mAreaUnits == null ? mDefaultAreaUnits.length : mAreaUnits.length;
  }

  /**
   * Customize the area units
   *
   * @param areaUnits Array of Unit for measurement of dimensions
   */
  public void setAreaUnits(Unit[] areaUnits) {
    mAreaUnits = areaUnits;
  }

  Unit getAreaUnit(int position) {
    return mAreaUnits == null ? mDefaultAreaUnits[position] : mAreaUnits[position];
  }

  int getLinearUnitSize() {
    return mLinearUnits == null ? mDefaultLinearUnits.length : mLinearUnits.length;
  }

  int getUnitSize() {
    return mMeasureMode == MeasureType.LINEAR ? getLinearUnitSize():getAreaUnitSize();
  }

  Unit getUnit(int position) {
    return mMeasureMode == MeasureType.LINEAR ? getLinearUnit(position):getAreaUnit(position);
  }

  Unit getCurrentUnit() {
    return getUnit(mMeasureMode == MeasureType.LINEAR ? mCurrentLinearUnit:mCurrentAreaUnit);
  }

  /**
   * Customize the line symbol
   *
   * @param symbol To draw lines on GraphicsLayer
   */
  public void setLineSymbol(LineSymbol symbol) {
    mLineSymbol = symbol;
  }

  /**
   * Customize the marker symbol
   *
   * @param symbol To draw marker symbol on GraphicsLayer
   */
  public void setMarkerSymbol(MarkerSymbol symbol) {
    mMarkerSymbol = symbol;
  }

  /**
   * Customize the fill symbol
   *
   * @param symbol To draw fill symbol on GraphicsLayer
   */
  public void setFillSymbol(FillSymbol symbol) {
    mFillSymbol = symbol;
  }

  MultiPath getGeometry() {
    return (mMeasureMode == MeasureType.LINEAR) ? mLine:mPolygon;
  }

  private String getResultString() {
    return mResult > 0 ? String.format("%.2f", mResult)+" "+(getCurrentUnit().getAbbreviation()):"";
  }

  class Preferences extends ActionProvider {

    private ImageView imageView;

    public Preferences(Context context) {
      super(context);
      imageView = new ImageView(mContext);
      imageView.setImageDrawable(context.getResources().getDrawable(android.R.drawable.ic_menu_manage));
    }

    @Override
    public View onCreateActionView() {

      Spinner spinner = new Spinner(mContext);
      spinner.setAdapter(new BaseAdapter() {

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
          return imageView;
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
          if (position == 0) {
            LinearLayout layout = new LinearLayout(mContext);
            layout.setOrientation(LinearLayout.VERTICAL);
            TextView text = new TextView(mContext);
            text.setText("Select Geometry Type");
            text.setTextColor(mContext.getResources().getColor(android.R.color.holo_blue_light));
            text.setTextSize(16);
            layout.addView(text);
            RadioButton r1 = new RadioButton(mContext);
            r1.setText("Distance");
            RadioButton r2 = new RadioButton(mContext);
            r2.setText("Area");
            RadioGroup group = new RadioGroup(mContext);

            group.addView(r1);
            group.addView(r2);
            group.check(mMeasureMode == MeasureType.LINEAR ? r1.getId():r2.getId());
            layout.addView(group);
            layout.setPadding(10, 10, 10, 10);
            group.setOnCheckedChangeListener(new OnCheckedChangeListener() {

              @Override
              public void onCheckedChanged(RadioGroup rGroup, int checkedId) {
                for (int i = 0; i < rGroup.getChildCount(); i++) {
                  if (rGroup.getChildAt(i).getId() == checkedId) {
                    mMeasureMode = MeasureType.getType(i);
                    notifyDataSetChanged();
                    clearAndDraw();
                  }
                }
              }
            });
            return layout;
          }
          LinearLayout layout = new LinearLayout(mContext);
          layout.setOrientation(LinearLayout.VERTICAL);
          TextView text = new TextView(mContext);
          text.setText("Select Unit");
          text.setTextColor(mContext.getResources().getColor(android.R.color.holo_blue_light));
          text.setTextSize(16);
          layout.addView(text);
          RadioGroup group = new RadioGroup(mContext);
          for (int i = 0; i < getUnitSize(); i++) {
            RadioButton r = new RadioButton(mContext);
            r.setText(getUnit(i).getDisplayName());
            group.addView(r);
            if (i == (mMeasureMode == MeasureType.LINEAR ? mCurrentLinearUnit:mCurrentAreaUnit)) {
              group.check(r.getId());
            }
          }
          group.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(RadioGroup rGroup, int checkedId) {
              for (int i = 0; i < rGroup.getChildCount(); i++) {
                if (rGroup.getChildAt(i).getId() == checkedId) {
                  if (mMeasureMode == MeasureType.LINEAR) {
                    if (mResult > 0) {
                      mResult = Unit.convertUnits(mResult, getLinearUnit(mCurrentLinearUnit), getLinearUnit(i));
                      mCurrentLinearUnit = i;
                      showResult();
                    } else {
                      mCurrentLinearUnit = i;
                    }
                  } else {
                    if (mResult > 0) {
                      mResult = Unit.convertUnits(mResult, getAreaUnit(mCurrentAreaUnit), getAreaUnit(i));
                      mCurrentAreaUnit = i;
                      showResult();
                    } else {
                      mCurrentAreaUnit = i;
                    }
                  }
                }
              }
            }
          });

          layout.addView(group);
          layout.setPadding(10, 10, 10, 10);
          return layout;
        }

        @Override
        public long getItemId(int position) {
          return position;
        }

        @Override
        public Object getItem(int position) {
          return null;
        }

        @Override
        public int getCount() {
          return 2;
        }
      });
      return spinner;
    }
  }
}
