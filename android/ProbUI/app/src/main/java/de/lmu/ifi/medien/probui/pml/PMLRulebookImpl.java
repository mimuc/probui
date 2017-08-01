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

import android.util.Log;

import java.util.HashMap;
import java.util.Map;

import de.lmu.ifi.medien.probui.behaviours.ProbBehaviour;
import de.lmu.ifi.medien.probui.behaviours.ProbBehaviourTouch;
import de.lmu.ifi.medien.probui.pml.rules.PMLRule;
import de.lmu.ifi.medien.probui.pml.rules.PMLRuleBehaviour;
import de.lmu.ifi.medien.probui.pml.rules.PMLRuleSet;

public class PMLRulebookImpl implements PMLRulebook {


    private PMLRuleSet ruleset;

    private Map<String, ProbBehaviour> behaviourMap;



    /**
     * Example:
     * swipe_left: C->W
     * swipe_right: C->E
     * zoom: swipe_left & swipe_right
     */


    public PMLRulebookImpl() {

        this.ruleset = new PMLRuleSet();
        this.behaviourMap = new HashMap<String, ProbBehaviour>();
    }

    @Override
    public void addBehaviour(ProbBehaviourTouch behaviour) {

        this.behaviourMap.put(behaviour.getLabel(), behaviour);
        if(behaviour.getListenerForPML() != null){
            this.addRule(behaviour.getLabel() + "_notification_dummy: " + behaviour.getLabel() + " is complete", null);
        }
    }

    @Override
    public void reset() {
        for(PMLRule rule : this.ruleset.getRules()){
            rule.reset();
        }
    }


    @Override
    public void addRule(String pmlStatement, PMLRuleListener listener) {

        PMLRuleParser parser = new PMLRuleParserImpl(this.ruleset, this.behaviourMap);
        parser.parse(pmlStatement);
        if (listener != null)
            this.ruleset.getRule(parser.getRuleLabel()).addListener(listener);
    }



    @Override
    public boolean evaluate(String label) {

        return this.ruleset.getRule(label).check();
    }

    @Override
    public void update() {

        Log.d("Prob PML RULEBOOK", "rulebook update!");

        // reset all "already checked in this update" states:
        for (PMLRule rule : this.ruleset.getRules()) {
            rule.checkedThisUpdate = false;
        }
        // check all rules:
        for (PMLRule rule : this.ruleset.getRules()) {
            rule.check();
        }
    }


    @Override
    public PMLRulePatternResult getLastResultForBehaviourRule(String label) {
        return ((PMLRuleBehaviour) this.ruleset.getRule(label)).lastResult;
    }


}
