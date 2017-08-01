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

package de.lmu.ifi.medien.probui.observations;


public abstract class AbstractProbObservation implements ProbObservation {


    public static final int FEATURE_TIMESTAMP = 0;


    /**
     * Holds the real-valued features of this observation.
     */
    private double[] realFeatures;

    /**
     * Holds the nominal (i.e. categorical) features of this observation.
     */
    private int[] nominalFeatures;

    /**
     * The timestamp of this observation, i.e. when did it occur.
     */
    private long timestamp;


    public AbstractProbObservation(double[] realFeatures, int[] nominalFeatures, long timestamp) {
        this.realFeatures = realFeatures;
        this.nominalFeatures = nominalFeatures;
        this.timestamp = timestamp;
    }

    @Override
    public double[] getRealFeatures() {
        return this.realFeatures;
    }

    @Override
    public int[] getNominalFeatures() {
        return this.nominalFeatures;
    }

    @Override
    public long getTimestamp() {
        return this.timestamp;
    }

}
