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

import de.lmu.ifi.medien.probui.gui.ProbInteractor;
import de.lmu.ifi.medien.probui.pml.PMLBehaviourListener;
import de.lmu.ifi.medien.probui.pml.PMLParserTouch;
import de.lmu.ifi.medien.probui.pml.PMLParserTouchImpl;
import de.lmu.ifi.medien.probui.system.ProbUIManager;

/**
 * "Injects" ProbBehaviours into ProbInteractors.
 */
public class ProbBehaviourLinker {


    /**
     * Adds the touch behaviour given as a PML statement to the given interactor.
     *  @param interactor
     * @param pmlStatement
     * @param addToExistingBehaviours If true, behaviours attached to the interactor are kept;
     * @param listener
     */
    public static ProbBehaviourTouch linkProbBehaviourTouch(ProbInteractor interactor, String pmlStatement, boolean addToExistingBehaviours, PMLBehaviourListener listener, float ddensity) {

        // Delete all existing behaviours:
        if (!addToExistingBehaviours) {
            interactor.getCore().clearBehaviours();
        }

        // Create new behaviour(s) according to the preset:
        PMLParserTouch parser = new PMLParserTouchImpl(ddensity);
        ProbBehaviourTouch behaviour = parser.parse(pmlStatement,
                interactor.getView().getX(), interactor.getView().getY(),
                interactor.getView().getWidth(), interactor.getView().getHeight(),
                interactor.getCore().getSurfaceWidth(), interactor.getCore().getSurfaceHeight());

        // Add the listener if available:
        if(listener != null){
            behaviour.setListenerForPML(listener);
        }

        // Add the behaviour:
        interactor.getCore().addBehaviour(behaviour);

        return behaviour;

    }


    /**
     * Adds the given preset touch behaviour to the given interactor.
     *
     * @param manager
     * @param interactor
     * @param preset
     * @param addToExistingBehaviours If true, behaviours attached to the interactor are kept;
     *                                if false, all existing behaviour are removed before linking.
     */
    public static void linkPresetProbBehaviourTouch(ProbUIManager manager, ProbInteractor interactor, String preset, boolean addToExistingBehaviours, float ddensity) {

        // Delete all existing behaviours:
        if (!addToExistingBehaviours) {
            interactor.getCore().clearBehaviours();
        }


        // Create new behaviour(s) according to the preset:
        ProbBehaviourTouch[] behaviours = ProbBehaviourFactory.createPresetProbBehaviourTouch(
                preset,
                interactor.getView().getX(), interactor.getView().getY(),
                interactor.getView().getWidth(), interactor.getView().getHeight(),
                manager.getSurfaceWidth(), manager.getSurfaceHeight(), ddensity);

        // Set the behaviour(s):
        for (ProbBehaviourTouch behaviour : behaviours)
            interactor.getCore().addBehaviour(behaviour);

        // Finalise behaviour setup (parameter: null means set a uniform prior):
        interactor.getCore().finaliseBehaviourSetup(null);
        //TODO: maybe add prior as parameter to this method to be passed on here instead of null?
    }


}
