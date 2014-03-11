package com.esri.android.mapsapp.basemaps;

import java.util.ArrayList;

import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
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
import com.esri.android.mapsapp.basemaps.BasemapsAdapter.BasemapsAdapterClickListener;
import com.esri.core.portal.Portal;
import com.esri.core.portal.PortalItem;
import com.esri.core.portal.PortalQueryParams;
import com.esri.core.portal.PortalQueryParams.PortalQuerySortOrder;
import com.esri.core.portal.PortalQueryResultSet;
import com.esri.core.portal.WebMap;

public class BasemapsDialogFragment extends DialogFragment implements BasemapsAdapterClickListener {

  private static final String TAG = "BasemapsDialogFragment";

  /**
   * A callback interface that all activities containing this fragment must implement, to receive a directions request
   * from this fragment.
   */
  public interface BasemapsDialogListener {
    /**
     * Callback for when a new basemap is selected.
     * 
     * @param webMap WebMap object containing the new basemap.
     */
    public void onBasemapChanged(WebMap webMap);
  }

  BasemapsDialogListener mBasemapsDialogListener;

  BasemapsAdapter mBasemapsAdapter;

  ArrayList<BasemapItem> mBasemapItemList;

  ProgressDialog mProgressDialog;

  // Mandatory empty constructor for fragment manager to recreate fragment after it's destroyed.
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

    // The following makes the dialog full-screen because it gives it a non-dialog theme:
    // setStyle(DialogFragment.STYLE_NORMAL, android.R.style.Theme_Holo_Light_DarkActionBar);

    // Create and initialise the progress dialog
    mProgressDialog = new ProgressDialog(getActivity()) {
      @Override
      public void onBackPressed() {
        // Back key pressed - just dismiss the dialog
        mProgressDialog.dismiss();
      }
    };
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

    // If no basemaps yet, search for available basemaps and populate the grid with them.
    // Note we do this here rather in onCreateView() because otherwise the progress dialog doesn't show
    if (mBasemapItemList.size() == 0) {
      new BasemapSearchAsyncTask().execute();
    }

  }

  @Override
  public void onBasemapItemClicked(int listPosition) {
    new BasemapFetchAsyncTask().execute(Integer.valueOf(listPosition));
  }

  /**
   * This class provides an AsyncTask that fetches info about available basemaps on a background thread and displays a
   * grid containing these on the UI thread.
   */
  private class BasemapSearchAsyncTask extends AsyncTask<Void, Void, Void> {
    private Exception mException;

    public BasemapSearchAsyncTask() {
    }

    @Override
    protected void onPreExecute() {
      // Display progress dialog on UI thread
      mProgressDialog.setMessage(getString(R.string.fetching_basemaps));
      mProgressDialog.setOnDismissListener(new OnDismissListener() {
        @Override
        public void onDismiss(DialogInterface arg0) {
          BasemapSearchAsyncTask.this.cancel(true);
        }
      });
      mProgressDialog.show();
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
     * Connects to portal and fetches info about basemaps.
     * 
     * @throws Exception
     */
    private void fetchBasemapItems() throws Exception {
      // Create a Portal object
      String url = getString(R.string.portal_url);
      Portal portal = new Portal(url, null);

      // Create a PortalQueryParams to query for items in basemap group
      PortalQueryParams queryParams = new PortalQueryParams();
      queryParams.setCanSearchPublic(true);
      queryParams.setSortField("name").setSortOrder(PortalQuerySortOrder.ASCENDING);
      queryParams.setQuery(createQueryString());

      // Find items that match the query
      PortalQueryResultSet<PortalItem> queryResultSet = portal.findItems(queryParams);
      if (isCancelled()) {
        return;
      }

      // Loop through query results
      for (PortalItem item : queryResultSet.getResults()) {
        // Fetch item thumbnail from server
        byte[] data = item.fetchThumbnail();
        if (isCancelled()) {
          return;
        }
        if (data != null) {
          // Decode thumbnail and add this item to list for display
          Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
          BasemapItem portalItemData = new BasemapItem(item, bitmap);
          Log.i(TAG, "Item id = " + item.getTitle());
          mBasemapItemList.add(portalItemData);
        }
      }
    }

    String[] mBasemapIds = { "d5e02a0c1f2b4ec399823fdd3c2fdebd", // topo
        "716b600dbbac433faa4bec9220c76b3a", // imagery with labels
        "2bc6e99fcb9640f0aa14aebcbcbaccd9", // DeLorme World Basemap
        "8bf7167d20924cbf8e25e7b11c7c502c" // streets
    };

    private String createQueryString() {
      StringBuilder str = new StringBuilder();
      for (int i = 0; i < mBasemapIds.length; i++) {
        str.append("id:").append(mBasemapIds[i]);
        if (i < mBasemapIds.length - 1) {
          str.append(" OR ");
        }
      }
      return str.toString();
    }

  }

  /**
   * This class provides an AsyncTask that fetches the selected basemap on a background thread, creates a WebMap from it
   * and passes it to MapsAppActivity to display it.
   */
  private class BasemapFetchAsyncTask extends AsyncTask<Integer, Void, WebMap> {
    private Exception mException;

    public BasemapFetchAsyncTask() {
      mProgressDialog.setMessage(getString(R.string.fetching_selected_basemap));
      mProgressDialog.setOnDismissListener(new OnDismissListener() {
        @Override
        public void onDismiss(DialogInterface arg0) {
          BasemapFetchAsyncTask.this.cancel(true);
        }
      });
    }

    @Override
    protected void onPreExecute() {
      // Display progress dialog on UI thread
      mProgressDialog.show();
    }

    @Override
    protected WebMap doInBackground(Integer... params) {
      // Fetch basemap data on background thread
      WebMap baseWebMap = null;
      mException = null;
      try {
        int position = params[0].intValue();
        Portal portal = new Portal("http://www.arcgis.com", null);
        String basemapID = mBasemapItemList.get(position).item.getItemId();

        // create a new WebMap of selected basemap from default portal
        baseWebMap = WebMap.newInstance(basemapID, portal);
      } catch (Exception e) {
        mException = e;
      }
      return baseWebMap;
    }

    @Override
    protected void onPostExecute(WebMap baseWebMap) {
      // Display results on UI thread
      mProgressDialog.dismiss();
      if (mException != null) {
        mException.printStackTrace();
        Toast.makeText(getActivity(), getString(R.string.basemapSearchFailed), Toast.LENGTH_LONG).show();
      } else {
        // Success - pass WebMap to MapsAppActivity to display
        mBasemapsDialogListener.onBasemapChanged(baseWebMap);
      }
      dismiss();
    }
  }
}
