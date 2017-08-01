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

package de.lmu.ifi.medien.probui.gui;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.util.Log;
import android.view.View;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.lmu.ifi.medien.probui.behaviours.ProbBehaviour;
import de.lmu.ifi.medien.probui.behaviours.ProbBehaviourLinker;
import de.lmu.ifi.medien.probui.behaviours.ProbBehaviourTouch;
import de.lmu.ifi.medien.probui.exceptions.WrongObservationDelegationException;
import de.lmu.ifi.medien.probui.observations.ProbObservation;
import de.lmu.ifi.medien.probui.observations.ProbObservationTouch;
import de.lmu.ifi.medien.probui.pml.PMLBehaviourListener;
import de.lmu.ifi.medien.probui.pml.PMLRuleListener;
import de.lmu.ifi.medien.probui.pml.PMLRulebook;
import de.lmu.ifi.medien.probui.pml.PMLRulebookImpl;
import de.lmu.ifi.medien.probui.system.MediationRequestListener;
import de.lmu.ifi.medien.probui.system.ProbUIMediator;


public class ProbInteractorCore {

    /**
     * The interactor that is managed by this core.
     */
    protected ProbInteractor body;


    /**
     * List of the behavioural patterns attached to this core.
     */
    protected List<ProbBehaviour> behaviours;

    /**
     * Map of the behavioural patterns attached to this core.
     * Enables accessing behaviours by their labels/names.
     */
    private Map<String, ProbBehaviour> behaviourMap;


    /**
     * Prior over the behavioural patterns of this core,
     * i. e. relative "weights" of the patterns.
     */
    public double[] behavioursPrior;


    /**
     * Posterior over the behavioural patterns of this core,
     * i. e. the probability of each behaviour given the observations.
     */
    protected double[] behavioursPosterior;


    /**
     * The current evidence (from Bayes formula) of this core.
     * "Current" here means as after the last evaluation, that was triggered by the manager.
     */
    protected double evidence;


    /**
     * Number of observations since the last reset,
     * i. e. number of "current" observations.
     * This is used to compute the mean evidence of a sequence of observations.
     */
    //protected int numObservations;
    // TODO: maybe the mean computation has to go to ProbBehaviour when we introduce multiple modalities?


    /**
     * Flag to indicate whether is core is determined.
     * In most scenarios, "determined" means that the mediator has selected this core "to be active".
     */
    protected boolean determined = false;


    /**
     * Flag to indiciate whether is core is (still) a candidate for determination in the current
     * reasoning process of the mediator.
     */
    protected boolean candidate = false;


    /**
     * Flag to indicate whether this core requests to be determined.
     */
    protected boolean claimsDetermination;


    /**
     * The probability of this interactor being the one to activate.
     * This is part  of the mediation process, and is thus always set by the mediator,
     * never by the core itself.
     */
    protected double candidateProb;


    /**
     * Paint used to draw probabilistic feedback related parts of the body.
     */
    protected Paint feedbackPaint;


    /**
     * Flag to indicate whether this core should draw debug information when rendering the body.
     */
    public boolean debugDraw = false;
    public boolean debugDrawOutline = false;

    protected int surfaceWidth;
    protected int surfaceHeight;


    protected PMLRulebook rulebook;


    private int indexPosteriorMax;


    /**
     * Holds the time of the last observed touch down event.
     * Useful for measuring time taken for the current touch interactions.
     */
    private long lastTouchDownTime;

    /**
     * Hold the time of the last obseverd toouch event.
     * Useful for measuring time taken for the current touch interactions.
     */
    private long lastTouchEventTime;

    /**
     * Flag to indicate that this interactor has excluded itself in the current reasoning process.
     */
    private boolean hasSelfExcluded;
    private MediationRequestListener mediationRequestListener;
    private boolean delayedSelfExcludeCancelled;
    private int maxObservations = ProbBehaviourTouch.DEFAULT_MAX_OBSERVATIONS;


    public ProbInteractorCore(ProbInteractor body) {

        this.body = body;
    }


    /**
     * Initialises this core. Should be called after creating the object.
     */
    public void init() {

        // init behaviour list:
        behaviours = new ArrayList<ProbBehaviour>();
        behaviourMap = new HashMap<String, ProbBehaviour>();


        // init drawing stuff:
        feedbackPaint = new Paint();
        feedbackPaint.setColor(Color.rgb(140, 180, 255));
        feedbackPaint.setStrokeWidth(10);
        feedbackPaint.setStyle(Paint.Style.STROKE);

        // init rulebook:
        this.rulebook = new PMLRulebookImpl();
    }


