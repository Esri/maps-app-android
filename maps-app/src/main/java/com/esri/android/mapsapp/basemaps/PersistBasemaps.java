package com.esri.android.mapsapp.basemaps;

/**
 * Created by sand8529 on 7/28/16.
 */

import java.util.ArrayList;
import java.util.HashMap;

/**
 * singleton to persist BasemapItem data
 */
public class PersistBasemaps {

  public static PersistBasemaps getInstance(){
    return ourInstance;
  }

  public final HashMap<String, ArrayList<BasemapItem>> storage = new HashMap<>();

  private PersistBasemaps(){}

  private static final PersistBasemaps ourInstance = new PersistBasemaps();

}