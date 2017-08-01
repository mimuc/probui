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

package de.lmu.ifi.medien.probui.behaviours;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

import java.util.ArrayList;
import java.util.List;

import be.ac.ulg.montefiore.run.jahmm.Hmm;
import be.ac.ulg.montefiore.run.jahmm.toolbox.MarkovGenerator;
import de.lmu.ifi.medien.probui.exceptions.WrongObservationDelegationException;
import de.lmu.ifi.medien.probui.hmm.OpdfTouchGaussian;
import de.lmu.ifi.medien.probui.hmm.OpdfTouchGaussianFactory;
import de.lmu.ifi.medien.probui.hmm.ObservationVectorTouch;
import de.lmu.ifi.medien.probui.observations.ProbObservation;
import de.lmu.ifi.medien.probui.observations.ProbObservationTouch;
import de.lmu.ifi.medien.probui.pml.PMLBehaviourListener;
import de.lmu.ifi.medien.probui.pml.PMLRulePattern;
import de.lmu.ifi.medien.probui.pml.notifications.AbstractNotificationMarker;

/**
 * A touch input "bounding behaviour".
 */
public class ProbBehaviourTouch implements ProbBehaviour {


    /**
     * Maximum number of observations to consider for inference.
     * The last MAX_OBSERVATIONS observations are considered at each point in time.
     * This is a means to ensure fast computations; even if, for example, the user drags around for
     * a looong time.
     */
    public static final int DEFAULT_MAX_OBSERVATIONS = 50;
    //TODO: move this constant to some manager or settings class?

    /**
     * Apply a AIC-based correction term to the likelihood to avoid bias against behaviours with more states.
     */
    private static final boolean USE_AIC_CORRECTION = false;

    private int maxObservations = ProbBehaviourTouch.DEFAULT_MAX_OBSERVATIONS;

    /**
     * List to store the current observations.
     * If the probability of this behavioural pattern is queried/updated, it is computed by
     * evaluating this sequence of observations with the underlying model.
     */
    private List<List<ObservationVectorTouch>> observations = new ArrayList<List<ObservationVectorTouch>>();


    /**
     * The underlying model, meaning the formal representation of this behaviour.
     * We currently use HMMs with mutlivariate Gaussians as states.
     */
    private Hmm<ObservationVectorTouch> model;


    /**
     * The number of states for the underlying model (HMM).
     */
    private final int numStates;


    /**
     * The number of dimensions of the observations for this behavioural pattern.
     * Defaults to 2, since most behaviours will be 2D touch input (x,y coordinates).
     */
    private int numD = 2;


    /**
     * The current log-probability of this behaviour.
     * This is the last evaluation of the underlying model (HMM).
     */
    private double[] runningProbLn = null;


    private int[][] mostLikelySequences = null;


    /**
     * A label, that is a name for this behavioural pattern.
     * This is only useful for debugging, and serves no other purpose.
     */
    private final String label;

    /**
     * Paint for rendering debug visualisations.
     */
    private final Paint debugPaint;

    /**
     * Rectangles for rendering debug visualisations.
     */
    List<RectF[]> debugRects = new ArrayList<RectF[]>();

    /**
     * Angles (of sigma ellipses) for rendering debug visualisations.
     */
    double[] debugAngles = null;


    private List<Integer> acceptedPointerIDs = new ArrayList<Integer>();

    /**
     * ID of the pointer that had the highest running prob at the last update.
     */
    private int maxProbPID;
    private double maxRunningProbLn;

    private double behaviourPosteriorProb;


    /**
     * This is set to true in the reasoning process if this behaviour is found to be the currently
     * most likely behaviour of its interactor.
     */
    private boolean isMostLikelyBehaviour;

    /**
     * The sequence rule that describes the gesture of this behaviour model.
     * This is only set if this behaviour was created via a PML statement.
     */
    private PMLRulePattern sequenceRule;

    /**
     * The time at which the current observation/reasoning process has started.
     */
    private long startTime;
    private long lastTime;
    private List<AbstractNotificationMarker> notificationMarkers;
    private float[] meanTouchPressures;
    private float[] meanTouchSizes;

    private PMLBehaviourListener listenerForPML;
    private boolean needsDebugDrawUpdate;


    private boolean relativeGesture;
    private boolean relativeOriginSet;
    private float[] relativeOriginFakeState;

    private double debugAlpha;
    private double debugAlpha2;



