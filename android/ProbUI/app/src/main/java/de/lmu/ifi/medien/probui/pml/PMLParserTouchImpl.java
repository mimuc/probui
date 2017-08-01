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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.lmu.ifi.medien.probui.behaviours.ProbBehaviourTouch;
import de.lmu.ifi.medien.probui.observations.ProbObservationTouch;

/**
 * A parser for creating touch behaviours from statements written in PML (ProbUI Modelling Language).
 */
public class PMLParserTouchImpl implements PMLParserTouch {


    public static final double LAPLACE_CORRECTION_FACTOR = 0.001;


    private String currentToken;
    private String lastToken;


    private String currentStateName;
    private String behaviourLabel;

    private int stateIndex;

    private double stateLocationX;
    private double stateLocationY;
    private double stateWidth;
    private double stateHeight;
    private double screenWidth;
    private double screenHeight;

    /**
     * x-value of the top left corner of the rectangular bounding box of the GUI element's visuals.
     */
    private double interactorX;

    /**
     * y-value of the top left corner of the rectangular bounding box of the GUI element's visuals.
     */
    private double interactorY;

    /**
     * Width of the rectangular bounding box of the GUI element's visuals.
     */
    private double interactorW;

    /**
     * Height of the rectangular bounding box of the GUI element's visuals.
     */
    private double interactorH;

    /**
     * Map that stores parsed states by their name.
     */
    private Map<String, ParsedState> statesMap;

    /**
     * List that stores parsed states in order of parsing.
     */
    private List<ParsedState> statesList;

    /**
     * List that stores parsed state transitions in order of parsing.
     */
    private List<ParsedTransition> transitions;

    /**
     * Pointer to the last parsed state.
     */
    private ParsedState lastParsedState;

    /**
     * Pointer to the last parsed transition.
     */
    private ParsedTransition lastParsedTransition;

    /**
     * List that stores the parsed parameters (for the currently parsed state) in order of parsing.
     */
    private List<ParsedParam> stateParams;

    /**
     * List that stores the parsed "required touch events" (e.g. down, up)
     * for the currently parsed state in order of parsing.
     */
    private List<ParsedRequiredTouchEvent> requiredTouchEvents;

    /**
     * Flag that indicates if the parser is currently inside a state's details
     * (e.g. for "C[s=2]", the part between "[" and "]" is the details part).
     */
    private boolean inStateDetailMode;

    /**
     * Flag that indicates if the parser has already passed the label
     * (e.g. for "mylabel: N->S", this is true after the ":").
     */
    private boolean afterLabel;

    /**
     * The sequence rule derived from the parsed PML statement.
     * The sequence rule encodes e.g. when this behaviour is considered to be "completed" by the user.
     * For example, a "N->S" behaviour is completed if the user started at "N" and has reached "S".
     * See the paper for more details.
     */
    private PMLRulePattern sequenceRule;

    /**
     * Flag that indicates if the currently parsed state is an end state.
     */
    private boolean currentStateIsEndState;

    /**
     * Pointer to the current notification marker, if any, that is directly added to a state.
     * For example, for "N->C$->S", this pointer will be non-null after parsing "C$"
     * and before parsing the rest.
     */
    private ParsedNotificationMarker currentNotificationMarkerState;

    /**
     * Pointer to the current notification marker, if any, that is added to a "required touch event".
     * For example, for "N->Su$", this pointer will be non-null after parsing "Su$",
     * i.e. in particular only after parsing the "u" (up event).
     */
    private ParsedNotificationMarker currentNotificationMarkerTouchEvent;

    /**
     * Flag that indicates whether the whole parsed statement only contains two-way transitions
     */
    private boolean onlyTwoWayTransitions;

    /**
     * Fltag that indicates whether the parsed statement describes a relative gesture.
     * This is the case if the statement starts with the "O" token.
     * See the paper for more details.
     */
    private boolean relativeGesture;

    /**
     * The density of the device's display. This is needed to correctly compute locations and sizes
     * from given pixel values.
     */
    private float displayDensity;

    /**
     * Constructor.
     *
     * @param displayDensity the density of the device's display. This is needed to correctly
     *                       compute locations and sizes from given pixel values.
     */
    public PMLParserTouchImpl(float displayDensity) {

        this.statesMap = new HashMap<String, ParsedState>();
        this.statesList = new ArrayList<ParsedState>();
        this.transitions = new ArrayList<ParsedTransition>();

        this.stateParams = new ArrayList<ParsedParam>();
        this.requiredTouchEvents = new ArrayList<ParsedRequiredTouchEvent>();

        this.displayDensity = displayDensity;
    }


