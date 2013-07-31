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

import java.util.ArrayList;

import android.os.AsyncTask;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;

import com.esri.android.map.GroupLayer;
import com.esri.android.map.Layer;
import com.esri.android.map.MapView;
import com.esri.android.map.ags.ArcGISDynamicMapServiceLayer;
import com.esri.android.map.ags.ArcGISFeatureLayer;
import com.esri.android.map.ags.ArcGISLayerInfo;
import com.esri.android.map.ags.ArcGISTiledMapServiceLayer;
import com.esri.android.map.event.OnSingleTapListener;
import com.esri.android.map.popup.Popup;
import com.esri.core.geometry.Envelope;
import com.esri.core.geometry.SpatialReference;
import com.esri.core.map.FeatureSet;
import com.esri.core.map.Graphic;
import com.esri.core.map.popup.PopupInfo;
import com.esri.core.tasks.ags.query.Query;
import com.esri.core.tasks.ags.query.QueryTask;

public class SingleTapListener implements OnSingleTapListener {

  private static final long serialVersionUID = 1L;

  private MapView mMapView;

  private PopupFragment mPopupFragment;

  private int tolerance = 40;

  public SingleTapListener(MapView map) {
    this.mMapView = map;
  }

  @Override
  public void onSingleTap(float x, float y) {

    if (mMapView.isLoaded()) {
      // Loop through each layer in the webmap
      mPopupFragment = new PopupFragment(mMapView);
      Envelope env = new Envelope(mMapView.toMapPoint(x, y), tolerance * mMapView.getResolution(), tolerance
          * mMapView.getResolution());
      Layer[] layers = mMapView.getLayers();
      for (Layer layer : layers) {
        // If the layer has not been initialized or is invisible, do nothing.
        if (!layer.isInitialized() || !layer.isVisible())
          continue;

        if (layer instanceof GroupLayer) {
          Layer[] sublayers = ((GroupLayer) layer).getLayers();
          if (sublayers != null) {
            for (Layer flayer : sublayers) {
              ArcGISFeatureLayer featureLayer = (ArcGISFeatureLayer) flayer;
              checkAndQueryFeatureLayer(x, y, featureLayer);
            }
          }
        } else if (layer instanceof ArcGISFeatureLayer) {
          // Query feature layer and display popups
          ArcGISFeatureLayer featureLayer = (ArcGISFeatureLayer) layer;
          checkAndQueryFeatureLayer(x, y, featureLayer);
        } else if (layer instanceof ArcGISDynamicMapServiceLayer || layer instanceof ArcGISTiledMapServiceLayer) {
          // Query dynamic map service layer and display popups.
          ArcGISLayerInfo[] layerinfos;
          // Retrieve layer info for each sub-layer of the dynamic map
          // service layer.
          if (layer instanceof ArcGISDynamicMapServiceLayer)
            layerinfos = ((ArcGISDynamicMapServiceLayer) layer).getAllLayers();
          else
            layerinfos = ((ArcGISTiledMapServiceLayer) layer).getAllLayers();

          if (layerinfos == null)
            continue;

          // Loop through each sub-layer
          for (ArcGISLayerInfo layerInfo : layerinfos) {
            // Obtain PopupInfo for sub-layer.
            // Has sublayer?
            ArcGISLayerInfo[] children = layerInfo.getLayers();
            if (children != null && children.length > 0)
              continue;

            checkAndQueryMapServiceSubLayer(x, y, env, layer, layerInfo);

          }
        }
      }
    }
  }

  private boolean checkScaleRange(double layerMaxScale, double layerMinScale, PopupInfo popupInfo) {
    // Check if the sub-layer is within the scale range

    boolean inScale = false;
    double maxScale = (layerMaxScale != 0) ? layerMaxScale : (popupInfo == null ? 0 : popupInfo.getMaxScale());
    double minScale = (layerMinScale != 0) ? layerMinScale : (popupInfo == null ? 0 : popupInfo.getMinScale());

    if ((maxScale == 0 || mMapView.getScale() > maxScale) && (minScale == 0 || mMapView.getScale() < minScale))
      inScale = true;

    return inScale;
  }

  private void checkAndQueryFeatureLayer(float x, float y, ArcGISFeatureLayer featureLayer) {
    if (featureLayer.getPopupInfo() != null
        && checkScaleRange(featureLayer.getMaxScale(), featureLayer.getMinScale(), featureLayer.getPopupInfo())) {
      // Query feature layer which is associated with a popup definition.
      new RunQueryFeatureLayerTask(x, y, tolerance).execute(featureLayer);
    }
  }

  private boolean checkIfLayerHasVisibleParents(ArcGISLayerInfo layerInfo) {
    // Check if a sub-layer is visible.
    boolean isVisible = true;
    ArcGISLayerInfo info = layerInfo;
    while (info != null && info.isVisible())
      info = info.getParentLayer();
    // Skip invisible sub-layer
    if (info != null && !info.isVisible())
      isVisible = false;

    return isVisible;

  }

