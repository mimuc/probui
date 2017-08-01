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

package de.lmu.ifi.medien.probui.system;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.medien.probui.gui.ProbInteractor;

public class ProbUIMediatorImpl implements ProbUIMediator {


    /**
     * List of the probInteractors known to this mediator.
     */
    protected List<ProbInteractor> interactors;


    /**
     * List of the candidates in the current reasoning process.
     */
    protected List<ProbInteractor> candidates;


    /**
     * Trash list to hold excluded candidates that should be removed from the candidates list.
     * Just used internally to avoid concurrent list iteration and removal.
     */
    protected List<ProbInteractor> trash;


    /**
     * Cradle list to hold all probInteractors that should be added to the candidates list.
     * Just used internally to avoid concurrent list iteration and removal.
     */
    protected List<ProbInteractor> cradle;


    /**
     * Claimers list to hold all probInteractors that currently claim to be determined by this mediator.
     * E.g. a button might claim determination upon receiving a touch up event.
     * If an interactor is determined by the mediator it is essentially the "chosen" interactor that
     * is allowed to act upon the just observed user interaction (e.g. a determined button would
     * trigger its associated action).
     */
    protected List<ProbInteractor> claimers;


    /**
     * Posterior over the candidates, i.e. the probabilities of each candidate interactor being
     * the one to activate.
     */
    private double[] candidatesPosterior;
    private double max_evidence;
    private ProbInteractor mostLikelyInteractor;


    public ProbUIMediatorImpl() {

        this.interactors = new ArrayList<ProbInteractor>();
        this.candidates = new ArrayList<ProbInteractor>();
        this.trash = new ArrayList<ProbInteractor>();
        this.cradle = new ArrayList<ProbInteractor>();
        this.claimers = new ArrayList<ProbInteractor>();
    }


    /**
     * Performs a probabilistic reasoning update, by checking candidates, removing unlikely ones,
     * updating the candidate posterior, and finally determining a candidate.
     *
     * @param forceDecision If set to true, the mediator will force a decision
     *                      (e.g. used on touch up, when user expects something will happen).
     */
    public void mediate(boolean forceDecision) {

        // First round:
        // Rule out candidates that are too unlikely.
        // And find most likely interactor and its evidence value.
        updateInteractorStates(false);

        // Update posterior, if still candidates left:
        if (this.candidates.size() > 0) {
            this.updateCandidatePosterior();
        }

        // Update the rulebook:
        for (ProbInteractor interactor : this.candidates) { //TODO: do I really only want to trigger rules for candidates?
            interactor.getCore().updateRulebook();
        }

        // Second round:
        // Rule out candidates that are too unlikely.
        // And find most likely interactor and its evidence value.
        // This is necessary since probInteractors might have changed sth. in response to the rule update above.
        // In particular, they might have self-excluded themselves.
        updateInteractorStates(true);

        // Update claimers list:
        updateClaimersList();

        // Handle claims:
        handleClaims(max_evidence, mostLikelyInteractor);
    }



    private void updateInteractorStates(boolean considerSelfExclusion) {
        max_evidence = Double.NEGATIVE_INFINITY;
        mostLikelyInteractor = null;
        for (ProbInteractor interactor : this.interactors) {
            double evidence = interactor.getCore().getMeanEvidence();
            // Remove if it has excluded itself:
            if (considerSelfExclusion && interactor.getCore().hasSelfExcluded()) {
                this.trash.add(interactor);
                interactor.getCore().grantSelfExclude();
                interactor.getCore().resetSelfExcluded();
            }
            // Exclude candidate if too unlikely:
            else if (interactor.getCore().isCandidate() && evidence < ProbUIMediator.RULE_OUT_MEAN_EVIDENCE) {
                interactor.getCore().exclude();
                this.trash.add(interactor);
            }
            // Promote to candidate if likely enough
            else if (!interactor.getCore().isCandidate() && evidence >= ProbUIMediator.RULE_OUT_MEAN_EVIDENCE
                    && !this.candidates.contains(interactor)) {
                interactor.getCore().makeCandidate();
                this.cradle.add(interactor);
            }
            // Let non-candidate "forget" what it has seen so far
            // to have a new/better chance for future promotion :)
            else if (!interactor.getCore().isCandidate() && evidence < ProbUIMediator.RULE_OUT_MEAN_EVIDENCE) {
                interactor.getCore().resetObservations();
            }
            // DEBUG: print candidate:
            //if (interactor.getCore().isCandidate())
            //    Log.d("ProbUIMediatorImpl", "(still) candidate: " + k + ": " + evidence);

            // Find most likely interactor and its evidence:
            if (interactor.getCore().isCandidate() && evidence > max_evidence) {
                max_evidence = evidence;
                mostLikelyInteractor = interactor;
            }
        }

        // Remove trashed ones:
        removeTrashedCandidates();

        // Add cradled ones:
        addCradledCandidates();
    }