    @Override
    public ProbBehaviourTouch parse(String pmlStatement,
                                    double x, double y,
                                    double width, double height,
                                    double screenWidth, double screenHeight) {

        initParserState(x, y, width, height, screenWidth, screenHeight);
        initStatePointer();

        /**
         * Example statement:
         * C->E // a centre to east swipe
         */

        // Iterate through the statement, character by character:
        currentToken = "";
        lastToken = "";
        String currentTokenConsumeSafe;
        char currentChar;
        for (int i = 0; i < pmlStatement.length(); i++) {
            currentChar = pmlStatement.charAt(i);

            // append current char to current token:
            if (currentChar != ' ') { // ignore spaces:
                currentToken += currentChar;
            }
            currentTokenConsumeSafe = currentToken;

            Log.d("PML", "parser iteration " + i + ": " + currentChar + ", " + currentToken);

            // -------------------------------------------------------------------------------------
            // BEHAVIOUR_LABEL : set the label for the behaviour
            // -------------------------------------------------------------------------------------
            if (currentToken.endsWith(PMLTokens.BEHAVIOUR_LABEL_SEPARATOR)) {
                commitBehaviourLabel();
            }

            // -------------------------------------------------------------------------------------
            // STATE_LOCATION_MOVE : move state location pointer:
            // -------------------------------------------------------------------------------------
            // move state location pointer one step north:
            else if (currentToken.equals(PMLTokens.STATE_LOCATION_MOVE_NORTH)) {
                moveNorth();
                updateCurrentStateName(currentChar);
            }
            // move state location pointer one step east:
            else if (currentToken.equals(PMLTokens.STATE_LOCATION_MOVE_EAST)) {
                moveEast();
                updateCurrentStateName(currentChar);
            }
            // move state location pointer one step south:
            else if (currentToken.equals(PMLTokens.STATE_LOCATION_MOVE_SOUTH)) {
                moveSouth();
                updateCurrentStateName(currentChar);
            }
            // move state location pointer one step west:
            else if (currentToken.equals(PMLTokens.STATE_LOCATION_MOVE_WEST)) {
                moveWest();
                updateCurrentStateName(currentChar);
            }
            // do not move state location pointer (i.e. stay at centre of current location):
            else if (currentToken.equals(PMLTokens.STATE_LOCATION_MOVE_CENTRE)) {
                moveCentre();
                updateCurrentStateName(currentChar);
            } else if (currentToken.equals(PMLTokens.STATE_LOCATION_RELATIVE_ORIGIN)) {
                setRelativeGesture();
                updateCurrentStateName(currentChar);
            }

            // -------------------------------------------------------------------------------------
            // STATE_SUBLOCATION_MOVE : move state location pointer to sublocation,
            // which changes the size of the state.
            // -------------------------------------------------------------------------------------
            // move state location pointer to the top sublocation of the current location:
            else if (currentToken.equals(PMLTokens.STATE_SUBLOCATION_MOVE_TOP)) {
                moveSublocationTop();
                updateCurrentStateName(currentChar);
            }
            // move state location pointer to the right sublocation of the current location:
            else if (currentToken.equals(PMLTokens.STATE_SUBLOCATION_MOVE_RIGHT)) {
                moveSublocationRight();
                updateCurrentStateName(currentChar);
            }
            // move state location pointer to the bottom sublocation of the current location:
            else if (currentToken.equals(PMLTokens.STATE_SUBLOCATION_MOVE_BOTTOM)) {
                moveSublocationBottom();
                updateCurrentStateName(currentChar);
            }
            // move state location pointer to the left sublocation of the current location:
            else if (currentToken.equals(PMLTokens.STATE_SUBLOCATION_MOVE_LEFT)) {
                moveSublocationLeft();
                updateCurrentStateName(currentChar);
            }
            // do not move state location pointer (i.e. zoom at centre of current location):
            else if (currentToken.equals(PMLTokens.STATE_SUBLOCATION_ZOOM)) {
                zoomSublocationXY();
                updateCurrentStateName(currentChar);
            } else if (currentToken.equals(PMLTokens.STATE_SUBLOCATION_ZOOM_X)) {
                zoomSublocationX();
                updateCurrentStateName(currentChar);
            } else if (currentToken.equals(PMLTokens.STATE_SUBLOCATION_ZOOM_Y)) {
                zoomSublocationY();
                updateCurrentStateName(currentChar);
            } else if (currentToken.equals(PMLTokens.STATE_SUBLOCATION_ZOOM_OUT)) {
                zoomOutSublocationXY();
                updateCurrentStateName(currentChar);
            } else if (currentToken.equals(PMLTokens.STATE_SUBLOCATION_ZOOM_OUT_X) && !this.inStateDetailMode) {
                zoomOutSublocationX();
                updateCurrentStateName(currentChar);
            } else if (currentToken.equals(PMLTokens.STATE_SUBLOCATION_ZOOM_OUT_Y) && !this.inStateDetailMode) {
                zoomOutSublocationY();
                updateCurrentStateName(currentChar);
            }


            // -------------------------------------------------------------------------------------
            // STATE DETAILS: allows developer/user to specify custom or additional details for a
            // state, such as tweaking its size.
            // -------------------------------------------------------------------------------------
            if (currentToken.equals(PMLTokens.STATE_DETAILS_OPENER)) {
                enterStateDetailsMode();
            } else if (currentToken.endsWith(PMLTokens.STATE_DETAILS_CLOSER)) {
                commitStateParam();
                leaveStateDetailsMode();
            } else if (currentToken.endsWith(PMLTokens.STATE_DETAILS_SEPARATOR)) {
                commitStateParam();
            }
            // -------------------------------------------------------------------------------------
            // FURTHER STATE STUFF:
            // -------------------------------------------------------------------------------------
            if (currentToken.equals(PMLTokens.STATE_ENDSTATE_MARKER) && !this.inStateDetailMode) {
                markCurrentStateAsEndState();
            } else if (currentToken.equals(PMLTokens.STATE_TOUCH_EVENT_TYPE_DOWN) && afterLabel) {
                commitRequiredTouchEvent(PMLTokens.STATE_TOUCH_EVENT_TYPE_DOWN);
            } else if (currentToken.equals(PMLTokens.STATE_TOUCH_EVENT_TYPE_UP) && afterLabel) {
                commitRequiredTouchEvent(PMLTokens.STATE_TOUCH_EVENT_TYPE_UP);
            } else if (currentToken.equals(PMLTokens.STATE_TOUCH_EVENT_TYPE_MOVE) && afterLabel) {
                commitRequiredTouchEvent(PMLTokens.STATE_TOUCH_EVENT_TYPE_MOVE);
            } else if (currentToken.equals(PMLTokens.STATE_TOUCH_EVENT_MOD_MIN_ONE) && afterLabel) {
                commitRequiredTouchEventModifier(PMLTokens.STATE_TOUCH_EVENT_MOD_MIN_ONE);
            } else if (currentToken.equals(PMLTokens.STATE_TOUCH_EVENT_MOD_ZERO_OR_MORE) && afterLabel) {
                commitRequiredTouchEventModifier(PMLTokens.STATE_TOUCH_EVENT_MOD_ZERO_OR_MORE);
            }

            // -------------------------------------------------------------------------------------
            // NOTIFICATION MARKER: places a notification marker. Callbacks can check which markers
            // have been reached during an ongoing interaction.
            // -------------------------------------------------------------------------------------
            if (currentToken.equals(PMLTokens.BEHAVIOUR_NOTIFICATION_MARKER)) {
                commitNotificationMarker();
            }

            // -------------------------------------------------------------------------------------
            // TRANSITION: move state location pointer to sublocation,
            // which changes the size of the state.
            // -------------------------------------------------------------------------------------
            if (currentToken.equals(PMLTokens.TRANSITION_ONE_WAY)) {
                commitCurrentState();
                commitTransition(false);
            } else if (currentToken.equals(PMLTokens.TRANSITION_TWO_WAY)) {
                commitCurrentState();
                commitTransition(true);
            }

            this.lastToken = currentTokenConsumeSafe;
        }

        // If a state token is the last token, commit that state as well:
        if (this.currentStateName.length() > 0) {
            commitCurrentState();
        }

        Log.d("PML", "num states parsed: " + this.statesList.size());

        // Sets list of outgoing transitions for each state and calculates transition probabilities
        // from the transition weights:
        finaliseTransitions();

        // Finally create the behaviour based on the parsed information:
        ProbBehaviourTouch behaviour = createBehaviour();

        return behaviour;
    }

