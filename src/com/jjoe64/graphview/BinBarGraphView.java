package com.jjoe64.graphview;

import android.content.Context;
import android.graphics.Canvas;

import com.jjoe64.graphview.GraphViewSeries.GraphViewSeriesStyle;
import com.jjoe64.graphview.SelectableBinBarGraphView.SelectableBinGraphContentView;




public class BinBarGraphView extends GraphView {
    private boolean isNumBins;
    private long binValue;

    public BinBarGraphView(Context context, String title, boolean isNumBins) {
        super(context, title);
        this.isNumBins = isNumBins;
        binValue = 20;
    }


    /**
     * This GraphView subclass will generate a Bar-style GraphView with the
     * given number of bins, binning the data appropriately.
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
    public BinBarGraphView(Context context, String title, boolean isNumBins, long numBinsOrSizes) {
        super(context, title);
        this.isNumBins = isNumBins;
        this.binValue = numBinsOrSizes;
    }


    public BinBarGraphView(Context context, String title, boolean isNumBins, long numBinsOrSize,
                           boolean customGraphContentView) {
        super(context, title, customGraphContentView);
        this.isNumBins = isNumBins;
        this.binValue = numBinsOrSize;
    }


    public void setNumBins(int numBins) {
        isNumBins = true;
        this.binValue = numBins;
        invalidate();
    }


    public void setBinSize(int binSize) {
        isNumBins = false;
        binValue = binSize;
    }


    @Override
    protected void
        drawSeries(Canvas canvas, GraphViewDataInterface[] values, float graphwidth,
                   float graphheight, float border, double minX, double minY, double diffX,
                   double diffY, float horstart, GraphViewSeriesStyle style) {
        float colwidth;
        if (isNumBins) {
            colwidth = (graphwidth - (2 * border)) / binValue;
        } else {
            double numBins = diffX / binValue;
            colwidth = (float) (graphwidth / numBins);
        }

        paint.setStrokeWidth(style.thickness);
        paint.setColor(style.color);
        // values = binData(values, numBins);
        // draw data
        for (int i = 0; i < values.length; i++) {
            float valY = (float) (values[i].getY() - minY);
            float ratY = (float) (valY / diffY);
            float y = graphheight * ratY;

            // hook for value dependent color
            if (style.getValueDependentColor() != null) {
                paint.setColor(style.getValueDependentColor().get(values[i]));
            }

            canvas.drawRect((i * colwidth) + horstart, (border - y) + graphheight,
                            ((i * colwidth) + horstart) + (colwidth - 1), graphheight + border - 1,
                            paint);
        }
    }


    @Override
    protected GraphViewDataInterface[] _values(int idxSeries) {
        if (isNumBins) {
            return binData(super._values(idxSeries), (int) binValue);
        } else {
            return binData(super._values(idxSeries), binValue);
        }
    }


    public static GraphViewData[] binData(GraphViewDataInterface[] data, double binSize) {

        double range = data[data.length - 1].getX() - data[0].getX();
        int numBins = (int) (range / binSize) + 1;
        return binData(data, numBins);
    }


    public static GraphViewData[] binData(GraphViewDataInterface[] data, int numBins) {

        double range = data[data.length - 1].getX() - data[0].getX();
        double binSize = range / numBins;
        double curBinCeiling = data[0].getX() + binSize;
        double curBinFloor = data[0].getX();
        GraphViewData bins[] = new GraphViewData[numBins];
        int valuesIndex = 0;
        for (int i = 0; i < numBins; i++) {
            int y = 0;

            while ((valuesIndex < data.length) && (data[valuesIndex].getX() < curBinCeiling)
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


}
