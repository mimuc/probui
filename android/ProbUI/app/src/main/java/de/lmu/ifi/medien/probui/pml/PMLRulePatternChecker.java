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

import java.util.List;

import de.lmu.ifi.medien.probui.observations.ProbObservationTouch;
import de.lmu.ifi.medien.probui.pml.notifications.NotificationMarkerStateReached;
import de.lmu.ifi.medien.probui.pml.notifications.NotificationMarkerTouchEventReached;


public class PMLRulePatternChecker {


    public static PMLRulePatternResult checkRulePattern(
            int[] stateSequence, int[] stateSequenceTypes,
            PMLRulePattern sequenceRule, PMLRulePatternResult lastResult) {


        if (stateSequence == null)
            return null;


        String debug_str = "";
        for (int i = 0; i < stateSequence.length; i++) {
            debug_str += stateSequence[i] + ", ";
        }
        Log.d("PML RULEBOOK", "stateSequence in PMLRulePatternChecker.checkRulePattern: " + debug_str);

        debug_str = "";
        for (int i = 0; i < sequenceRule.pis.length; i++) {
            debug_str += sequenceRule.pis[i] + ", ";
        }
        Log.d("PML RULEBOOK", "sequenceRule.pis.length in PMLRulePatternChecker.checkRulePattern: " + debug_str);


        PMLRulePatternResult result = new PMLRulePatternResult();

        int[] cover = new int[sequenceRule.pis.length]; // used to check if all states covered
        int[] reachedCover = new int[sequenceRule.pis.length];

        int currentState = -1;
        //int currentStateTouchDownEvents = 0;
        //int currentStateTouchUpEvents = 0;
        int stateStartPointer = 0;
        int stateEndPointer = -1;
        int nextState = -1;
        for (int i = 0; i < stateSequence.length; i++) {

            nextState = stateSequence[i];

            reachedCover[nextState]++;
            // ^ we reached this state -> so it is covered in the reachedCover
            // (but not in the normal cover which also requires matching the state's touch pattern)

            // If we have reached a new state:
            boolean touchEventPatternOK = false;
            if (i >= 1 && currentState != nextState) {
                stateEndPointer = i;
                // Check if the touch event pattern of the previous state is ok:
                touchEventPatternOK = checkTouchEventPattern(stateStartPointer, stateEndPointer,
                        stateSequenceTypes, sequenceRule.getTouchEventTokens(currentState),
                        sequenceRule.getTouchEventReachedMarkers(currentState), false /*isFinalStateInSequence*/);
                stateStartPointer = i; // Set the new start pointer for the next check.
            }
            // If required events fulfilled for state -> count state for cover:
            if (touchEventPatternOK) {
                cover[currentState]++;
            }


            // if first state already breaks the sequence:
            /*if (i == 0 && sequenceRule.pis[nextState] == 0) {
                //Log.d("PML RULEBOOK", "started in state with pi=0 -->  result.setSequenceBroken(true)");
                result.setSequenceBroken(true);
            }*/

            // if valid transition:
            if (i > 0 && sequenceRule.mT[currentState][nextState] > 0) {
                if (checkCovered(cover)) { // if this valid move makes it covered:
                    result.setAllCoveredOnce(true);
                    result.setAllCovered(true);
                }
            }
            // else - invalid transition:
            else if (i > 0 || i == 0 && sequenceRule.pis[nextState] == 0) {
                Log.d("PML RULEBOOK", "invalid transition -->  result.setSequenceBroken(true); result.setAllCovered(false)");
                result.setSequenceBroken(true);
                result.setAllCovered(false); // if the move broke the pattern, then it cannot be currently covered
                resetCover(cover); // and so we have to reset the current cover state
                resetCover(reachedCover);
            }

            currentState = nextState; // move along
        }

        // Check touch event pattern for final state:
        boolean touchEventPatternOK = checkTouchEventPattern(stateStartPointer, stateSequence.length,
                stateSequenceTypes, sequenceRule.getTouchEventTokens(currentState),
                sequenceRule.getTouchEventReachedMarkers(currentState), true /* isFinalStateInSequence */);
        //Log.d("PML RULEBOOK", "checkRulePattern --> final touchEventPatternOK: " + touchEventPatternOK);
        if (touchEventPatternOK) {
            cover[currentState]++;
        }
        if (checkCovered(cover)) {
            result.setAllCoveredOnce(true);
            result.setAllCovered(true);
        }

        // set the final cover:
        result.setCover(cover);

        // set the last (i.e. "current") state:
        result.setFinalState(stateSequence[stateSequence.length - 1]); //could also use currentState here

        // special case - first observation:
        if (stateSequence.length == 1) {
            // first observation is a potential new cover:
            result.setJustCoveredNewState(result.getCover()[result.getFinalState()] == 1);
        }

        // special case - just one state:
        if (sequenceRule.pis.length == 1) {
            // one state is potentially covered:
            result.setAllCoveredOnce(result.getCover()[result.getFinalState()] == 1);
            result.setAllCovered(result.getCover()[result.getFinalState()] == 1);
        }

        // set if in endstate:
        result.setEndsInEndState(sequenceRule.ends[result.getFinalState()] > 0);

        // history stuff:
        if (lastResult != null) {

            //if (result.getFinalState() != lastResult.getFinalState()) {
            if (result.getCover()[result.getFinalState()] == 1
                    && lastResult.getCover()[result.getFinalState()] == 0) {
                result.setJustCoveredNewState(true);
            }
            if (lastResult.hasJustBeenCompleted() || lastResult.hasBeenCompleted()) { // if has been completed or if had been completed before
                result.setHasBeenCompleted(result.isEndsInEndState()); // ... and if it is (still) in an end state -> then it still counts as having been completed (in the past)
            }
        }

        if(result.hasJustBeenCompleted())
            result.setHasBeenCompleted(true);


        // Notification stuff:
        // Set state reached notifications:
        for (NotificationMarkerStateReached stateReachedMarker : sequenceRule.getStateReachedMarkers()) {
            if (reachedCover[stateReachedMarker.getStateIndex()] > 0)
                stateReachedMarker.setReached(true);
            else
                stateReachedMarker.setReached(false);

            Log.d("PML RULEBOOK", "checkRulePattern --> stateReachedMarker.getStateIndex() == result.getFinalState(): "
                    + stateReachedMarker.getStateIndex() + " ==? " + result.getFinalState());
            if (stateReachedMarker.getStateIndex() == result.getFinalState() // is marker for currently final state
                    && reachedCover[stateReachedMarker.getStateIndex()] > 0 // this state is reached
                    && (lastResult == null || lastResult.getFinalState() != result.getFinalState())) // it's the first time -> "just" reached
                stateReachedMarker.setJustReached(true);
            else
                stateReachedMarker.setJustReached(false);
        }


        String cover_str = "";
        String reached_cover_str = "";
        for (int i = 0; i < cover.length; i++) {
            cover_str += cover[i] + ", ";
            reached_cover_str += reachedCover[i] + ", ";
        }
        Log.d("PML RULEBOOK", "checkRulePattern --> final cover array: " + cover_str);
        Log.d("PML RULEBOOK", "checkRulePattern --> final reached_cover array: " + reached_cover_str);
        Log.d("PML RULEBOOK", "checkRulePattern --> result.isJustCoveredNewState: " + result.isJustCoveredNewState());
        Log.d("PML RULEBOOK", "checkRulePattern --> result.isAllCovered: " + result.isAllCovered());
        Log.d("PML RULEBOOK", "checkRulePattern --> result.isEndsInEndState: " + result.isEndsInEndState());

        return result;
    }


