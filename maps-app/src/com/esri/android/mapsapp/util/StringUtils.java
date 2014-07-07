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

/**
 * Helper class for string operations.
 */
public class StringUtils {

  /**
   * Returns whether or not the string is not empty.
   * 
   * @param str The String to check for emptiness
   * @return whether or not the string is not empty
   */
  public static boolean isNotEmpty(String str) {
    return !isEmpty(str);
  }

  /**
   * Returns whether or not the string is empty. Note: returns true if the string has a value of "null".
   * 
   * @param str the String to check for emptiness
   * @return whether or not the string is empty
   */
  public static boolean isEmpty(String str) {
    return str == null || str.isEmpty();
  }
}
