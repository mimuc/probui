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

package de.lmu.ifi.medien.probui.analysis;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.medien.probui.behaviours.ProbBehaviourTouch;
import de.lmu.ifi.medien.probui.exceptions.WrongObservationDelegationException;
import de.lmu.ifi.medien.probui.gui.ProbInteractor;
import de.lmu.ifi.medien.probui.hmm.ObservationVectorTouch;
import de.lmu.ifi.medien.probui.observations.ProbObservationTouch;
import de.lmu.ifi.medien.probui.system.ProbUIManager;

/**
 * Note: This is experimental, not fully developed, and not required to run any of ProbUIs actual functionality.
 */
public class StaticProbUIAnalysis {


    private static double[] computePosterior(double[] prior, double[] evidences) {

        // Default to uniform prior:
        if (prior == null) {
            prior = new double[evidences.length];
            for (int i = 0; i < prior.length; i++) {
                prior[i] = 1.0 / prior.length;
            }
        }

        // Compute posterior:
        double[] posterior = new double[evidences.length];
        for (int i = 0; i < evidences.length; i++) {
            posterior[i] = Math.log(prior[i]) + evidences[i];
        }

        // Log sum exp trick:
        // 1. Compute max:
        double max = posterior[0];
        for (int i = 1; i < posterior.length; i++) {
            max = Math.max(max, posterior[i]);
        }
        // 2. Shift and sum:
        double sum = 0;
        for (int i = 0; i < posterior.length; i++) {
            sum += Math.exp(posterior[i] - max);
        }
        //3. Shift back:
        sum = Math.log(sum) + max;
        //4. Divide (minus since it's log):
        for (int i = 0; i < posterior.length; i++) {
            posterior[i] -= sum;
        }

        return posterior;
    }


    private static int drawFromPrior(double[] prior) {

        double p = Math.random();
        double cumsum = 0;
        for (int i = 0; i < prior.length - 1; i++) {
            if (p > cumsum && p < cumsum + prior[i+1]) {
                return i;
            }
            cumsum += prior[i];
        }
        return prior.length - 1;
    }


    /**
     * Converts the given sequence of touch observations (from HMM library extension)
     * to ProbUI touch observations, "injecting" the given additional parameters.
     *
     * @param obsSeq
     * @param orientation
     * @param axisMinor
     * @param axisMajor
     * @return
     */
    private static List<ProbObservationTouch> convertTouchObservationVectors(
            List<ObservationVectorTouch> obsSeq,
            double orientation, double axisMinor, double axisMajor) {

        List<ProbObservationTouch> result = new ArrayList<ProbObservationTouch>();

        int i = 0;
        for (ObservationVectorTouch obs : obsSeq) {
            int touchType = ProbObservationTouch.TYPE_TOUCH_MOVE;
            if (i == 0) touchType = ProbObservationTouch.TYPE_TOUCH_DOWN;
            if (i == obsSeq.size() - 1) touchType = ProbObservationTouch.TYPE_TOUCH_UP;
            int[] touchTypeAsArray = {touchType};
            double[] touchFeatures = {obs.value(0), obs.value(1), orientation, axisMinor, axisMajor};
            ProbObservationTouch obsConverted = new ProbObservationTouch(touchFeatures, touchTypeAsArray,-1);//-1 --> no timestamp
            result.add(obsConverted);
            i++;
        }
        return result;
    }


