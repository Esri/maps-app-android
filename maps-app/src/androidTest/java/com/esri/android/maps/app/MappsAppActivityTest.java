package com.esri.android.maps.app;

import android.app.Instrumentation;
import android.os.SystemClock;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.DataInteraction;
import android.support.test.espresso.Espresso;
import android.support.test.espresso.IdlingPolicies;
import android.support.test.espresso.IdlingResource;
import android.support.test.espresso.ViewAction;
import android.support.test.espresso.ViewInteraction;
import android.support.test.espresso.assertion.ViewAssertions;
import android.support.test.espresso.contrib.DrawerActions;
import android.support.test.espresso.contrib.DrawerMatchers;
import android.support.test.espresso.matcher.ViewMatchers;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.v7.widget.AppCompatTextView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.SearchView.SearchAutoComplete;
import android.text.format.DateUtils;
import android.view.View;

import com.esri.android.mapsapp.DrawerItem;
import com.esri.android.mapsapp.MapFragment;
import com.esri.android.mapsapp.MapsAppActivity;
import com.esri.android.mapsapp.R;
import com.esri.android.mapsapp.R.id;
import com.esri.android.mapsapp.R.string;
import com.esri.android.mapsapp.basemaps.BasemapItem;
import com.esri.android.mapsapp.basemaps.BasemapsAdapter;
import com.esri.arcgisruntime.mapping.view.MapView;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.hamcrest.core.AllOf;
import org.hamcrest.core.AnyOf;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.TimeUnit;

import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.longClick;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.hasDescendant;
import static android.support.test.espresso.matcher.ViewMatchers.isClickable;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.isEnabled;
import static android.support.test.espresso.matcher.ViewMatchers.withClassName;
import static android.support.test.espresso.matcher.ViewMatchers.withContentDescription;
import static android.support.test.espresso.matcher.ViewMatchers.withHint;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withInputType;
import static android.support.test.espresso.matcher.ViewMatchers.withParent;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.anything;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasToString;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.isEmptyString;
import static org.hamcrest.Matchers.startsWith;

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
@RunWith(AndroidJUnit4.class)
public class MappsAppActivityTest  {

    @Rule
    public final ActivityTestRule<MapsAppActivity> main = new ActivityTestRule<>(MapsAppActivity.class);


    @Before
    public void resetTimeout() {
        IdlingPolicies.setMasterPolicyTimeout(60, TimeUnit.SECONDS);
        IdlingPolicies.setIdlingResourceTimeout(26, TimeUnit.SECONDS);
    }

    @Test
    public void testMainScreen(){
        // Various layouts should be present and/or enabled
        onView(withId(id.maps_app_activity_drawer_layout)).check(matches(isEnabled()));
        onView(withId(id.maps_app_activity_content_frame)).check(matches(isDisplayed()));
        onView(withId(id.maps_app_activity_left_drawer)).check(matches(isEnabled()));

        // Map should be enabled
        onView(withId(id.map)).check(matches(isEnabled()));

        // Floating action button should be enabled
        onView(withId(id.fab)).check(matches(isEnabled()));

        // Search view should be displayed
        onView(allOf(withId(id.searchView1))).check(matches(isDisplayed()));

        // Should be able to tap on the search box
        onView(withClassName(endsWith(SearchAutoComplete.class.getSimpleName()))).perform(click());
    }

    @Test
    public void testDrawer(){
        //Open up the drawer
        onView(withId(id.maps_app_activity_drawer_layout)).perform(DrawerActions.open());

        // The 'Switch Basemap' and 'Sign In' items should be present in the drawer
        onView(withText(main.getActivity().getString(string.menu_basemaps)));
        onView(withText(main.getActivity().getString(string.sign_in)));

        // Close the drawer
        onView(withId(id.maps_app_activity_drawer_layout)).perform(DrawerActions.close());
    }

    @Test
    /**
     * This test should open the drawer and select a publicly
     * available basemap.
     */
    public void testPublicBasemaps(){
        onView(withId(id.maps_app_activity_drawer_layout)).perform(DrawerActions.open());

        // Does the list item for basemaps exist?
        onView(withText(main.getActivity().getString(string.menu_basemaps))).perform(click());

        //We should be seeing a grid view populated with items
        onView(withId(id.basemap_gridview)).check(matches(isDisplayed()));

        // It should be populated using a basemaps adapter
        onData(allOf(is(instanceOf(BasemapItem.class))));

        // Find the basemap item with a thumbnail Click on the Light Gray Canvas
        onData(anyOf(withId(id.basemap_grid_item_title_textview),withText("Light Gray Canvas"), isDisplayed()));
        onData(anyOf(withId(id.basemap_grid_item_title_textview),withText("Light Gray Canvas"), isClickable()));

    }

}
