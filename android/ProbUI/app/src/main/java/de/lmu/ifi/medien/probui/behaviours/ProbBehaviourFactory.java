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

package de.lmu.ifi.medien.probui.behaviours;

import android.util.Log;

import de.lmu.ifi.medien.probui.pml.PMLParserTouch;
import de.lmu.ifi.medien.probui.pml.PMLParserTouchImpl;

/**
 * Creates behaviours based on presets (names).
 * Can be set manually or "injected" into ProbInteractors with the ProbBehaviourLinker.
 *
 * Slightly outdated: Could setup these behaviours via PML as well.
 */
public class ProbBehaviourFactory {


    public static final String BEHAVIOUR_DEBUG_TEST = "touch_debug_test";

    /**
     * Denotes a touch behaviour pattern consisting of a simple Gaussian centred on the interactor,
     * with the length-scales (variances) set according to the interactor's (box) width and height.
     * This is the simplest probabilistic touch behaviour pattern possible.
     */
    public static final String BEHAVIOUR_TOUCH_CENTRE = "touch_centre";

    public static final String BEHAVIOUR_TOUCH_CENTRE_WIDE = "touch_centre_wide";

    public static final String BEHAVIOUR_TOUCH_ZOOM_HORIZONTAL = "touch_zoom_horizontal";

    public static final String BEHAVIOUR_TOUCH_3_CONCENTRIC_CIRCLES = "touch_3_concentric_circles";

    public static final String BEHAVIOUR_TOUCH_DRAG_LEFT_TO_RIGHT = "touch_drag_left_to_right";

    public static final String BEHAVIOUR_TOUCH_DRAG_RIGHT_TO_LEFT = "touch_drag_right_to_left";


    public static final String BEHAVIOUR_TOUCH_DRAG_FAR_LEFT_TO_RIGHT = "touch_drag_far_left_to_right";

    public static final String BEHAVIOUR_TOUCH_DRAG_FAR_RIGHT_TO_LEFT = "touch_drag_far_right_to_left";

    public static final String BEHAVIOUR_TOUCH_SLIDE_ARC_TOP_LEFT = "touch_slide_arc_top_left";
    public static final String BEHAVIOUR_TOUCH_SLIDE_ARC_TOP_RIGHT = "touch_slide_arc_top_right";
    public static final String BEHAVIOUR_TOUCH_SLIDE_ARC_BOTTOM_LEFT = "touch_slide_arc_bottom_left";
    public static final String BEHAVIOUR_TOUCH_SLIDE_ARC_BOTTOM_RIGHT = "touch_slide_arc_bottom_right";