    /**
     * Computes mean and std entropy and error rate of the given interface (given via manager)
     * using a Monte-Carlo approach with the given parameters.
     * @param manager
     * @param numSamples
     * @param sequenceLength
     * @param orientation
     * @param axisMinor
     * @param axisMajor
     * @return
     */
    public static double[] analyse(ProbUIManager manager, int numSamples, int sequenceLength, double orientation, double axisMinor, double axisMajor) {


        // 1. Get all probInteractors:
        List<ProbInteractor> interactors = manager.getProbInteractors();

        int errors = 0;
        int cases = 0;

        double entropySum = 0;
        double[] entropies = new double[numSamples];


        Log.d("ProbUI Analysis", "probInteractors.size(): " + interactors.size());

        // 2. Generate behaviour:
        double[] interactorsPrior = new double[interactors.size()];
        for(int i = 0; i < interactorsPrior.length; i++){
            interactorsPrior[i] = 1.0 / interactorsPrior.length;
        }
        for (int i = 0; i < numSamples; i++) {

            if(i % 100 == 0){
                Log.d("ProbUI Analysis", "working on sample " + i);
            }

            int interactorIndex = drawFromPrior(interactorsPrior);
            ProbInteractor interactor = interactors.get(interactorIndex);

            // 2.1 Get the behavioral patterns for this interactor:
            List<ProbBehaviourTouch> behaviours = interactor.getCore().getBehavioursTouch();
            double[] behavioursPrior = interactor.getCore().getBehavioursPrior();


            // 2.2 Sample from the behavioural patterns:


            // 2.2.1 Choose a behaviour based on the prior:
            int idx = drawFromPrior(behavioursPrior);

            //Log.d("ProbUI Analysis", "chosen behaviour " + idx);


            // 2.2.2 Sample from the chosen behaviour:
            List<ObservationVectorTouch> obsSeq = behaviours.get(idx).sample(sequenceLength);

            // 2.2.3 Convert samples from HMM library extension to the ProbUI observation type:
            List<ProbObservationTouch> obsSeqProbUI =
                    convertTouchObservationVectors(obsSeq, orientation, axisMinor, axisMajor);

            // 2.2.4 For each other element, evaluate the generated behaviour:
            double[] evidences = new double[interactors.size()];
            int interactor2Index = 0;
            for (ProbInteractor interactor2 : interactors) {

                // 2.2.3.1 Reset observations for this interactor:
                interactor2.getCore().resetObservations();

                // 2.2.3.2 Feed generated observations to this interactor:
                for (ProbObservationTouch obs : obsSeqProbUI) {
                    try {
                        interactor2.getCore().onTouchObservation(obs);
                    } catch (WrongObservationDelegationException e) {
                        // should never happen; no, really ;)
                    }
                }

                // 2.2.3.3 Get mean evidence after observations:
                double meanEvidence = interactor2.getCore().getMeanEvidence();

                // 2.2.3.3 Set posterior here (needs to be normalised at the end):
                evidences[interactor2Index] = meanEvidence;

                interactor2Index++;
            }

            // 2.2.5 Compute posterior:
            double[] posterior = computePosterior(null, evidences);
            for (int pi = 0; pi < posterior.length; pi++) {
                posterior[pi] = Math.exp(posterior[pi]);
            }

            Log.d("ProbUI Analysis", "evidences: " + evidences[0] + ", " + evidences[1] + ", " + evidences[2]);
            Log.d("ProbUI Analysis", "posterior: " + posterior[0] + ", " + posterior[1] + ", " + posterior[2]);

            // 2.2.6 Compute decision (i.e. argmax posterior):
            int decisionIdx = -1;
            double postMax = -1;
            for (int pi = 0; pi < posterior.length; pi++) {
                if (postMax < posterior[pi]) {
                    postMax = posterior[pi];
                    decisionIdx = pi;
                }
            }

            // 2.2.7 Check if correct interactor has highest posterior prob:
            boolean error = (decisionIdx != interactorIndex);
            if (error) errors++;
            cases++;

            // 2.2.7 Compute entropy:
            double postEntropy = 0;
            for (int pi = 0; pi < posterior.length; pi++) {
                postEntropy += -posterior[pi] * Math.log(posterior[pi]);
            }
            entropySum += postEntropy;
            entropies[i] = postEntropy;
        }

        // 3. Compute error rate and mean and std of entropy:
        double errorRate = errors * 1.0 / cases;
        double meanPostEntropy = entropySum / cases;

        double varPostEntropy = 0;
        for(int i = 0; i < entropies.length; i++){
            varPostEntropy += Math.pow(entropies[i] - meanPostEntropy, 2);
        }
        varPostEntropy /= entropies.length;
        double stdPostEntropy = Math.sqrt(varPostEntropy);

        // 4. Return result
        double[] result = {meanPostEntropy, stdPostEntropy, errorRate};
        return result;

        //TODO: return full posterior entropy distribution, like in the Python script
    }
}
