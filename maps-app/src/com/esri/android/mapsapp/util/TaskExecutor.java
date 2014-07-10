/*
 * COPYRIGHT 1995-2014 ESRI
 *
 * TRADE SECRETS: ESRI PROPRIETARY AND CONFIDENTIAL
 * Unpublished material - all rights reserved under the
 * Copyright Laws of the United States.
 *
 * For additional information, contact:
 * Environmental Systems Research Institute, Inc.
 * Attn: Contracts Dept
 * 380 New York Street
 * Redlands, California, USA 92373
 *
 * email: contracts@esri.com
 */

package com.esri.android.mapsapp.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Singleton that implements a thread pool to execute tasks asynchronously.
 */
public class TaskExecutor {

  private static final int POOL_SIZE = 3;

  private static TaskExecutor sInstance;
  
  private ExecutorService mPool = Executors.newFixedThreadPool(POOL_SIZE);

  private TaskExecutor() {
  }

  public static TaskExecutor getInstance() {
    if (sInstance == null) {
      sInstance = new TaskExecutor();
    }
    return sInstance;
  }

  public ExecutorService getThreadPool() {
    return mPool;
  }
}