    /**
     * Marks this behaviour as a relative touch gesture.
     */
    private void setRelativeGesture() {
        this.relativeGesture = true;
        consumeCurrentToken();
    }

    /**
     * Marks the currently parsed state as an end state.
     */
    private void markCurrentStateAsEndState() {
        this.currentStateIsEndState = true;
        this.consumeCurrentToken();
    }


    /**
     * Sets the initial values for the parser's attributes.
     * This is called prior to parsing in the "parse" method.
     *
     * @param x            x-value of the top left corner of the rectangular bounding box of the GUI element's visuals.
     * @param y            y-value of the top left corner of the rectangular bounding box of the GUI element's visuals.
     * @param width        width of the rectangular bounding box of the GUI element's visuals.
     * @param height       height of the rectangular bounding box of the GUI element's visuals.
     * @param screenWidth  width of the screen
     * @param screenHeight height of the screen
     */
    private void initParserState(double x, double y, double width, double height, double screenWidth, double screenHeight) {
        // Init values:

        this.currentToken = "";
        this.lastToken = "";

        this.stateIndex = 0;
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        this.interactorX = x;
        this.interactorY = y;
        this.interactorW = width;
        this.interactorH = height;
        this.behaviourLabel = null;

        this.onlyTwoWayTransitions = true;

        this.statesMap.clear();
        this.statesList.clear();
        this.transitions.clear();

        this.stateParams.clear();
        this.requiredTouchEvents.clear();

        this.currentStateName = "";

        this.lastParsedState = null;
        this.lastParsedTransition = null;

        this.inStateDetailMode = false;

        this.afterLabel = false;

        this.sequenceRule = new PMLRulePattern();

        this.currentNotificationMarkerState = null;
        this.currentNotificationMarkerTouchEvent = null;
    }