    /**
     * Adds a behavioural pattern to this core.
     *
     * @param behaviour
     */
    public void addBehaviour(ProbBehaviourTouch behaviour) {
        Log.d("PROBMENU", "core add behaviour --> label: " + behaviour.getLabel());
        this.behaviours.add(behaviour);
        this.behaviourMap.put(behaviour.getLabel(), behaviour);
        this.rulebook.addBehaviour(behaviour);
        Log.d("PROBMENU", "core add behaviour --> behaviours.size(): " + this.behaviours.size());
    }

    public ProbBehaviour addBehaviour(String pmlStatement) {
        return addBehaviour(pmlStatement, null);
    }

    public ProbBehaviour addBehaviour(String pmlStatement, PMLBehaviourListener listener) {
        return ProbBehaviourLinker.linkProbBehaviourTouch(this.body, pmlStatement, true, listener,
                this.body.getView().getContext().getResources().getDisplayMetrics().density);
    }

    public void addRule(String pmlStatement, PMLRuleListener listener) {
        this.rulebook.addRule(pmlStatement, listener);
    }

    /**
     * Wrapper for finalising the behaviour setup with a default prior.
     */
    public void setReady() {
        this.finaliseBehaviourSetup(null);
    }

    /**
     * Finalises the setup of the behavioural patterns.
     * Must be called after adding the last behavioural pattern to this core.
     *
     * @param prior Prior over behavioural patterns, or null for a uniform prior.
     */
    public void finaliseBehaviourSetup(double[] prior) {

        // set a uniform prior, if prior-parameter == null:
        if (prior == null) {
            prior = new double[this.behaviours.size()];
            for (int i = 0; i < prior.length; i++) {
                prior[i] = 1. / this.behaviours.size();
            }
        }
        this.behavioursPrior = prior;

        // init array to hold posterior:
        this.behavioursPosterior = new double[this.behaviours.size()];
        this.setPosteriorMinusInfinity();


        // Set the max observations for the behaviours:
        for(ProbBehaviour b : this.behaviours)
            b.setMaxObservations(this.maxObservations);

        // call the specific method:
        this.body.onCoreFinaliseBehaviourSetup();
    }


    /**
     * Reset the observations. After calling this, all behavioural patterns
     * of this core will have been reset.
     */
    public void resetObservations() {
        for (ProbBehaviour behaviour : this.behaviours) {
            behaviour.reset();
        }
        //this.numObservations = 0;
        this.evidence = 0;
    }


    /**
     * Updates the posterior over the behavioral pattern attached to this core, and the evidence,
     * given the new observation. This should usually be called when new input events are coming in,
     * for example a touch move event (called by mediator, no need to call yourself).
     *
     * @param obs
     * @throws WrongObservationDelegationException
     */
    public void observe(ProbObservation obs) throws WrongObservationDelegationException {


        // Check if interactor invisible
        // -> if so, we ignore observations and cancel any current "involvements"
        if (this.body.getView().getVisibility() == View.INVISIBLE) {
            if (this.isCandidate())
                this.selfExclude();
            else
                this.resetReasoning();
            return;
        }

        //Log.d("ProbInteractorCore", "in observe method with observation: " + obs);

        // Update some general values:
        updateTimeInformation(obs);

        // Let all patterns observe this observation, if they want it:
        delegateObservationToBehaviours(obs);

        // Compute the posterior over the behavioural patterns (in log space):
        updateBehaviourPosterior();

        // Update the index of the posterior max and the most likely behaviour:
        updatePosteriorMaxValues();

        // Update the rulebook:


        // Call the body's related method,
        // which is often overwritten by developers to implement their custom functionality:
        this.body.onCoreObserve(obs);

        //Log.d("ProbInteractorCore", "in observe method with evidence: " + this.evidence);
    }


