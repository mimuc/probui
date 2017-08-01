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

/**
 * This class holds constants for the tokens in PML.
 */
public class PMLTokens {


    public static final String BEHAVIOUR_LABEL_SEPARATOR = ":";

    public static final String BEHAVIOUR_NOTIFICATION_MARKER = "$";


    public static final String STATE_LOCATION_MOVE_NORTH = "N";
    public static final String STATE_LOCATION_MOVE_EAST = "E";
    public static final String STATE_LOCATION_MOVE_SOUTH = "S";
    public static final String STATE_LOCATION_MOVE_WEST = "W";
    public static final String STATE_LOCATION_MOVE_CENTRE = "C";

    public static final String STATE_LOCATION_RELATIVE_ORIGIN = "O";


    public static final String STATE_SUBLOCATION_MOVE_TOP = "T";
    public static final String STATE_SUBLOCATION_MOVE_RIGHT = "R";
    public static final String STATE_SUBLOCATION_MOVE_BOTTOM = "B";
    public static final String STATE_SUBLOCATION_MOVE_LEFT = "L";
    public static final String STATE_SUBLOCATION_ZOOM_X = "X";
    public static final String STATE_SUBLOCATION_ZOOM_Y = "Y";
    public static final String STATE_SUBLOCATION_ZOOM = "Z";
    public static final String STATE_SUBLOCATION_ZOOM_OUT_X = "x";
    public static final String STATE_SUBLOCATION_ZOOM_OUT_Y = "y";
    public static final String STATE_SUBLOCATION_ZOOM_OUT = "z";

    public static final String STATE_TOUCH_EVENT_TYPE_DOWN = "d";
    public static final String STATE_TOUCH_EVENT_TYPE_UP = "u";
    public static final String STATE_TOUCH_EVENT_TYPE_MOVE = "m";

    public static final String STATE_TOUCH_EVENT_MOD_MIN_ONE = "+";
    public static final String STATE_TOUCH_EVENT_MOD_ZERO_OR_MORE = "*";

    public static final String STATE_ENDSTATE_MARKER = ".";


    public static final String STATE_DETAILS_OPENER = "[";
    public static final String STATE_DETAILS_CLOSER = "]";

    public static final String STATE_DETAILS_SEPARATOR = ",";
    public static final String STATE_DETAILS_ASSIGNER = "=";

    public static final String STATE_DETAILS_SCALE_XY = "s";
    public static final String STATE_DETAILS_SCALE_X = "sx";
    public static final String STATE_DETAILS_SCALE_Y = "sy";
    public static final String STATE_DETAILS_SIZE_W = "w";
    public static final String STATE_DETAILS_SIZE_H = "h";


    public static final String TRANSITION_ONE_WAY = "->";
    public static final String TRANSITION_TWO_WAY = "<->";



    public static final String RULE_LABEL_SEPARATOR = ":";
    public static final String RULE_AUTONAME_SEPARATOR = "_";

    public static final String RULE_BRACKET_OPEN = "(";
    public static final String RULE_BRACKET_CLOSE = ")";

    public static final String RULE_OPERATOR_AND = "and";
    public static final String RULE_OPERATOR_OR = "or";
    public static final String RULE_OPERATOR_NOT = "not";

    public static final String RULE_EVENT_OPERATOR_IS = "is";
    public static final String RULE_EVENT_OPERATOR_ON = "on";
    public static final String RULE_EVENT_COMPLETED = "complete";
    public static final String RULE_EVENT_JUST_COMPLETED = "complete";
    public static final String RULE_EVENT_MOST_LIKELY = "most_likely";

    public static final String RULE_QUALIFIER_TIME_TAKEN = "in";
    public static final String RULE_QUALIFIER_NUM_FINGERS = "using";
    public static final String RULE_QUALIFIER_TOUCH_PRESSURE = "with";
    public static final String RULE_QUALIFIER_TOUCH_SIZE = "with";
    public static final String RULE_QUALIFIER_UNIT_MILLISECONDS = "ms";
    public static final String RULE_QUALIFIER_UNIT_SECONDS = "s";
    public static final String RULE_QUALIFIER_UNIT_FINGERS = "fingers";
    public static final String RULE_QUALIFIER_UNIT_TOUCH_PRESSURE = "p";
    public static final String RULE_QUALIFIER_UNIT_TOUCH_SIZE = "a";


}
