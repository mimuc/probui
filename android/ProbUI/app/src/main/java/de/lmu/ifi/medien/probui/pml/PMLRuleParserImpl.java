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

import android.support.annotation.NonNull;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import de.lmu.ifi.medien.probui.behaviours.ProbBehaviour;
import de.lmu.ifi.medien.probui.pml.rules.PMLRule;
import de.lmu.ifi.medien.probui.pml.rules.PMLRuleAND;
import de.lmu.ifi.medien.probui.pml.rules.PMLRuleBehaviour;
import de.lmu.ifi.medien.probui.pml.rules.PMLRuleBehaviourCompleted;
import de.lmu.ifi.medien.probui.pml.rules.PMLRuleBehaviourIsMostLikely;
import de.lmu.ifi.medien.probui.pml.rules.PMLRuleBehaviourJustCompleted;
import de.lmu.ifi.medien.probui.pml.rules.PMLRuleBinary;
import de.lmu.ifi.medien.probui.pml.rules.PMLRuleNOT;
import de.lmu.ifi.medien.probui.pml.rules.PMLRuleOR;
import de.lmu.ifi.medien.probui.pml.rules.PMLRuleQualifierNumFingers;
import de.lmu.ifi.medien.probui.pml.rules.PMLRuleQualifierTimeTaken;
import de.lmu.ifi.medien.probui.pml.rules.PMLRuleQualifierTouchPressure;
import de.lmu.ifi.medien.probui.pml.rules.PMLRuleQualifierTouchSize;
import de.lmu.ifi.medien.probui.pml.rules.PMLRuleSet;
import de.lmu.ifi.medien.probui.pml.rules.PMLRuleUnary;


public class PMLRuleParserImpl implements PMLRuleParser {


    private PMLRuleSet ruleset;
    private Map<String, ProbBehaviour> behaviourMap;
    private String ruleLabel;


    private int debug_counter;


    public PMLRuleParserImpl(PMLRuleSet ruleset, Map<String, ProbBehaviour> behaviourMap) {
        this.ruleset = ruleset;
        this.behaviourMap = behaviourMap;
    }


    @Override
    public PMLRule parse(String pmlStatement) {

        // 1. Some preprocessing:
        pmlStatement = preprocessStatement(pmlStatement);

        // 2. Create tokens:
        List<ParsedToken> tokens = createTokens(pmlStatement);

        // 3. Parse label (assumed to be always at the beginning, otherwise there will be expections):
        parseRuleLabel(tokens);

        for (ParsedToken token : tokens) {
            Log.d("PML RULE PARSER", token.value + ", " + token.type);
        }

        // 4. Parse rule:
        PMLRule rootRule = recursiveTreeParse(tokens, 0, tokens.size());
        rootRule.label = this.ruleLabel;
        this.ruleset.addRule(this.ruleLabel, rootRule);

        return rootRule;
    }

    private void parseRuleLabel(List<ParsedToken> tokens) {
        if (tokens.get(0).type == ParsedToken.TOKEN_TYPE_LABEL) {
            this.ruleLabel = tokens.remove(0).value.replace(PMLTokens.RULE_LABEL_SEPARATOR, ""); // remove and store label
            Log.d("PML RULE PARSER", "parsed label: " + this.ruleLabel);
        }
    }

    @NonNull
    private List<ParsedToken> createTokens(String pmlStatement) {
        String[] parts = pmlStatement.split(" ");
        List<ParsedToken> tokens = new ArrayList<ParsedToken>();
        int tokenIndex = 0;
        int lastTokenType = -1;
        for (String part : parts) {
            int tokenType = determineTokenType(part, lastTokenType);
            tokens.add(new ParsedToken(tokenIndex, tokenType, part));
            tokenIndex++;
            lastTokenType = tokenType;
        }
        return tokens;
    }

