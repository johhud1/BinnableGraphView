package com.jjoe64.graphview;

import java.util.Timer;
import java.util.TimerTask;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import com.jjoe64.graphview.GraphViewSeries.GraphViewSeriesStyle;

public class SelectableBinBarGraphView extends BinBarGraphView {
	private static final int SELECTION_ALPHA = 60;
	private static final int SELECTION_COLOR = Color.RED;
	private static final long SEC = 1000;
	private static final String TAG = SelectableBinBarGraphView.class.getName();

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

		public void draw(Canvas canvas, float graphwidth, float graphheight,
				float border, float horstart, Paint p) {
			Paint paint = new Paint(p);
			paint.setColor(COLOR);
			canvas.drawRect(position, border - WIDTH, position + WIDTH, border
					+ graphheight, paint);
			handleRect = new Rect((int) position, (int) border - RECT_WIDTH,
					(int) position + RECT_WIDTH, (int) border);
			Paint handlePaint = new Paint(paint);
			handlePaint.setColor(HANDLE_COLOR);
			canvas.drawRect(handleRect, handlePaint);

		}

		public boolean contains(MotionEvent e) {
			boolean res = (position <= e.getX())
					&& ((position + WIDTH) >= e.getX());
			res |= handleRect.contains((int) e.getX(), (int) e.getY());
			return res;
		}

		public float getPosition() {
			return position;
		}

		public void setPosition(float newPos) {
			position = newPos;
		}

		public void movePosition(float xDistance) {
			position += xDistance;
		}

