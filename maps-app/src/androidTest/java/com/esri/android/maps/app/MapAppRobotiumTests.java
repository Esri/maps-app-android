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

package com.esri.android.maps.app;
import android.Manifest.permission;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.test.InstrumentationRegistry;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityCompat.OnRequestPermissionsResultCallback;
import android.support.v4.content.ContextCompat;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;
import android.view.View;
import android.widget.*;
import com.esri.android.mapsapp.MapsAppActivity;
import com.esri.android.mapsapp.R;
import com.esri.android.mapsapp.R.id;
import com.esri.android.mapsapp.R.string;
import com.robotium.solo.Solo;
import com.robotium.solo.Solo.Config;
import com.robotium.solo.Solo.Config.ScreenshotFileType;
import junit.framework.Assert;

import java.io.File;

/**
 * Test performance may vary based on network latency.  Remember to add specific values to your app_settings.xml for
 * some of these tests and ensure your have internet access and location tracking on.  The first time these
 * tests are run you will be prompted by the app for WRITE_EXTERNAL permission.  Test screenshots are stored in the
 * Robotium folder of your device's SD card.
 *
 */
public class MapAppRobotiumTests extends ActivityInstrumentationTestCase2 implements OnRequestPermissionsResultCallback{

  private Solo solo;
  private static final int PERMISSION_WRITE_STORAGE = 4;
  private static final String BASEMAP_NAME = "Light Gray Canvas";
  private static boolean mPermissionsGranted = false;
  private static final String TAG = MapAppRobotiumTests.class.getSimpleName();

  public MapAppRobotiumTests(){
    super(MapsAppActivity.class);
  }


  @Override
  public void setUp() throws Exception {
    //setUp() is run before a test case is started.
    //This is where the solo object is created.

    Config config = new Config();
    config.screenshotFileType = ScreenshotFileType.JPEG;
    File sdcard = Environment.getExternalStorageDirectory();
    File data = new File(sdcard, "/Data");
    config.screenshotSavePath = data.getAbsolutePath() + "/Robotium/";
    Log.i(MapAppRobotiumTests.TAG, config.screenshotSavePath);
    config.shouldScroll = false;
    solo = new Solo(getInstrumentation(), config);
    if (!MapAppRobotiumTests.mPermissionsGranted){
      Log.i(MapAppRobotiumTests.TAG, "Seeking permissions");
      requestWritePermission();
    }


    getActivity();
  }

  @Override
  public void tearDown() throws Exception {
    //tearDown() is run after a test case has finished.
    //finishOpenedActivities() will finish all the activities that have been opened during the test execution.
    solo.finishOpenedActivities();
    super.tearDown();
  }

  /**
   * Long press on the view to test reverse geocoding
   */

  public void testReverseGeocode(){

    // Get the map view
    View mapView = solo.getView(id.map);
    Assert.assertTrue(solo.waitForView(mapView));

    // Trigger the reverse geocoding of screen location
    solo.clickLongOnView(mapView, 3000);
    solo.takeScreenshot("reverse_geocode");

    // Expect reverse geocoding to assign an
    // address to the text view
    TextView textView = (TextView) solo.getView(id.textView1);
    Assert.assertTrue(textView.getText().length()>0);
    solo.takeScreenshot("reverse_geocode_results");
  }

  /**
   * Sanity check presence of UI widgets that
   * should be present at startup
   */
  public void testWidgetsOnStartup(){
    // Map view
    View mapView = solo.getView(id.map);
    Assert.assertTrue(solo.waitForView(mapView));

    // FAB
    View fab = solo.getView(id.fab);
    Assert.assertTrue(fab.isClickable());

    // Drawer
    solo.clickOnImageButton(0);
    solo.takeScreenshot("drawer");
    Assert.assertTrue(solo.searchText(solo.getString(string.signin)));
    solo.searchText(solo.getString(string.switch_basemap));
    solo.setNavigationDrawer(Solo.CLOSED);

    // Dismiss the drawer
    solo.clickOnView(mapView,true);

    // Search view
    View searchView = solo.getView(id.searchView1);
    Assert.assertTrue(searchView.isFocusable());
  }

  /**
   * Test that publicly available basemaps
   * can be selected from drawer
   */
  public void testLoadPublicBasemap(){
    // Map view
    View mapView = solo.getView(id.map);
    Assert.assertTrue(solo.waitForView(mapView));
    // Open the drawer
    solo.clickOnImageButton(0);
    solo.waitForText("Sign In");
    // Click on "Switch Basemap"
    solo.clickOnText(solo.getString(string.switch_basemap));

    Assert.assertTrue(solo.waitForText("Select Base Map"));
    // Search for the item with "Ligth Gray Canvas"
    View basemapView = solo.getView(id.basemap_gridview);
    Assert.assertTrue(solo.waitForView(basemapView));
    Assert.assertTrue(solo.waitForDialogToOpen());
    Assert.assertTrue(solo.searchText(MapAppRobotiumTests.BASEMAP_NAME, true));

    // Take a snapshot
    solo.takeScreenshot("switch_public_basemap");

    // Click on the first basemap
    solo.clickOnImage(0);
    Assert.assertTrue(solo.waitForDialogToClose());


    solo.clickLongOnView(mapView, 3000);
    Assert.assertTrue(solo.waitForDialogToClose());
    solo.takeScreenshot("new_basemap");

  }

