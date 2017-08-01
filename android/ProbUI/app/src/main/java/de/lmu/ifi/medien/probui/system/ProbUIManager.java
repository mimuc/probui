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

import android.support.v4.view.MotionEventCompat;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;

import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.medien.probui.R;
import de.lmu.ifi.medien.probui.behaviours.ProbBehaviourLinker;
import de.lmu.ifi.medien.probui.exceptions.WrongObservationDelegationException;
import de.lmu.ifi.medien.probui.gui.ProbInteractor;
import de.lmu.ifi.medien.probui.gui.ProbUIContainer;
import de.lmu.ifi.medien.probui.observations.ProbObservationFactory;
import de.lmu.ifi.medien.probui.observations.ProbObservationTouch;

/**
 * Created by Daniel on 05.07.2015.
 */
public class ProbUIManager {


    /**
     * Instance for the singleton pattern.
     */
    protected static ProbUIManager instance;


    /**
     * The view to be managed by this manager. This can be thought of as the root view
     * of the (part of the) interface that should be managed probabilistically.
     */
    protected View view;

    /**
     * The container layout within the view that is managed by this manager.
     * This is mainly needed to intercept touch events across all normal Android UI elements,
     * for example dragging across multiple buttons.
     */
    ProbUIContainer container;


    protected int screenWidth;
    protected int screenHeight;


    /**
     * The mediator used for probabilistic reasoning over the probInteractors managed by this manager.
     */
    protected ProbUIMediator mediator;


    /**
     * Array to hold last touch observations.
     * Defined here instead of local variable for GC reasons.
     */
    protected List<ProbObservationTouch> currentTouchObservations = new ArrayList<ProbObservationTouch>();
    protected List<ProbObservationTouch> previousTouchObservations = new ArrayList<ProbObservationTouch>();


    /**
     * List of the probInteractors managed by this manager.
     */
    protected List<ProbInteractor> probInteractors;
    private List<View> nonProbInteractors;


    protected boolean setupFinalised = false;
    private MotionEvent lastTouchEvent;


