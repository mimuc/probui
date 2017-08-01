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

import de.lmu.ifi.medien.probui.pml.PMLTokens;


public class PMLRuleQualifierTimeTaken extends PMLRuleBehaviourQualifier {

    protected long timeMin;
    protected long timeMax;


    public PMLRuleQualifierTimeTaken(String label, PMLRuleBehaviour rule, long timeMin, long timeMax, String unit) {
        super(label, rule);
        this.timeMin = timeMin;
        this.timeMax = timeMax;
        if (unit.equals(PMLTokens.RULE_QUALIFIER_UNIT_SECONDS)) {
            this.timeMin *= 1000; // since we want to work with milliseconds
            this.timeMax *= 1000;
        }
        //Log.d("PML RULEBOOK TIMETAKEN", this.timeMin + " - " + this.timeMax);
    }


    @Override
    protected boolean checkBehaviourRuleSpecific() {
        return this.rule.check()
                && this.rule.behaviour.getTimeTaken() > this.timeMin
                && (this.timeMax < 0 || this.rule.behaviour.getTimeTaken() <= this.timeMax); //this.timeMax < 0 since we set it to -1 if not used
        //TODO: is it ok with the behaviour time here, instead of the time since touch down?
        // the two times might be different, since we only consider the last X touch events...
        // if we want the time from the core (i.e. time since last touch down),
        // the core could "inject" it into its behaviours in its observe method.
    }
}
