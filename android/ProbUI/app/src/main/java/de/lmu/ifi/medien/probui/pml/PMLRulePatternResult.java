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


public class PMLRulePatternResult {

    private boolean sequenceBroken; // was it broken at least once?
    private boolean allCovered; // is it covered?
    private boolean allCoveredOnce; // was it covered at least once?
    private int finalState; // state at the end of the sequence that was analysed with a rule pattern
    private boolean justCoveredNewState; // is it new in its final (covered) state since the last result?
    private boolean endsInEndState; // is the final state an end state?
    private boolean hasBeenCompleted; // had a previous result included a "has just been completed"?

    private boolean isMostLikelyBehaviour; // is currently the most likely behaviour of its interactor
    private boolean hasJustBecomeMostLikelyBehaviour; // is currently the most likely behaviour of its interactor but was not at the last rule check

    private int[] cover;


    public boolean isSequenceBroken() {
        return sequenceBroken;
    }

    public void setSequenceBroken(boolean sequenceBroken) {
        this.sequenceBroken = sequenceBroken;
    }

    public boolean isAllCovered() {
        return allCovered;
    }

    public void setAllCovered(boolean allCovered) {
        this.allCovered = allCovered;
    }

    public boolean isAllCoveredOnce() {
        return allCoveredOnce;
    }

    public void setAllCoveredOnce(boolean allCoveredOnce) {
        this.allCoveredOnce = allCoveredOnce;
    }

    public int getFinalState() {
        return finalState;
    }

    public void setFinalState(int finalState) {
        this.finalState = finalState;
    }

    public void setJustCoveredNewState(boolean justArrivedInNewState) {
        this.justCoveredNewState = justArrivedInNewState;
    }

    public void setEndsInEndState(boolean endsInEndState) {
        this.endsInEndState = endsInEndState;
    }

    public boolean isJustCoveredNewState() {
        return justCoveredNewState;
    }

    public boolean isEndsInEndState() {
        return endsInEndState;
    }


    public boolean hasJustBeenCompleted() {

        return this.isJustCoveredNewState() // "just"
                && this.isAllCovered() // "completely"
                && this.isEndsInEndState(); // "ended"
    }

    public boolean hasBeenCompleted() {
        return hasBeenCompleted;
    }

    public void setHasBeenCompleted(boolean hasBeenCompletedOnce) {
        this.hasBeenCompleted = hasBeenCompletedOnce;
    }

    public int[] getCover() {
        return cover;
    }

    public void setCover(int[] cover) {
        this.cover = cover;
    }

    public boolean isMostLikelyBehaviour() {
        return isMostLikelyBehaviour;
    }

    public void setIsMostLikelyBehaviour(boolean isMostLikelyBehaviour) {
        this.isMostLikelyBehaviour = isMostLikelyBehaviour;
    }

    public void setHasJustBecomeMostLikelyBehaviour(boolean hasJustBecomeMostLikelyBehaviour) {
        this.hasJustBecomeMostLikelyBehaviour = hasJustBecomeMostLikelyBehaviour;
    }

    public boolean hasJustBecomeMostLikelyBehaviour() {
        return hasJustBecomeMostLikelyBehaviour;
    }
}
