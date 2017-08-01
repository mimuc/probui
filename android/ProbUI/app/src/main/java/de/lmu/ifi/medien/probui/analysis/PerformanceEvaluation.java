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

import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;

public class PerformanceEvaluation {

    public static boolean running = false;

    public static void simulateVerticalDownUpSwipes(View view) {

        int num_swipes = 60;
        int num_moves = 30;
        for(int j=0; j < num_swipes; j++) {
            simulateDownEvent(view, view.getWidth() / 2, 0);
            // go down:
            for (int i = 0; i < num_moves; i++) {
                simulateMoveEvent(view, view.getWidth() / 2, view.getHeight() * i * 1f / num_moves);
            }
            // go up:
            for (int i = 0; i < num_moves; i++) {
                simulateMoveEvent(view, view.getWidth() / 2, view.getHeight() * (1 - i * 1f / num_moves));
            }
            simulateUpEvent(view, view.getWidth() / 2, 0);
        }

    }


    private static void simulateDownEvent(View view, float x, float y){
        // Obtain MotionEvent object
        long downTime = SystemClock.uptimeMillis();
        long eventTime = SystemClock.uptimeMillis() + 100;
        // List of meta states found here: developer.android.com/reference/android/view/KeyEvent.html#getMetaState()
        int metaState = 0;
        MotionEvent motionEvent = MotionEvent.obtain(
                downTime,
                eventTime,
                MotionEvent.ACTION_DOWN,
                x,
                y,
                metaState
        );
        // Dispatch touch event to view
        view.dispatchTouchEvent(motionEvent);
    }


    private static void simulateMoveEvent(View view, float x, float y){
        // Obtain MotionEvent object
        long downTime = SystemClock.uptimeMillis();
        long eventTime = SystemClock.uptimeMillis() + 100;
        // List of meta states found here: developer.android.com/reference/android/view/KeyEvent.html#getMetaState()
        int metaState = 0;
        MotionEvent motionEvent = MotionEvent.obtain(
                downTime,
                eventTime,
                MotionEvent.ACTION_MOVE,
                x,
                y,
                metaState
        );
        // Dispatch touch event to view
        view.dispatchTouchEvent(motionEvent);
    }


    private static void simulateUpEvent(View view, float x, float y){
        // Obtain MotionEvent object
        long downTime = SystemClock.uptimeMillis();
        long eventTime = SystemClock.uptimeMillis() + 100;
        // List of meta states found here: developer.android.com/reference/android/view/KeyEvent.html#getMetaState()
        int metaState = 0;
        MotionEvent motionEvent = MotionEvent.obtain(
                downTime,
                eventTime,
                MotionEvent.ACTION_UP,
                x,
                y,
                metaState
        );
        // Dispatch touch event to view
        view.dispatchTouchEvent(motionEvent);
    }
}
