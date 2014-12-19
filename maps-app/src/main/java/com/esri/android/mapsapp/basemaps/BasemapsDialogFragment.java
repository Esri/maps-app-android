/* Copyright 1995-2014 Esri
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

package com.esri.android.mapsapp.basemaps;

import java.util.ArrayList;

import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;
import android.widget.Toast;

import com.esri.android.mapsapp.R;
import com.esri.android.mapsapp.account.AccountManager;
import com.esri.android.mapsapp.basemaps.BasemapsAdapter.BasemapsAdapterClickListener;
import com.esri.android.mapsapp.dialogs.ProgressDialogFragment;
import com.esri.core.portal.Portal;
import com.esri.core.portal.PortalGroup;
import com.esri.core.portal.PortalInfo;
import com.esri.core.portal.PortalItem;
import com.esri.core.portal.PortalQueryParams;
import com.esri.core.portal.PortalQueryParams.PortalQuerySortOrder;
import com.esri.core.portal.PortalQueryResultSet;

/**
 * Implements the dialog that provides a collection of basemaps to the user.
 */
public class BasemapsDialogFragment extends DialogFragment implements BasemapsAdapterClickListener, OnCancelListener {

  private static final String TAG = "BasemapsDialogFragment";

  private static final int REQUEST_CODE_PROGRESS_DIALOG = 1;

  /**
   * A callback interface that all activities containing this fragment must implement, to receive a new basemap from
   * this fragment.
   */
  public interface BasemapsDialogListener {
    /**
     * Called when a basemap is selected.
     * 
     * @param itemId portal item id of the selected basemap
     */
    public void onBasemapChanged(String itemId);
  }

  private BasemapsDialogListener mBasemapsDialogListener;

  private BasemapsAdapter mBasemapsAdapter;

  private ArrayList<BasemapItem> mBasemapItemList;

  private BasemapSearchAsyncTask mPendingBasemapSearch;

  // Mandatory empty constructor for fragment manager to recreate fragment after it's destroyed
  public BasemapsDialogFragment() {
  }

  /**
   * Sets listener for selection of new basemap.
   * 
   * @param listener
   */
  public void setBasemapsDialogListener(BasemapsDialogListener listener) {
    mBasemapsDialogListener = listener;
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setStyle(DialogFragment.STYLE_NORMAL, 0);
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    getDialog().setTitle(R.string.title_basemaps_dialog);

    // Inflate basemaps grid layout and setup list and adapter to back it
    GridView view = (GridView) inflater.inflate(R.layout.grid_layout, container, false);
    mBasemapItemList = new ArrayList<BasemapItem>();
    mBasemapsAdapter = new BasemapsAdapter(getActivity(), mBasemapItemList, this);
    view.setAdapter(mBasemapsAdapter);
    return view;
  }

  @Override
  public void onResume() {
    super.onResume();

    // If no basemaps yet, execute AsyncTask to search for available basemaps and populate the grid with them.
    // Note we do this here rather than in onCreateView() because otherwise the progress dialog doesn't show
    if (mBasemapItemList.size() == 0) {
      if (mPendingBasemapSearch != null) {
        mPendingBasemapSearch.cancel(true);
      }

      mPendingBasemapSearch = new BasemapSearchAsyncTask();
      mPendingBasemapSearch.execute();
    }

  }

  @Override
  public void onBasemapItemClicked(int position) {
    dismiss();

    String itemId = mBasemapItemList.get(position).item.getItemId();
    mBasemapsDialogListener.onBasemapChanged(itemId);
  }

  @Override
  public void onCancel(DialogInterface dialog) {
    // the progress dialog has been canceled - cancel pending basemap search task
    if (mPendingBasemapSearch != null) {
      mPendingBasemapSearch.cancel(true);
    }
  }

  /**
   * This class provides an AsyncTask that fetches info about available basemaps on a background thread and displays a
   * grid containing these on the UI thread.
   */
  private class BasemapSearchAsyncTask extends AsyncTask<Void, Void, Void> {
    private static final String TAG_BASEMAP_SEARCH_PROGRESS_DIALOG = "TAG_BASEMAP_SEARCH_PROGRESS_DIALOG";

    private Exception mException;

    private ProgressDialogFragment mProgressDialog;

    public BasemapSearchAsyncTask() {
    }