    private static boolean checkTouchEventPattern(int stateStartPointer, int stateEndPointer,
                                                  int[] stateSequenceTypes,
                                                  List<PMLRulePattern.TouchEventToken> touchEventTokens,
                                                  List<NotificationMarkerTouchEventReached> touchEventMarkers,
                                                  boolean isFinalStateInSequence) {


        // If no tokens are specified we're happy with any touch event pattern:
        if (touchEventTokens == null || touchEventTokens.size() == 0)
            return true;

        if (stateSequenceTypes == null || stateSequenceTypes.length == 0)
            return false;

        /*
        Log.d("PML RULEBOOK", "checkTouchEventPattern --> stateStartPointer: "
                + stateStartPointer + ", stateEndPointer: " + stateEndPointer
                + ", touchEventTokens.size: " + touchEventTokens.size());
        */

        String debug_str = "";
        for (int i = 0; i < stateSequenceTypes.length; i++) {
            debug_str += stateSequenceTypes[i] + ", ";
        }
        Log.d("Prob PML RULEBOOK", "checkTouchEventPattern --> seq types: " + debug_str);


        int[] matches = new int[touchEventTokens.size()];
        int tokenIndex = 0;
        PMLRulePattern.TouchEventToken token = null;
        int seqIdx = stateStartPointer;
        int seqi = -1;
        boolean currentMinOneSatisfied = false;

        // Check for the given pattern (i.e. given by the tokens)
        // within the given part of the state sequence:
        while (seqIdx < stateEndPointer && tokenIndex < touchEventTokens.size()) {

            seqi = stateSequenceTypes[seqIdx];


            token = touchEventTokens.get(tokenIndex);

            // If we have no move tokens at the moment, we may skip move events:
            // This ensures that tiny movements do not have to be considered.
            if (seqi == ProbObservationTouch.TYPE_TOUCH_MOVE && token.type != ProbObservationTouch.TYPE_TOUCH_MOVE) {
                seqIdx++;
                //Log.d("PML RULEBOOK", "checkTouchEventPattern --> skipped touch move event");
                continue;
            }




            // If it's a match -> consume it:
            if (token.type == seqi) {
                matches[tokenIndex]++;
                seqIdx++;
                // If we used a normal token for the match, we need to move on:
                if (token.modifier == PMLRulePattern.TOUCH_EVENT_TOKEN_MODIFIER_NORMAL) {
                    tokenIndex++;
                    currentMinOneSatisfied = false;
                }
            }
            // Else: no match, try to move on
            else {
                // If we have a normal token and failed to match, we failed to match the pattern:
                if (token.modifier == PMLRulePattern.TOUCH_EVENT_TOKEN_MODIFIER_NORMAL) {
                    //Log.d("PML RULEBOOK", "checkTouchEventPattern --> failed to match normal token: token.type=" + token.type + ", seqi=" + seqi);
                    updateTouchEventMarkers(touchEventMarkers, matches, isFinalStateInSequence);
                    return false;
                }
                // If we have a "min one" token, we need to check further:
                else if (token.modifier == PMLRulePattern.TOUCH_EVENT_TOKEN_MODIFIER_MIN_ONE) {
                    // If the "min one" token has already been satisfied
                    // we are allowed to try to match with the next token:
                    if (currentMinOneSatisfied) {
                        tokenIndex++;
                        currentMinOneSatisfied = false;
                    }
                    // If we failed to match and the "min one" token has not yet been satisfied
                    // then that means that we failed to match the pattern:
                    else {
                        //Log.d("PML RULEBOOK", "checkTouchEventPattern --> failed to match min one token");
                        updateTouchEventMarkers(touchEventMarkers, matches, isFinalStateInSequence);
                        return false;
                    }
                }
                // If we have a "zero or more" token, we are free to try to match with the next token
                // without any further checks:
                else if (token.modifier == PMLRulePattern.TOUCH_EVENT_TOKEN_MODIFIER_ZERO_OR_MORE) {
                    tokenIndex++;
                    currentMinOneSatisfied = false;
                }
            }
        }


        // Check if we matched all tokens that need matching. Without this check, we would return
        // true for matching affixes, not necessarily the whole pattern.
        for (int i = 0; i < matches.length; i++) {
            // If not matched, but a required token -> we failed to match the whole pattern:
            if (matches[i] == 0 && touchEventTokens.get(i).modifier != PMLRulePattern.TOUCH_EVENT_TOKEN_MODIFIER_ZERO_OR_MORE) {
                //Log.d("PML RULEBOOK", "checkTouchEventPattern --> failed to match all necessary tokens");
                updateTouchEventMarkers(touchEventMarkers, matches, isFinalStateInSequence);
                return false;
            }
        }


        // If we've reached this point then that means we have matched the whole pattern:
        updateTouchEventMarkers(touchEventMarkers, matches, isFinalStateInSequence);
        return true;
    }


    private static void updateTouchEventMarkers(List<NotificationMarkerTouchEventReached> touchEventMarkers, int[] matches, boolean isFinalStateInSequence) {

        if (touchEventMarkers == null)
            return;


        for (NotificationMarkerTouchEventReached touchEventMarker : touchEventMarkers) {
            if (matches[touchEventMarker.getTouchEventIndex()] > 0)
                touchEventMarker.setReached(true);
            else
                touchEventMarker.setReached(false);

            // TODO: not quite correct -> will trigger repeatedly if fulfilled, not just once:
            if (isFinalStateInSequence && matches[touchEventMarker.getTouchEventIndex()] > 0)
                touchEventMarker.setJustReached(true);
            else
                touchEventMarker.setJustReached(false);
        }
    }


    private static boolean checkCovered(int[] cover) {
        for (int i = 0; i < cover.length; i++) {
            if (cover[i] == 0)
                return false;
        }
        return true;
    }

    private static void resetCover(int[] cover) {
        for (int i = 0; i < cover.length; i++) {
            cover[i] = 0;
        }
    }
}
