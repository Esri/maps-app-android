package com.esri.android.mapsapp.tools;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Color;
import android.view.ActionMode;
import android.view.ActionMode.Callback;
import android.view.ActionProvider;
import android.view.Gravity;
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

import com.esri.android.map.GraphicsLayer;
import com.esri.android.map.MapView;
import com.esri.android.map.event.OnSingleTapListener;
import com.esri.core.geometry.AreaUnit;
import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.LinearUnit;
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

public class MeasuringTool implements Callback, OnSingleTapListener {

  // default serial
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
  private static final int MENU_RESULT = 2;
  private MapView mMap;
  private GraphicsLayer mLayer;
  private OnSingleTapListener mOldListener;
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
  private ResultView mResultView;
  private Context mContext;
  private ArrayList<Point> mPoints;
  private FillSymbol mFillSymbol;
  private int mPolygonID;
  private int mLineID;

  public MeasuringTool(MapView map) {
    this.mMap = map;
    mContext = mMap.getContext();
    mOldListener = mMap.getOnSingleTapListener();
    mMap.setOnSingleTapListener(this);
    mLayer = new GraphicsLayer();
    mMap.addLayer(mLayer);
    mMarkerSymbol = new SimpleMarkerSymbol(Color.RED, 10, STYLE.CIRCLE);
    mLineSymbol = new SimpleLineSymbol(Color.BLACK, 3);
    mText = new TextView(mContext);
    mText.setBackgroundColor(Color.TRANSPARENT);
    mMap.addView(mText);
    mDefaultLinearUnits = new Unit[] { Unit.create(LinearUnit.Code.METER), Unit.create(LinearUnit.Code.KILOMETER),
        Unit.create(LinearUnit.Code.FOOT), Unit.create(LinearUnit.Code.MILE_STATUTE) };
    mDefaultAreaUnits = new Unit[] { Unit.create(AreaUnit.Code.SQUARE_METER),
        Unit.create(AreaUnit.Code.SQUARE_KILOMETER), Unit.create(AreaUnit.Code.SQUARE_FOOT),
        Unit.create(AreaUnit.Code.SQUARE_MILE_STATUTE) };
    mPoints = new ArrayList<Point>();
    mFillSymbol = new SimpleFillSymbol(Color.argb(100, 225, 225, 0));
    mFillSymbol.setOutline(new SimpleLineSymbol(Color.TRANSPARENT, 0));
  }

