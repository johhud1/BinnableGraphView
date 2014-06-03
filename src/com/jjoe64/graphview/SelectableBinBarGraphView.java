package com.jjoe64.graphview;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;

import com.jjoe64.graphview.GraphViewSeries.GraphViewSeriesStyle;



public class SelectableBinBarGraphView extends BinBarGraphView {
    private static final int SELECTION_ALPHA = 60;
    private static final int SELECTION_COLOR = Color.RED;

    protected SelectRangeHandle lowerSelectBoundary = new SelectRangeHandle(0);
    protected SelectRangeHandle upperSelectBoundary = new SelectRangeHandle(0);

    private class SelectRangeHandle {
        private static final int WIDTH = 8;
        private static final int RECT_WIDTH = 50;
        private static final int HANDLE_COLOR = Color.DKGRAY;
        private static final int COLOR = Color.BLACK;
        private float position;
        protected boolean isDragging = false;
        protected Rect handleRect;
        public SelectRangeHandle() {
            position = 0;
        }

        public SelectRangeHandle(int position) {
            this.position = position;
            handleRect = new Rect();
        }


        public void draw(Canvas canvas, float graphwidth, float graphheight, float border,
                         float horstart, Paint p) {
            Paint paint = new Paint(p);
            paint.setColor(COLOR);
            canvas.drawRect(position, border - WIDTH, position + WIDTH,
                            border + graphheight, paint);
            handleRect = new Rect((int)position, (int)border-RECT_WIDTH, (int)position+RECT_WIDTH, (int)border);
            Paint handlePaint = new Paint(paint);
            handlePaint.setColor(HANDLE_COLOR);
            canvas.drawRect(handleRect, handlePaint);

        }

        public boolean contains(MotionEvent e){
            boolean res = (position <= e.getX()) && ((position+WIDTH) >= e.getX());
            res |= handleRect.contains((int)e.getX(), (int)e.getY());
            return res;
        }

        public float getPosition(){
            return position;
        }
        public void setPosition(float newPos){
            position = newPos;
        }
        public void movePosition(float xDistance){
            position += xDistance;
        }

        public boolean moveHandle(MotionEvent e1, float xDistance) {
            float f = e1.getX() + xDistance;
            if((position <= f) && (position+WIDTH) >= f){
                this.movePosition(-xDistance);

                return true;
            }
            return false;
        }

        public void setIsDragging(boolean v) {
            isDragging = v;
        }

        public boolean isDragging() {
            return isDragging;
        }
    }


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
        if (lowerSelectBoundary.getPosition() > 0 || upperSelectBoundary.getPosition() > 0) {
            float lowerBoundary = lowerSelectBoundary.getPosition();
            float upperBoundary = upperSelectBoundary.getPosition();
            Paint p = new Paint();
            p.setColor(SELECTION_COLOR);
            p.setAlpha(SELECTION_ALPHA);
            canvas.drawRect(lowerSelectBoundary.getPosition(), border, upperSelectBoundary.getPosition(), border + graphheight, p);
            lowerSelectBoundary.draw(canvas, graphwidth, graphheight, border, horstart, p);
            upperSelectBoundary.draw(canvas, graphwidth, graphheight, border, horstart, p);
        }
    }

    protected class SelectableBinGraphContentView extends GraphViewContentView {

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
            // setOnClickListener(this);
            mDetector = new GestureDetector(getContext(), new SelectableBinGestureListener());
            onValuesSelectedListener = listener;
        }


        @Override
        public boolean onTouchEvent(MotionEvent e) {
            if(e.getAction() == MotionEvent.ACTION_UP){
                lowerSelectBoundary.setIsDragging(false);
                upperSelectBoundary.setIsDragging(false);
            }
            if (mDetector.onTouchEvent(e)) {
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
                if (x > upperSelectBoundary.getPosition()) {
                    upperSelectBoundary.setPosition(x);
                } else if (x < lowerSelectBoundary.getPosition()) {
                    lowerSelectBoundary.setPosition(x);
                } else {
                    // if within boundaries, move the boundary that the touch
                    // was
                    // closer to
                    if ((upperSelectBoundary.getPosition() - x) < (x - lowerSelectBoundary.getPosition())) {
                        // touch was closer to upper,
                        upperSelectBoundary.setPosition(x);
                    } else if ((x - lowerSelectBoundary.getPosition()) < (upperSelectBoundary.getPosition() - x)) {
                        lowerSelectBoundary.setPosition(x);
                    }
                }
                redrawAll();
                notifyOnValuesSelectedListener();
                return true;
            }


            @Override
            public boolean onDown(MotionEvent e) {
                if(lowerSelectBoundary.contains(e)){
                    lowerSelectBoundary.setIsDragging(true);
                } else if(upperSelectBoundary.contains(e)){
                    upperSelectBoundary.setIsDragging(true);
                }
                lastTouchEventX = e.getX();
                //scrollingStarted = true;
                return true;
            }


            @Override
            public boolean
                onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                // onMoveGesture(e2.getX() - lastTouchEventX);
                if(lowerSelectBoundary.isDragging()){
                    lowerSelectBoundary.movePosition(-distanceX);
                } else if(upperSelectBoundary.isDragging()){
                    upperSelectBoundary.movePosition(-distanceX);
                } else {
                    onMoveGesture(-distanceX);
                    lastTouchEventX = e2.getX();
                }
                invalidate();
                notifyOnValuesSelectedListener();
                return true;
            }


            private void notifyOnValuesSelectedListener() {
                String tag = getClass().getName() + ":notifyOnValuesSelectedListener";
                if (onValuesSelectedListener != null) {
                    double lowerValueBoundary =
                        (viewportStart + (lowerSelectBoundary.getPosition() / getWidth()) * viewportSize);
                    double upperValueBoundary =
                        (viewportStart + (upperSelectBoundary.getPosition() / getWidth()) * viewportSize);
                    Log.d(tag, "calling OnValueSelectedListener. lowerSelectBoundary: "
                               + lowerValueBoundary + " upperSelectBoundary: " + upperValueBoundary);
                    onValuesSelectedListener.OnValuesSelected((long) lowerValueBoundary,
                                                              (long) upperValueBoundary);
                }
            }
        }

    }

}