    /**
     * Finalises the transitions, when all states and transitions have been parsed.
     * In particular, it calculates the correct relative weights, so that the transition weights
     * become probabilities (i.e. add up to one for all outgoing transitions per state).
     */
    private void finaliseTransitions() {

        // 1. set list of outgoing transitions per state
        for (ParsedTransition pt : this.transitions) {
            this.statesList.get(pt.from).outgoingTransitions.add(pt);
        }

        // 2. normalise weights of outgoing transitions per state
        for (ParsedState ps : this.statesList) {
            double sumOfWeights = 0;
            for (ParsedTransition pt : ps.outgoingTransitions) {
                sumOfWeights += pt.weight;
            }
            for (ParsedTransition pt : ps.outgoingTransitions) {
                pt.weight /= sumOfWeights;
            }
        }
    }


    /**
     * Creates the behaviour from the parsed states and transitions.
     *
     * @return the parsed touch behaviour
     */
    private ProbBehaviourTouch createBehaviour() {

        /*
        if (this.relativeGesture) {
            ParsedState fakeOriginState = this.statesList.remove(0);
            this.statesMap.remove(fakeOriginState.name);
        }
        */


        ProbBehaviourTouch behaviour = new ProbBehaviourTouch(this.behaviourLabel, this.statesList.size());


        // 1. Set initial state probabilities
        setInitialStateProbabilities();

        // 2. Check for any end states and setup sequence rule:
        this.sequenceRule.pis = new int[this.statesList.size()];
        this.sequenceRule.ends = new int[this.statesList.size()];
        this.sequenceRule.touchEventTokens.clear();
        boolean anyEndStates = false;
        for (ParsedState ps : this.statesMap.values()) {
            // see if we can find an end state:
            anyEndStates = anyEndStates || ps.endState;

            // Add a notification marker if one was parsed for this state:
            if (ps.notificationMarker != null) {
                this.sequenceRule.addStateMarker(ps.index);
            }

            // sequence rule stuff:
            this.sequenceRule.pis[ps.index] = ps.pi > 0 ? 1 : 0;
            this.sequenceRule.ends[ps.index] = ps.endState ? 1 : 0;
            int teIndex = 0;
            for (ParsedRequiredTouchEvent te : ps.requiredTouchEvents) {
                int type = ProbObservationTouch.TYPE_TOUCH_DOWN;
                if (te.type.equals(PMLTokens.STATE_TOUCH_EVENT_TYPE_UP))
                    type = ProbObservationTouch.TYPE_TOUCH_UP;
                else if (te.type.equals(PMLTokens.STATE_TOUCH_EVENT_TYPE_MOVE))
                    type = ProbObservationTouch.TYPE_TOUCH_MOVE;
                int modifier = PMLRulePattern.TOUCH_EVENT_TOKEN_MODIFIER_NORMAL;
                if (te.modifier.equals(PMLTokens.STATE_TOUCH_EVENT_MOD_MIN_ONE))
                    modifier = PMLRulePattern.TOUCH_EVENT_TOKEN_MODIFIER_MIN_ONE;
                else if (te.modifier.equals(PMLTokens.STATE_TOUCH_EVENT_MOD_ZERO_OR_MORE))
                    modifier = PMLRulePattern.TOUCH_EVENT_TOKEN_MODIFIER_ZERO_OR_MORE;
                this.sequenceRule.addTouchEventToken(ps.index, type, modifier);

                // Add a notification marker if one was parsed for this touch event:
                if (te.notificationMarker != null) {
                    this.sequenceRule.addTouchEventMarker(ps.index, teIndex);
                }
                teIndex++;
            }
        }
        // if no end states at all (i.e. no "." used), make the last state an end state:
        if (!anyEndStates) {
            this.sequenceRule.ends[this.sequenceRule.ends.length - 1] = 1;

            // if also only two-way transitions, set first state as end state as well:
            if (this.onlyTwoWayTransitions)
                this.sequenceRule.ends[0] = 1;
        }


        applyLaplaceCorrectionToInitialStateProbs();
        //this also normalises the pis, which until this point are weights, not probabilities


        // 2. Create states:
        for (ParsedState ps : this.statesMap.values()) {
            double[] mean = {ps.cx / screenWidth, ps.cy / screenHeight};
            double[][] mCov = {{Math.pow(ps.w / screenWidth / 4, 2), 0}, {0, Math.pow(ps.h / screenHeight / 4, 2)}};
            behaviour.setState(ps.index, mean, mCov, ps.pi);
        }


        // 3. Create transitions:
        this.sequenceRule.mT = new int[this.statesList.size()][this.statesList.size()];
        double[][] transitions = new double[this.statesList.size()][this.statesList.size()];
        for (ParsedTransition pt : this.transitions) {
            transitions[pt.from][pt.to] = pt.weight;
            this.sequenceRule.mT[pt.from][pt.to] = pt.weight > 0 ? 1 : 0;
        }
        applyLaplaceCorrectionToTransitionMatrix(transitions);
        behaviour.setTransitions(transitions);


        // 4. Set accepted pointer IDs:
        behaviour.setAcceptedPointerIDs(0, 1); //TODO: should this be settable in PML? How?


        // 5. Create the sequence rule:
        behaviour.setSequenceRule(this.sequenceRule);

        // 6. Set as relative if defined as such:
        behaviour.setRelativeGesture(this.relativeGesture);
        if (this.relativeGesture) {
            float[] fakeState = {(float) ((interactorX + interactorW / 2f) / screenWidth),
                    (float) ((interactorY + interactorH / 2f) / screenHeight)};
            behaviour.setRelativeOriginFakeState(fakeState);
        }

        return behaviour;
    }