    public ProbUIManager(View view, int containerID) {

        this.view = view;
        container = (ProbUIContainer) view.findViewById(containerID);
        container.registerProbUIManager(this);
        // Call finaliseSetup() after Android has finished layouting the container:
        ViewTreeObserver vto = container.getViewGroup().getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                container.getViewGroup().getViewTreeObserver().removeGlobalOnLayoutListener(this);
                screenWidth = container.getViewGroup().getMeasuredWidth();
                screenHeight = container.getViewGroup().getMeasuredHeight();
                Log.d("ProbUIManager", "onGlobalLayout: " + screenWidth + ", " + screenHeight);
                finaliseSetup();
            }
        });

        // init probInteractors list:
        this.probInteractors = new ArrayList<ProbInteractor>();

        this.nonProbInteractors = new ArrayList<View>();

        // init mediator:
        this.mediator = new ProbUIMediatorImpl();
    }


    /**
     * Adds the given interactor to this manager.
     * The manager can only manage probInteractors added to it in this way.
     *
     * @param interactor
     */
    public void addProbInteractor(ProbInteractor interactor) {

        this.probInteractors.add(interactor);
        this.mediator.addInteractor(interactor);
        interactor.getCore().setMediationRequestListener(this.mediator);
    }


    private void addNonProbInteractor(View interactor) {
        this.nonProbInteractors.add(interactor);
    }


    /**
     * Called once implicitly, when the ProbUIContainer is ready (i.e. has been layouted by Android).
     * Triggers behaviour setup of the probInteractors.
     */
    public void finaliseSetup() {

        for (ProbInteractor interactor : this.probInteractors) {
            interactor.getCore().updateSurfaceSize(this.screenWidth, this.screenHeight);
        }

        // one time switch (once container has actual size, i.e. Android has finally finished layouting...):
        if (this.setupFinalised || this.container.getWidth() == 0)
            return;
        this.setupFinalised = true;

        // call on setup of the probInteractors:
        for (ProbInteractor interactor : this.probInteractors) {
            interactor.onProbSetup();
            interactor.getCore().setReady();
        }

        // add a default touch pattern:
        for (ProbInteractor interactor : this.probInteractors) {
            String[] bbs = interactor.getDefaultBehaviours();
            if (bbs != null && bbs.length > 0)
                for (int i = 0; i < bbs.length; i++) {
                    ProbBehaviourLinker.linkPresetProbBehaviourTouch(
                            this,
                            interactor,
                            bbs[i],
                            i > 0,
                            this.view.getContext().getResources().getDisplayMetrics().density);
                }
        }

    }


    public int getSurfaceWidth() {
        return this.screenWidth;
    }

    public int getSurfaceHeight() {
        return this.screenHeight;
    }

    public List<ProbInteractor> getProbInteractors() {
        return this.probInteractors;
    }


    /**
     * Called by the container ViewGroup when a touch event is received.
     *
     * @param ev
     */
    public void manageTouchEvent(MotionEvent ev) throws WrongObservationDelegationException {

        this.lastTouchEvent = ev;
        this.manageHelper(ev);

    }

    public void manageHelper(MotionEvent ev) throws WrongObservationDelegationException {

        this.currentTouchObservations.clear();

        int action = MotionEventCompat.getActionMasked(ev);
        int index = MotionEventCompat.getActionIndex(ev);
        int pointerID = ev.getPointerId(index);

        int type = -1;
        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                type = ProbObservationTouch.TYPE_TOUCH_DOWN;
                break;
            case MotionEvent.ACTION_MOVE:
                type = ProbObservationTouch.TYPE_TOUCH_MOVE;
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                type = ProbObservationTouch.TYPE_TOUCH_UP;
                break;
            default:
                type = -1;
                break;
        }


        long timestamp = ev.getEventTime();

        ProbObservationTouch observation = ProbObservationFactory.createTouchObservation(
                ev.getX(index), ev.getY(index),
                ev.getX(index) * 1.0 / container.getWidth(), ev.getY(index) * 1.0 / container.getHeight(),
                ev.getOrientation(index), ev.getTouchMinor(index) * 1.0 / container.getWidth(),
                ev.getTouchMajor(index) * 1.0 / container.getHeight(), ev.getPressure(index),
                type, pointerID, timestamp);
        this.currentTouchObservations.add(observation);

        // Since move is always associated with the first pointer,
        // we need to manually duplicate it for the second one
        // (TODO: and for further pointers, if we change it to more than 2):
        if (ev.getPointerCount() == 2 && type == ProbObservationTouch.TYPE_TOUCH_MOVE) {
            ProbObservationTouch observation2 = ProbObservationFactory.createTouchObservation(
                    ev.getX(index), ev.getY(index),
                    ev.getX(1) * 1.0 / container.getWidth(), ev.getY(1) * 1.0 / container.getHeight(),
                    ev.getOrientation(1), ev.getToolMinor(1) * 1.0 / container.getWidth(),
                    ev.getToolMajor(1) * 1.0 / container.getHeight(), ev.getPressure(1),
                    type, ev.getPointerId(1), timestamp);
            this.currentTouchObservations.add(observation2);
        }

        //Log.d("MULTITOUCH", "type: " + type + ", index: " + pointerID + ", size: " + ev.getTouchMajor(index) * 1.0 / container.getHeight());

        // Distribute touch observation to the cores of all probInteractors
        // (for reasoning by these interactor cores!, not for visual feedback etc. - that comes below: interactor.onTouchDown etc.)
        boolean passedOn = false;
        for (ProbInteractor interactor : this.probInteractors) {

            for (int i = 0; i < this.currentTouchObservations.size(); i++) {
                ProbObservationTouch obs = this.currentTouchObservations.get(i);
                if (obs == null) continue;
                if (obs.getNominalFeatures()[0] != ProbObservationTouch.TYPE_TOUCH_MOVE
                        || this.currentTouchObservations.size() != this.previousTouchObservations.size()) {
                    interactor.getCore().onTouchObservation(obs);
                    passedOn = true;
                } else { // This code filters out move events that moved very little (potentially improves performance):
                    double[] obsXY = this.currentTouchObservations.get(i).getRealFeatures();
                    double[] obsPrevXY = this.previousTouchObservations.get(i).getRealFeatures();
                    double dx = obsXY[0] - obsPrevXY[0];
                    double dy = obsXY[1] - obsPrevXY[1];
                    double dist = Math.sqrt(dx * dx + dy * dy);
                    if (dist > 0.0125) { // TODO: movement threshold currently hardcoded: 0.0125
                        interactor.getCore().onTouchObservation(obs);
                        passedOn = true;
                    } else {
                    }
                }
            }

        }

        if (passedOn) {
            this.previousTouchObservations.clear();
            this.previousTouchObservations.addAll(this.currentTouchObservations);
        }


        // Forward the touch observation for probInteractors
        // to react (e.g. visual feedback, triggering actions, nothing to do with the mediation):
        for (ProbInteractor interactor : this.probInteractors) {
            for (ProbObservationTouch obs : this.currentTouchObservations) {
                if (obs != null) {
                    switch (obs.getNominalFeatures()[0]) {

                        case ProbObservationTouch.TYPE_TOUCH_DOWN:
                            interactor.onTouchDown(obs);
                            break;

                        case ProbObservationTouch.TYPE_TOUCH_MOVE:
                            interactor.onTouchMove(obs);
                            break;

                        case ProbObservationTouch.TYPE_TOUCH_UP:
                            interactor.onTouchUp(obs, ev.getPointerCount() - 1);
                            break;
                        default:
                            break;
                    }
                }
            }
        }


        // If no element is determined yet (i.e. no decision yet), update the reasoning process.
        if (!isOneDetermined() && passedOn) {
            this.mediator.mediate(false);
        }


        // Post mediation: Forward the touch observation again
        // to the post-mediation versions of the onTouch... methods
        for (ProbInteractor interactor : this.probInteractors) {
            for (ProbObservationTouch obs : this.currentTouchObservations) {
                if (obs != null) {
                    switch (obs.getNominalFeatures()[0]) {

                        case ProbObservationTouch.TYPE_TOUCH_DOWN:
                            interactor.onTouchDownPost(obs);
                            break;

                        case ProbObservationTouch.TYPE_TOUCH_MOVE:
                            interactor.onTouchMovePost(obs);
                            break;

                        case ProbObservationTouch.TYPE_TOUCH_UP:
                            interactor.onTouchUpPost(obs, ev.getPointerCount() - 1);
                            break;
                        default:
                            break;
                    }
                }
            }
        }


        // Pass on to other GUI elements:
        if (!isOneDetermined()) {
            for (View view : this.nonProbInteractors) {
                if (view.isFocusable() && view.isEnabled())
                    view.onTouchEvent(ev);
            }
        }
    }


    /**
     * Checks whether there is a determined interactor among those managed by this manager.
     *
     * @return
     */
    public boolean isOneDetermined() {
        for (ProbInteractor interactor : this.probInteractors) {
            if (interactor.getCore().isDetermined()) {
                return true;
            }
        }
        return false;
    }


    /**
     * Automatically adds all ProbInteractor views within the probUIContainer ViewGroup to this manager.
     */
    public void autoAssignInteractors() {

        List<ProbInteractor> foundProbInteractors = Tools.getAllProbInteractors(this.container.getViewGroup());

        for (ProbInteractor interactor : foundProbInteractors) {
            this.addProbInteractor(interactor);
        }


        List<View> foundNonProbInteractors = Tools.getAllNonProbViews(this.container.getViewGroup());

        for (View view : foundNonProbInteractors) {
            this.addNonProbInteractor(view);
        }


        Log.d("ProbUIManager", "Auto-assign added " + foundProbInteractors.size() + " probInteractors.");
    }


}
