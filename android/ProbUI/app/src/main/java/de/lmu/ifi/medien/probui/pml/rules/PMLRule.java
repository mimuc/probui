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

import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.medien.probui.pml.PMLRuleListener;


public abstract class PMLRule {


    public String label;
    public boolean checkedThisUpdate;
    public boolean currentCheckResult;
    private List<PMLRuleListener> listeners;
    public int subsequentChecksTrue;
    private int subsequentChecksFalse;



    public PMLRule(String label) {
        this.label = label;
        this.listeners = new ArrayList<PMLRuleListener>();

    }

    public abstract boolean checkSpecific();

    public boolean check() {
        if (!this.checkedThisUpdate) {

            this.onFirstCheckSpecific();

            // 1. Check according to implemented rule:
            boolean checkedNow = this.checkSpecific();

            // 2. Update number of subsequent positive check results:
            if (checkedNow && this.currentCheckResult)
                this.subsequentChecksTrue++;
            else
                this.subsequentChecksTrue = 0;

            // 3. Update number of subsequent negative check results:
            if (!checkedNow && !this.currentCheckResult)
                this.subsequentChecksFalse++;
            else
                this.subsequentChecksFalse = 0;

            // 4. Store result as current one:
            this.currentCheckResult = checkedNow;

            // 5. Flag that this rule has been checked in the current update:
            this.checkedThisUpdate = true;

            // 6. Notify listeners:
            this.notifyRuleListeners();
        }
        return this.currentCheckResult;
    }

    protected void onFirstCheckSpecific(){

    }

    public void addListener(PMLRuleListener listener) {
        this.listeners.add(listener);
    }

    public void notifyRuleListeners() {
        for (PMLRuleListener l : this.listeners) {
            if (this.currentCheckResult) {
                l.onRuleSatisfied(this.label, this.subsequentChecksTrue);
            }
            else {
                //l.onRuleDissatisfied(this.label, this.subsequentChecksFalse);
            }
        }
    }

    public void reset() {

    }
}