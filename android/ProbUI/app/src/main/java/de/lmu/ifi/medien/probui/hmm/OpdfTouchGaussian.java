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

package de.lmu.ifi.medien.probui.hmm;

import android.util.Log;

import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Collection;

import be.ac.ulg.montefiore.run.distributions.MultiGaussianDistribution;
import be.ac.ulg.montefiore.run.jahmm.ObservationVector;
import be.ac.ulg.montefiore.run.jahmm.Opdf;
import be.ac.ulg.montefiore.run.distributions.SimpleMatrix;
import de.lmu.ifi.medien.probui.observations.ProbObservationTouch;
import de.lmu.ifi.medien.probui.system.SystemSetup;

public class OpdfTouchGaussian implements Opdf<ObservationVectorTouch> {

    protected MultiGaussianDistribution distribution;

    protected double[] muTouch = new double[2];
    protected double[][] mCovTouch = new double[2][2];

    // Opt vars:
    double[][] P = new double[2][2];
    double[][] PL = new double[2][2];
    double[] lj = new double[2];
    double[] lk = new double[2];
    double[][] muDiffAsMatrix = new double[1][2];
    double[][] mCovTouchL = new double[2][2];
    double[][] R = new double[2][2];
    double[][] T = new double[2][2];

    /**
     * Builds a new gaussian probability distribution with zero mean and
     * identity covariance matrix.
     *
     * @param dimension The dimension of the vectors.
     */
    public OpdfTouchGaussian(int dimension) {
        distribution = new MultiGaussianDistribution(dimension);
    }


    /**
     * Builds a new gaussian probability distribution with a given mean and
     * covariance matrix.
     *
     * @param mean       The distribution's mean.
     * @param covariance The distribution's covariance matrix.
     */
    public OpdfTouchGaussian(double[] mean, double[][] covariance) {
        if (covariance.length == 0 || mean.length != covariance.length ||
                covariance.length != covariance[0].length)
            throw new IllegalArgumentException();

        distribution = new MultiGaussianDistribution(mean, covariance);
    }


    /**
     * Returns (a copy of) this distribution's mean vector.
     *
     * @return The mean vector.
     */
    public double[] mean() {
        return distribution.mean();
    }

    public void updateMean(double dx, double dy){
        this.distribution.originalMean()[0] += dx;
        this.distribution.originalMean()[1] += dy;
    }

    public void setMean(double x, double y) {
        this.distribution.originalMean()[0] = x;
        this.distribution.originalMean()[1] = y;
    }


    /**
     * Returns (a copy of) this distribution's covariance matrix.
     *
     * @return The covariance matrix.
     */
    public double[][] covariance() {
        return distribution.covariance();
    }


    /**
     * Returns the dimension of the vectors handled by this distribution.
     *
     * @return The dimension of the vectors handled by this distribution.
     */
    public int dimension() {
        return distribution.dimension();
    }


    private double computeBCOpt(double[] muTouch, double[][] mCovTouch) {

        //double[][] P = SimpleMatrix.plus(this.distribution.covariance(), mCovTouch);
        for (int r = 0; r < P.length; r++)
            for (int c = 0; c < P[0].length; c++)
                P[r][c] = this.distribution.covariance()[r][c] + mCovTouch[r][c];

        for (int i = 0; i < P.length; i++)
            for (int j = 0; j < P[0].length; j++)
                P[i][j] /= 2.0;


        //PL = SimpleMatrix.decomposeCholesky(P);
        double d;
        double s;
        PL[0][0] = 0;
        PL[0][1] = 0;
        PL[1][0] = 0;
        PL[1][1] = 0;
        for (int j = 0; j < 2; j++) {
            lj = PL[j];
            d = 0.;
            for (int k = 0; k < j; k++) {
                lk = PL[k];
                s = 0.;
                for (int i = 0; i < k; i++)
                    s += lk[i] * lj[i];
                lj[k] = s = (P[j][k] - s) / PL[k][k];
                d = d + s * s;
            }
            d = P[j][j] - d;
            PL[j][j] = Math.sqrt(d);
            for (int k = j + 1; k < 2; k++)
                PL[j][k] = 0.;
        }

        //double[] muDiff = SimpleMatrix.minus(muTouch, this.distribution.mean());
        muDiffAsMatrix[0][0] = muTouch[0] - this.distribution.mean()[0];
        muDiffAsMatrix[0][1] = muTouch[1] - this.distribution.mean()[1];

        double bc = Math.exp(-1.0 / 8.0 *
                SimpleMatrix.times(
                        SimpleMatrix.times(
                                muDiffAsMatrix,
                                SimpleMatrix.inverseCholesky(PL)),
                        SimpleMatrix.transpose(muDiffAsMatrix))[0][0]);

        //mCovTouchL = SimpleMatrix.decomposeCholesky(mCovTouch);
        mCovTouchL[0][0] = 0;
        mCovTouchL[0][1] = 0;
        mCovTouchL[1][0] = 0;
        mCovTouchL[1][1] = 0;
        for (int j = 0; j < 2; j++) {
            lj = mCovTouchL[j];
            d = 0.;
            for (int k = 0; k < j; k++) {
                lk = mCovTouchL[k];
                s = 0.;
                for (int i = 0; i < k; i++)
                    s += lk[i] * lj[i];
                lj[k] = s = (mCovTouch[j][k] - s) / mCovTouchL[k][k];
                d = d + s * s;
            }
            d = mCovTouch[j][j] - d;
            mCovTouchL[j][j] = Math.sqrt(d);
            for (int k = j + 1; k < 2; k++)
                mCovTouchL[j][k] = 0.;
        }

        bc *= Math.sqrt(
                Math.sqrt(
                        this.distribution.covarianceDet() * SimpleMatrix.determinantCholesky(mCovTouchL))
                        / SimpleMatrix.determinantCholesky(PL));



        return bc;
    }