    @NonNull
    private String preprocessStatement(String pmlStatement) {
        pmlStatement = pmlStatement.replaceAll("\\s*\\(\\s*", " ( "); // ensure whitespace around opening brackets
        pmlStatement = pmlStatement.replaceAll("\\s*\\)\\s*", " ) "); // ensure whitespace around closing brackets
        pmlStatement = pmlStatement.replaceAll("\\s*(?<=\\d)ms\\s*", " ms "); // ensure whitespace around unit ms
        pmlStatement = pmlStatement.replaceAll("\\s*(?<=\\d)s\\s*", " s "); // ensure whitespace around unit s
        pmlStatement = pmlStatement.replaceAll("\\s*(?<=\\d)p\\s*", " p "); // ensure whitespace around unit p
        pmlStatement = pmlStatement.replaceAll("\\s*(?<=\\d)a\\s*", " a "); // ensure whitespace around unit a
        pmlStatement = pmlStatement.replaceAll("\\s*(?<=\\d)fingers\\s*", " fingers "); // ensure whitespace around "fingers"
        pmlStatement = pmlStatement.replaceAll("\\s*:\\s*", ": "); // ensure whitespace after ":", but not before
        pmlStatement = pmlStatement.replaceAll("\\s+", " "); // remove multiple subsequent whitespace characters
        pmlStatement = pmlStatement.trim(); // remove trailing whitespace
        return pmlStatement;
    }


