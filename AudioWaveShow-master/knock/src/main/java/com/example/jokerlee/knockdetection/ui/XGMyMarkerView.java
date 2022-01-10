
package com.example.jokerlee.knockdetection.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.widget.TextView;

import com.example.jokerlee.knockdetection.R;
import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.data.CandleEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.utils.MPPointF;
import com.github.mikephil.charting.utils.Utils;


/**
 * Custom implementation of the MarkerView.
 *
 * @author Philipp Jahoda
 */
@SuppressLint("ViewConstructor")
public class XGMyMarkerView extends MarkerView {

    private final TextView tvContent;

    public XGMyMarkerView(Context context, int layoutResource) {
        super(context, layoutResource);

        tvContent = findViewById(R.id.tvContent);
    }

    // runs every time the MarkerView is redrawn, can be used to update the
    // content (user-interface)
    @Override
    public void refreshContent(Entry e, Highlight highlight) {


        String textY = Utils.formatNumber(e.getY(), 0, true);
        String textX = Utils.formatNumber(e.getX(), 0, false);

        tvContent.setText(textX+","+textY);


        super.refreshContent(e, highlight);
    }

    @Override
    public MPPointF getOffset() {
        return new MPPointF(-(getWidth() / 2), -getHeight());
    }
}
