package com.jjoe64.graphview;

import com.jjoe64.graphview.GraphViewSeries.GraphViewSeriesStyle;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources.NotFoundException;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.PaintDrawable;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.View;



public class SelectableBinBarGraphView extends BinBarGraphView {
    private static final int SELECTION_ALPHA = 60;
    private static final int SELECTION_COLOR = Color.RED;

    protected float lowerSelectBoundary = 0;
    protected float upperSelectBoundary = 0;


    public SelectableBinBarGraphView(Context context, String title, boolean isNumBins,
                                     long numBinsOrSize, OnValuesSelectedListener listener) {
        super(context, title, isNumBins, numBinsOrSize, true);
        graphViewContentView = new SelectableBinGraphContentView(context, listener);
        addView(graphViewContentView, new LayoutParams(LayoutParams.FILL_PARENT,
                                                       LayoutParams.FILL_PARENT, 1));
    }


    @Override
    protected void
        drawSeries(Canvas canvas, GraphViewDataInterface[] values, float graphwidth,
                   float graphheight, float border, double minX, double minY, double diffX,
                   double diffY, float horstart, GraphViewSeriesStyle style) {
        super.drawSeries(canvas, values, graphwidth, graphheight, border, minX, minY, diffX, diffY,
                         horstart, style);
        if (lowerSelectBoundary > 0 || upperSelectBoundary > 0) {
            float lowerBoundary = lowerSelectBoundary;
            float upperBoundary = upperSelectBoundary;
            Paint p = new Paint();
            p.setColor(SELECTION_COLOR);
            p.setAlpha(SELECTION_ALPHA);
            canvas.drawRect(lowerBoundary, border, upperBoundary, border + graphheight, p);
        }
    }

    protected class SelectableBinGraphContentView extends GraphViewContentView  {

        /*
         * @Override public boolean onTouchEvent(MotionEvent event) { String tag
         * = getClass().getName()+ ":dispatchTouchEvent"; switch
         * (event.getAction()) { case MotionEvent.ACTION_UP: x = event.getX();
         * Log.d(tag, " ACTION_UP. settting x: "+x); default: } return
         * graphViewContentView.onTouchEvent(event);
         *
         * }
         */
        private GestureDetector mDetector;
        private OnValuesSelectedListener onValuesSelectedListener;

        @SuppressLint("NewApi")
        public SelectableBinGraphContentView(Context context, OnValuesSelectedListener listener) {
            super(context);
            //setOnClickListener(this);
            mDetector = new GestureDetector(getContext(), new SelectableBinGestureListener());
            onValuesSelectedListener = listener;
        }


        @Override
        public boolean onTouchEvent(MotionEvent e) {
            if( mDetector.onTouchEvent(e)){
                return true;
            }
            return super.onTouchEvent(e);
        }

        private class SelectableBinGestureListener extends GestureDetector.SimpleOnGestureListener {

            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                String tag = getClass().getName() + ":onSinlgeTapUp";
                Log.d(tag, " got a singleTapUp!");
                float x = e.getX();
                if (x > upperSelectBoundary) {
                    upperSelectBoundary = x;
                } else if (x < lowerSelectBoundary) {
                    lowerSelectBoundary = x;
                } else {
                    // if within boundaries, move the boundary that the touch was
                    // closer to
                    if ((upperSelectBoundary - x) < (x - lowerSelectBoundary)) {
                        // touch was closer to upper,
                        upperSelectBoundary = x;
                    } else if ((x - lowerSelectBoundary) < (upperSelectBoundary - x)) {
                        lowerSelectBoundary = x;
                    }
                }
                redrawAll();
                notifyOnValuesSelectedListener();
                return true;
            }

            @Override
            public boolean onDown(MotionEvent e) {
                lastTouchEventX = e.getX();
                scrollingStarted = true;
                return true;
            }


            @Override
            public boolean
                onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                onMoveGesture(e2.getX() - lastTouchEventX);
                lastTouchEventX = e2.getX();
                notifyOnValuesSelectedListener();
                return true;
            }

            private void notifyOnValuesSelectedListener(){
                String tag = getClass().getName() + ":notifyOnValuesSelectedListener";
                if(onValuesSelectedListener != null){
                    double lowerValueBoundary =  (viewportStart + (lowerSelectBoundary / getWidth()) * viewportSize);
                    double upperValueBoundary =  (viewportStart + (upperSelectBoundary / getWidth())* viewportSize);
                    Log.d(tag, "calling OnValueSelectedListener. lowerSelectBoundary: " + lowerValueBoundary
                          + " upperSelectBoundary: "+upperValueBoundary);
                    onValuesSelectedListener.OnValuesSelected((long)lowerValueBoundary, (long)upperValueBoundary);
                }
            }
        }

    }

}
