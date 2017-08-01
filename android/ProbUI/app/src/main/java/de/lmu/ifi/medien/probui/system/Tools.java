/*
ProbUI - a probabilistic reinterpretation of bounding boxes
designed to facilitate creating dynamic and adaptive mobile touch GUIs.
Copyright (C) 2017 Daniel Buschek

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program. If not, see <http://www.gnu.org/licenses/>.
*/

package de.lmu.ifi.medien.probui.system;

import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.medien.probui.gui.ProbInteractor;


public class Tools {

    /**
     * Adapted from: http://stackoverflow.com/questions/5062264/find-all-views-with-tag
     * Returns a list with all views within the given root view, that have the given tag.
     *
     * @param root
     * @return
     */
    public static List<ProbInteractor> getAllProbInteractors(ViewGroup root) {

        List<ProbInteractor> views = new ArrayList<ProbInteractor>();
        final int childCount = root.getChildCount();
        for (int i = 0; i < childCount; i++) {
            final View child = root.getChildAt(i);
            if (child instanceof ViewGroup) {
                views.addAll(getAllProbInteractors((ViewGroup) child));
            }

            /*final Object tagObj = child.getTag();
            if (tagObj != null && tagObj.equals(tag)) {
                views.add(child);
            }*/
            if (child instanceof ProbInteractor) {
                views.add((ProbInteractor) child);
            }

        }
        return views;
    }



    public static List<View> getAllNonProbViews(ViewGroup root) {

        List<View> views = new ArrayList<View>();
        final int childCount = root.getChildCount();
        for (int i = 0; i < childCount; i++) {
            final View child = root.getChildAt(i);
            if (child instanceof ViewGroup) {
                views.addAll(getAllNonProbViews((ViewGroup) child));
            }

            if (child instanceof ProbInteractor) {
                // nothing
            }
            else {
                views.add(child);
            }

        }
        return views;
    }
}