    /**
     * Mediates between all probInteractors with claims.
     *
     * @param max_evidence         The evidence of the currently most likely interactor.
     * @param mostLikelyInteractor The currently most likely interactor.
     */
    private void handleClaims(double max_evidence, ProbInteractor mostLikelyInteractor) {

        // A) If only one interactor has a claim, then determine that one, and exclude all others:
        if (this.claimers.size() == 1) {
            boolean determined = false; // check if that one claimer is good enough:
            if (this.claimers.get(0).getCore().getMeanEvidence() > ProbUIMediator.RULE_OUT_MEAN_EVIDENCE) {
                this.claimers.get(0).getCore().determine();
                determined = true;
            }
            for (ProbInteractor interactor : this.candidates) {
                if (!determined || interactor != this.claimers.get(0)) // if -> only skip that one claimer if it was good enough
                    interactor.getCore().exclude();
            }
            this.candidates.clear();
        }
        // B) If more than one interactor has a claim, choose the most likely one:
        else if (this.claimers.size() > 1) {
            for (ProbInteractor interactor : this.interactors) {
                if (interactor == mostLikelyInteractor
                        && max_evidence > ProbUIMediator.RULE_OUT_MEAN_EVIDENCE) {
                    interactor.getCore().determine();
                } else {
                    interactor.getCore().exclude();
                }
            }
            this.candidates.clear();
        }
    }

    /**
     * Checks all candidates for claims and puts the claiming ones in the claimers list.
     */
    private void updateClaimersList() {
        this.claimers.clear();
        for (ProbInteractor interactor : this.candidates) {
            if (interactor.getCore().isClaimingDetermination()) {
                this.claimers.add(interactor);
            }
        }
    }

    /**
     * Adds all those probInteractors to the candidates list that are currently in the cradle list.
     */
    private void addCradledCandidates() {
        for (ProbInteractor interactor : this.cradle) {
            this.candidates.add(interactor);
        }
        this.cradle.clear();
    }

    /**
     * Removes all those probInteractors from the candidates list that are currently in the trash list.
     */
    private void removeTrashedCandidates() {
        for (ProbInteractor interactor : this.trash) {
            this.candidates.remove(interactor);
        }
        this.trash.clear();
    }


    private void updateCandidatePosterior() {

        this.candidatesPosterior = new double[this.candidates.size()];
        for (int i = 0; i < this.candidatesPosterior.length; i++) {
            this.candidatesPosterior[i] = this.candidates.get(i).getCore().getMeanEvidence();
        }

        // Log sum exp trick:
        // 1. Compute max:
        double max = this.candidatesPosterior[0];
        for (int i = 1; i < this.candidatesPosterior.length; i++) {
            max = Math.max(max, this.candidatesPosterior[i]);
        }
        // 2. Shift and sum:
        double sum = 0;
        for (int i = 0; i < this.candidatesPosterior.length; i++) {
            sum += Math.exp(this.candidatesPosterior[i] - max);
        }
        //3. Shift back:
        sum = Math.log(sum) + max;
        //4. Divide (minus since it's log):
        for (int i = 0; i < this.candidatesPosterior.length; i++) {
            this.candidatesPosterior[i] -= sum;
            this.candidatesPosterior[i] = Math.exp(this.candidatesPosterior[i]);
        }


        for (int i = 0; i < this.candidatesPosterior.length; i++) {
            this.candidates.get(i).getCore().setCandidateProb(this.candidatesPosterior[i]);
        }
        Log.d("ProbUIMediatorImpl", "candidate posterior (v2): " + this.candidatesPosterior[0]
                + (this.candidatesPosterior.length > 1 ? (", " + this.candidatesPosterior[1]) : "")
                + (this.candidatesPosterior.length > 2 ? (", " + this.candidatesPosterior[2]) : ""));
    }



    @Override
    public void onRequestMediation(ProbInteractor source) {
        this.mediate(false);
    }



    @Override
    public void addInteractor(ProbInteractor interactor) {
        this.interactors.add(interactor);
    }





}
