/**
 * MIT License
 * <p>
 * Copyright (c) 2017 Wolox S.A
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software
 * and associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

package ar.com.wolox.wolmo.core.adapter.viewpager;

import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.util.Pair;

import java.util.ArrayList;
import java.util.Collections;

public class SimpleFragmentStatePagerAdapter extends FragmentStatePagerAdapter {

    private ArrayList<Pair<Fragment, String>> mFragmentsAndTitles = new ArrayList<>();

    /**
     * Constructor, requires an instance of a {@link FragmentManager} and a variable length
     * argument with instances of {@link Pair} containing a {@link Fragment} and corresponding
     * {@link String} with the title for that {@link Fragment}
     *
     * @param fm    An instance of {@link FragmentManager}. Cannot be null.
     * @param pairs A variable length of pairs of fragments and titles. Can be empty, but each
     *              element cannot be null.
     */
    @SafeVarargs
    public SimpleFragmentStatePagerAdapter(@NonNull FragmentManager fm,
                                           @NonNull Pair<Fragment, String>... pairs) {
        super(fm);
        Collections.addAll(mFragmentsAndTitles, pairs);
    }

    /**
     * Adds a {@link Fragment} instance with a corresponding {@link String} title to the adapter
     *
     * @param fragment an instance of {@link Fragment} that will be displayed in the adapter.
     * @param title    a title corresponding to the {@link Fragment} that will be added
     */
    public void addFragment(@NonNull Fragment fragment, @NonNull String title) {
        mFragmentsAndTitles.add(new Pair<>(fragment, title));
        notifyDataSetChanged();
    }

    /**
     * Refer to {@link FragmentStatePagerAdapter} documentation.
     */
    @Override
    public Fragment getItem(int position) {
        return mFragmentsAndTitles.get(position).first;
    }

    /**
     * Refer to {@link FragmentStatePagerAdapter} documentation.
     */
    @Override
    public int getCount() {
        return mFragmentsAndTitles.size();
    }

    /**
     * Refer to {@link FragmentStatePagerAdapter} documentation.
     */
    @Override
    public CharSequence getPageTitle(int position) {
        return mFragmentsAndTitles.get(position).second;
    }
}
