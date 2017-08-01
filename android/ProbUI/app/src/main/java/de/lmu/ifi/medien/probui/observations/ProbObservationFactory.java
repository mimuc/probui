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


public class ProbObservationFactory {


    /**
     * Creates a touch observation object with the given touch location and type.
     *
     * @param touchX
     * @param touchY
     * @param orientation
     * @param axisMinor
     * @param axisMajor
     * @param pressure
     * @param touchEventType e.g. touch down, touch move, touch up
     * @param timestamp
     * @return
     */
    public static ProbObservationTouch createTouchObservation(
                                                              double rawX, double rawY,
                                                              double touchX, double touchY,
                                                              double orientation,
                                                              double axisMinor, double axisMajor,
                                                              double pressure,
                                                              int touchEventType,
                                                              int touchPointerID,
                                                              long timestamp) {
        double[] realFeatures = {touchX, touchY, orientation, axisMinor, axisMajor, pressure, rawX, rawY};
        int[] nominalFeatures = {touchEventType, touchPointerID};
        return new ProbObservationTouch(realFeatures, nominalFeatures, timestamp);
    }


}
