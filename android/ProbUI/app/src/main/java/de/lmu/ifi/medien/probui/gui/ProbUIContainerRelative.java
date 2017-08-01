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

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.medien.probui.exceptions.WrongObservationDelegationException;
import de.lmu.ifi.medien.probui.system.ProbUIManager;

public class ProbUIContainerRelative extends RelativeLayout implements ProbUIContainer {


    private ProbUIManager manager;


    public ProbUIContainerRelative(Context context) {
        super(context);
    }

    public ProbUIContainerRelative(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ProbUIContainerRelative(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    /*
    public ProbUIContainerRelative(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }
    */




    @Override
    public boolean onTouchEvent(MotionEvent ev)
    {

        try {
            this.manager.manageTouchEvent(ev);
        } catch (WrongObservationDelegationException e) {
            e.printStackTrace();
        }



        return true;
    }


    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev)
    {
        //this.manager.manageTouchEvent(ev);
        return true;
    }

    @Override
    public void registerProbUIManager(ProbUIManager manager) {
        this.manager = manager;
    }

    @Override
    public ViewGroup getViewGroup() {
        return this;
    }

/*
    @Override
    protected void onSizeChanged (int w, int h, int oldw, int oldh){
        this.manager.finaliseSetup();
    }
*/

}
