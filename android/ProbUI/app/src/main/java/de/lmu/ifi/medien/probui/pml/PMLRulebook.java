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

import java.util.List;

import de.lmu.ifi.medien.probui.behaviours.ProbBehaviour;
import de.lmu.ifi.medien.probui.behaviours.ProbBehaviourTouch;

/**
 * The rulebook stores and evaluates a set of rules.
 */
public interface PMLRulebook {


    /**
     * Add a rule to this rulebook.
     *
     * @param pmlStatement  The rule in PML.
     */
    void addRule(String pmlStatement, PMLRuleListener listener);

    /**
     * Add a behaviour rule to this rulebook. The behaviour MUST have a sequenceRule attached to it,
     * i.e. it should be a behaviour created via PML.
     *
     * @param behaviour
     */
    /*void addBehaviourRule(ProbBehaviour behaviour);*/

    /**
     * Checks the given rule using the behaviours (and thus their current observation status) associated with this rulebook.
     *
     * @param label The label of the rule to evaluate.
     * @return Is the rule fulfilled?
     */
    public boolean evaluate(String label);

    /**
     * Updates the rulebook, using the behaviours (and thus their current observation status) associated with this rulebook.
     * This is called implicitly by the core when new observations come in. If using ProbUI with its normal manager and core elements,
     * there is no need to call this method yourself.
     */
    public void update();

    /**
     * Returns the result object from the rule with the given label.
     *
     * @param label The label of the rule from which to get the last result object.
     * @return The lat result object from the rule with the given label.
     */
    public PMLRulePatternResult getLastResultForBehaviourRule(String label);

    /**
     * Adds the given behaviour to the behaviour set relevant for the rules in this rulebook.
     * @param behaviour
     */
    void addBehaviour(ProbBehaviourTouch behaviour);

    /**
     * Should be called when the rules should forget about their last states, if they have one.
     */
    void reset();
}