    /**
     * Applies a Laplace corretion to the state transitions. See the paper for more details.
     *
     * @param transitions the state transition matrix to which the Laplace correction is applied
     */
    private void applyLaplaceCorrectionToTransitionMatrix(double[][] transitions) {
        for (int i = 0; i < transitions.length; i++) {
            double rowSum = 0;
            for (int j = 0; j < transitions[0].length; j++) {
                transitions[i][j] += LAPLACE_CORRECTION_FACTOR;
                rowSum += transitions[i][j];
            }
            for (int j = 0; j < transitions[0].length; j++) {
                transitions[i][j] /= rowSum;
            }
        }
    }


    /**
     * Sets the state location and size variables to
     * the initial location (GUI element visual centre)
     * and the initial size (GUI element visual width/height).
     */
    private void initStatePointer() {

        // Start state location pointer at (visual) centre of GUI element:
        this.stateLocationX = interactorX + interactorW / 2.0;
        this.stateLocationY = interactorY + interactorH / 2.0;

        // Start state size with (visual) size of GUI element:
        this.stateWidth = interactorW;
        this.stateHeight = interactorH;
    }


    /**
     * Clears the current token. This is called after a token was used for some action.
     */
    private void consumeCurrentToken() {
        this.currentToken = "";
    }


    /**
     * Appends the given character to the currently parsed state name.
     *
     * @param currentChar the character to append.
     */
    private void updateCurrentStateName(char currentChar) {
        this.currentStateName += currentChar;
    }


    /**
     * Saves the parsed label.
     * A label is used to give the behaviour a name meaningful to the human reader.
     */
    private void commitBehaviourLabel() {
        this.behaviourLabel = currentToken.replace(PMLTokens.BEHAVIOUR_LABEL_SEPARATOR, "");
        this.afterLabel = true;
        this.consumeCurrentToken();
    }


