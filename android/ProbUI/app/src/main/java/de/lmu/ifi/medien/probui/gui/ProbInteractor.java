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

package de.lmu.ifi.medien.probui.gui;

import android.graphics.Canvas;
import android.view.View;

import de.lmu.ifi.medien.probui.observations.ProbObservation;
import de.lmu.ifi.medien.probui.observations.ProbObservationTouch;
import de.lmu.ifi.medien.probui.system.ProbUIManager;

public interface ProbInteractor {

    public ProbInteractorCore getCore();

    public View getView();

    public void drawSpecific(Canvas canvas);

    void onTouchDown(ProbObservationTouch obs);

    void onTouchMove(ProbObservationTouch obs);

    void onTouchUp(ProbObservationTouch obs, int numRemainingPointers);

    void onTouchDownPost(ProbObservationTouch obs);

    void onTouchMovePost(ProbObservationTouch obs);

    void onTouchUpPost(ProbObservationTouch obs, int numRemainingPointers);

    /**
     * Returns a set of default behaviour presets (as preset codes). The ProbUI Manager creates these
     * behaviours automatically and links them to the interactor when setting up the probInteractors.
     * If you don't need default behaviours, for example, since you're setting up everything manually
     * in the onProbSetup method, then you can safely return an empty string array or null here.
     *
     * @return
     */
    String[] getDefaultBehaviours();

    /**
     * Called when the core has just handled a new observation.
     * Allows probInteractors to react to sth. just after handling new observations in the core.
     * If you have no good reason to react to something here, you can safely leave this method empty.
     *
     * @param obs The new observation.
     */
    void onCoreObserve(ProbObservation obs);

    /**
     * Called when the core has just finalised its behaviour setup.
     * Allows probInteractors to reacht to sth. just after finalising the behaviour setup.
     * If you have no good reason to react to something here, you can safely leave this method empty.
     */
    void onCoreFinaliseBehaviourSetup();

    /**
     * This is the #1 method you want to implement in your custom interactor!
     * Put your behaviour and rule setup code in here.
     * Called when the ProbUI Manager has finished layouting and is setting up the probInteractors.
     */
    void onProbSetup();

    /**
     * Called by the ProbUI Mediator when this interactor is no longer considered in the current
     * reasoning process. Typically, this method should handle removing intermediate feedback.
     */
    void onExclude();

    /**
     * Called by the ProbUI Mediator when this interactor is no longer considered in the current
     * reasoning process as a result of the interactor itself having indicated so.
     * Typically, this method should handle removing intermediate feedback.
     */
    void onSelfExclude();

    /**
     * Called when this interactor was determined by the ProbUI Mediator. Typically, this method
     * should perform the default action of the interactor
     * (e.g. for a button call the normal "on click" stuff from here).
     */
    void onDetermined();


}