    /**
     * Create a new behavioural pattern.
     *
     * @param label     A label for this behavioural pattern; for debugging purposes only.
     * @param numStates The number of states for the underlying model (HMM).
     */
    public ProbBehaviourTouch(String label, int numStates) {

        this.numStates = numStates;
        OpdfTouchGaussianFactory factory = new OpdfTouchGaussianFactory(this.numD);
        this.model = new Hmm<ObservationVectorTouch>(this.numStates, factory);

        this.notificationMarkers = new ArrayList<AbstractNotificationMarker>();

        // Currently up to two pointers:
        this.observations.add(new ArrayList<ObservationVectorTouch>());
        this.observations.add(new ArrayList<ObservationVectorTouch>());
        //TODO: MULTITOUCH: extend to more than two pointers

        this.reset();

        this.label = label;
        this.debugPaint = new Paint();
        this.debugPaint.setStrokeWidth(12);
        this.debugPaint.setStyle(Paint.Style.STROKE);
        this.debugPaint.setColor(Color.rgb(255, 178, 23));//Color.rgb(160, 80, 160)
    }


    public void setState(int state_id, double[] mean, double[][] mCov, double initial) {
        OpdfTouchGaussian dist = new OpdfTouchGaussian(mean, mCov);
        this.model.setOpdf(state_id, dist);
        this.model.setPi(state_id, initial);
        this.reset();
    }

    public void setTransitions(double[][] mTrans) {
        for (int i = 0; i < this.numStates; i++)
            for (int j = 0; j < this.numStates; j++)
                this.model.setAij(i, j, mTrans[i][j]);
    }


    public void setAcceptedPointerIDs(int... acceptedPointerIDs) {
        for (int acceptedPointerID : acceptedPointerIDs)
            this.acceptedPointerIDs.add(acceptedPointerID);

        this.reset();
    }

    public boolean isAcceptedPointerID(int pointerID) {
        for (int acceptedPointerID : this.acceptedPointerIDs)
            if (pointerID == acceptedPointerID)
                return true;
        return false;
    }

    public void observe(ProbObservation obs) throws WrongObservationDelegationException {

        if (this.startTime == -1) {
            this.startTime = obs.getTimestamp();
            this.lastTime = this.startTime;
        } else {
            this.lastTime = obs.getTimestamp();
        }

        // Relative origin behaviour stuff:
        if (this.relativeGesture && !this.relativeOriginSet
                && obs.getNominalFeatures()[0] == ProbObservationTouch.TYPE_TOUCH_DOWN) {

            double x = obs.getRealFeatures()[ProbObservationTouch.FEATURE_X];
            double y = obs.getRealFeatures()[ProbObservationTouch.FEATURE_Y];

            //double[] originStateMean = ((OpdfTouchGaussian) this.model.getOpdf(0)).mean(); // origin state is always the first state (i.e. index 0)
            this.move((float) (x - this.relativeOriginFakeState[0]),
                    (float) (y - this.relativeOriginFakeState[1]));

            this.relativeOriginSet = true;
        }

        //Log.d("ProbBehaviourTouch", "in observe with: " + obs);
        //Log.d("ProbBehaviourTouch", "in observe with model: " + this.model);

        if (!(obs instanceof ProbObservationTouch)) {
            throw new WrongObservationDelegationException(
                    "Expected touch observation (ProbBehaviourTouch), but received " + obs.getClass().toString());
        }

        // Check if observed pointer ID is accepted by this behaviour pattern:
        int pointerID = ((ProbObservationTouch) obs).getNominalFeatures()[1];
        if (!isAcceptedPointerID(pointerID))
            return;


        this.observations.get(pointerID).add(new ObservationVectorTouch((ProbObservationTouch) obs));
        while (this.observations.get(pointerID).size() > this.maxObservations) {
            for (int pID : this.acceptedPointerIDs)
                if (this.observations.get(pID).size() > 0)
                    this.observations.get(pID).remove(0); // if one is full, remove one obs from ALL! (results in "decay" for pointers that already left)
        }

        // "OR" like combination of pointer-specific sequences (i.e. the max prob is the one that counts):
        //TODO: allow developers to specify OR, AND etc. combinations? i.e. make this a setting exposed in the API
        this.maxRunningProbLn = Double.NEGATIVE_INFINITY;
        this.maxProbPID = 0;
        for (int pID : this.acceptedPointerIDs)
            if (this.observations.get(pID).size() > 0) {
                double lnprob = this.model.lnProbability(this.observations.get(pID));
                this.runningProbLn[pID] = lnprob;
                if (lnprob > this.maxRunningProbLn) {
                    this.maxRunningProbLn = lnprob;
                    this.maxProbPID = pID;
                }
            }


        // Update most likely state sequence:
        for (int pID : this.acceptedPointerIDs)
            if (this.observations.get(pID).size() > 0)
                this.mostLikelySequences[pID] = this.model.mostLikelyStateSequence(this.observations.get(pID));

        //Log.d("ProbBehaviourTouch", "in observe with runnningProbLn: " + this.runningProbLn);

        // Update mean touch pressures:
        this.meanTouchPressures[pointerID] =
                (float) ((this.meanTouchPressures[pointerID] * this.observations.get(pointerID).size()
                        + obs.getRealFeatures()[ProbObservationTouch.FEATURE_PRESSURE])
                        / (this.observations.get(pointerID).size() + 1));

        // Update mean touch sizes:
        this.meanTouchSizes[pointerID] =
                (float) ((this.meanTouchSizes[pointerID] * this.observations.get(pointerID).size()
                        + obs.getRealFeatures()[ProbObservationTouch.FEATURE_AXIS_MAJOR])
                        / (this.observations.get(pointerID).size() + 1));

        //Log.d("PML MEAN TOUCH PRESSURE", this.meanTouchPressures[pointerID]+"");


    }




