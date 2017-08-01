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


public class SystemSetup {

    /**
     * Currently unused. Should hold names of all implemented ProbUI probInteractors.
     */
    public static final String[] IMPLEMENTED_INTERACTOR_TYPES
            = {"de.lmu.ifi.medien.probui.gui.elements.ProbButton"};

    public static final int TOUCH_PROBABILITY_MODE_NORMAL = 0;
    public static final int TOUCH_PROBABILITY_MODE_BC = 1;


    public static int TOUCH_PROBABILITY_MODE = 0;
}
