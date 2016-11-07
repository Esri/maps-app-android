Maps App
=======================
## Description
Get your organization's authoritative map data into the hands of your workers with this ArcGIS Runtime Android app. The application you build can include a custom web map from your [ArcGIS Online organization](https://doc.arcgis.com/en/arcgis-online/reference/what-is-agol.htm). For example, a [web map](http://doc.arcgis.com/en/living-atlas/item/?itemId=26888b0c21a44eb1ba2f26d1eb7981fe) from the Living Atlas can be used as a starting place for your app. The maps-app also includes examples of place search and routing capabilities using either ArcGIS Online's powerful services or your own services. It also leverages your organizations configured basemaps to allow users to switch between the basemap that make sense for them.

This example application is open source so grab the code at [GitHub](https://github.com/Esri/maps-app-android) and either configure the app for your organization, or just learn how to integrate similar capabilities into your own app!

## Using Web Maps
You can author your own web maps from ArcGIS Online or ArcGIS Pro and share them in your app via your ArcGIS Online organization, this is the central power of the Web GIS model built into ArcGIS. Building an app which uses a web map allows the cartography and map configuration to be completed in ArcGIS Online rather than in code. This then allows the map to change over time, without any code changes or app updates. Learn more about the benefits of developing with web maps [here](https://developers.arcgis.com/web-map-specification/). Also, learn about authoring web maps in [ArcGIS Online](http://doc.arcgis.com/en/arcgis-online/create-maps/make-your-first-map.htm) and [ArcGIS Pro](http://pro.arcgis.com/en/pro-app/help/mapping/map-authoring/author-a-basemap.htm).

Loading web maps in code is really easy, the maps app loads a web map from a portal (which may require the user to login, see the identity section below) with the following code:

```
Portal portal = new Portal("http://<your portal url>");
Map map = new Map(portal, "<your map id>");
mapView.setMap(map);
```
## Accessing Your Organization's Basemaps
As an administrator of an ArcGIS Online organization or Portal you can configure the basemaps that your users can switch between via a [group](http://doc.arcgis.com/en/arcgis-online/share-maps/share-items.htm). Applications can leverage this configuration using the [Portal API](https://developers.arcgis.com/android/beta/guide/access-the-arcgis-platform.htm#ESRI_SECTION2_B8EDBBD3D4F1499C80AF43CFA73B8292). The Maps App does this by an async call to find the group containing web maps in the basemap gallery. With the returned group id, the collection of basemaps is retrieved from the portal.

```
PortalQueryParams queryParams = new PortalQueryParams();

// get the query string to fetch the portal group that defines the portal's basemaps
queryParams.setQuery(portalInfo.getBasemapGalleryGroupQuery()); 

// Use a listenable future for retrieving search results from portal
ListenableFuture> groupFuture = portal.findGroupsAsync(queryParams);
groupFuture.addDoneListener(new Runnable() {
    @Override
    public void run() {
        try {
          PortalQueryResultSet basemapGroupResult = groupFuture.get();
          PortalGroup group = basemapGroupResult.getResults().get(0);
          // Build out a query to find items by group id
          PortalQueryParams basemapQueryParams = new PortalQueryParams();
          basemapQueryParams.setQueryForItemsInGroup(group.getId());
          // Execute the search
          final ListenableFuture> contentFuture = portal.findItemsAsync(basemapQueryParams);
          contentFuture.addDoneListener(new Runnable() {
              @Override
              public void run() {
                try {
                        PortalQueryResultSet items = contentFuture.get();
                        // Process results and display      
                } catch (Exception e) {
                    dealWithException(e);
                }
              }
          });         
      } catch (Exception ie) {
          dealWithException(ie);
      }
    }
});
```
## Identity
The Maps App leverages the ArcGIS [identity](https://developers.arcgis.com/authentication/) model to provide access to resources via the the [named user](https://developers.arcgis.com/authentication/#named-user-login) login pattern. During the routing workflow, the app prompts you for your organization’s ArcGIS Online credentials used to obtain a token later consumed by the Portal and routing service. The ArcGIS Runtime SDKs provide a simple to use API for dealing with ArcGIS logins.

The process of accessing token secured services with a challenge handler is illustrated in the following diagram.

1. A request is made to a secured resource.
2. The portal responds with an unauthorized access error.
3. A challenge handler associated with the identity manager is asked to provide a credential for the portal.
4. A UI displays and the user is prompted to enter a user name and password.
5. If the user is successfully authenticated, a credential (token) is incuded in requests to the secured service.
6. The identity manager stores the credential for this portal and all requests for secured content includes the token in the request.

The DefaultAuthenticationChallengeHandler class takes care of steps 1-6 in the diagram above. For an application to use this pattern, follow these [guides](https://developers.arcgis.com/authentication/signing-in-arcgis-online-users/) to register your app.
```
// Add these two lines to your Android fragment or activity
DefaultAuthenticationChallengeHandler authenticationChallengeHandler = new DefaultAuthenticationChallengeHandler(getActivity());
AuthenticationManager.setAuthenticationChallengeHandler(authenticationChallengeHandler);
```

Any time a secured service issues an authentication challenge, the DefaultAuthenticationChallengeHandler and the corresponding DefaultOAuthIntentReceiver work together to broker the authentication transaction. In addition to the two lines above, the Android manifest.xml file must define a DefaultOAuthIntentReceiver that receives intents once a user has entered their credentials.

```
<activity>
  android:name="com.esri.arcgisruntime.security.DefaultOAuthIntentReceiver"
  android:label="OAuthIntentReceiver"
  android:launchMode="singleTask">
  <intent-filter>
    <action android:name="android.intent.action.VIEW"/>
    <category android:name="android.intent.category.DEFAULT"/>
    <category android:name="android.intent.category.BROWSABLE"/>

    <data android:scheme="my-maps-app"/>
  </intent-filter>
</activity>
```
       
Note the value for android:scheme in the XML. This is [redirect URI](https://developers.arcgis.com/authentication/browser-based-user-logins/#configuring-a-redirect-uri) that you configured when you registered your app [here](https://developers.arcgis.com/). For more details on the user authorization flow, see the [Authorize REST API](http://resources.arcgis.com/en/help/arcgis-rest-api/#/Authorize/02r300000214000000/).

## Place Search
[Geocoding](https://developers.arcgis.com/android/beta/guide/search-for-places-geocoding-.htm#ESRI_SECTION1_406F4F35F62C465ABC52F3FF04BB6B04) lets you transform an address or a place name to a specific geographic location. The reverse lets you use a geographic location to find a description of the location, like a postal address or place name. In the Maps App, we use a [LocatorTask](http://developers.arcgis.com/android/beta/guide/search-for-places-geocoding-.htm#ESRI_SECTION1_62AE6A47EB4B403ABBC72337A1255F8A) to perform geocoding and reverse geocoding functions provided by [Esri's World Geocoding Service](https://developers.arcgis.com/features/geocoding/). The LocatorTask has various asynchronous methods that we use to provide address suggestions when searching for places or geocoding locations.

In the Maps App, LocatorTasks are initialized using an online locator provided by an ArcGIS service.

```
// The Locator Task class variable is initialized using the ArcGIS World Geocoding Server.
mLocator = new LocatorTask(getString("http://geocode.arcgis.com/arcgis/rest/services/World/GeocodeServer");
```

You can also provision your own [custom geocode service](https://doc.arcgis.com/en/arcgis-online/administer/configure-services.htm#ESRI_SECTION1_0A9A071A7AB748028C8213D1D863FA18) to support your organization. Before using the LocatorTask for geocode or searching for places, the LocatorTask must be LOADED. The loadable pattern is described [here](https://developers.arcgis.com/android/beta/guide/loadable-pattern.htm). LocatorTask operations are performed asynchronously using ListenableFutures, an implementation of Java’s Future interface. ListenableFutures add the ability to attach a listener that runs upon completion of the task. One of the first user interactions the Maps App supports is suggesting places near the device location.

## Place Suggestions

Typing the first few letters of a place into the Map App search box (e.g. “Voodoo Doughnut”) shows a number of suggestions near the device’s location

```
// Attach a listener to the locator task since the LocatorTask may or may not be loaded the
// the very first time a user types text into the search box.  If the Locator is already loaded,
// the following listener is invoked immediately.
mLocator.addDoneLoadingListener(new Runnable() {
    @Override 
    public void run() {
        final ListenableFuture> suggestionsFuture = mLocator.suggestAsync(query, suggestParams);
        // Attach a done listener that executes upon completion of the async call
        suggestionsFuture.addDoneListener(new Runnable() {
            @Override
            public void run() {
                try {
                    // Get the suggestions returned from the locator task.
                    // Process and display

                } catch (Exception e) {
                   dealWithException(e);
            }
          }
        });
    }
});
// Initiate the asynchronous call
mLocator.loadAsync();
```

## Geocoding
Once a suggestion in the list has been selected by the user, the suggested address is geocoded using the geocodeAsync method of the LocatorTask. Along with the address, specific [geocoding parameters](https://developers.arcgis.com/android/beta/guide/search-for-places-geocoding-.htm#ESRI_SECTION2_48C5C281B21B4BF1BBBDBCEA71F105B9) can be set to tune the results. For example, in the maps app, we set the preferred location and refine that further by setting a boundary of the area to search for matching addresses.
```
mGeocodeParams = new GeocodeParameters();
// Set max results and spatial reference
mGeocodeParams.setMaxResults(2);
mGeocodeParams.setOutputSpatialReference(mMapView.getSpatialReference());
// Use the centre of the current map extent as the location
mGeocodeParams.setSearchArea(calculateSearchArea());
mGeocodeParams.setPreferredSearchLocation(mLocation);
```
```
ListenableFuture> locFuture = mLocator.geocodeAsync(matchedAddress, mGeocodeParams);
// Attach a done listener that executes upon completion of the async call
locFuture.addDoneListener(new Runnable() {
    @Override
        public void run() {
          try {
            List locationResults = locFuture.get();
          showSuggestedPlace(locationResults, address);
          } catch (Exception e) {
            dealWithException(e);
       }
    }
});
```

## Reverse Geocoding
The Map App uses the built-in map magnifier to help users fine tune a location on the map for reverse geocoding. The magnifier appears after a long-press on the map view. Once the long-press is released, the map point is reverse geocoded.

We’ve extended the DefaultMapViewOnTouchListener and implemented logic for onUp motion event.

```
@Override
public boolean onUp(MotionEvent motionEvent) {
    if (mLongPressEvent != null) {
        // This is the end of a long-press that will have displayed the magnifier.  Get the graphic
        // coordinates for the motion event.
        android.graphics.Point mapPoint = new android.graphics.Point((int) motionEvent.getX(),
            (int) motionEvent.getY());
        Point point = mMapView.screenToLocation(mapPoint);
        // Reverse geocode the point
       reverseGeocode(point);

       // Remove any previous graphics
       resetGraphicsLayers();
    }
    return true;
}
```
The corresponding reverse geocode method:

```
private void reverseGeocode(Point point) {
    // Pass in the point and geocode parameters to the reverse geocode method
    final ListenableFuture> reverseFuture = mLocator.reverseGeocodeAsync(point,
        mReverseGeocodeParams);
    // Attach a done listener that shows the results upon completion of the async call
    reverseFuture.addDoneListener(new Runnable() {
      @Override
      public void run() {
        try {
          List results = reverseFuture.get();
          // Process and display results
          showReverseGeocodeResult(results);
        } catch (Exception e) {
          dealWithException(e);
        }
      }
    });
    return true;
}
```

## Route
Getting navigation directions in the maps-app is just as easy in the [Runtime SDK](https://developers.arcgis.com/features/directions/) as it is on [ArcGIS Online](http://doc.arcgis.com/en/arcgis-online/use-maps/get-directions.htm). You can [customize](http://doc.arcgis.com/en/arcgis-online/administer/configure-services.htm#ESRI_SECTION1_567C344D5DEE444988CA2FE5193F3CAD) your navigation services for your organization, add new travel modes that better reflect your organization’s workflows, or remove travel modes that are not suitable for your organization’s workflows.

Navigating from point to point in the Map App is enabled in two ways. In either scenario, the origin and destination must be geocoded before routing can be attempted. In the maps-app, routing requires you to provide credentials to your Portal or ArcGIS Online organization. As mentioned earlier in the Identity section above, we use the DefaultAuthenticationChallengeHandler to manage the authentication process.

```
// As soon as the route task is instantiated, an authentication challenge is issued.
mRouteTask = new RouteTask("http://geocode.arcgis.com/arcgis/rest/services/World/GeocodeServer");
```
You can instantiate a new RouteParameters object by using the generateDefaultParametersAsync() method on your RouteTask object. Using this method will set the appropriate default settings for routing. Add the stops and request route directions, specifying the units of measure for the directions.

```
/**
   * A helper class for solving routes
   */
  private class RouteSolver implements Runnable{
    private final Stop origin;

    private final Stop destination;

    public RouteSolver(Point start, Point end){
      origin = new Stop(start);
      destination = new Stop(end);
    }
    @Override
    public void run (){
      LoadStatus status = mRouteTask.getLoadStatus();
      // Has the route task loaded successfully?
      if (status == LoadStatus.FAILED_TO_LOAD) {    
        // We may want to try reloading it  --> mRouteTask.retryLoadAsync();
      } else {
        final ListenableFuture routeTaskFuture = mRouteTask
            .generateDefaultParametersAsync();
        // Add a done listener that uses the returned route parameters
        // to build up a specific request for the route we need
        routeTaskFuture.addDoneListener(new Runnable() {

          @Override
          public void run() {
            try {
              RouteParameters routeParameters = routeTaskFuture.get();
              // Add a stop for origin and destination
              routeParameters.getStops().add(origin);
              routeParameters.getStops().add(destination);
              // We want the task to return driving directions and routes
              routeParameters.setReturnDirections(true);
              routeParameters.setDirectionsDistanceTextUnits(
                  DirectionDistanceTextUnits.IMPERIAL);
              routeParameters.setOutputSpatialReference(MapFragment.mMapView.getSpatialReference());

              final ListenableFuture routeResFuture = mRouteTask
                  .solveAsync(routeParameters);
              routeResFuture.addDoneListener(new Runnable() {
                @Override
                public void run() {
                  try {
                    RouteResult routeResult = routeResFuture.get();
                    // Show route results
                    showRoute(routeResult, origin.getGeometry(), destination.getGeometry());

                  } catch (Exception e) {
                    dealWithException(e);
                  }
                }
              });
            } catch (Exception e) {
              dealWithException(e);
            }
          }
        });
      }
    }
  }
  ```


## Features
* Dynamically switch basemaps
* Place search
* Routing
* Geocode addresses
* Reverse geocode
* Sign in to ArcGIS account

## Development Instructions
This Maps App repo is an Android Studio Project and App Module that can be directly cloned and imported into Android Studio.

### Fork the repo
**Fork** the [Maps App Android](https://github.com/Esri/maps-app-android/fork) repo

### Clone the repo
Once you have forked the repo, you can make a clone

#### Command line Git
1. [Clone the Maps App repo](https://help.github.com/articles/fork-a-repo#step-2-clone-your-fork)
2. ```cd``` into the ```maps-app-android``` folder
3. Make your changes and create a [pull request](https://help.github.com/articles/creating-a-pull-request)

### Configuring a Remote for a Fork
If you make changes in the fork and would like to [sync](https://help.github.com/articles/syncing-a-fork/) those changes with the upstream repository, you must first [configure the remote](https://help.github.com/articles/configuring-a-remote-for-a-fork/). This will be required when you have created local branches and would like to make a [pull request](https://help.github.com/articles/creating-a-pull-request) to your upstream branch.

1. In the Terminal (for Mac users) or command prompt (fow Windows and Linus users) type ```git remote -v``` to list the current configured remote repo for your fork.
2. ```git remote add upstream https://github.com/Esri/maps-app-android.git``` to specify new remote upstream repository that will be synced with the fork. You can type ```git remote -v``` to verify the new upstream.

If there are changes made in the Original repository, you can sync the fork to keep it updated with upstream repository.

1. In the terminal, change the current working directory to your local project
2. Type ```git fetch upstream``` to fetch the commits from the upstream repository
3. ```git checkout master``` to checkout your fork's local master branch.
4. ```git merge upstream/master``` to sync your local `master' branch with `upstream/master`. **Note**: Your local changes will be retained and your fork's master branch will be in sync with the upstream repository.


## Requirements
* [JDK 6 or higher](http://www.oracle.com/technetwork/java/javase/downloads/index.html)
* [Android Studio](http://developer.android.com/sdk/index.html)

## Resources
* [ArcGIS Runtime SDK for Android Developers Site](https://developers.arcgis.com/android/)
* [ArcGIS Mobile Blog](http://blogs.esri.com/esri/arcgis/category/mobile/)
* [ArcGIS Developer Blog](http://blogs.esri.com/esri/arcgis/category/developer/)
* [Google+](https://plus.google.com/+esri/posts)
* [twitter@ArcGISRuntime](https://twitter.com/ArcGISRuntime)
* [twitter@esri](http://twitter.com/esri)

## Issues
Find a bug or want to request a new feature enhancement?  Let us know by submitting an issue.

## Contributing
Anyone and everyone is welcome to [contribute](https://github.com/Esri/maps-app-android/blob/master/CONTRIBUTING.md). We do accept pull requests.

1. Get involved
2. Report issues
3. Contribute code
4. Improve documentation

## Licensing
Copyright 2016 Esri

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.

A copy of the license is available in the repository's [license.txt](https://github.com/Esri/maps-app-android/blob/master/license.txt) file.

For information about licensing your deployed app, see [License your app](https://developers.arcgis.com/android/guide/license-your-app.htm).

[](Esri Tags: ArcGIS Android Mobile)
[](Esri Language: Java)​