    @Override
    protected void onPreExecute() {
      mProgressDialog = ProgressDialogFragment.newInstance(getActivity().getString(R.string.fetching_basemaps));
      // set the target fragment to receive cancel notification
      mProgressDialog.setTargetFragment(BasemapsDialogFragment.this, REQUEST_CODE_PROGRESS_DIALOG);
      mProgressDialog.show(getActivity().getFragmentManager(), TAG_BASEMAP_SEARCH_PROGRESS_DIALOG);
    }

    @Override
    protected Void doInBackground(Void... params) {
      // Fetch basemaps on background thread
      mException = null;
      try {
        fetchBasemapItems();
      } catch (Exception e) {
        mException = e;
      }
      return null;
    }

    @Override
    protected void onPostExecute(Void result) {
      // Display results on UI thread
      mProgressDialog.dismiss();
      if (mException != null) {
        Log.w(TAG, "BasemapSearchAsyncTask failed with:");
        mException.printStackTrace();
        Toast.makeText(getActivity(), getString(R.string.basemapSearchFailed), Toast.LENGTH_LONG).show();
        dismiss();
        return;
      }
      // Success - update grid with results
      mBasemapsAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onCancelled(Void result) {
      // Dismiss the whole dialog if this task is cancelled
      dismiss();
    }

    /**
     * Fetches basemaps from the user's portal if signed in, otherwise from arcgis.com portal.
     * 
     * @throws Exception
     */
    private void fetchBasemapItems() throws Exception {

      PortalQueryResultSet<PortalItem> basemapResult = null;

      if (AccountManager.getInstance().isSignedIn()) {
        // we are signed in - fetch the basemaps of the user's portal
        Portal portal = AccountManager.getInstance().getPortal();
        PortalInfo portalInfo = AccountManager.getInstance().getPortalInfo();

        PortalQueryParams queryParams = new PortalQueryParams();

        // get the query string to fetch the portal group that defines the portal's basemaps
        queryParams.setQuery(portalInfo.getBasemapGalleryGroupQuery());

        PortalQueryResultSet<PortalGroup> basemapGroupResult = portal.findGroups(queryParams);
        if (basemapGroupResult != null && basemapGroupResult.getResults() != null
            && !basemapGroupResult.getResults().isEmpty()) {

          PortalGroup group = basemapGroupResult.getResults().get(0);

          PortalQueryParams basemapQueryParams = new PortalQueryParams();
          basemapQueryParams.setQueryForItemsInGroup(group.getGroupId());

          basemapResult = portal.findItems(basemapQueryParams);
        }
      } else {
        // we are not signed in - fetch a selection of basemaps from arcgis.com
        Portal portal = AccountManager.getInstance().getAGOLPortal();

        // Create a PortalQueryParams to query for items in basemap group
        PortalQueryParams queryParams = new PortalQueryParams();
        queryParams.setSortField("name").setSortOrder(PortalQuerySortOrder.ASCENDING);
        queryParams.setQuery(createDefaultQueryString());

        // Find items that match the query
        basemapResult = portal.findItems(queryParams);
      }

      if (isCancelled() || basemapResult == null || basemapResult.getResults() == null) {
        return;
      }

      // Loop through query results
      for (PortalItem item : basemapResult.getResults()) {
        // Fetch item thumbnail from server
        byte[] data = item.fetchThumbnail();
        if (isCancelled()) {
          return;
        }
        if (data != null) {
          // Decode thumbnail and add this item to list for display
          Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
          BasemapItem portalItemData = new BasemapItem(item, bitmap);
          mBasemapItemList.add(portalItemData);
        }
      }
    }

    /**
     * Creates a query string to fetch basemap portal items from arcgis.com.
     */
    private String createDefaultQueryString() {
      String query = null;

      String[] mBasemapIds = { "d5e02a0c1f2b4ec399823fdd3c2fdebd", // topographic
          "716b600dbbac433faa4bec9220c76b3a", // imagery with labels
          "b834a68d7a484c5fb473d4ba90d35e71", // open street map
          "8bf7167d20924cbf8e25e7b11c7c502c", // streets
          "2adf08a4a1a84834a773805a6e86f69e", // Oceans
          "149a9bb14d604bd18f4597b21c19fac7" // Gray
      };

      StringBuilder str = new StringBuilder();
      for (int i = 0; i < mBasemapIds.length; i++) {
        str.append("id:").append(mBasemapIds[i]);
        if (i < mBasemapIds.length - 1) {
          str.append(" OR ");
        }
      }
      query = str.toString();

      return query;
    }
  }
}