    private PMLRule recursiveTreeParse(List<ParsedToken> tokens, int start, int end) {


        Log.d("PML RULE PARSER", "recursiveTreeParse -> # tokens: " + tokens.size()
                + ", start: " + start + ", end: " + end);


        // -----------------------------------------------------------------------------------------
        // 1. Remove surrounding pair(s) of brackets, if there are any:
        while (this.checkIfSurroundedByPairOfBrackets(tokens, start, end)) {
            start += 1; // "removes" the opening bracket
            end -= 1; // "removes" the closing bracket
        }

        // -----------------------------------------------------------------------------------------
        // 2. Handle pattern: identifier - event operator - event
        // This is a termination case of the recursion, so no recursive calls in here.
        if (end - start == 3 && tokens.get(start).type == ParsedToken.TOKEN_TYPE_IDENTIFIER
                && tokens.get(start + 1).type == ParsedToken.TOKEN_TYPE_EVENT_OPERATOR
                && tokens.get(start + 2).type == ParsedToken.TOKEN_TYPE_EVENT) {

            return createEventRule(tokens.get(start).value,
                    tokens.get(start + 1).value,
                    tokens.get(start + 2).value);
        }


        // -----------------------------------------------------------------------------------------
        // 3. Handle single identifier
        // This is a termination case of the recursion, so no recursive calls in here.
        if (end - start == 1 && tokens.get(start).type == ParsedToken.TOKEN_TYPE_IDENTIFIER) {
            return this.ruleset.getRule(tokens.get(start).value);
        }


        // -----------------------------------------------------------------------------------------
        // 4. Handle unary operator:
        if (end - start >= 1 && tokens.get(start).type == ParsedToken.TOKEN_TYPE_UNARY_OPERATOR) {
            PMLRule subrule = this.recursiveTreeParse(tokens, start + 1, end);
            return createUnaryOperatorRule(tokens.get(start).value, subrule);
        }


        // -----------------------------------------------------------------------------------------
        // 5. Handle qualifier: qualifier - value - unit
        // Must be at the end of the currently parsed part. If not, user has placed it incorrectly.
        if (end - start >= 3 && tokens.get(end - 1).type == ParsedToken.TOKEN_TYPE_QUALIFIER_UNIT
                && tokens.get(end - 2).type == ParsedToken.TOKEN_TYPE_QUALIFIER_VALUE
                && tokens.get(end - 3).type == ParsedToken.TOKEN_TYPE_QUALIFIER) {
            PMLRule subrule = this.recursiveTreeParse(tokens, start, end - 3);
            return createQualifierRule(
                    tokens.get(end - 3).value,
                    tokens.get(end - 2).value,
                    tokens.get(end - 1).value,
                    subrule);
        }

        // -----------------------------------------------------------------------------------------
        // 6. Handle binary operator:

        int bracketLevel = 0;
        int recStartLeft = start;
        int recEndLeft = start;
        int recStartRight = start;
        ParsedToken operatorHere = null;

        // Go through the part of the list that is relevant on this recursion level:
        ParsedToken ti;
        for (int i = start; i < end; i++) {
            ti = tokens.get(i);

            // If we encounter brackets:
            if (ti.type == ParsedToken.TOKEN_TYPE_BRACKET_OPEN) {
                if (bracketLevel == 0)
                    recStartLeft = i + 1; // new start is index after OUTERMOST opening bracket
                bracketLevel++;
            }
            if (ti.type == ParsedToken.TOKEN_TYPE_BRACKET_CLOSE) {
                bracketLevel--;
                if (bracketLevel == 0) {
                    recEndLeft = i; // new end is index at OUTERMOST closing bracket (i.e. last token is at index before this bracket)
                }
            }

            // If we are out of brackets, "collect" what we encounter into left part for recursion:
            if (bracketLevel == 0
                    && (ti.type == ParsedToken.TOKEN_TYPE_IDENTIFIER
                    || ti.type == ParsedToken.TOKEN_TYPE_UNARY_OPERATOR
                    || ti.type == ParsedToken.TOKEN_TYPE_EVENT_OPERATOR
                    || ti.type == ParsedToken.TOKEN_TYPE_EVENT
                    || ti.type == ParsedToken.TOKEN_TYPE_QUALIFIER
                    || ti.type == ParsedToken.TOKEN_TYPE_QUALIFIER_VALUE
                    || ti.type == ParsedToken.TOKEN_TYPE_QUALIFIER_UNIT)) {
                recEndLeft++;
            }

            // If we are out of brackets and we encounter an operator,
            // this is the one to use on this recursion level:
            if (bracketLevel == 0 && ti.type == ParsedToken.TOKEN_TYPE_BINARY_OPERATOR) {
                operatorHere = ti;
                recStartRight = i + 1;
                break; // end here, since now we know the subparts for the recursive calls
            }
        }

        Log.d("PML RULE PARSER", "recursiveTreeParse -> recStartLeft: " + recStartLeft
                + ", recEndLeft: " + recEndLeft + ", recStartRight: " + recStartRight + ", end: " + end);

        // Recursion: parse left part of tree:
        PMLRule leftSubRule = this.recursiveTreeParse(tokens, recStartLeft, recEndLeft);
        // Recursion: parse right part of tree:
        PMLRule rightSubRule = this.recursiveTreeParse(tokens, recStartRight, end);
        // Note: there always have to be TWO subparts,
        // otherwise the operator was used wrong, i.e. wrong syntax.

        // Merge results:
        return createBinaryOperatorRule(operatorHere.value, leftSubRule, rightSubRule);
    }


    private PMLRule createUnaryOperatorRule(String operator, PMLRule subrule) {

        // Create a label: [operator]_[name]
        String label = operator + PMLTokens.RULE_AUTONAME_SEPARATOR + subrule.label;

        PMLRuleUnary rule = null;

        if(ruleset.getRule(label)!=null)
            return ruleset.getRule(label);

        // Is it a "NOT" rule?
        if (operator.equals(PMLTokens.RULE_OPERATOR_NOT)) {
            rule = new PMLRuleNOT(label, subrule);
        }

        // Store the new rule under the generated label:
        this.ruleset.addRule(rule.label, rule);

        return rule;
    }