    /**
     * Saves the currenly parsed state.
     */
    private void commitCurrentState() {

        // Abort if there is actually nothing:
        if (this.currentStateName == null || this.currentStateName.length() == 0)
            return;

        // Only actually save something if it's not the fake state:
        if (true || !this.currentStateName.startsWith(PMLTokens.STATE_LOCATION_RELATIVE_ORIGIN)) {
        // 13.09.16 -> added "true ||" to use O as C

            ParsedState ps = new ParsedState(
                    this.stateIndex,
                    this.currentStateName,
                    this.stateLocationX, this.stateLocationY,
                    this.stateWidth, this.stateHeight, 0, this.currentStateIsEndState);
            this.statesMap.put(this.currentStateName + this.stateIndex, ps);//TODO: is adding index to name a good idea?
            this.statesList.add(ps);

            // Apply the params parsed for this state:
            // TODO: maybe move to its own method, if it should become more extensive
            for (ParsedParam pp : this.stateParams) {
                if (pp.type.equals(PMLTokens.STATE_DETAILS_SCALE_XY)) {
                    ps.w *= pp.numericValue;
                    ps.h *= pp.numericValue;
                } else if (pp.type.equals(PMLTokens.STATE_DETAILS_SCALE_X)) {
                    ps.w *= pp.numericValue;
                } else if (pp.type.equals(PMLTokens.STATE_DETAILS_SCALE_Y)) {
                    ps.h *= pp.numericValue;
                } else if (pp.type.equals(PMLTokens.STATE_DETAILS_SIZE_W)) {
                    ps.w = pp.numericValue * this.displayDensity;
                } else if (pp.type.equals(PMLTokens.STATE_DETAILS_SIZE_H)) {
                    ps.h = pp.numericValue * this.displayDensity;
                }
            }
            /*Log.d("RELATIVE GAUDI", this.currentStateName + ", " + this.currentStateName.equals(PMLTokens.STATE_LOCATION_RELATIVE_ORIGIN));
            // Special stuff for relative origin state -> scale "interactor" to the given size:
            if (this.currentStateName.startsWith(PMLTokens.STATE_LOCATION_RELATIVE_ORIGIN)) {
                this.interactorW = ps.w;
                this.interactorH = ps.h;
            }*/

            this.stateParams.clear(); // clear the params since we've now "used them up" for this state.


            // Add the required touch events parsed for this state:
            for (ParsedRequiredTouchEvent te : this.requiredTouchEvents) {
                ps.addRequiredTouchEvent(te);
            }
            this.requiredTouchEvents.clear(); // clear the required touch events since we've now "used them up" for this state.

            // Add a notification marker if one was parsed for this state:
            if (this.currentNotificationMarkerState != null) {
                Log.d("PML", "added a notification marker to state: " + ps.index);
                ps.setNotificationMarker(this.currentNotificationMarkerState);
                this.currentNotificationMarkerState = null;
            }

            // Add transition that stays within that state:
            ParsedTransition ptSelf = new ParsedTransition();
            ptSelf.from = this.stateIndex;
            ptSelf.to = this.stateIndex;
            ptSelf.weight = 1;
            this.transitions.add(ptSelf);

            this.lastParsedState = ps;
            this.stateIndex++;
        }
        // If it's the fake origin state, all we have to do is to clear the params:
        else {
            this.stateParams.clear();
        }

        this.currentStateName = "";
        this.currentStateIsEndState = false;

        initStatePointer();
        this.consumeCurrentToken();
    }


    /**
     * Sets the state detail mode to false, i.e. the parser is then no longer in state detail mode.
     */
    private void leaveStateDetailsMode() {
        this.inStateDetailMode = false;
        this.consumeCurrentToken();
    }

    /**
     * Sets the state detail mode to true, i.e. the parser is then in state detail mode.
     */
    private void enterStateDetailsMode() {
        this.inStateDetailMode = true;
        this.consumeCurrentToken();
    }

    /**
     * Saves the currently parsed state parameters.
     */
    private void commitStateParam() {

        // Remove symbols that do not belong to the parameterName=value pair:
        String tokenCleaned = this.currentToken.trim(); // user might have inserted spaces
        tokenCleaned = tokenCleaned.replace(PMLTokens.STATE_DETAILS_SEPARATOR, ""); // could be at the end inbetween params
        tokenCleaned = tokenCleaned.replace(PMLTokens.STATE_DETAILS_CLOSER, ""); // could be at the end if last param before closer
        tokenCleaned = tokenCleaned.trim();

        // Split into key=value pair:
        String[] paramTypeAndValue = tokenCleaned.split(PMLTokens.STATE_DETAILS_ASSIGNER);
        String paramType = paramTypeAndValue[0];
        double paramValueNumeric = Double.valueOf(paramTypeAndValue[1]);
        ParsedParam pp = new ParsedParam(paramType, paramValueNumeric);

        //Store the param:
        this.stateParams.add(pp);

        Log.d("RELATIVE", this.currentStateName + ", " + this.currentStateName.equals(PMLTokens.STATE_LOCATION_RELATIVE_ORIGIN));
        // Special stuff for relative origin state -> scale "interactor" to the given size:
        if (this.currentStateName.equals(PMLTokens.STATE_LOCATION_RELATIVE_ORIGIN)) {
            if (paramType.equals(PMLTokens.STATE_DETAILS_SIZE_W))
                this.interactorW = paramValueNumeric * this.displayDensity;
            else if (paramType.equals(PMLTokens.STATE_DETAILS_SIZE_H))
                this.interactorH = paramValueNumeric * this.displayDensity;
            initStatePointer();
        }

        this.consumeCurrentToken();
    }