  /**
   * Test searching with suggestions
   */
  public void testSearchWithSuggestions(){
    // Get the map view
    View mapView = solo.getView(id.map);
    Assert.assertTrue(solo.waitForView(mapView));

    // Get the text field for inputting place names or addresses
    EditText editText = solo.getEditText(solo.getString(string.search),true);
    Assert.assertNotNull(editText);

    // Type in a few letters, assumes at
    // least one or more places nearby
    // using a value from your app settings.
    solo.typeText(editText,getActivity().getString(string.testPartialName));
    solo.sleep(4000);
    // Grab the list displaying the suggestions
    ListView listView = solo.getView(ListView.class,0);
    Assert.assertNotNull(listView);
    // Assert there are at least 1  or more suggestions
    Assert.assertTrue(listView.getAdapter().getCount()>0);
  }

  /**
   * Test searching by typing in address
   */
  public void testTypedAddress(){
    // Get the map view
    View mapView = solo.getView(id.map);
    Assert.assertTrue(solo.waitForView(mapView));

    // Get the text field for inputting place names or addresses
    final EditText searchText = solo.getEditText("Search",true);
    Assert.assertNotNull(searchText);

    // Use a local address near you...
    solo.typeText(searchText, solo.getString(string.localAddressNearYou));

    solo.sleep(4000);
    solo.pressSoftKeyboardSearchButton();
    // Wait for it...
    solo.sleep(9000);
    TextView result = (TextView) solo.getView(id.textView1);
    Assert.assertTrue(result.getText().length()> 0);
    solo.takeScreenshot("typed_address");
  }

  /**
   * Test routing request
   */
  public void testRouting() {
    // Get the map view
    View mapView = solo.getView(id.map);
    Assert.assertTrue(solo.waitForView(mapView));

    // Get the text field for inputting place names or addresses
    final EditText searchText = solo.getEditText(solo.getString(string.search), true);
    Assert.assertNotNull(searchText);

    // Use a local address near you...
    solo.typeText(searchText, solo.getString(string.localAddressNearYou));

    solo.sleep(3000);
    solo.pressSoftKeyboardSearchButton();
    Log.i(MapAppRobotiumTests.TAG, " ****ROUTE**** Clicked on submit search button");
    // Wait for it...
    solo.sleep(6000);

    // Press the route icon
    solo.clickOnImage(2);
    Log.i(MapAppRobotiumTests.TAG, " ****ROUTE**** Clicked on route button");

    // The progress dialog appears
    Assert.assertTrue(solo.waitForDialogToOpen());

    // The auth window is shown
    solo.sleep(3000);

    // Fill in auth
    EditText userName = solo.getEditText(solo.getString(string.usernameLabel));

    solo.typeText(userName, solo.getString(R.string.username));
    EditText password = solo.getEditText(solo.getString(string.passwordLabel));
    solo.typeText(password, solo.getString(R.string.password));

    // Hit the Log In button
    solo.clickOnButton(1);
    Log.i(MapAppRobotiumTests.TAG, " ****ROUTE**** Log In");

    solo.sleep(3000);
    // Wait for progress dialog dismissal
    Assert.assertTrue(solo.waitForDialogToClose());
    solo.takeScreenshot("routing");
  }

  private void requestWritePermission() {

    if (ContextCompat.checkSelfPermission(getActivity(),
        permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
      // Request the permission
      ActivityCompat.requestPermissions(getActivity(), new String[]{ permission.WRITE_EXTERNAL_STORAGE},
          MapAppRobotiumTests.PERMISSION_WRITE_STORAGE);
    }else{
      MapAppRobotiumTests.mPermissionsGranted = true;
    }
  }
  /**
   * Once the app has prompted for permission to write to external storage, the response
   * from the user is handled here.
   *
   * @param requestCode
   *            int: The request code passed into requestPermissions
   * @param permissions
   *            String: The requested permission(s).
   * @param grantResults
   *            int: The grant results for the permission(s). This will be
   *            either PERMISSION_GRANTED or PERMISSION_DENIED
   */
  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    if (requestCode == MapAppRobotiumTests.PERMISSION_WRITE_STORAGE) {
      // Request for write permission.
      if (grantResults.length != 1 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
        Toast.makeText(getActivity(),"Permission to write to external storage required for screenshots", Toast.LENGTH_LONG).show();
      }else{
        MapAppRobotiumTests.mPermissionsGranted = true;
      }
    }
  }
}
