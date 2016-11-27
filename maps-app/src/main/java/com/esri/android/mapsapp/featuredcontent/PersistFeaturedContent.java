package com.esri.android.mapsapp.featuredcontent;
/* Copyright 2016 Esri
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

import com.esri.android.mapsapp.basemaps.MapsAppItem;

import java.util.ArrayList;
import java.util.HashMap;

public class PersistFeaturedContent {
  public static PersistFeaturedContent getInstance(){ return instance; }

  public final HashMap<String, ArrayList<MapsAppItem>> storage = new HashMap<>();

  private PersistFeaturedContent(){}

  private static final PersistFeaturedContent instance = new PersistFeaturedContent();
}