    private PMLRule createQualifierRule(String qualifier, String value, String unit, PMLRule subrule) {


        Log.d("PML RULE PARSER", "createQualifierRule: " + qualifier + ", " + value + ", " + unit + ", " + subrule.label);


        // Create a label: [operator]_[name]
        String label = subrule.label + PMLTokens.RULE_AUTONAME_SEPARATOR + qualifier
                + PMLTokens.RULE_AUTONAME_SEPARATOR + value + unit;

        PMLRule rule = null;

        if(ruleset.getRule(label)!=null)
            return ruleset.getRule(label);

        // Is it a "time taken" rule?
        if (qualifier.equals(PMLTokens.RULE_QUALIFIER_TIME_TAKEN)) {


            boolean isRange = value.contains("-"); // check if it has a range
            if (isRange) {
                String[] parts = value.split("-");
                rule = new PMLRuleQualifierTimeTaken(
                        label, (PMLRuleBehaviour) subrule,
                        Long.valueOf(parts[0]), Long.valueOf(parts[1]), unit);
            }

            if (!isRange) {
                boolean hasMinValue = value.startsWith(">"); // check if it has a min value
                if (hasMinValue) {
                    value = value.substring(1);
                    rule = new PMLRuleQualifierTimeTaken(
                            label, (PMLRuleBehaviour) subrule, Long.valueOf(value), -1, unit);
                }
                // else it mus have max value:
                else {
                    if (value.startsWith("<")) // remove "<" if there
                        value = value.substring(1);
                    rule = new PMLRuleQualifierTimeTaken(
                            label, (PMLRuleBehaviour) subrule, -1, Long.valueOf(value), unit);
                }
            }

        }
        // Is it a "using fingers" rule?
        else if (qualifier.equals(PMLTokens.RULE_QUALIFIER_NUM_FINGERS)) {
            rule = new PMLRuleQualifierNumFingers(
                    label, (PMLRuleBehaviour) subrule, Integer.valueOf(value), unit);
        }
        // Is it a "with pressure" rule?
        else if (qualifier.equals(PMLTokens.RULE_QUALIFIER_TOUCH_PRESSURE)
                && unit.equals(PMLTokens.RULE_QUALIFIER_UNIT_TOUCH_PRESSURE)) {
            boolean isMaxValue = value.startsWith("<"); // check if its a max pressure statement
            float minPressure = -1;
            float maxPressure = -1;
            if (isMaxValue) { // if max pressure, remove "<" and parse as maxPressure:
                maxPressure = Float.valueOf(value.substring(1));
            }
            // else it's a min pressure statement (we do not allow pressure ranges at the moment):
            else {
                if (value.startsWith(">")) // remove ">" if there
                    value = value.substring(1);
                minPressure = Float.valueOf(value);
            }
            rule = new PMLRuleQualifierTouchPressure(
                    label, (PMLRuleBehaviour) subrule, minPressure, maxPressure, unit);
        }
        // Is it a "with touch size (area)" rule?
        else if (qualifier.equals(PMLTokens.RULE_QUALIFIER_TOUCH_SIZE)
                && unit.equals(PMLTokens.RULE_QUALIFIER_UNIT_TOUCH_SIZE)) {
            boolean isMaxValue = value.startsWith("<"); // check if its a max pressure statement
            float minSize = -1;
            float maxSize = -1;
            if (isMaxValue) { // if max pressure, remove "<" and parse as maxPressure:
                maxSize = Float.valueOf(value.substring(1));
            }
            // else it's a min pressure statement (we do not allow pressure ranges at the moment):
            else {
                if (value.startsWith(">")) // remove ">" if there
                    value = value.substring(1);
                minSize = Float.valueOf(value);
            }
            rule = new PMLRuleQualifierTouchSize(
                    label, (PMLRuleBehaviour) subrule, minSize, maxSize, unit);
        }

        // Store the new rule under the generated label:
        this.ruleset.addRule(rule.label, rule);

        return rule;
    }