		public boolean moveHandle(MotionEvent e1, float xDistance) {
			float f = e1.getX() + xDistance;
			if ((position <= f) && (position + WIDTH) >= f) {
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

	public SelectableBinBarGraphView(Context context, String title,
			boolean isNumBins, long numBinsOrSize,
			OnValuesSelectedListener listener) {
		super(context, title, isNumBins, numBinsOrSize, true);
		graphViewContentView = new SelectableBinGraphContentView(context,
				listener);
		addView(graphViewContentView, new LayoutParams(
				LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT, 1));
	}

	@Override
	protected void drawSeries(Canvas canvas, GraphViewDataInterface[] values,
			float graphwidth, float graphheight, float border, double minX,
			double minY, double diffX, double diffY, float horstart,
			GraphViewSeriesStyle style) {
		super.drawSeries(canvas, values, graphwidth, graphheight, border, minX,
				minY, diffX, diffY, horstart, style);
		if (lowerSelectBoundary.getPosition() > 0
				|| upperSelectBoundary.getPosition() > 0) {
			float lowerBoundary = lowerSelectBoundary.getPosition();
			float upperBoundary = upperSelectBoundary.getPosition();
			Paint p = new Paint();
			p.setColor(SELECTION_COLOR);
			p.setAlpha(SELECTION_ALPHA);
			canvas.drawRect(lowerSelectBoundary.getPosition(), border,
					upperSelectBoundary.getPosition(), border + graphheight, p);
			lowerSelectBoundary.draw(canvas, graphwidth, graphheight, border,
					horstart, p);
			upperSelectBoundary.draw(canvas, graphwidth, graphheight, border,
					horstart, p);
		}
	}

	protected class SelectableBinGraphContentView extends GraphViewContentView
			implements GestureDetector.OnGestureListener {

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
		public SelectableBinGraphContentView(Context context,
				OnValuesSelectedListener listener) {
			super(context);
			// setOnClickListener(this);
			mDetector = new GestureDetector(getContext(),
					new SelectableBinGestureListener());
			mDetector.setIsLongpressEnabled(false);
			onValuesSelectedListener = listener;

		}

		// long press is half second press
		private static final long longPressTime = SEC / 2;
		private float pos;
		private Timer timer = new Timer("longPess_timer");
		Handler h = new GestureListenerHandler(this);

		@Override
		public boolean onTouchEvent(MotionEvent e) {
			if (e.getAction() == MotionEvent.ACTION_UP) {
				lowerSelectBoundary.setIsDragging(false);
				upperSelectBoundary.setIsDragging(false);
			}
			if (mDetector.onTouchEvent(e)) {
				return true;
			} else {
				// our own little longPressDetector, so we can long press
				// followed by scroll
				if (e.getAction() == MotionEvent.ACTION_DOWN) {
					Log.d(TAG + ":SelectableBinBarGraphContentView",
							"action event ACTION_DOWN");
					isLongPressTimerTask longPressTask = new isLongPressTimerTask(
							e.getX(), e.getY(), this, h);
					this.setOnTouchListener(longPressTask);
					timer.schedule(longPressTask, longPressTime);
					return true;
				}
			}

			return super.onTouchEvent(e);
		}

		private class GestureListenerHandler extends Handler {
			GestureDetector.OnGestureListener l;

			public GestureListenerHandler(GestureDetector.OnGestureListener l) {
				super();
				this.l = l;
			}

			@Override
			public void handleMessage(Message m) {
				float x = m.getData().getFloat("x");
				float y = m.getData().getFloat("y");
				long time = m.getData().getLong("eventTime");
				l.onLongPress(MotionEvent.obtain(longPressTime, time,
						MotionEvent.ACTION_DOWN, x, y, 0));
			}
		}

		private class isLongPressTimerTask extends TimerTask implements
				OnTouchListener {
			private boolean uninterruptedPress = true;
			private float x, y;
			Handler h;
			// max deviation, x or y distance from origin of press
			float difThresh = 5;

			public isLongPressTimerTask(float x, float y,
					GestureDetector.OnGestureListener l, Handler handler) {
				this.h = handler;
				this.x = x;
				this.y = y;
			}

			@Override
			public void run() {
				if (uninterruptedPress) {
					Bundle b = new Bundle();
					b.putFloat("x", x);
					b.putFloat("y", y);
					b.putLong("eventTime", SystemClock.uptimeMillis());
					Message m = Message.obtain();
					m.setData(b);
					h.sendMessage(m);
				}
			}

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				int a = event.getAction();
				if ((a == MotionEvent.ACTION_UP)
						|| (a == MotionEvent.ACTION_SCROLL)) {
					uninterruptedPress = false;
				}
				if (a == MotionEvent.ACTION_MOVE) {
					if ((Math.abs(event.getX() - x) > difThresh)
							&& (Math.abs(event.getY() - y) > difThresh)) {
						Log.v(TAG, "long press interrupted by MOVE");
						uninterruptedPress = false;
					}
				}
				return false;
			}
		}

		private class SelectableBinGestureListener extends
				GestureDetector.SimpleOnGestureListener {

			@Override
			public boolean onSingleTapUp(MotionEvent e) {
				String tag = getClass().getName() + ":onSinlgeTapUp";
				Log.d(TAG, " got a singleTapUp!");
				float x = e.getX();
				if (x > upperSelectBoundary.getPosition()) {
					upperSelectBoundary.setPosition(x);
				} else if (x < lowerSelectBoundary.getPosition()) {
					lowerSelectBoundary.setPosition(x);
				} else {
					// if within boundaries, move the boundary that the touch
					// was
					// closer to
					if ((upperSelectBoundary.getPosition() - x) < (x - lowerSelectBoundary
							.getPosition())) {
						// touch was closer to upper,
						upperSelectBoundary.setPosition(x);
					} else if ((x - lowerSelectBoundary.getPosition()) < (upperSelectBoundary
							.getPosition() - x)) {
						lowerSelectBoundary.setPosition(x);
					}
				}
				redrawAll();
				notifyOnValuesSelectedListener();
				return true;
			}

			@Override
			public boolean onDown(MotionEvent e) {
				if (lowerSelectBoundary.contains(e)) {
					lowerSelectBoundary.setIsDragging(true);
				} else if (upperSelectBoundary.contains(e)) {
					upperSelectBoundary.setIsDragging(true);
				}
				lastTouchEventX = e.getX();
				// scrollingStarted = true;
				return false;
			}

			@Override
			public boolean onScroll(MotionEvent e1, MotionEvent e2,
					float distanceX, float distanceY) {
				Log.d(TAG, "onScroll in SelectableBinGestuerListener");
				// onMoveGesture(e2.getX() - lastTouchEventX);
				if (lowerSelectBoundary.isDragging()) {
					lowerSelectBoundary.movePosition(-distanceX);
				} else if (upperSelectBoundary.isDragging()) {
					upperSelectBoundary.movePosition(-distanceX);
				} else {
					onMoveGesture(-distanceX);
					lastTouchEventX = e2.getX();
				}
				invalidate();
				notifyOnValuesSelectedListener();
				return true;
			}

		}

		private void notifyOnValuesSelectedListener() {
			String tag = getClass().getName()
					+ ":notifyOnValuesSelectedListener";
			if (onValuesSelectedListener != null) {
				double lowerValueBoundary = (viewportStart + (lowerSelectBoundary
						.getPosition() / getWidth()) * viewportSize);
				double upperValueBoundary = (viewportStart + (upperSelectBoundary
						.getPosition() / getWidth()) * viewportSize);
				Log.d(tag,
						"calling OnValueSelectedListener. lowerSelectBoundary: "
								+ lowerValueBoundary + " upperSelectBoundary: "
								+ upperValueBoundary);
				onValuesSelectedListener.OnValuesSelected(
						(long) lowerValueBoundary, (long) upperValueBoundary);
			}
		}

		@Override
		public boolean onDown(MotionEvent e) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public void onShowPress(MotionEvent e) {
			// TODO Auto-generated method stub

		}

		@Override
		public boolean onSingleTapUp(MotionEvent e) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean onScroll(MotionEvent e1, MotionEvent e2,
				float distanceX, float distanceY) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public void onLongPress(MotionEvent e) {
			Log.d(TAG, "onLongPress! Event: " + e);
			SelectRangeHandle closer = closer(e.getX(), lowerSelectBoundary,
					upperSelectBoundary);
			closer.setPosition(e.getX());
			closer.setIsDragging(true);
			invalidate();
			notifyOnValuesSelectedListener();
		}

		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
				float velocityY) {
			// TODO Auto-generated method stub
			return false;
		}

	}

	/**
	 * returns the {@link SelectRangeHandle} that is closer to the given
	 * position
	 * 
	 * @param pos
	 * @param h1
	 * @param h2
	 * @return
	 */
	protected SelectRangeHandle closer(float pos, SelectRangeHandle h1,
			SelectRangeHandle h2) {
		if (Math.abs(h1.getPosition() - pos) < Math.abs(pos - h2.getPosition())) {
			// touch was closer to h1
			return h1;
		} else if (Math.abs(h1.getPosition() - pos) > Math.abs(pos
				- h2.getPosition())) {
			return h2;
		} else {
			// they're equidistant wowowowo
			Log.e(this.getClass().getName() + ":closer",
					"handles were equidistant from touch event!");
			return h1;
		}
	}
}
