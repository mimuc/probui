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

import de.lmu.ifi.medien.probui.behaviours.ProbBehaviour;
import de.lmu.ifi.medien.probui.behaviours.ProbBehaviourTouch;

/**
 * An interface for a parser for creating touch behaviours from statements written in PML (ProbUI Modelling Language).
 */
public interface PMLParserTouch {

    /**
     * Parses the given PML statement to create and return the resulting touch behaviour, using the given additional information.
     * @param pmlStatement A statement in PML that specifies a touch behaviour.
     * @param x x-value of the top left corner of the rectangular bounding box of the GUI element's visuals.
     * @param y y-value of the top left corner of the rectangular bounding box of the GUI element's visuals.
     * @param width width of the rectangular bounding box of the GUI element's visuals.
     * @param height height of the rectangular bounding box of the GUI element's visuals.
     * @param screenWidth width of the screen
     * @param screenHeight height of the screen
     * @return The touch behaviour specified by the PML statement.
     */
    public ProbBehaviourTouch parse(String pmlStatement,
                                    double x, double y,
                                    double width, double height,
                                    double screenWidth, double screenHeight);
}