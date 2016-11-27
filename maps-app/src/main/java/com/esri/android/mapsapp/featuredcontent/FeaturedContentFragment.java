package com.esri.android.mapsapp.featuredcontent;

import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;
import com.esri.android.mapsapp.R;
import com.esri.android.mapsapp.account.AccountManager;
import com.esri.android.mapsapp.basemaps.MapItemAdapter;
import com.esri.android.mapsapp.basemaps.MapsAppItem;
import com.esri.android.mapsapp.basemaps.PersistBasemaps;
import com.esri.android.mapsapp.dialogs.ProgressDialogFragment;
import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.portal.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

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

public class FeaturedContentFragment extends DialogFragment implements MapItemAdapter.MapItemClickListener,
    DialogInterface.OnCancelListener{
  private static final String TAG = "FeaturedContentFragment";

  private static final String TAG_FEATURED_CONTENT_SEARCH_PROGRESS_DIALOG = "TAG_FEATURED_CONTENT_SEARCH_PROGRESS_DIALOG";
  private FeaturedContentListener mFeaturedContentListener;
  private ProgressDialogFragment mProgressDialog;
  private static final int REQUEST_CODE_PROGRESS_DIALOG = 1;
  private ArrayList<MapsAppItem> mFeaturedContentList;
  private MapItemAdapter mFeaturedContentAdapter;
  private final List<PortalItem> mPortalItems = new ArrayList<>();
  private static final String PUBLIC_FEATURED_CONTENT = "public featured content";
  private static final String PRIVATE_FEATURED_CONTENT = "private featured content";

  // Empty constructor for recreating fragment
  // after it's destroyed
  public FeaturedContentFragment(){}

  /**
   * Listens for selection of new featured content
   * @param listener
   */
  public void setFeaturedContentListener(FeaturedContentListener listener){
    mFeaturedContentListener = listener;
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setStyle(android.app.DialogFragment.STYLE_NORMAL, R.style.CustomDialog);
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    getDialog().setTitle("Select A Map");

    // Inflate feature content grid and data to go with it
    GridView view = (GridView) inflater.inflate(R.layout.featuredcontent_grid, container,false);

    // Set up adapter
    mFeaturedContentList = new ArrayList<>();
    mFeaturedContentAdapter = new MapItemAdapter(getActivity(), mFeaturedContentList, this);
    view.setAdapter(mFeaturedContentAdapter);
    
    // Show progress dialog while we fetch featured content
    mProgressDialog = ProgressDialogFragment.newInstance("Fetching Featured Content");

    // set the target fragment to receive cancel notification
    mProgressDialog.setTargetFragment(this, REQUEST_CODE_PROGRESS_DIALOG);
    
    //Featured content available?
    if (mFeaturedContentList.size() == 0){
      fetchFeaturedContent();
    }

    return view;
  }

  private void fetchFeaturedContent() {
    // Show a progress dialog
    mProgressDialog.show(getActivity().getFragmentManager(), TAG_FEATURED_CONTENT_SEARCH_PROGRESS_DIALOG);
    List<MapsAppItem> cachedContents = null;
    // If user is signed in, check if we've already
    // downloaded their basemaps
    if (AccountManager.getInstance().isSignedIn()) {
      if(itemsLoadedFromCache(PRIVATE_FEATURED_CONTENT)){
        mProgressDialog.dismiss();
        return;
      }else{
        getFeaturedContentFromUserPortal();
      }
    } else { // user is not signed in, but have
      // they previously retrieved public
      // basemaps from AGOL
      if(itemsLoadedFromCache(PUBLIC_FEATURED_CONTENT)){
        mProgressDialog.dismiss();
        return;
      }else{ // get basemaps from AGOL
        final Portal portal = AccountManager.getInstance().getAGOLPortal();
        if (portal.getPortalInfo() == null){
          portal.addDoneLoadingListener(new Runnable() {
            @Override public void run() {
              loadFeaturedContentFromAGOL(portal);
            }
          });
          portal.loadAsync();
        }else{
          loadFeaturedContentFromAGOL(portal);
        }
      }
    }
  }

  private void getFeaturedContentFromUserPortal() {
    // we are signed in - fetch the basemaps of the user's portal
    final Portal portal = AccountManager.getInstance().getPortal();
    PortalInfo portalInfo = AccountManager.getInstance().getPortalInfo();

    // get the query string to fetch the portal
    // group that defines the portal's feature content
    PortalQueryParameters featuredContentGroupQuery = new PortalQueryParameters(portalInfo.getHomePageFeaturedContentGroupQuery());


    // Use a listenable future for retrieving search results from portal
    final ListenableFuture<PortalQueryResultSet<PortalGroup>> groupFuture = portal.findGroupsAsync(featuredContentGroupQuery);
    groupFuture.addDoneListener(new Runnable() {
      @Override

      public void run() {

        try {
          PortalQueryResultSet<PortalGroup> fcGroupResult = groupFuture.get();
          if (fcGroupResult != null && fcGroupResult.getResults() != null
              && !fcGroupResult.getResults().isEmpty()) {

            PortalGroup group = fcGroupResult.getResults().get(0);

            PortalQueryParameters groupContentQuery = new PortalQueryParameters();
            groupContentQuery.setQueryForItemsInGroup(group.getGroupId());

            final ListenableFuture<PortalQueryResultSet<PortalItem>> contentFuture = portal
                .findItemsAsync(groupContentQuery);
            contentFuture.addDoneListener(new Runnable() {
              @Override
              public void run() {
                try {
                  PortalQueryResultSet<PortalItem> items = contentFuture.get();
                  mPortalItems.addAll(items.getResults());
                  getFeaturedContentThumbnails();
                  PersistBasemaps.getInstance().storage.put(PRIVATE_FEATURED_CONTENT,mFeaturedContentList);
                  Log.i(TAG, "Persisting " + PRIVATE_FEATURED_CONTENT + " wtih " + mFeaturedContentList.size() + " items");
                } catch (Exception e) {
                  mProgressDialog.dismiss();
                  e.printStackTrace();
                }

              }
            });
          }
        } catch (Exception ie) {
          ie.printStackTrace();
        }
      }
    });
  }

  private void getFeaturedContentThumbnails() {
    for (final PortalItem item : mPortalItems) {
      if (item.getThumbnailFileName() != null) {
        final ListenableFuture<byte[]> futureThumbnail = item.fetchThumbnailAsync();
        futureThumbnail.addDoneListener(new Runnable() {

          @Override
          public void run() {
            try {
              byte[] itemThumbnailData = futureThumbnail.get();
              if (itemThumbnailData != null && itemThumbnailData.length > 0) {
                // Decode thumbnail and add this item to list
                // for display
                Bitmap bitmap = BitmapFactory.decodeByteArray(itemThumbnailData, 0,
                    itemThumbnailData.length);
                MapsAppItem portalItemData = new MapsAppItem(item, bitmap);
                mFeaturedContentList.add(portalItemData);
                // Update grid with results
                mFeaturedContentAdapter.notifyDataSetChanged();
              }
            } catch (Exception e) {
              e.printStackTrace();
            }
          }
        });
      }
    }
    mProgressDialog.dismiss();
  }

  private boolean itemsLoadedFromCache(String cacheName) {
    if (PersistFeaturedContent.getInstance().storage.get(cacheName) != null){
      List<MapsAppItem> cachedItems = PersistFeaturedContent.getInstance().storage.get(cacheName);
      mFeaturedContentList.clear();
      mFeaturedContentList.addAll(cachedItems);
      mFeaturedContentAdapter.notifyDataSetChanged();
      return  true;
    }else{
      return false;
    }

  }
  private void loadFeaturedContentFromAGOL(final Portal portal) {
    //Provides information about a portal as seen by the current user, anonymous or logged in. 
    PortalInfo portalInfo = portal.getPortalInfo();
    // Get the query string for items in basemap group 
    String featuredContentQueryString = portalInfo.getFeaturedItemsGroupQuery();
    //Create query parameters suitable for finding content or groups contained in a portal 
    PortalQueryParameters queryParams = new PortalQueryParameters(featuredContentQueryString);
    // Limit query to publicly available items
    queryParams.setCanSearchPublic(true);
    final ListenableFuture<PortalQueryResultSet<PortalGroup>> groupFuture = portal.findGroupsAsync(queryParams);
    // Listen for completion
    groupFuture.addDoneListener(new Runnable() {
      @Override public void run() {
        try {
          PortalQueryResultSet<PortalGroup> groupResults = groupFuture.get();
          if (groupResults.getResults().isEmpty()) {
            // Handle UI response for empty results
          } else {
            PortalGroup basemapGroup = groupResults.getResults().get(0);
            String groupId = basemapGroup.getGroupId();
            // Build a new query param object to retrieve featured content for given group
            PortalQueryParameters fcQuery = new PortalQueryParameters();
            fcQuery.setQuery(PortalItem.Type.WEBMAP, groupId, null);
            fcQuery.setCanSearchPublic(true);
            // Set sort order on basemap name 
            fcQuery.setSortField("name").setSortOrder(PortalQueryParameters.SortOrder.ASCENDING);
            // Find items that match the query
            final ListenableFuture<PortalQueryResultSet<PortalItem>> itemFuture = portal.findItemsAsync(fcQuery);
            itemFuture.addDoneListener(new Runnable() {
              @Override
              public void run() {
                try {
                  PortalQueryResultSet<PortalItem> items = itemFuture.get();
                  mPortalItems.addAll(items.getResults());
                  getFeaturedContentThumbnails();
                  PersistBasemaps.getInstance().storage.put(PUBLIC_FEATURED_CONTENT,mFeaturedContentList);
                  Log.i(TAG, "Persisting " + PUBLIC_FEATURED_CONTENT + " wtih " + mFeaturedContentList.size() + " items");
                } catch (Exception e) {
                  mProgressDialog.dismiss();
                  e.printStackTrace();
                }

              }
            });

          }
        } catch (InterruptedException e) {
          e.printStackTrace();
        } catch (ExecutionException e) {
          e.printStackTrace();
        }
      }
    });
  }
  @Override public void onMapItemClicked(int listPosition) {
    dismiss();

    String itemId = mFeaturedContentList.get(listPosition).item.getItemId();
    mFeaturedContentListener.onFeaturedContentChanged(itemId);
  }

  /**
     * A callback interface for managing what happens
     * when a featured content map is tapped
     */
  public interface FeaturedContentListener{
    void onFeaturedContentChanged(String portalItemId);
  }

}