    public void reset() {
        this.mostLikelySequences = new int[this.acceptedPointerIDs.size()][];
        this.runningProbLn = new double[this.acceptedPointerIDs.size()];
        this.meanTouchPressures = new float[this.acceptedPointerIDs.size()];
        this.meanTouchSizes = new float[this.acceptedPointerIDs.size()];
        for (int i = 0; i < this.observations.size(); i++)
            this.observations.get(i).clear();
        this.startTime = -1;
        this.relativeOriginSet = false;
    }


    public double getRunningProbLn(int pID) {
        return this.runningProbLn[pID] + (USE_AIC_CORRECTION?2*this.model.nbStates():0);//+ (USE_AIC_CORRECTION?2*Math.pow(this.model.nbStates(),2):0);
    }

    public double getRunningProbLn() {
        return this.maxRunningProbLn + (USE_AIC_CORRECTION?2*this.model.nbStates():0);
    }

    @Override
    public void setProbLn(double behaviourPosteriorProb) {
        this.behaviourPosteriorProb = behaviourPosteriorProb;
    }

    @Override
    public double getProbLn() {
        return this.behaviourPosteriorProb;
    }

    @Override
    public double getProb() {
        return Math.exp(this.behaviourPosteriorProb);
    }

    public int getMostLikelyState(int pointerID) {
        if (this.mostLikelySequences != null && this.mostLikelySequences[pointerID] != null)
            return this.mostLikelySequences[pointerID][this.mostLikelySequences[pointerID].length - 1];
        else return -1;
    }

    public int[] getMostLikelyStateSequence(int pointerID) {
        if (this.mostLikelySequences != null && this.mostLikelySequences[pointerID] != null)
            return this.mostLikelySequences[pointerID];
        else return null;
    }


    /**
     * Generates a sequence of observations from this behavioural model.
     *
     * @param numSamples Length of the sequence to generate.
     * @return The generated sequence.
     */
    public List<ObservationVectorTouch> sample(int numSamples) {

        MarkovGenerator<ObservationVectorTouch> mg = new MarkovGenerator<ObservationVectorTouch>(this.model);
        List<ObservationVectorTouch> samples = mg.observationSequence(numSamples);
        return samples;
    }


    public int getNumObservations() {
        int numObs = -1;
        for (int i = 0; i < this.observations.size(); i++)
            numObs = Math.max(numObs, this.observations.get(i).size());
        return numObs;
    }


    public int getObservedNumPointers() {
        int numPointers = 0;
        for (int i = 0; i < this.observations.size(); i++)
            if (this.observations.get(i).size() > 0)
                numPointers++;
        return numPointers;
    }


    public int getMaxProbPID() {
        return this.maxProbPID;
    }

