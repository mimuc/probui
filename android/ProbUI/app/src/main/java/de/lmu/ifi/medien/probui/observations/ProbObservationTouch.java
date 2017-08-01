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

package de.lmu.ifi.medien.probui.observations;


public class ProbObservationTouch extends AbstractProbObservation {

    public static final int TYPE_TOUCH_DOWN = 0;
    public static final int TYPE_TOUCH_MOVE = 1;
    public static final int TYPE_TOUCH_UP = 2;


    public static final int FEATURE_X = 0;
    public static final int FEATURE_Y = 1;
    public static final int FEATURE_ORIENTATION = 2;
    public static final int FEATURE_AXIS_MINOR = 3;
    public static final int FEATURE_AXIS_MAJOR = 4;
    public static final int FEATURE_PRESSURE = 5;
    public static final int FEATURE_RAW_X = 6;
    public static final int FEATURE_RAW_Y = 7;


    public ProbObservationTouch(double[] touchFeatures, int[] touchTypeAndPointerID, long timestamp) {
        super(touchFeatures, touchTypeAndPointerID, timestamp);
    }

    public String toString() {

        return "ProbObservationTouch [type: " + this.getNominalFeatures()[0]
                + ", pointer ID: " + this.getNominalFeatures()[1]
                + ", x: " + this.getRealFeatures()[FEATURE_X]
                + ", y: " + this.getRealFeatures()[FEATURE_Y]
                + ", minorAxis: " + this.getRealFeatures()[FEATURE_AXIS_MINOR]
                + ", majorAxis: " + this.getRealFeatures()[FEATURE_AXIS_MAJOR] + "]";
    }
}