    /**
     * Saves the currently parsed "required touch events".
     * @param type the type of the "required touch event" (e.g. "u", "d" - up, down)
     */
    private void commitRequiredTouchEvent(String type) {

        ParsedRequiredTouchEvent te = new ParsedRequiredTouchEvent(type, "");
        this.requiredTouchEvents.add(te);
        this.consumeCurrentToken();

        // Add a notification marker if one was parsed for this touch event:
        /*if (this.currentNotificationMarkerTouchEvent != null) {
            te.setNotificationMarker(this.currentNotificationMarkerTouchEvent);
            this.currentNotificationMarkerTouchEvent = null;
        }*/
    }

    /**
     * Saves the currently parsed modifier for a "required touch event".
     * @param modifier the type of the modifier (e.g. "*")
     */
    private void commitRequiredTouchEventModifier(String modifier) {
        this.requiredTouchEvents.get(this.requiredTouchEvents.size() - 1).modifier = modifier;
        this.consumeCurrentToken();
    }

    /**
     * Saves the currently parsed notification marker.
     */
    private void commitNotificationMarker() {

        ParsedNotificationMarker marker = new ParsedNotificationMarker();
        if (isRequiredTouchEventToken(lastToken)) {
            //this.currentNotificationMarkerTouchEvent = marker;
            this.requiredTouchEvents.get(this.requiredTouchEvents.size() - 1)
                    .setNotificationMarker(marker);
            //this.currentNotificationMarkerTouchEvent = null;
        } else {
            this.currentNotificationMarkerState = marker;
            //this.lastParsedState.setNotificationMarker(marker);
        }
        this.consumeCurrentToken();
    }

    /**
     * Helper method to check if the given token is a "required touch event" token.
     * @param token the token to check.
     * @return true if the given token is a "required touch event" token, else false.
     */
    private boolean isRequiredTouchEventToken(String token) {
        return token.equals(PMLTokens.STATE_TOUCH_EVENT_TYPE_DOWN)
                || token.equals(PMLTokens.STATE_TOUCH_EVENT_TYPE_UP)
                || token.equals(PMLTokens.STATE_TOUCH_EVENT_MOD_ZERO_OR_MORE)
                || token.equals(PMLTokens.STATE_TOUCH_EVENT_MOD_MIN_ONE);
    }


    /**
     * Saves the currently parsed transition.
     *
     * @param twoWay flag indicating whether it is a two way transition.
     */
    private void commitTransition(boolean twoWay) {


        if (this.lastParsedState != null) {

            // TODO: find out how to extend to make custom weights possible?

            if (!twoWay)
                this.onlyTwoWayTransitions = false;

            ParsedTransition pt = new ParsedTransition();
            pt.from = this.lastParsedState.index;
            pt.to = this.stateIndex;
            pt.weight = 1;
            this.transitions.add(pt);

            if (twoWay) {
                ParsedTransition pt2 = new ParsedTransition();
                pt2.from = this.stateIndex;
                pt2.to = this.lastParsedState.index;
                pt2.weight = 1;
                this.transitions.add(pt2);
            }

        }

        this.consumeCurrentToken();
    }


    /**
     * Computes and sets the initial state probabilities for the parsed states (i.e. their "pis").
     * This is called when all states and transitions have been parsed and committed/created.
     * In particular, it should be called after calling the finaliseTransitions() method.
     */
    private void setInitialStateProbabilities() {
        for (ParsedState ps : this.statesList) {
            ps.pi = 0;
            // if first state in chain, set pi weight to 1:
            if (ps.index == 0)
                ps.pi = 1;
                // if last state in chain, but has two-way transition, thus has an outgoing transition,
                // other than the own one, also set pi weight to 1:
            else if (ps.index == this.statesList.size() - 1 && ps.outgoingTransitions.size() > 1)
                ps.pi = 1;
            //TODO: add other cases (which are there)?
            //TODO: how to handle case where pis are set by user (not yet implemented)
        }

    }

    /**
     * Ensures that no state has a zero initial probability.
     * This is needed, otherwise it will not be possible for the model to output anything but
     * one hypothesis (i.e. the same starting state, if only one starting state is possible),
     * even if that hypothesis is very unlikely.
     */
    private void applyLaplaceCorrectionToInitialStateProbs() {
        double piSum = 0;
        for (ParsedState ps : this.statesList) {
            ps.pi += LAPLACE_CORRECTION_FACTOR;
            piSum += ps.pi;
        }
        for (ParsedState ps : this.statesList) {
            ps.pi /= piSum;
        }
    }


    /**
     * Moves the state location pointer northwards.
     */
    private void moveNorth() {
        stateLocationY -= stateHeight;
        consumeCurrentToken();
    }

    /**
     * Moves the state location pointer eastwards.
     */
    private void moveEast() {
        stateLocationX += stateWidth;
        consumeCurrentToken();
    }

    /**
     * Moves the state location pointer southwards.
     */
    private void moveSouth() {
        stateLocationY += stateHeight;
        consumeCurrentToken();
    }