    /**
     * Visualises this behavioural pattern as sigma ellipses and transition lines.
     * Intended for debugging only, not meant to be shown to the user.
     *
     * @param canvas
     * @param translate_x
     * @param translate_y
     * @param screen_x
     * @param screen_y
     */
    public void drawDebug(Canvas canvas, float translate_x, float translate_y, float screen_x, float screen_y) {

        if (this.debugRects.size() == 0 || this.needsDebugDrawUpdate) {
            this.needsDebugDrawUpdate = false;
            this.debugRects.clear();
            this.debugAngles = new double[this.numStates];
            for (int i = 0; i < this.numStates; i++) {
                OpdfTouchGaussian dist = (OpdfTouchGaussian) this.model.getOpdf(i);

                double[] mean = dist.mean();
                double[][] mCov = dist.covariance();

                //Log.d("DEBUGDRAW", "mCov:\n" + mCov[0][0] + ", " + mCov[1][0] + "\n" + mCov[0][1] + ", " + mCov[1][1]);

                // Compute angle of covariance ellipse:
                double trace = mCov[0][0] + mCov[1][1];
                double det = mCov[0][0] * mCov[1][1] - mCov[0][1] * mCov[1][0];
                double eig1 = trace / 2 + Math.sqrt(trace * trace / 4 - det);
                double eig2 = trace / 2 - Math.sqrt(trace * trace / 4 - det);
                double[] eigvec1 = new double[2];
                double[] eigvec2 = new double[2];
                if (mCov[1][0] != 0) {
                    eigvec1[0] = eig1 - mCov[1][1];
                    eigvec1[1] = mCov[1][0];
                    eigvec2[0] = eig2 - mCov[1][1];
                    eigvec2[1] = mCov[1][0];
                } else if (mCov[0][1] != 0) {
                    eigvec1[0] = mCov[0][1];
                    eigvec1[1] = eig1 - mCov[0][0];
                    eigvec2[0] = mCov[0][1];
                    eigvec2[1] = eig2 - mCov[0][0];
                } else {
                    eigvec1[0] = 1;
                    eigvec1[1] = 0;
                    eigvec2[0] = 0;
                    eigvec2[1] = 1;
                }

                double angle = 0;
                double eig_max = 0;
                double eig_min = 0;
                if (eig1 > eig2) {
                    angle = Math.toDegrees(Math.atan2(eigvec1[1], eigvec1[0]));
                    eig_max = eig1;
                    eig_min = eig2;
                    if (mCov[1][1] > mCov[0][0]) {// "mCov[1][1] > mCov[0][0]" added to fix the bug when it's higher than wide...
                        eig_max = eig2;
                        eig_min = eig1;
                    }
                } else {
                    angle = Math.toDegrees(Math.atan2(eigvec2[1], eigvec2[0]));
                    eig_max = eig2;
                    eig_min = eig1;
                }
                this.debugAngles[i] = angle;

                // Compute rectangles for drawing the ellipses:
                RectF[] rects = new RectF[2];
                rects[0] = new RectF((float) mean[0] * screen_x - translate_x - 2 * (float) Math.sqrt(eig_max) * screen_x,
                        (float) mean[1] * screen_y - translate_y - 2 * (float) Math.sqrt(eig_min) * screen_y,
                        (float) mean[0] * screen_x - translate_x + 2 * (float) Math.sqrt(eig_max) * screen_x,
                        (float) mean[1] * screen_y - translate_y + 2 * (float) Math.sqrt(eig_min) * screen_y);
                rects[1] = new RectF((float) mean[0] * screen_x - translate_x - 3 * (float) Math.sqrt(eig_max) * screen_x,
                        (float) mean[1] * screen_y - translate_y - 3 * (float) Math.sqrt(eig_min) * screen_y,
                        (float) mean[0] * screen_x - translate_x + 3 * (float) Math.sqrt(eig_max) * screen_x,
                        (float) mean[1] * screen_y - translate_y + 3 * (float) Math.sqrt(eig_min) * screen_y);

                this.debugRects.add(rects);
            }
        }



        for (int i = 0; i < this.numStates; i++) {

            //this.debugPaint.setColor(Color.argb((int) (this.debugAlpha * 255), 255, 178, 23));
            this.debugPaint.setColor(Color.argb((int) (this.debugAlpha2 * 255), 255, 178, 23));

            OpdfTouchGaussian dist = (OpdfTouchGaussian) this.model.getOpdf(i);
            float cx = (float) dist.mean()[0] * screen_x - translate_x;
            float cy = (float) dist.mean()[1] * screen_y - translate_y;
            canvas.drawCircle(cx, cy, 10, this.debugPaint);

            canvas.save();
            canvas.rotate((float) this.debugAngles[i], cx, cy);
            canvas.drawOval(this.debugRects.get(i)[0], this.debugPaint);
            canvas.drawOval(this.debugRects.get(i)[1], this.debugPaint);
            canvas.restore();

            for (int j = 0; j < this.numStates; j++) {
                if (i == j) continue;
                OpdfTouchGaussian dist2 = (OpdfTouchGaussian) this.model.getOpdf(j);
                //this.debugPaint.setAlpha((int) (255 * this.model.getAij(i, j)));
                canvas.drawLine(cx, cy,
                        (float) dist2.mean()[0] * screen_x - translate_x, (float) dist2.mean()[1] * screen_y - translate_y, this.debugPaint);
            }
            //this.debugPaint.setAlpha(255);
        }
    }