    private void updateBehaviourPosterior() {

        // Prior * likelihood (in log space) for all behaviours:
        for (int i = 0; i < this.behavioursPrior.length; i++) {
            this.behavioursPosterior[i] =
                    Math.log(this.behavioursPrior[i]) + this.behaviours.get(i).getRunningProbLn();
        }

        // Log sum exp trick:
        // 1. Compute max:
        double max = this.behavioursPosterior[0];
        for (int i = 1; i < this.behavioursPosterior.length; i++) {
            max = Math.max(max, this.behavioursPosterior[i]);
        }
        // 2. Shift and sum:
        double sum = 0;
        for (int i = 0; i < this.behavioursPosterior.length; i++) {
            sum += Math.exp(this.behavioursPosterior[i] - max);
        }
        //3. Shift back:
        sum = Math.log(sum) + max;
        //4. Divide (minus since it's log):
        for (int i = 0; i < this.behavioursPosterior.length; i++) {
            this.behavioursPosterior[i] -= sum;
        }
        this.evidence = sum;

        // 16.09.16: Set posterior prob in behaviour object so that it can be assessed
        // via the behaviour object as well, not just via the core (based on study feedback):
        for (int i = 0; i < this.behavioursPosterior.length; i++) {
            this.behaviours.get(i).setProbLn(this.behavioursPosterior[i]);
        }

        // for debug drawing:
        if (this.debugDraw) {
            for (int i = 0; i < this.behavioursPosterior.length; i++) {
                //Log.d("DEBUG DRAW", "updateBehaviourPosterior --> debug alpha2: " + this.behavioursPosterior[i] + ", exp: " + Math.exp(this.behavioursPosterior[i]));
                ((ProbBehaviourTouch) this.behaviours.get(i)).setDebugAlpha2(Math.exp(this.behavioursPosterior[i]));
            }
            this.body.getView().invalidate();
        }
    }


    private void delegateObservationToBehaviours(ProbObservation obs) throws WrongObservationDelegationException {

        for (ProbBehaviour behaviour : this.behaviours) {

            // Touch - delegate touch observations to touch behavioural patterns:
            if (behaviour instanceof ProbBehaviourTouch
                    && obs instanceof ProbObservationTouch) {
                behaviour.observe(obs);
            }

            //TODO: add delegations for other modalities here
        }
    }


    private void updateTimeInformation(ProbObservation obs) {

        // Measure time of ongoing touch interaction:
        if (obs instanceof ProbObservationTouch) {
            if (obs.getNominalFeatures()[0] == ProbObservationTouch.TYPE_TOUCH_DOWN) {
                this.lastTouchDownTime = obs.getTimestamp();
            }
            this.lastTouchEventTime = obs.getTimestamp();
        }
    }


    private void updatePosteriorMaxValues() {

        this.behaviours.get(0).setMostLikelyBehaviour(false);
        double max = this.behavioursPosterior[0];
        int index = 0;
        for (int i = 1; i < this.behavioursPosterior.length; i++) {
            this.behaviours.get(i).setMostLikelyBehaviour(false);
            if (this.behavioursPosterior[i] > max) {
                max = this.behavioursPosterior[i];
                index = i;
            }
        }
        this.indexPosteriorMax = index;
        this.behaviours.get(this.indexPosteriorMax).setMostLikelyBehaviour(true);
    }

    /**
     * Returns the index of the behavioural pattern with the highes
     * probability mass in the posterior.
     *
     * @return
     */
    public int getIndexPosteriorMax() {

        return this.indexPosteriorMax;
    }


    /**
     * Returns the current evidence (from Bayes formula) of this core.
     *
     * @return
     */
    public double getEvidence() {
        return this.evidence;
    }


    /**
     * Returns the current evidence (from Bayes formula) of this core,
     * divided by the number of observations in the current sequence.
     *
     * @return
     */
    public double getMeanEvidence() {


        int numObservations = 0;
        for (ProbBehaviour b : this.behaviours) {
            numObservations = Math.max(numObservations, b.getNumObservations());
        }

        Log.d("ProbInteractorCore", "getMeanEvidence: " + this.evidence + " / " + numObservations);
        return this.evidence / numObservations; //this.numObservations;
    }


    /**
     * Sets this interactor as a candidate for the current reasoning process.
     * Called by the mediator.
     */
    public void makeCandidate() {
        this.candidate = true;
        //this.setCandidateProb(0);
        //this.resetObservations();
    }


    /**
     * Removes this interactor from the candidates for the current reasoning process.
     * Called by the mediator.
     */
    public void dropCandidate() {
        this.candidate = false;
        this.resetObservations();
    }


    /**
     * "Determines" this interactor.
     * In most scenarios, "determined" means that the mediator has selected this core "to be active".
     * Called by the mediator.
     */
    public void determine() {
        this.candidate = true;
        this.determined = true;
        this.body.getView().invalidate();
        this.body.onDetermined();
    }


    public void exclude() {
        Log.d("ProbInteractorCore", "called exclude()!");
        resetReasoning();
        this.body.onExclude();
    }


    public void selfExclude() {
        Log.d("ProbInteractorCore", "called selfExclude()!");
        this.hasSelfExcluded = true;
    }

