package com.jjoe64.graphview;

import android.content.Context;
import android.graphics.Canvas;
import android.util.Log;

import com.jjoe64.graphview.GraphViewSeries.GraphViewSeriesStyle;

public class BinBarGraphView extends GraphView {
	private boolean isNumBins;
	private double binValue;
	private double remainder;
	/**
	 * i wanna call this the reverse remainder, probably has a real name. =
	 * (binSize - remainder)
	 **/
	private double rremainder;

	private static final long SEC = 1000;
	private static final long MIN = 60 * SEC;
	private static final long HOUR = 60 * MIN;
	private static final long DAY = 24 * HOUR;
	private static final long WEEK = 7 * DAY;

	/**
	 * This GraphView subclass will generate a Bar-style GraphView with the
	 * given number of bins, or bins of the given size. Binning the data
	 * appropriately, even while zooming. To clarify, if isNumBins is set to
	 * true; then when the user zooms, then number of bins in the graph will
	 * remain constant (equal to numBinsOrSize), and the size of the bins will
	 * change to keep the same number in the viewport as the viewport is
	 * resized. If isNumBins is false, then the range of the bins will remain
	 * constant while the viewport is resized.
	 * 
	 * @param context
	 * @param title
	 * @param isNumBins
	 *            True indicates the number of bins in view on the graph is held
	 *            constant regardless of changes in the viewport. False means
	 *            the size of the bins are held constant.
	 * @param numBinsOrSize
	 *            if isNumBins is true, indicates the number of bins you would
	 *            like to have the data grouped in, if numBins is false this
	 *            parameters determines the bin size.
	 */
	public BinBarGraphView(Context context, String title, boolean isNumBins,
			long numBinsOrSizes) {
		super(context, title);
		this.isNumBins = isNumBins;
		this.binValue = numBinsOrSizes;
	}

	public BinBarGraphView(Context context, String title, boolean isNumBins,
			long numBinsOrSize, boolean customGraphContentView) {
		super(context, title, customGraphContentView);
		this.isNumBins = isNumBins;
		this.binValue = numBinsOrSize;
	}

	public void setNumBins(int numBins) {
		isNumBins = true;
		this.binValue = numBins;
		invalidate();
	}

	public void setBinSize(long binSize) {
		isNumBins = false;
		binValue = binSize;
		redrawAll();
	}

	public double getBinSize() {
		if (!isNumBins) {
			return binValue;
		} else {
			return (viewportSize / binValue);
		}
	}

	@Override
	protected void drawSeries(Canvas canvas, GraphViewDataInterface[] values,
			float graphwidth, float graphheight, float border, double minX,
			double minY, double diffX, double diffY, float horstart,
			GraphViewSeriesStyle style) {
		double numBins = values.length - 1;
		/*
		 * if (isNumBins) { numBins = binValue; } else { numBins = diffX /
		 * binValue; }
		 */
		float colwidth = (float) (graphwidth / numBins);// (float) (graphwidth /
														// numBins);
		Log.d("drawSeries", "numBins = " + binValue + " binValue = " + binValue);

		paint.setStrokeWidth(style.thickness);
		paint.setColor(style.color);

		// values = binData(values, numBins);
		// draw data
		float remainderRatX = (float) (remainder / getBinSize());
		float rremainderRatX = (float) (rremainder / getBinSize());
		for (int i = 0; i < values.length; i++) {
			float valY = (float) (values[i].getY() - minY);
			float ratY = (float) (valY / diffY);
			float y = graphheight * ratY;

			// hook for value dependent color
			if (style.getValueDependentColor() != null) {
				paint.setColor(style.getValueDependentColor().get(values[i]));
			}
			if (i == 0) {
				canvas.drawRect(horstart, (border - y) + graphheight,
						((rremainderRatX * colwidth) + horstart - 1),
						graphheight + border - 1, paint);
				horstart += (rremainderRatX * colwidth);
			}
			if (i == values.length) {
				canvas.drawRect(
						((i - 1 + remainderRatX) * colwidth) + horstart,
						(border - y) + graphheight,
						(((i - 1) * colwidth) + horstart) + (colwidth - 1),
						graphheight + border - 1, paint);
			} else
				canvas.drawRect(((i - 1) * colwidth) + horstart, (border - y)
						+ graphheight, (((i - 1) * colwidth) + horstart)
						+ (colwidth - 1), graphheight + border - 1, paint);
		}
	}

	@Override
	protected GraphViewDataInterface[] _values(int idxSeries) {

		if (isNumBins) {
			// calculate binsize given binValue bins in graph
			double binSize = viewportSize / binValue;
			return binData(graphSeries.get(idxSeries).values, binSize);
		} else {
			// just bin data according to binsize
			return binData(graphSeries.get(idxSeries).values, binValue);
		}
	}

	private GraphViewData[] binData(GraphViewDataInterface[] data,
			double binSize) {

		// double range = data[data.length - 1].getX() - data[0].getX();
		double range = viewportSize;
		int numBins = (int) Math.ceil(range / binSize);
		return binData(data, numBins, binSize);
	}

	private GraphViewData[] binData(GraphViewDataInterface[] data, int numBins,
			double binSize) {
		remainder = viewportStart % binSize;
		rremainder = binSize - remainder;

		double curBinCeiling = viewportStart + rremainder;
		double curBinFloor = viewportStart - remainder;
		GraphViewData bins[] = new GraphViewData[numBins];
		int valuesIndex = 0;
		for (int i = 0; i < numBins; i++) {
			int y = 0;
			while ((valuesIndex < data.length)
					&& (data[valuesIndex].getX() < curBinFloor)) {
				valuesIndex++;
			}
			while ((valuesIndex < data.length)
					&& (data[valuesIndex].getX() < curBinCeiling)
					&& (data[valuesIndex].getX() >= curBinFloor)) {
				y++;
				valuesIndex++;
			}
			GraphViewData bin = new GraphViewData(curBinFloor, y);
			bins[i] = bin;
			curBinFloor = curBinCeiling;
			curBinCeiling += binSize;
		}
		return bins;
	}

	@Override
	protected String buildTitle() {
		String unitStr = "no unit";
		double unit = 0;
		if (isNumBins) {
			double binSize = viewportSize / binValue;
			if (binSize / MIN < 60) {
				unitStr = "min";
				unit = binSize / MIN;
			} else if (binSize / HOUR < 24) {
				unitStr = "hour";
				unit = binSize / HOUR;
			} else {
				unitStr = "days";
				unit = binSize / DAY;
			}
		} else {
			if ((binValue / MIN) < 60) {
				unitStr = "min";
				unit = binValue / MIN;
			} else if (binValue / HOUR < 24) {
				unitStr = "hour";
				unit = binValue / HOUR;
			} else {
				unitStr = "days";
				unit = binValue / DAY;
			}
		}
		return title + " (bin: " + String.format("%.2f", unit) + " " + unitStr
				+ " )";
	}
}
