package com.esri.android.maps.app;

import android.support.test.espresso.matcher.BoundedMatcher;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.View;

import org.hamcrest.Description;
import org.hamcrest.Matcher;

import static android.support.test.internal.util.Checks.checkNotNull;
import static org.hamcrest.Matchers.is;

/**
 * Created by sand8529 on 4/26/16.
 * A custom matcher that checks the query hint property of a {@link android.widget.SearchView}. It
 * accepts a {@link String}.
 */
public final class QueryHintMatcher {


    public static Matcher<View> withQueryHint(final Matcher<String> stringMatcher){
        checkNotNull(stringMatcher);

        return new BoundedMatcher<View, SearchView>(SearchView.class) {
            @Override
            public boolean matchesSafely(SearchView view){

                return stringMatcher.matches(view.getQueryHint().toString());
            }
            @Override
            public void describeTo(Description description){
                description.appendText("with query hint: ");
                stringMatcher.describeTo(description);
            }
        };
    }
}