    public void selfExclude(long delay) {
        this.delayedSelfExcludeCancelled = false;
        Log.d("ProbInteractorCore", "called selfExclude(long delay) with delay: " + delay);
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!ProbInteractorCore.this.delayedSelfExcludeCancelled) {
                    ProbInteractorCore.this.selfExclude();
                    ProbInteractorCore.this.mediationRequestListener.onRequestMediation(ProbInteractorCore.this.body);
                }
                ProbInteractorCore.this.delayedSelfExcludeCancelled = false;
            }
        }, delay);
    }


    public void grantSelfExclude() {
        Log.d("ProbInteractorCore", "granted self exclude!");
        resetReasoning();
        this.body.onSelfExclude();
    }

    private void resetReasoning() {
        this.candidate = false;
        this.determined = false;
        this.resetClaim();
        this.setCandidateProb(0);
        this.resetObservations();
        this.rulebook.reset();
        this.setPosteriorMinusInfinity();
    }

    private void setPosteriorMinusInfinity(){
        for (int i = 0; i < this.behavioursPosterior.length; i++) {
            this.behavioursPosterior[i] = -999999;
            this.behaviours.get(i).setProbLn(this.behavioursPosterior[i]);
        }
    }


    public List<ProbBehaviour> getBehaviours() {
        return this.behaviours;
    }

    public ProbBehaviour getBehaviour(int index) {
        return this.behaviours.get(index);
    }

    public ProbBehaviour getBehaviour(String label) {
        return this.behaviourMap.get(label);
    }

    public List<ProbBehaviourTouch> getBehavioursTouch() {
        List<ProbBehaviourTouch> result = new ArrayList<ProbBehaviourTouch>();
        for (ProbBehaviour behaviour : this.behaviours) {
            if (behaviour instanceof ProbBehaviourTouch) {
                result.add((ProbBehaviourTouch) behaviour);
            }
        }
        return result;
    }


    public double[] getBehavioursPrior() {
        return this.behavioursPrior;
    }

    /**
     * Sets probability of this interactor being the one to activate in the current reasoning process.
     * Called by the mediator.
     *
     * @param candidateProb
     */
    public void setCandidateProb(double candidateProb) {
        this.candidateProb = candidateProb;

        // for debug drawing:
        for (int i = 0; i < this.behavioursPosterior.length; i++) {
            ((ProbBehaviourTouch) this.behaviours.get(i)).setDebugAlpha(this.candidateProb);
        }

        //TODO: playing around with alpha feedback:
        /*if(this.candidate && ! this.isDetermined())
            this.body.getView().setAlpha((float)this.candidateProb);
        else
            this.body.getView().setAlpha(1);
        //-
        */
        this.body.getView().invalidate();
    }


    /**
     * Returns the probability of this interactor being the one to activate in the current reasoning process.
     *
     * @return
     */
    public double getCandidateProb() {
        return candidateProb;
    }


    /**
     * Lets this interactor observe (and react to) a touch observation.
     * Called by the manager.
     *
     * @param obs
     * @throws WrongObservationDelegationException
     */
    public void onTouchObservation(ProbObservationTouch obs) throws WrongObservationDelegationException {

        // decide what to do based on type of touch observation:
        switch (obs.getNominalFeatures()[0]) {

            case ProbObservationTouch.TYPE_TOUCH_DOWN:
                this.observe(obs);
                break;

            case ProbObservationTouch.TYPE_TOUCH_MOVE:
                this.observe(obs);
                break;

            case ProbObservationTouch.TYPE_TOUCH_UP:
                this.observe(obs);
                break;
            default:
                break;
        }
    }


    /**
     * Renders the interactor including probabilistic feedback, adaptations, etc.
     *
     * @param canvas
     */
    public void drawBody(Canvas canvas) {


        //canvas.save();
        //canvas.translate(this.body.getView().getX(), this.body.getView().getY());

        // call the draw method of the body, if the object wants to do some custom drawing itself:
        this.body.drawSpecific(canvas);


        if (this.debugDraw) {
            for (ProbBehaviour behaviour : this.behaviours) {
                behaviour.drawDebug(canvas,
                        this.body.getView().getX(),
                        this.body.getView().getY(),
                        this.surfaceWidth,
                        this.surfaceHeight);
            }
        }
        //TODO: DEBUG - just draw a blue outline:
        if (this.debugDrawOutline) {
            feedbackPaint.setAlpha((int) (this.candidateProb * 255));
            canvas.drawRect(
                    0, 0,
                    this.body.getView().getWidth(),
                    this.body.getView().getHeight(),
                    feedbackPaint);

            /*if (this.isDetermined()) {
                feedbackPaint.setAlpha(255);
                canvas.drawRect(this.body.getView().getWidth() / 2 - 40, this.body.getView().getHeight() / 2 - 40, this.body.getView().getWidth() / 2 + 40, this.body.getView().getHeight() / 2 + 40, feedbackPaint);
            }*/

        }
        //canvas.restore();

    }


    public boolean isDetermined() {
        return determined;
    }

    /**
     * Removes all behaviours attached to this core.
     * Usually called by the ProbBehaviourLinker before setting up new patterns.
     */
    public void clearBehaviours() {
        this.behaviours.clear();
    }

    public boolean isCandidate() {
        return candidate;
    }


    public void updateSurfaceSize(int width, int height) {
        this.surfaceWidth = width;
        this.surfaceHeight = height;
    }

    public int getSurfaceWidth() {
        return surfaceWidth;
    }

    public int getSurfaceHeight() {
        return surfaceHeight;
    }

    public PMLRulebook getRulebook() {
        return this.rulebook;
    }

    public Paint getFeedbackPaint() {
        return feedbackPaint;
    }

    public double[] getBehavioursPosterior() {
        return behavioursPosterior;
    }

    public double getBehaviourProbLn(String behaviour) {
        int bIndex = this.behaviours.indexOf(this.behaviourMap.get(behaviour));
        return this.behavioursPosterior[bIndex];
    }

    public double getBehaviourProb(String behaviour) {
        int bIndex = this.behaviours.indexOf(this.behaviourMap.get(behaviour));
        return Math.exp(this.behavioursPosterior[bIndex]);
    }


    public int getNumPointers() {

        for (ProbBehaviour b : this.behaviours) {
            if (b instanceof ProbBehaviourTouch) {
                return ((ProbBehaviourTouch) b).getObservedNumPointers();
            }
        }
        return 0;
    }


    /**
     * Returns the time taken for the current/last touch interaction.
     * This is the time measured since the last touch event and the last touch down event.
     *
     * @return
     */
    public long getTimeTakenForTouchInteraction() {
        return this.lastTouchEventTime - this.lastTouchDownTime;
    }


    public boolean isClaimingDetermination() {
        return claimsDetermination;
    }

    public void claimDetermination() {
        this.claimsDetermination = true;
        this.hasSelfExcluded = false;
    }

    public void resetClaim() {
        this.claimsDetermination = false;
    }


    public boolean checkNotificationMarkerReached(String behaviourLabel, int markerIndex) {
        return this.checkNotificationMarkerReached(this.behaviourMap.get(behaviourLabel), markerIndex);
    }

    public boolean checkNotificationMarkerReached(int behaviourIndex, int markerIndex) {
        return this.checkNotificationMarkerReached(this.behaviours.get(behaviourIndex), markerIndex);
    }

    public boolean checkNotificationMarkerReached(ProbBehaviour behaviour, int markerIndex) {
        return behaviour.getNotificationMarker(markerIndex).isReached();
    }

    public boolean checkNotificationMarkerJustReached(String behaviourLabel, int markerIndex) {
        return this.checkNotificationMarkerJustReached(this.behaviourMap.get(behaviourLabel), markerIndex);
    }

    public boolean checkNotificationMarkerJustReached(int behaviourIndex, int markerIndex) {
        return this.checkNotificationMarkerJustReached(this.behaviours.get(behaviourIndex), markerIndex);
    }

    public boolean checkNotificationMarkerJustReached(ProbBehaviour behaviour, int markerIndex) {
        return behaviour.getNotificationMarker(markerIndex).isJustReached();
    }

    public void updateRulebook() {
        this.rulebook.update();
    }

    public void resetRulebook() {
        this.rulebook.reset();
    }

    public boolean hasSelfExcluded() {
        return this.hasSelfExcluded;
    }

    public void resetSelfExcluded() {
        this.delayedSelfExcludeCancelled = true;
        this.hasSelfExcluded = false;
    }


    public void undetermine() {
        Log.d("ProbInteractorCore", "called undetermine()!");
        this.selfExclude();
        this.grantSelfExclude();
        this.resetSelfExcluded();
    }


    public void move(float dx, float dy) {
        for (ProbBehaviour behaviour : this.behaviours) {
            behaviour.move(dx, dy);
        }
    }

    public void setMediationRequestListener(MediationRequestListener mediationRequestListener) {
        this.mediationRequestListener = mediationRequestListener;
    }

    public void setBehavioursMaxObservations(int maxObservations) {

       this.maxObservations = maxObservations;
    }
}