  private void checkAndQueryMapServiceSubLayer(float x, float y, Envelope env, Layer layer, ArcGISLayerInfo layerInfo) {

    int subLayerId = layerInfo.getId();
    PopupInfo popupInfo = layer.getPopupInfo(subLayerId);

    if (checkIfLayerHasVisibleParents(layerInfo) && popupInfo != null
        && checkScaleRange(layerInfo.getMaxScale(), layerInfo.getMinScale(), popupInfo)) {
      // Query sub-layer which is associated with a popup definition and
      // is visible and in scale range.
      String url = layer.getQueryUrl(subLayerId);
      if (url == null || url.length() < 1)
        url = layer.getUrl() + "/" + subLayerId;
      new RunQueryDynamicLayerTask(env, layer, subLayerId, popupInfo, layer.getSpatialReference()).execute(url);
    }

  }

  // Display popup in a fragment
  private void showPopup(PopupFragment fragment) {
    if (fragment.isDisplayed())
      return;

    FragmentActivity activity = (FragmentActivity) mMapView.getContext();
    FragmentTransaction transaction = activity.getSupportFragmentManager().beginTransaction();
    transaction.setCustomAnimations(R.anim.popup_rotate_in, R.anim.popup_rotate_out);
    transaction.add(android.R.id.content, fragment, null);
    transaction.addToBackStack(null);
    transaction.commit();
    fragment.setDisplayed(true);
  }

  // Query dynamic map service layer by QueryTask
  private class RunQueryDynamicLayerTask extends AsyncTask<String, Void, FeatureSet> {

    private Envelope env;

    private SpatialReference sr;

    private Layer layer;

    private int subLayerId;

    private PopupInfo popupInfo;

    public RunQueryDynamicLayerTask(Envelope env, Layer layer, int subLayerId, PopupInfo popupInfo, SpatialReference sr) {
      super();
      this.env = env;
      this.sr = sr;
      this.layer = layer;
      this.subLayerId = subLayerId;
      this.popupInfo = popupInfo;
    }

    @Override
    protected FeatureSet doInBackground(String... urls) {

      for (String url : urls) {
        if (popupInfo == null)
          continue;

        // Retrieve graphics within the envelope by query.
        Query query = new Query();
        query.setInSpatialReference(sr);
        query.setOutSpatialReference(sr);
        query.setGeometry(env);
        query.setMaxFeatures(10);
        query.setOutFields(new String[] { "*" });
        query.setReturnGeometry(true);

        try {
          QueryTask queryTask;
          queryTask = new QueryTask(url);
          FeatureSet results = queryTask.execute(query);
          return results;
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
      return null;
    }

    @Override
    protected void onPostExecute(final FeatureSet result) {
      // Validate parameter.
      if (result == null) {
        return;
      }
      Graphic[] graphics = result.getGraphics();
      if (graphics == null || graphics.length == 0) {
        return;
      }

      for (Graphic gr : graphics) {
        // Create popup
        Popup popup = layer.createPopup(mMapView, subLayerId, gr);
        // Add popup to frament
        mPopupFragment.addPopup(popup);
        // Display popup
        showPopup(mPopupFragment);
      }
    }
  }

  // Query feature layer by hit test
  private class RunQueryFeatureLayerTask extends AsyncTask<ArcGISFeatureLayer, Void, Graphic[]> {

    private int tolerance;

    private float x;

    private float y;

    private ArcGISFeatureLayer featureLayer;

    private PopupInfo popupInfo;

    public RunQueryFeatureLayerTask(float x, float y, int tolerance) {
      super();
      this.x = x;
      this.y = y;
      this.tolerance = tolerance;
    }

    @Override
    protected Graphic[] doInBackground(ArcGISFeatureLayer... params) {
      for (ArcGISFeatureLayer featureLayer : params) {
        // Get popupinfo
        this.featureLayer = featureLayer;
        popupInfo = featureLayer.getPopupInfo();
        if (popupInfo == null)
          continue;

        // Retrieve graphic ids near the point.
        int[] ids = featureLayer.getGraphicIDs(x, y, tolerance);
        if (ids != null && ids.length > 0) {
          ArrayList<Graphic> graphics = new ArrayList<Graphic>();
          for (int id : ids) {
            // Obtain graphic based on the id.
            Graphic g = featureLayer.getGraphic(id);
            if (g == null)
              continue;
            graphics.add(g);
          }
          // Return an array of graphics near the point.
          return graphics.toArray(new Graphic[0]);
        }
      }
      return null;
    }

    @Override
    protected void onPostExecute(Graphic[] graphics) {
      // Validate parameter.
      if (graphics == null || graphics.length == 0) {
        return;
      }

      for (Graphic gr : graphics) {
        // Create popup
        Popup popup = featureLayer.createPopup(mMapView, 0, gr);
        // Add popup to fragment
        mPopupFragment.addPopup(popup);
        // Display popup
        showPopup(mPopupFragment);
      }
    }
  }

}
