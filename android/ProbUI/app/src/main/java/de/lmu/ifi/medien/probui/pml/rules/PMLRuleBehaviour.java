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

package de.lmu.ifi.medien.probui.pml.rules;

import android.util.Log;

import de.lmu.ifi.medien.probui.behaviours.ProbBehaviour;
import de.lmu.ifi.medien.probui.pml.PMLRulePatternChecker;
import de.lmu.ifi.medien.probui.pml.PMLRulePatternResult;


public abstract class PMLRuleBehaviour extends PMLRule {

    public ProbBehaviour behaviour;
    public PMLRulePatternResult lastResult;

    public PMLRuleBehaviour(ProbBehaviour behaviour, String label) {
        super(label==null?behaviour.getLabel():label);
        this.behaviour = behaviour;
    }


    public boolean checkSpecific() {
        boolean wasMostLikelyBehaviourBefore =
                this.lastResult != null && this.lastResult.isMostLikelyBehaviour();

        Log.d("PML RULE", "PMLRuleBehaviour -> checkSpecific: " + this.label + " #########################");
        this.lastResult = PMLRulePatternChecker.checkRulePattern(
                this.behaviour.getMostLikelyStateSequence(this.behaviour.getMaxProbPID()), //TODO: should it always use the max PID?
                this.behaviour.getEventTypes(this.behaviour.getMaxProbPID()),
                this.behaviour.getSequenceRule(),
                this.lastResult);
        Log.d("PML RULE", "#################################################################");


        if (this.behaviour.getListenerForPML() != null)
            this.behaviour.getListenerForPML().onBehaviourUpdate(this.behaviour);


        if (this.lastResult == null)
            return false;

        this.lastResult.setIsMostLikelyBehaviour(this.behaviour.isMostLikelyBehaviour());
        this.lastResult.setHasJustBecomeMostLikelyBehaviour(
                this.behaviour.isMostLikelyBehaviour() && !wasMostLikelyBehaviourBefore);


        return this.checkBehaviourRuleSpecific();
    }

    protected abstract boolean checkBehaviourRuleSpecific();


    public void reset() {
        this.lastResult = null;
    }


}