    public String getLabel() {
        return this.label;
    }


    public String toString() {

        String str = "ProbBehaviourTouch: ";
        if (this.label != null && this.label.length() > 0)
            str += this.label + ", ";
        str += "   num states: " + this.numStates + "\n";
        for (int i = 0; i < this.numStates; i++) {
            str += "      state " + i + ":\n";
            str += "      pi: " + this.model.getPi(i) + "\n";
            str += "      dist: " + this.model.getOpdf(i).toString() + "\n";
        }
        str += "   transitions:\n";
        for (int i = 0; i < this.numStates; i++) {
            for (int j = 0; j < this.numStates; j++) {
                str += "      " + i + " to " + j + ": " + this.model.getAij(i, j);
            }
            str += "\n";
        }

        return str;
    }

    @Override
    public void setSequenceRule(PMLRulePattern sequenceRule) {
        this.sequenceRule = sequenceRule;
    }

    @Override
    public PMLRulePattern getSequenceRule() {
        return sequenceRule;
    }

    @Override
    public void setMostLikelyBehaviour(boolean isMostLikely) {
        this.isMostLikelyBehaviour = isMostLikely;
    }

    @Override
    public boolean isMostLikelyBehaviour() {
        return this.isMostLikelyBehaviour;
    }

    @Override
    public long getTimeTaken() {
        return this.lastTime - this.startTime;
    }


    @Override
    public AbstractNotificationMarker getNotificationMarker(int index) {
        return this.sequenceRule.getNotificationMarkers().get(index);
    }

    @Override
    public int[] getEventTypes(int pointerID) {

        int[] result = new int[this.observations.get(pointerID).size()];
        int i = 0;
        for (ObservationVectorTouch obs : this.observations.get(pointerID)) {
            result[i] = obs.nominalFeatures[0];
            i++;
        }
        return result;
    }

    public float getMeanTouchPressures(int pID) {
        return this.meanTouchPressures[pID];
    }

    public float getMeanTouchSize(int pID) {
        return this.meanTouchSizes[pID];
    }

    public void setListenerForPML(PMLBehaviourListener PMLBehaviourListener) {
        this.listenerForPML = PMLBehaviourListener;
    }

    @Override
    public PMLBehaviourListener getListenerForPML() {
        return this.listenerForPML;
    }

    @Override
    public void move(float dx, float dy) {
        for (int i = 0; i < this.model.nbStates(); i++) {
            OpdfTouchGaussian opdf = (OpdfTouchGaussian) this.model.getOpdf(i);
            opdf.updateMean(dx, dy);
        }

        if(this.relativeOriginFakeState != null){
            this.relativeOriginFakeState[0] += dx;
            this.relativeOriginFakeState[1] += dy;
        }
        this.needsDebugDrawUpdate = true;

    }

    private void moveTo(float x, float y) {

        for (int i = 0; i < this.model.nbStates(); i++) {
            OpdfTouchGaussian opdf = (OpdfTouchGaussian) this.model.getOpdf(i);
            opdf.setMean(x, y);
        }

        this.needsDebugDrawUpdate = true;
    }


    public boolean isRelativeGesture() {
        return relativeGesture;
    }

    public void setRelativeGesture(boolean relativeGesture) {
        this.relativeGesture = relativeGesture;
    }

    public float[] getRelativeOriginFakeState() {
        return relativeOriginFakeState;
    }

    public void setRelativeOriginFakeState(float[] relativeOriginFakeState) {
        this.relativeOriginFakeState = relativeOriginFakeState;
    }

    public double getDebugAlpha() {
        return debugAlpha;
    }

    public void setDebugAlpha(double debugAlpha) {
        this.debugAlpha = debugAlpha;
    }

    public void setDebugAlpha2(double debugAlpha2) {
        this.debugAlpha2 = debugAlpha2;
    }

    public void setMaxObservations(int maxObservations){
        this.maxObservations = maxObservations;
    }
}