    /**
     * Returns behavioural pattern(s) for touch input,
     * based on the given preset name and the given information.
     *
     * @param preset
     * @param x
     * @param y
     * @param width
     * @param height
     * @param screenWidth
     * @param screenHeight
     * @return
     */
    public static ProbBehaviourTouch[] createPresetProbBehaviourTouch(
            String preset, double x, double y,
            double width, double height,
            double screenWidth, double screenHeight, float ddensity) {

        ProbBehaviourTouch[] behaviours = null;

        Log.d("ProbBehaviourFactory", "in createPresetProbBehaviourTouch with parameters: " + x + ", " + y + ", " + width + ", " + height + ", " + screenWidth + ", " + screenHeight);


        if (preset.equals(BEHAVIOUR_DEBUG_TEST)) {

            behaviours = new ProbBehaviourTouch[1];

            PMLParserTouch pmlParser = new PMLParserTouchImpl(ddensity);
            behaviours[0] = pmlParser.parse(BEHAVIOUR_DEBUG_TEST+": N<->C<->S", x, y, width, height, screenWidth, screenHeight);
            Log.d("PML", "factory created behaviour with PML: " + BEHAVIOUR_DEBUG_TEST);
            Log.d("PML", behaviours[0].toString());
        }


        if (preset.equals(BEHAVIOUR_TOUCH_CENTRE)) {

            behaviours = new ProbBehaviourTouch[1];

            /*
            ProbBehaviourTouch behaviour = new ProbBehaviourTouch(BEHAVIOUR_TOUCH_CENTRE, 1);
            double[] prot_centre_mean = {(x + width / 2) / screenWidth, (y + height / 2) / screenHeight};
            //double[][] prot_centre_mCov = {{Math.pow(width/ screenWidth / 4, 2) , 0}, {0, Math.pow(height/ screenHeight / 4, 2) }};
            double[][] prot_centre_mCov = {{Math.pow(width/ screenWidth / 4, 2) , 0}, {0, Math.pow(height/ screenHeight / 4, 2) }};
            behaviour.setState(0, prot_centre_mean, prot_centre_mCov, 1);
            behaviour.setAcceptedPointerIDs(0);
            */
            PMLParserTouch pmlParser = new PMLParserTouchImpl(ddensity);
            behaviours[0] = pmlParser.parse(BEHAVIOUR_TOUCH_CENTRE+": C", x, y, width, height, screenWidth, screenHeight);
            Log.d("PML", "factory created behaviour with PML: " + BEHAVIOUR_TOUCH_CENTRE);
            Log.d("PML", behaviours[0].toString());
        }


        if (preset.equals(BEHAVIOUR_TOUCH_CENTRE_WIDE)) {

            behaviours = new ProbBehaviourTouch[1];

            /*
            ProbBehaviourTouch behaviour = new ProbBehaviourTouch(BEHAVIOUR_TOUCH_CENTRE_WIDE, 1);
            double[] prot_centre_mean = {(x + width / 2) / screenWidth, (y + height / 2) / screenHeight};
            double[][] prot_centre_mCov = {{Math.pow(width/ screenWidth / 3, 2) , 0}, {0, Math.pow(height/ screenHeight / 3, 2) }};
            behaviour.setState(0, prot_centre_mean, prot_centre_mCov, 1);
            behaviour.setAcceptedPointerIDs(0);
            */

            PMLParserTouch pmlParser = new PMLParserTouchImpl(ddensity);
            behaviours[0] = pmlParser.parse(BEHAVIOUR_TOUCH_CENTRE_WIDE + ": C(sx=1.5)", x, y, width, height, screenWidth, screenHeight);
            Log.d("PML", "factory created behaviour with PML: " + BEHAVIOUR_TOUCH_CENTRE_WIDE);
            Log.d("PML", behaviours[0].toString());
        }


        if (preset.equals(BEHAVIOUR_TOUCH_ZOOM_HORIZONTAL)) {

            behaviours = new ProbBehaviourTouch[2];

            // Behaviour for pointer 1:
            ProbBehaviourTouch behaviour = new ProbBehaviourTouch(BEHAVIOUR_TOUCH_ZOOM_HORIZONTAL, 2);
            // State 1:
            double[] prot_left_mean = {(x + width / 10.) / screenWidth, (y + height / 2) / screenHeight};
            double[][] prot_left_mCov = {{Math.pow(width / screenWidth / 16, 2), 0}, {0, Math.pow(height / screenHeight / 4, 2)}};
            behaviour.setState(0, prot_left_mean, prot_left_mCov, 0.01);

            // State 2:
            double[] prot_centre_mean = {(x + width * 3. / 8) / screenWidth, (y + height / 2) / screenHeight};
            double[][] prot_centre_mCov = {{Math.pow(width / screenWidth / 16, 2), 0}, {0, Math.pow(height / screenHeight / 4, 2)}};
            behaviour.setState(1, prot_centre_mean, prot_centre_mCov, 0.99);


            double[][] transitions = {{1, 0}, {0.9, 0.1}};
            behaviour.setTransitions(transitions);

            behaviour.setAcceptedPointerIDs(0, 1);
            behaviours[0] = behaviour;

            // Behaviour for pointer 2:
            ProbBehaviourTouch behaviour2 = new ProbBehaviourTouch(BEHAVIOUR_TOUCH_ZOOM_HORIZONTAL, 2);
            // State 1:
            double[] prot_right_mean = {(x + width * 9.0 / 10) / screenWidth, (y + height / 2) / screenHeight};
            double[][] prot_right_mCov = {{Math.pow(width / screenWidth / 16, 2), 0}, {0, Math.pow(height / screenHeight / 4, 2)}};
            behaviour2.setState(0, prot_right_mean, prot_right_mCov, 0.01);

            // State 2:
            double[] prot_centre2_mean = {(x + width * 5. / 8) / screenWidth, (y + height / 2) / screenHeight};
            double[][] prot_centre2_mCov = {{Math.pow(width / screenWidth / 16, 2), 0}, {0, Math.pow(height / screenHeight / 4, 2)}};
            behaviour2.setState(1, prot_centre2_mean, prot_centre2_mCov, 0.99);

            double[][] transitions2 = {{1, 0}, {0.9, 0.1}};
            behaviour2.setTransitions(transitions2);

            behaviour2.setAcceptedPointerIDs(0, 1);
            behaviours[1] = behaviour2;

        }


        if (preset.equals(BEHAVIOUR_TOUCH_3_CONCENTRIC_CIRCLES)) {

            behaviours = new ProbBehaviourTouch[3];

            ProbBehaviourTouch behaviour = new ProbBehaviourTouch(BEHAVIOUR_TOUCH_3_CONCENTRIC_CIRCLES, 1);
            double[] prot_centre_mean = {(x + width / 2) / screenWidth, (y + height / 2) / screenHeight};
            double[][] prot_centre_mCov = {{Math.pow(width / screenWidth / 4, 2), 0}, {0, Math.pow(height / screenHeight / 4, 2)}};
            behaviour.setState(0, prot_centre_mean, prot_centre_mCov, 1);
            behaviour.setAcceptedPointerIDs(0);
            behaviours[0] = behaviour;

            ProbBehaviourTouch behaviour2 = new ProbBehaviourTouch(BEHAVIOUR_TOUCH_3_CONCENTRIC_CIRCLES, 1);
            double[][] prot_centre_mCov2 = {{Math.pow(width / screenWidth / 2, 2), 0}, {0, Math.pow(height / screenHeight / 2, 2)}};
            behaviour2.setState(0, prot_centre_mean, prot_centre_mCov2, 1);
            behaviour2.setAcceptedPointerIDs(0);
            behaviours[1] = behaviour2;

            ProbBehaviourTouch behaviour3 = new ProbBehaviourTouch(BEHAVIOUR_TOUCH_3_CONCENTRIC_CIRCLES, 1);
            double[][] prot_centre_mCov3 = {{Math.pow(width / screenWidth / 1.5, 2), 0}, {0, Math.pow(height / screenHeight / 1.5, 2)}};
            behaviour3.setState(0, prot_centre_mean, prot_centre_mCov3, 1);
            behaviour3.setAcceptedPointerIDs(0);
            behaviours[2] = behaviour3;

        }


        if (preset.equals(BEHAVIOUR_TOUCH_DRAG_LEFT_TO_RIGHT)) {

            behaviours = new ProbBehaviourTouch[1];

            ProbBehaviourTouch behaviour = new ProbBehaviourTouch(BEHAVIOUR_TOUCH_DRAG_LEFT_TO_RIGHT, 2);
            double[] prot_centre_mean = {(x + width) / screenWidth, (y + height / 2) / screenHeight};
            double[][] prot_centre_mCov = {{Math.pow(width / screenWidth / 5, 2), 0}, {0, Math.pow(height / screenHeight / 5, 2)}};
            behaviour.setState(0, prot_centre_mean, prot_centre_mCov, 1);
            behaviour.setAcceptedPointerIDs(0);

            double[] prot_centre_mean2 = {(x + width * 2) / screenWidth, (y + height / 2) / screenHeight};
            double[][] prot_centre_mCov2 = {{Math.pow(width / screenWidth / 5, 2), 0}, {0, Math.pow(height / screenHeight / 5, 2)}};
            behaviour.setState(1, prot_centre_mean2, prot_centre_mCov2, 0);
            behaviour.setAcceptedPointerIDs(0);

            double[][] transitions = {{0.9, 0.1}, {0, 1}};
            behaviour.setTransitions(transitions);
            behaviours[0] = behaviour;
        }


        if (preset.equals(BEHAVIOUR_TOUCH_DRAG_RIGHT_TO_LEFT)) {

            behaviours = new ProbBehaviourTouch[1];

            ProbBehaviourTouch behaviour = new ProbBehaviourTouch(BEHAVIOUR_TOUCH_DRAG_RIGHT_TO_LEFT, 2);
            double[] prot_centre_mean = {(x - width) / screenWidth, (y + height / 2) / screenHeight};
            double[][] prot_centre_mCov = {{Math.pow(width / screenWidth / 5, 2), 0}, {0, Math.pow(height / screenHeight / 5, 2)}};
            behaviour.setState(0, prot_centre_mean, prot_centre_mCov, 0);
            behaviour.setAcceptedPointerIDs(0);

            double[] prot_centre_mean2 = {(x) / screenWidth, (y + height / 2) / screenHeight};
            double[][] prot_centre_mCov2 = {{Math.pow(width / screenWidth / 5, 2), 0}, {0, Math.pow(height / screenHeight / 5, 2)}};
            behaviour.setState(1, prot_centre_mean2, prot_centre_mCov2, 1);
            behaviour.setAcceptedPointerIDs(0);

            double[][] transitions = {{1, 0}, {0.1, 0.9}};
            behaviour.setTransitions(transitions);
            behaviours[0] = behaviour;
        }


        if (preset.equals(BEHAVIOUR_TOUCH_DRAG_FAR_LEFT_TO_RIGHT)) {

            behaviours = new ProbBehaviourTouch[1];

            ProbBehaviourTouch behaviour = new ProbBehaviourTouch(BEHAVIOUR_TOUCH_DRAG_FAR_LEFT_TO_RIGHT, 2);
            double[] prot_centre_mean = {(x + width) / screenWidth, (y + height / 2) / screenHeight};
            double[][] prot_centre_mCov = {{Math.pow(width / screenWidth / 5, 2), 0}, {0, Math.pow(height / screenHeight / 5, 2)}};
            behaviour.setState(0, prot_centre_mean, prot_centre_mCov, 1);
            behaviour.setAcceptedPointerIDs(0);

            double[] prot_centre_mean2 = {(x + width * 2.5) / screenWidth, (y + height / 2) / screenHeight};
            double[][] prot_centre_mCov2 = {{Math.pow(width / screenWidth / 3, 2), 0}, {0, Math.pow(height / screenHeight / 5, 2)}};
            behaviour.setState(1, prot_centre_mean2, prot_centre_mCov2, 0);
            behaviour.setAcceptedPointerIDs(0);

            double[][] transitions = {{0.9, 0.1}, {0, 1}};
            behaviour.setTransitions(transitions);
            behaviours[0] = behaviour;
        }


        if (preset.equals(BEHAVIOUR_TOUCH_DRAG_FAR_RIGHT_TO_LEFT)) {

            behaviours = new ProbBehaviourTouch[1];

            ProbBehaviourTouch behaviour = new ProbBehaviourTouch(BEHAVIOUR_TOUCH_DRAG_FAR_RIGHT_TO_LEFT, 2);
            double[] prot_centre_mean = {(x - width * 1.5) / screenWidth, (y + height / 2) / screenHeight};
            double[][] prot_centre_mCov = {{Math.pow(width / screenWidth / 3, 2), 0}, {0, Math.pow(height / screenHeight / 5, 2)}};
            behaviour.setState(0, prot_centre_mean, prot_centre_mCov, 0);
            behaviour.setAcceptedPointerIDs(0);

            double[] prot_centre_mean2 = {(x) / screenWidth, (y + height / 2) / screenHeight};
            double[][] prot_centre_mCov2 = {{Math.pow(width / screenWidth / 5, 2), 0}, {0, Math.pow(height / screenHeight / 5, 2)}};
            behaviour.setState(1, prot_centre_mean2, prot_centre_mCov2, 1);
            behaviour.setAcceptedPointerIDs(0);

            double[][] transitions = {{1, 0}, {0.1, 0.9}};
            behaviour.setTransitions(transitions);
            behaviours[0] = behaviour;
        }


        if (preset.equals(BEHAVIOUR_TOUCH_SLIDE_ARC_BOTTOM_LEFT)) {

            behaviours = new ProbBehaviourTouch[1];

            ProbBehaviourTouch behaviour = new ProbBehaviourTouch(BEHAVIOUR_TOUCH_SLIDE_ARC_BOTTOM_LEFT, 2);
            double[] mean = {x / screenWidth, (y + height * 1.5f) / screenHeight};
            double[][] mCov = {{Math.pow(width / screenWidth / 8, 2), 0}, {0, Math.pow(height / screenHeight / 4, 2)}};
            behaviour.setState(0, mean, mCov, 0.5);

            double[] mean2 = {(x + width / 4) / screenWidth, (y + height * 2.5f) / screenHeight};
            double[][] mCov2 = {{Math.pow(width / screenWidth / 16, 2), 0}, {0, Math.pow(height / screenHeight / 2, 2)}};
            behaviour.setState(1, mean2, mCov2, 0.5);

            double[][] transitions = {{0.5, 0.5}, {0.5, 0.5}};
            behaviour.setTransitions(transitions);
            behaviour.setAcceptedPointerIDs(0);

            behaviours[0] = behaviour;
        }


        if (preset.equals(BEHAVIOUR_TOUCH_SLIDE_ARC_BOTTOM_RIGHT)) {

            behaviours = new ProbBehaviourTouch[1];

            ProbBehaviourTouch behaviour = new ProbBehaviourTouch(BEHAVIOUR_TOUCH_SLIDE_ARC_BOTTOM_RIGHT, 2);
            double[] mean = {(x + width) / screenWidth, (y + height * 1.5f) / screenHeight};
            double[][] mCov = {{Math.pow(width / screenWidth / 8, 2), 0}, {0, Math.pow(height / screenHeight / 4, 2)}};
            behaviour.setState(0, mean, mCov, 0.5);

            double[] mean2 = {(x + width * 3f / 4) / screenWidth, (y + height * 2.5f) / screenHeight};
            double[][] mCov2 = {{Math.pow(width / screenWidth / 16, 2), 0}, {0, Math.pow(height / screenHeight / 2, 2)}};
            behaviour.setState(1, mean2, mCov2, 0.5);

            double[][] transitions = {{0.5, 0.5}, {0.5, 0.5}};
            behaviour.setTransitions(transitions);
            behaviour.setAcceptedPointerIDs(0);

            behaviours[0] = behaviour;
        }


        if (preset.equals(BEHAVIOUR_TOUCH_SLIDE_ARC_TOP_LEFT)) {

            behaviours = new ProbBehaviourTouch[1];

            ProbBehaviourTouch behaviour = new ProbBehaviourTouch(BEHAVIOUR_TOUCH_SLIDE_ARC_TOP_LEFT, 2);
            double[] mean = {x / screenWidth, (y - height * 0.5f) / screenHeight};
            double[][] mCov = {{Math.pow(width / screenWidth / 8, 2), 0}, {0, Math.pow(height / screenHeight / 4, 2)}};
            behaviour.setState(0, mean, mCov, 0.5);

            double[] mean2 = {(x + width / 4) / screenWidth, (y - height * 1.5f) / screenHeight};
            double[][] mCov2 = {{Math.pow(width / screenWidth / 16, 2), 0}, {0, Math.pow(height / screenHeight / 2, 2)}};
            behaviour.setState(1, mean2, mCov2, 0.5);

            double[][] transitions = {{0.5, 0.5}, {0.5, 0.5}};
            behaviour.setTransitions(transitions);
            behaviour.setAcceptedPointerIDs(0);

            behaviours[0] = behaviour;
        }


        if (preset.equals(BEHAVIOUR_TOUCH_SLIDE_ARC_TOP_RIGHT)) {

            behaviours = new ProbBehaviourTouch[1];

            ProbBehaviourTouch behaviour = new ProbBehaviourTouch(BEHAVIOUR_TOUCH_SLIDE_ARC_TOP_RIGHT, 2);
            double[] mean = {(x + width) / screenWidth, (y - height * 0.5f) / screenHeight};
            double[][] mCov = {{Math.pow(width / screenWidth / 8, 2), 0}, {0, Math.pow(height / screenHeight / 4, 2)}};
            behaviour.setState(0, mean, mCov, 0.5);

            double[] mean2 = {(x + width * 3f / 4) / screenWidth, (y - height * 1.5f) / screenHeight};
            double[][] mCov2 = {{Math.pow(width / screenWidth / 16, 2), 0}, {0, Math.pow(height / screenHeight / 2, 2)}};
            behaviour.setState(1, mean2, mCov2, 0.5);

            double[][] transitions = {{0.5, 0.5}, {0.5, 0.5}};
            behaviour.setTransitions(transitions);
            behaviour.setAcceptedPointerIDs(0);

            behaviours[0] = behaviour;
        }


        return behaviours;
    }

}
