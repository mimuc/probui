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

import android.graphics.Canvas;

import de.lmu.ifi.medien.probui.exceptions.WrongObservationDelegationException;
import de.lmu.ifi.medien.probui.observations.ProbObservation;
import de.lmu.ifi.medien.probui.pml.PMLBehaviourListener;
import de.lmu.ifi.medien.probui.pml.PMLRulePattern;
import de.lmu.ifi.medien.probui.pml.notifications.NotificationProvider;

/**
 * Interface for a "bounding behaviour".
 */
public interface ProbBehaviour extends NotificationProvider {

    /**
     * Update the behaviour with the given observation.
     *
     * @param obs
     * @throws WrongObservationDelegationException
     */
    public void observe(ProbObservation obs) throws WrongObservationDelegationException;


    /**
     * Reset the observations and probability estimate of this behaviour.
     */
    public void reset();


    /**
     * Get the running (i.e. current) log-probability of this behaviour.
     *
     * @return
     */
    public double getRunningProbLn(int pID);

    public double getRunningProbLn();

    public void setProbLn(double behaviourPosteriorProb);
    public double getProbLn();
    public double getProb();

    /**
     * Get the currently most likely state for the sequence of the given pointer.
     *
     * @param pointerID
     * @return
     */
    public int getMostLikelyState(int pointerID);

    public int[] getMostLikelyStateSequence(int pointerID);

    /**
     * Returns the event types of the current observations (e.g. touch down, up, ...)
     * for the given pointer ID.
     *
     * @param pointerID
     * @return
     */
    int[] getEventTypes(int pointerID);

    /**
     * Get the number of observations evaluated by this behaviour.
     *
     * @return
     */
    public int getNumObservations();


    /**
     * Get the label assigned to this behaviour (only for debug purposes).
     *
     * @return
     */
    public String getLabel();

    /**
     * Returns the pointer ID of the pointer with the highest running ln prob at the last update.
     * Useful for multitouch stuff.
     *
     * @return
     */
    public int getMaxProbPID();


    /**
     * Visualises this behavioural pattern.
     * Intended for debugging only, not meant to be shown to the user.
     *
     * @param canvas
     * @param translate_x
     * @param translate_y
     * @param screen_x
     * @param screen_y
     */
    public void drawDebug(Canvas canvas, float translate_x, float translate_y, float screen_x, float screen_y);


    /**
     * Sets the sequence rule associated with this behaviour. This is usually only called if the
     * behaviour was created via PML. The sequene rule is used by the system (Rulebook)
     * to determine whether the behaviour is performed "completely".
     *
     * @param sequenceRule The sequence rule to associate with this behaviour.
     */
    public void setSequenceRule(PMLRulePattern sequenceRule);

    /**
     * Returns the sequence rule associated with this behaviour. This is usually only available if the
     * behaviour was created via PML.
     *
     * @return sequenceRule The sequence rule associated with this behaviour.
     */
    public PMLRulePattern getSequenceRule();

    /**
     * Sets this behaviour to be the most likely behaviour of its interactor (or not).
     * This is called during the reasoning process - don't call it yourself.
     *
     * @param isMostLikely
     */
    void setMostLikelyBehaviour(boolean isMostLikely);

    /**
     * Returns whether this behaviour is the most likely behaviour of its current interactor.
     * This is updated automatically during the reasoning process.
     *
     * @return
     */
    boolean isMostLikelyBehaviour();


    /**
     * Return the time difference in ms that has passed since the start
     * of the current observation/reasoning process.
     *
     * @return
     */
    long getTimeTaken();


    PMLBehaviourListener getListenerForPML();

    void move(float dx, float dy);

    void setMaxObservations(int maxObservations);
}