    private PMLRule createBinaryOperatorRule(String operator, PMLRule subrule, PMLRule subrule2) {

        PMLRuleBinary rule = null;

        // Create a label: [name1]_[operator]_[name2]
        String label = subrule.label + PMLTokens.RULE_AUTONAME_SEPARATOR
                + operator + PMLTokens.RULE_AUTONAME_SEPARATOR + subrule2.label;

        if(ruleset.getRule(label)!=null)
            return ruleset.getRule(label);

        // Is it an "AND" rule?
        if (operator.equals(PMLTokens.RULE_OPERATOR_AND)) {
            rule = new PMLRuleAND(label);
        }
        // Is it an "OR" rule?
        else if (operator.equals(PMLTokens.RULE_OPERATOR_OR)) {
            rule = new PMLRuleOR(label);
        }

        // Add the two subrules:
        rule.addRule(subrule);
        rule.addRule(subrule2);

        // Store the new rule under the generated label:
        this.ruleset.addRule(rule.label, rule);

        Log.d("PML RULE PARSER", "createBinaryOperatorRule: " + operator + ", " + subrule + ", " + subrule2 + ", " + rule.label);

        return rule;
    }


    private PMLRule createEventRule(String identifier, String operator, String event) {

        PMLRule rule = null;
        String label = identifier + PMLTokens.RULE_AUTONAME_SEPARATOR
                + operator + PMLTokens.RULE_AUTONAME_SEPARATOR + event;

        if(ruleset.getRule(label)!=null)
            return ruleset.getRule(label);

        // Is it an "ON" rule?
        if (operator.equals(PMLTokens.RULE_EVENT_OPERATOR_ON)) {
            rule = new PMLRuleBehaviourJustCompleted(this.behaviourMap.get(identifier), label);
        }
        // Is it an "IS" rule?
        else if (operator.equals(PMLTokens.RULE_EVENT_OPERATOR_IS)) {
            if (event.equals(PMLTokens.RULE_EVENT_COMPLETED))
                rule = new PMLRuleBehaviourCompleted(this.behaviourMap.get(identifier), label);
            else if (event.equals(PMLTokens.RULE_EVENT_MOST_LIKELY))
                rule = new PMLRuleBehaviourIsMostLikely(this.behaviourMap.get(identifier), label);
        }
        this.ruleset.addRule(label, rule);

        Log.d("PML RULE PARSER", "createEventRule: " + identifier + ", " + operator + ", " + event + ", " + rule.label);
        return rule;
    }


    private boolean checkIfSurroundedByPairOfBrackets(List<ParsedToken> tokens, int start, int end) {

        if (end - start >= 3 && tokens.get(start).type == ParsedToken.TOKEN_TYPE_BRACKET_OPEN
                && tokens.get(end - 1).type == ParsedToken.TOKEN_TYPE_BRACKET_CLOSE) {

            //Log.d("PML RULE PARSER", "checkIfSurroundedByPairOfBrackets -> start: " + start + ", end: " + end);

            // We need to check if the two outermost brackets actually are one pair
            // e.g. "(...)" and not something like "(...)...(...)"
            int bracketLevel = 0;
            ParsedToken ti = null;
            for (int i = start + 1; i < end - 1; i++) {
                ti = tokens.get(i);
                if (ti.type == ParsedToken.TOKEN_TYPE_BRACKET_OPEN)
                    bracketLevel++;
                else if (ti.type == ParsedToken.TOKEN_TYPE_BRACKET_CLOSE)
                    bracketLevel--;
                if (bracketLevel < 0)
                    return false;
            }
            if (bracketLevel == 0) {
                return true;
            }
        }
        return false;
    }


