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

package de.lmu.ifi.medien.probui.pml;

import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.medien.probui.pml.notifications.AbstractNotificationMarker;
import de.lmu.ifi.medien.probui.pml.notifications.NotificationMarkerStateReached;
import de.lmu.ifi.medien.probui.pml.notifications.NotificationMarkerTouchEventReached;

/**
 * Class representing a "sequence rule". This is not the same as a "PML rule"!
 * The sequence rule encodes e.g. when a behaviour is considered to be "completed" by the user.
 * For example, a "N->S" behaviour is completed if the user started at "N" and has reached "S".
 * See the paper for more details.
 */
public class PMLRulePattern {

    public static final int TOUCH_EVENT_TOKEN_MODIFIER_NORMAL = 0;
    public static final int TOUCH_EVENT_TOKEN_MODIFIER_MIN_ONE = 1;
    public static final int TOUCH_EVENT_TOKEN_MODIFIER_ZERO_OR_MORE = 2;

    public int[][] mT;
    public int[] pis;
    public int[] ends;

    //TODO: just counting will only be enough as long as we only have touch up and down.
    // Otherwise, we might need a list per state to store them in order.
    //public int[] touchDownCounts;
    //public int[] touchUpCounts;

    public List<List<TouchEventToken>> touchEventTokens;

    public List<AbstractNotificationMarker> notificationMarkers;
    private int markerIndex;


    public List<NotificationMarkerStateReached> stateReachedMarkers;
    public ArrayList<List<NotificationMarkerTouchEventReached>> touchEventReachedMarkers;


    public PMLRulePattern() {
        this.touchEventTokens = new ArrayList<List<TouchEventToken>>();
        this.notificationMarkers = new ArrayList<AbstractNotificationMarker>();
        this.stateReachedMarkers = new ArrayList<NotificationMarkerStateReached>();
        this.touchEventReachedMarkers = new ArrayList<List<NotificationMarkerTouchEventReached>>();
    }

    public void addTouchEventToken(int stateIndex, int type, int modifier) {
        while (this.touchEventTokens.size() <= stateIndex)
            this.touchEventTokens.add(new ArrayList<TouchEventToken>());
        this.touchEventTokens.get(stateIndex).add(new TouchEventToken(type, modifier));
    }


    public List<TouchEventToken> getTouchEventTokens(int stateIndex) {

        if (stateIndex < this.touchEventTokens.size())
            return this.touchEventTokens.get(stateIndex);
        return null;
    }

    public void addStateMarker(int stateIndex) {
        NotificationMarkerStateReached marker = new NotificationMarkerStateReached(markerIndex, stateIndex);
        this.notificationMarkers.add(marker);
        this.stateReachedMarkers.add(marker);
        this.markerIndex++;
    }

    public void addTouchEventMarker(int stateIndex, int touchEventIndex) {

        while (this.touchEventReachedMarkers.size() <= stateIndex)
            this.touchEventReachedMarkers.add(new ArrayList<NotificationMarkerTouchEventReached>());

        NotificationMarkerTouchEventReached marker = new NotificationMarkerTouchEventReached(markerIndex, stateIndex, touchEventIndex);
        this.notificationMarkers.add(marker);
        this.touchEventReachedMarkers.get(stateIndex).add(marker);
        this.markerIndex++;
    }

    public List<AbstractNotificationMarker> getNotificationMarkers() {
        return notificationMarkers;
    }

    public List<NotificationMarkerStateReached> getStateReachedMarkers() {
        return stateReachedMarkers;
    }

    public List<NotificationMarkerTouchEventReached> getTouchEventReachedMarkers(int stateIndex) {
        if (stateIndex < this.touchEventReachedMarkers.size())
            return this.touchEventReachedMarkers.get(stateIndex);
        return null;
    }


    protected class TouchEventToken {

        public int modifier;
        public int type;

        public TouchEventToken(int type, int modifier) {
            this.modifier = modifier;
            this.type = type;
        }
    }


}