    public void computeTouchCovOpt(double orientation, double axisMinor, double axisMajor) {
        /*
        major = [8, 0]
        minor = [0, 1]
        a = 45/180. * np.pi
        R = np.matrix([[np.cos(a), -np.sin(a)],[np.sin(a), np.cos(a)]])
        S = np.sqrt(np.matrix([major, minor]))
        T = R * S
        mCov = T * T.T*
        */
        //TODO: check appropriate scaling for 00 and 11 values:
        mCovTouch[0][0] = Math.pow(axisMinor/6, 2);
        mCovTouch[0][1] = 0;
        mCovTouch[1][0] = 0;
        mCovTouch[1][1] = Math.pow(axisMajor/6, 2);
        R[0][0] = Math.cos(orientation);
        R[0][1] = -Math.sin(orientation);
        R[1][0] = Math.sin(orientation);
        R[1][1] = Math.cos(orientation);
        //T = SimpleMatrix.times(R, mCovTouch);
        T[0][0] = 0;
        T[0][1] = 0;
        T[1][0] = 0;
        T[1][1] = 0;
        for (int r = 0; r < 2; r++)
            for (int c = 0; c < 2; c++)
                for (int i = 0; i < 2; i++)
                    T[r][c] += R[r][i] * mCovTouch[i][c];
        mCovTouch = SimpleMatrix.times(T, SimpleMatrix.transpose(T));
        //String debugStr = mCovTouch[0][0] + ", " + mCovTouch[0][1] + "; " + mCovTouch[1][0] + ", " + mCovTouch[1][1];
        //Log.d("ProbUI Matrix", "mCovTouch: " + debugStr + "   |||   " + orientation + ", " + axisMinor + ", " + axisMajor);
    }


    public double probability(ObservationVectorTouch o) {
        if (o.dimension() != distribution.dimension())
            throw new IllegalArgumentException("Vector has a wrong " +
                    "dimension");

        // use BC instead of prob:
        if (SystemSetup.TOUCH_PROBABILITY_MODE == SystemSetup.TOUCH_PROBABILITY_MODE_BC) {
            muTouch[0] = o.realFeatures[ProbObservationTouch.FEATURE_X];
            muTouch[1] = o.realFeatures[ProbObservationTouch.FEATURE_Y];
            computeTouchCovOpt(o.realFeatures[ProbObservationTouch.FEATURE_ORIENTATION],
                    o.realFeatures[ProbObservationTouch.FEATURE_AXIS_MINOR],
                    o.realFeatures[ProbObservationTouch.FEATURE_AXIS_MAJOR]);
            return computeBCOpt(muTouch, mCovTouch);
        } else {
            return distribution.probability(o.value);
        }

    }


    public ObservationVectorTouch generate() {
        return new ObservationVectorTouch(distribution.generate());
    }


    public void fit(ObservationVectorTouch... oa) {
        fit(Arrays.asList(oa));
    }


    public void fit(Collection<? extends ObservationVectorTouch> co) {
        if (co.isEmpty())
            throw new IllegalArgumentException("Empty observation set");

        double[] weights = new double[co.size()];
        Arrays.fill(weights, 1. / co.size());

        fit(co, weights);
    }


    public void fit(ObservationVectorTouch[] o, double[] weights) {
        fit(Arrays.asList(o), weights);
    }


    public void fit(Collection<? extends ObservationVectorTouch> co,
                    double[] weights) {
        if (co.isEmpty() || co.size() != weights.length)
            throw new IllegalArgumentException();

        // Compute mean
        double[] mean = new double[dimension()];
        for (int r = 0; r < dimension(); r++) {
            int i = 0;

            for (ObservationVector o : co)
                mean[r] += o.value[r] * weights[i++];
        }

        // Compute covariance
        double[][] covariance = new double[dimension()][dimension()];
        int i = 0;
        for (ObservationVector o : co) {
            double[] obs = o.value;
            double[] omm = new double[obs.length];

            for (int j = 0; j < obs.length; j++)
                omm[j] = obs[j] - mean[j];

            for (int r = 0; r < dimension(); r++)
                for (int c = 0; c < dimension(); c++) {
                    covariance[r][c] += omm[r] * omm[c] * weights[i];
                    //TODO: my hack! -> regularisation:
                    if (r == c)
                        covariance[r][c] += 0.00001;
                }

            i++;
        }

        distribution = new MultiGaussianDistribution(mean, covariance);
    }


    public OpdfTouchGaussian clone() {
        try {
            return (OpdfTouchGaussian) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }


    public String toString() {
        return toString(NumberFormat.getInstance());
    }


    public String toString(NumberFormat numberFormat) {
        String s = "Multi-variate Gaussian distribution --- Mean: [ ";
        double[] mean = distribution.mean();

        for (int i = 0; i < mean.length; i++)
            s += numberFormat.format(mean[i]) + " ";

        return s + "]";
    }


    private static final long serialVersionUID = 1L;



}