    private int determineTokenType(String part, int lastTokenType) {

        int tokenType = -1;

        // Is it a binary operator?
        if (part.equals(PMLTokens.RULE_OPERATOR_AND)
                || part.equals(PMLTokens.RULE_OPERATOR_OR)) {
            tokenType = ParsedToken.TOKEN_TYPE_BINARY_OPERATOR;
        }
        // Is it a unary operator?
        else if (part.equals(PMLTokens.RULE_OPERATOR_NOT)) {
            tokenType = ParsedToken.TOKEN_TYPE_UNARY_OPERATOR;
        }
        // Is it an event operator?
        else if (part.equals(PMLTokens.RULE_EVENT_OPERATOR_IS)
                || part.equals(PMLTokens.RULE_EVENT_OPERATOR_ON)) {
            tokenType = ParsedToken.TOKEN_TYPE_EVENT_OPERATOR;
        }
        // Is it an event?
        else if (part.equals(PMLTokens.RULE_EVENT_COMPLETED)
                || part.equals(PMLTokens.RULE_EVENT_JUST_COMPLETED)
                || part.equals(PMLTokens.RULE_EVENT_MOST_LIKELY)) {
            tokenType = ParsedToken.TOKEN_TYPE_EVENT;
        } else if (part.equals(PMLTokens.RULE_LABEL_SEPARATOR)) {
            tokenType = ParsedToken.TOKEN_TYPE_LABEL_SEPARATOR;
        }
        // Is it a label?
        else if (part.endsWith(PMLTokens.RULE_LABEL_SEPARATOR)) {
            tokenType = ParsedToken.TOKEN_TYPE_LABEL;
        }
        // Is it an opening bracket?
        else if (part.equals(PMLTokens.RULE_BRACKET_OPEN)) {
            tokenType = ParsedToken.TOKEN_TYPE_BRACKET_OPEN;
        }
        // Is it a closing bracket?
        else if (part.equals(PMLTokens.RULE_BRACKET_CLOSE)) {
            tokenType = ParsedToken.TOKEN_TYPE_BRACKET_CLOSE;
        }
        // Is it a qualifier?
        else if (part.equals(PMLTokens.RULE_QUALIFIER_TIME_TAKEN)
                || part.equals(PMLTokens.RULE_QUALIFIER_NUM_FINGERS)
                || part.equals(PMLTokens.RULE_QUALIFIER_TOUCH_PRESSURE)
                || part.equals(PMLTokens.RULE_QUALIFIER_TOUCH_SIZE)) {
            tokenType = ParsedToken.TOKEN_TYPE_QUALIFIER;
        }
        // Is it a qualifier value?
        else if (lastTokenType == ParsedToken.TOKEN_TYPE_QUALIFIER) {
            tokenType = ParsedToken.TOKEN_TYPE_QUALIFIER_VALUE;
        }
        // Is it a qualifier unit?
        else if (part.equals(PMLTokens.RULE_QUALIFIER_UNIT_MILLISECONDS)
                || part.equals(PMLTokens.RULE_QUALIFIER_UNIT_SECONDS)
                || part.equals(PMLTokens.RULE_QUALIFIER_UNIT_FINGERS)
                || part.equals(PMLTokens.RULE_QUALIFIER_UNIT_TOUCH_PRESSURE)
                || part.equals(PMLTokens.RULE_QUALIFIER_UNIT_TOUCH_SIZE)) {
            tokenType = ParsedToken.TOKEN_TYPE_QUALIFIER_UNIT;
        }
        // Else: it must be an identifier:
        else {
            tokenType = ParsedToken.TOKEN_TYPE_IDENTIFIER;
        }
        return tokenType;
    }

    @Override
    public String getRuleLabel() {
        return this.ruleLabel;
    }


    private class ParsedToken {

        public static final int TOKEN_TYPE_UNARY_OPERATOR = 8;
        public static final int TOKEN_TYPE_BINARY_OPERATOR = 0;
        public static final int TOKEN_TYPE_EVENT_OPERATOR = 1;
        public static final int TOKEN_TYPE_EVENT = 2;
        public static final int TOKEN_TYPE_IDENTIFIER = 3;
        public static final int TOKEN_TYPE_LABEL_SEPARATOR = 4;
        public static final int TOKEN_TYPE_LABEL = 5;
        public static final int TOKEN_TYPE_BRACKET_OPEN = 6;
        public static final int TOKEN_TYPE_BRACKET_CLOSE = 7;
        public static final int TOKEN_TYPE_QUALIFIER = 9;
        public static final int TOKEN_TYPE_QUALIFIER_VALUE = 10;
        public static final int TOKEN_TYPE_QUALIFIER_UNIT = 11;

        int index;
        int type = -1;
        String value;

        public ParsedToken(int index, int type, String value) {
            this.index = index;
            this.type = type;
            this.value = value;
        }

    }
}