  @Override
  public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
    switch (item.getItemId()) {
    case MENU_DELETE:
      mLayer.removeAll();
      mResult = 0;
      mPoints.clear();
      mResultView.update();
      break;

    default:
      break;
    }
    return false;
  }

  @Override
  public boolean onCreateActionMode(ActionMode mode, Menu menu) {
    MenuItem item = menu.add(Menu.NONE, MENU_RESULT, 0, "results");
    mResultView = new ResultView(mContext);
    item.setActionProvider(mResultView);
    item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
    item = menu.add(Menu.NONE, MENU_DELETE, 1, "clear");
    item.setIcon(android.R.drawable.ic_menu_delete);
    item = menu.add(Menu.NONE, MENU_PREF, 2, "preferences");
    item.setIcon(android.R.drawable.ic_menu_manage);
    item.setActionProvider(new Preferences(mContext));
    return true;
  }

  @Override
  public void onDestroyActionMode(ActionMode mode) {
    mMap.removeLayer(mLayer);
    mMap.setOnSingleTapListener(mOldListener);
  }

  @Override
  public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
    return false;
  }

  @Override
  public void onSingleTap(float x, float y) {
    Point point = mMap.toMapPoint(x, y);
    mLayer.addGraphic(new Graphic(point, mMarkerSymbol, 100));
    mPoints.add(point);
    if (mPoints.size() < 2)
      return;
    Polyline pline = new Polyline();
    pline.startPath(mPoints.get(mPoints.size() - 2));
    pline.lineTo(mPoints.get(mPoints.size() - 1));
    mLayer.addGraphic(new Graphic(pline, mLineSymbol));
    if (mMeasureMode == MeasureType.LINEAR) {
      mResult += GeometryEngine.geodesicLength(pline, mMap.getSpatialReference(),
          (LinearUnit) getLinearUnit(mCurrentLinearUnit));
      mResultView.update();
    } else if (mMeasureMode == MeasureType.AREA) {
      mLayer.removeGraphic(mLineID);
      mLayer.removeGraphic(mPolygonID);
      Polygon polygon = new Polygon();
      polygon.startPath(mPoints.get(0));
      for (int i = 1; i < mPoints.size(); i++) {
        polygon.lineTo(mPoints.get(i));
      }
      Polyline line = new Polyline();
      line.startPath(mPoints.get(mPoints.size() - 1));
      line.lineTo(mPoints.get(0));
      mLineID = mLayer.addGraphic(new Graphic(line, mLineSymbol));
      mPolygonID = mLayer.addGraphic(new Graphic(polygon, mFillSymbol));
      mResult = GeometryEngine.geodesicArea(polygon, mMap.getSpatialReference(),
          (AreaUnit) getAreaUnit(mCurrentAreaUnit));
      mResultView.update();
    }
  }

  public void setLinearUnits(Unit[] linearUnits) {
    mLinearUnits = linearUnits;
  }

  Unit getLinearUnit(int position) {
    return mLinearUnits == null ? mDefaultLinearUnits[position] : mLinearUnits[position];
  }

  int getAreaUnitSize() {
    return mAreaUnits == null ? mDefaultAreaUnits.length : mAreaUnits.length;
  }

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
    return mMeasureMode == MeasureType.LINEAR ? getLinearUnitSize() : getAreaUnitSize();
  }

  Unit getUnit(int position) {
    return mMeasureMode == MeasureType.LINEAR ? getLinearUnit(position) : getAreaUnit(position);
  }

  public void setLineSymbol(LineSymbol symbol) {
    mLineSymbol = symbol;
  }

  public void setMarkerSymbol(MarkerSymbol symbol) {
    mMarkerSymbol = symbol;
  }

  public void setFillSymbol(FillSymbol symbol) {
    mFillSymbol = symbol;
  }

  class ResultView extends ActionProvider {

    private TextView text;

    public ResultView(Context context) {
      super(context);
    }

    @Override
    public View onCreateActionView() {
      text = new TextView(mContext);
      text.setTextColor(Color.WHITE);
      text.setGravity(Gravity.CENTER);
      return text;
    }

    public void update() {
      if (mResult > 0) {
        text.setText(String.format("%.2f", mResult)
            + " "
            + (mMeasureMode == MeasureType.LINEAR ? getLinearUnit(mCurrentLinearUnit).getAbbreviation() : getAreaUnit(
                mCurrentAreaUnit).getAbbreviation()));
      } else {
        text.setText("");
      }
    }

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
            group.check(mMeasureMode == MeasureType.LINEAR ? r1.getId() : r2.getId());
            layout.addView(group);
            layout.setPadding(10, 10, 10, 10);
            group.setOnCheckedChangeListener(new OnCheckedChangeListener() {

              @Override
              public void onCheckedChanged(RadioGroup group, int checkedId) {
                for (int i = 0; i < group.getChildCount(); i++) {
                  if (group.getChildAt(i).getId() == checkedId) {
                    mMeasureMode = MeasureType.getType(i);
                    notifyDataSetChanged();
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
            if (i == (mMeasureMode == MeasureType.LINEAR ? mCurrentLinearUnit : mCurrentAreaUnit)) {
              group.check(r.getId());
            }
          }
          group.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
              for (int i = 0; i < group.getChildCount(); i++) {
                if (group.getChildAt(i).getId() == checkedId) {
                  if (mMeasureMode == MeasureType.LINEAR) {
                    if (mResult > 0) {
                      mResult = LinearUnit.convertUnits(mResult, getLinearUnit(mCurrentLinearUnit), getLinearUnit(i));
                      mCurrentLinearUnit = i;
                      mResultView.update();
                    } else {
                      mCurrentLinearUnit = i;
                    }
                  } else {
                    if (mResult > 0) {
                      mResult = AreaUnit.convertUnits(mResult, getAreaUnit(mCurrentAreaUnit), getAreaUnit(i));
                      mCurrentAreaUnit = i;
                      mResultView.update();
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
