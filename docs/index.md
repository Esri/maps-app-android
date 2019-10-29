## Description

Get your organization's authoritative map data into the hands of your workers with this ArcGIS Runtime Android app. The application you build can include a custom web map from your [ArcGIS Online organization](https://doc.arcgis.com/en/arcgis-online/reference/what-is-agol.htm). For example, a web map from the [Living Atlas](https://livingatlas.arcgis.com/en/browse/?#d=1&type=maps) can be used as a starting place for your app. The Maps App also includes examples of place search and routing capabilities using either ArcGIS Online's powerful services or your own services. It also leverages your organizations configured basemaps to allow users to switch between the basemap that make sense for them.

This example application is open source so grab the code at [GitHub](https://github.com/Esri/maps-app-android) and either configure the app for your organization, or just learn how to integrate similar capabilities into your own app!

## Using web maps

You can author your own web maps from ArcGIS Online or ArcGIS Pro and share them in your app via your ArcGIS Online organization, this is the central power of the Web GIS model built into ArcGIS. Building an app which uses a web map allows the cartography and map configuration to be completed in ArcGIS Online rather than in code. This then allows the map to change over time, without any code changes or app updates. Learn more about the benefits of developing with web maps [here](https://developers.arcgis.com/web-map-specification/). Also, learn about authoring web maps in [ArcGIS Online](http://doc.arcgis.com/en/arcgis-online/create-maps/make-your-first-map.htm) and [ArcGIS Pro](http://pro.arcgis.com/en/pro-app/help/mapping/map-authoring/author-a-basemap.htm).

Loading web maps in code is really easy. The Maps App loads a web map from a portal (which may require the user to sign in, see the identity section below) with the following code:

```java
Portal portal = new Portal("http://<your portal url>");
Map map = new Map(portal, "<your map id>");
mapView.setMap(map);
```

## Accessing your organization's basemaps

As an administrator of an ArcGIS Online organization or Portal you can configure the basemaps that your users can switch between via a [group](http://doc.arcgis.com/en/arcgis-online/share-maps/share-items.htm). Applications can leverage this configuration using the [Portal API](https://developers.arcgis.com/android/latest/guide/access-the-arcgis-platform.htm#ESRI_SECTION2_B8EDBBD3D4F1499C80AF43CFA73B8292). The Maps App does this by an async call to find the group containing web maps in the basemap gallery. With the returned group id, the collection of basemaps is retrieved from the portal.

```java
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

<img src="/docs/images/identity.png" width="600"  />

1. A request is made to a secured resource.
2. The portal responds with an unauthorized access error.
3. A challenge handler associated with the identity manager is asked to provide a credential for the portal.
4. A UI displays and the user is prompted to enter a user name and password.
5. If the user is successfully authenticated, a credential (token) is included in requests to the secured service.
6. The identity manager stores the credential for this portal and all requests for secured content includes the token in the request.

The `DefaultAuthenticationChallengeHandler` class takes care of steps 1-6 in the diagram above. For an application to use this pattern, follow these [guides](https://developers.arcgis.com/authentication/signing-in-arcgis-online-users/) to register your app.

```java
// Add these four lines to your Android fragment or activity
OAuthConfiguration oAuthConfiguration = new OAuthConfiguration("https://www.arcgis.com",clientId, redirectUri);
DefaultAuthenticationChallengeHandler authenticationChallengeHandler = new DefaultAuthenticationChallengeHandler(this);
AuthenticationManager.setAuthenticationChallengeHandler(authenticationChallengeHandler);
AuthenticationManager.addOAuthConfiguration(oAuthConfiguration);
```

Any time a secured service issues an authentication challenge, the `DefaultAuthenticationChallengeHandler` and the corresponding `DefaultOAuthIntentReceiver` work together to broker the authentication transaction. In addition to the two lines above, the Android manifest.xml file must define a `DefaultOAuthIntentReceiver` that receives intents once a user has entered their credentials.

```xml
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

Note the value for android:scheme in the XML. This is [redirect URI](https://developers.arcgis.com/authentication/browser-based-user-logins/#configuring-a-redirect-uri) that you configured when you registered your app [here](https://developers.arcgis.com/dashboard/). For more details on the user authorization flow, see the [Authorize REST API](https://developers.arcgis.com/rest/users-groups-and-items/authorize.htm).

## Place search

[Geocoding](https://developers.arcgis.com/android/latest/guide/search-for-places-geocoding-.htm#ESRI_SECTION1_406F4F35F62C465ABC52F3FF04BB6B04) lets you transform an address or a place name to a specific geographic location. The reverse lets you use a geographic location to find a description of the location, like a postal address or place name. In the Maps App, we use a [LocatorTask](https://developers.arcgis.com/android/latest/guide/search-for-places-geocoding-.htm#ESRI_SECTION1_62AE6A47EB4B403ABBC72337A1255F8A) to perform geocoding and reverse geocoding functions provided by [Esri's World Geocoding Service](https://developers.arcgis.com/features/geocoding/). The `LocatorTask` has various asynchronous methods that we use to provide address suggestions when searching for places or geocoding locations.

In the Maps App, `LocatorTask`s are initialized using an online locator provided by an ArcGIS service.

```java
// The Locator Task class variable is initialized using the ArcGIS World Geocoding Server.
mLocator = new LocatorTask(getString("http://geocode.arcgis.com/arcgis/rest/services/World/GeocodeServer");
```

You can also provision your own [custom geocode service](https://doc.arcgis.com/en/arcgis-online/administer/configure-services.htm#ESRI_SECTION1_0A9A071A7AB748028C8213D1D863FA18) to support your organization. Before using the `LocatorTask` to geocode or search for places, the `LocatorTask` must be `LOADED`. The loadable pattern is described [here](https://developers.arcgis.com/android/latest/guide/loadable-pattern.htm). `LocatorTask` operations are performed asynchronously using `ListenableFutures`, an implementation of Java’s Future interface. `ListenableFutures` add the ability to attach a listener that runs upon completion of the task. One of the first user interactions the Maps App supports is suggesting places near the device location.

## Place suggestions

Typing the first few letters of a place into the Map App search box (e.g. “Voodoo Doughnut”) shows a number of suggestions near the device’s location

<img src="/docs/images/suggest.png" width="300"  />

```java
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

Once a suggestion in the list has been selected by the user, the suggested address is geocoded using the `geocodeAsync` method of the `LocatorTask`. Along with the address, specific [geocoding parameters](https://developers.arcgis.com/android/latest/guide/search-for-places-geocoding-.htm#ESRI_SECTION2_48C5C281B21B4BF1BBBDBCEA71F105B9) can be set to tune the results. For example, in the Maps App, we set the preferred location and refine that further by setting a boundary of the area to search for matching addresses.

```java
mGeocodeParams = new GeocodeParameters();
// Set max results and spatial reference
mGeocodeParams.setMaxResults(2);
mGeocodeParams.setOutputSpatialReference(mMapView.getSpatialReference());
// Use the centre of the current map extent as the location
mGeocodeParams.setSearchArea(calculateSearchArea());
mGeocodeParams.setPreferredSearchLocation(mLocation);
```

```java
<ListenableFuture> locFuture = mLocator.geocodeAsync(matchedAddress, mGeocodeParams);
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

## Reverse geocoding

The Map App uses the built-in map magnifier to help users fine tune a location on the map for reverse geocoding. The magnifier appears after a long-press on the map view. Once the long-press is released, the map point is reverse geocoded.

On long press                    | Reverse geocode result
:-------------------------------:|:-------------------------------------:
<img src="/docs/images/reverse_geocode.png" width="300"  />|<img src="/doc/images/reverse_geocode2.png" width="300"  />

We’ve extended the `DefaultMapViewOnTouchListener` and implemented logic for `onUp` motion event.

```java
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

```java
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

Getting navigation directions in the Maps App is just as easy in the [Runtime SDK](https://developers.arcgis.com/features/directions/) as it is on [ArcGIS Online](http://doc.arcgis.com/en/arcgis-online/use-maps/get-directions.htm). You can [customize](http://doc.arcgis.com/en/arcgis-online/administer/configure-services.htm#ESRI_SECTION1_567C344D5DEE444988CA2FE5193F3CAD) your navigation services for your organization, add new travel modes that better reflect your organization’s workflows, or remove travel modes that are not suitable for your organization’s workflows.

Navigating from point to point in the Map App is enabled in two ways. In either scenario, the origin and destination must be geocoded before routing can be attempted. In the Maps App, routing requires you to provide credentials to your Portal or ArcGIS Online organization. As mentioned earlier in the Identity section above, we use the `DefaultAuthenticationChallengeHandler` to manage the authentication process.

```java
// As soon as the route task is instantiated, an authentication challenge is issued.
mRouteTask = new RouteTask("http://geocode.arcgis.com/arcgis/rest/services/World/GeocodeServer");
```

You can instantiate a new `RouteParameters` object by using the `generateDefaultParametersAsync()` method on your RouteTask object. Using this method will set the appropriate default settings for routing, add the stops and request route directions, and allow the units of measure for the directions to be specified.

```java
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

The resulting route is shown:

<img src="/docs/images/route.png" width="300"  />