    /**
     * Moves the state location pointer westwards.
     */
    private void moveWest() {
        stateLocationX -= stateWidth;
        consumeCurrentToken();
    }

    /**
     * Moves the state location pointer to the centre (i.e. no movement).
     */
    private void moveCentre() {
        //nothing to move here, since centre means staying where we are at now
        consumeCurrentToken();
    }

    /**
     * Moves the state location pointer to the top sublocation.
     */
    private void moveSublocationTop() {
        stateHeight /= 2;
        stateLocationY -= stateHeight;
        consumeCurrentToken();
    }

    /**
     * Moves the state location pointer to the right sublocation.
     */
    private void moveSublocationRight() {
        stateWidth /= 2;
        stateLocationX += stateWidth;
        consumeCurrentToken();
    }

    /**
     * Moves the state location pointer to the bottom sublocation.
     */
    private void moveSublocationBottom() {
        stateHeight /= 2;
        stateLocationY += stateHeight;
        consumeCurrentToken();
    }

    /**
     * Moves the state location pointer to the left sublocation.
     */
    private void moveSublocationLeft() {
        stateWidth /= 2;
        stateLocationX -= stateWidth;
        consumeCurrentToken();
    }

    /**
     * Scales the state size pointer, increasing both width and height by 50 percent.
     */
    private void zoomSublocationXY() {
        stateWidth *= 1.5;
        stateHeight *= 1.5;
        consumeCurrentToken();
    }

    /**
     * Scales the state size pointer, increasing width by 50 percent.
     */
    private void zoomSublocationX() {
        stateWidth *= 1.5;
        consumeCurrentToken();
    }

    /**
     * Scales the state size pointer, increasing height by 50 percent.
     */
    private void zoomSublocationY() {
        stateHeight *= 1.5;
        consumeCurrentToken();
    }

    /**
     * Scales the state size pointer, decreasing both width and height to their halves.
     */
    private void zoomOutSublocationXY() {
        stateWidth /= 2;
        stateHeight /= 2;
        consumeCurrentToken();
    }

    /**
     * Scales the state size pointer, decreasing width to its half.
     */
    private void zoomOutSublocationX() {
        stateWidth /= 2;
        consumeCurrentToken();
    }

    /**
     * Scales the state size pointer, decreasing height to its half.
     */
    private void zoomOutSublocationY() {
        stateHeight /= 2;
        consumeCurrentToken();
    }


    /**
     * Class representing a parsed state.
     * This is used as an intermediate storage for the parsed state information,
     * from which the actual model is created in the end.
     */
    private class ParsedState {

        String name = null;
        double cx;
        double cy;
        double w;
        double h;
        int index = -1;
        double pi;
        boolean endState = false;
        List<ParsedTransition> outgoingTransitions;
        List<ParsedRequiredTouchEvent> requiredTouchEvents;
        private ParsedNotificationMarker notificationMarker;

        ParsedState(int index, String name, double cx, double cy, double w, double h, double pi, boolean endState) {
            this.index = index;
            this.name = name;
            this.cx = cx;
            this.cy = cy;
            this.w = w;
            this.h = h;
            this.endState = endState;
            this.outgoingTransitions = new ArrayList<ParsedTransition>();
            this.requiredTouchEvents = new ArrayList<ParsedRequiredTouchEvent>();
        }

        public void addRequiredTouchEvent(ParsedRequiredTouchEvent te) {
            this.requiredTouchEvents.add(te);
        }

        public void setNotificationMarker(ParsedNotificationMarker notificationMarker) {
            this.notificationMarker = notificationMarker;
        }
    }


    /**
     * Class representing a parsed transition.
     * This is used as an intermediate storage for the parsed transition information,
     * from which the actual model is created in the end.
     */
    private class ParsedTransition {

        int from;
        int to;
        double weight;
    }


    private class ParsedParam {

        String type;
        double numericValue;
        //String stringValue;

        public ParsedParam(String type, double numericValue) {
            this.type = type;
            this.numericValue = numericValue;
            Log.d("PML", "created a ParsedParam object with type: " + type + " and value: " + numericValue);
            //this.stringValue = null;
        }
        /*public ParsedParam(String type, String stringValue){
            this.type = type;
            this.numericValue = Double.NEGATIVE_INFINITY;
            this.stringValue = stringValue;
        }*/
    }


    private class ParsedRequiredTouchEvent {
        String type;
        public String modifier;
        public ParsedNotificationMarker notificationMarker;

        public ParsedRequiredTouchEvent(String type, String modifier) {
            this.type = type;
            this.modifier = modifier;
        }

        public void setNotificationMarker(ParsedNotificationMarker notificationMarker) {
            this.notificationMarker = notificationMarker;
        }
    }


    private class ParsedNotificationMarker {

        public ParsedNotificationMarker() {
        }
    }
}
