package com.vegnab.vegnab;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.ResourceCursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;

public class SelSppItemAdapter extends ResourceCursorAdapter {

    private LayoutInflater mInflater;

    public SelSppItemAdapter(Context ctx, int layout, Cursor c, int flags) {
        super(ctx, layout, c, flags);
        mInflater = (LayoutInflater) ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public void bindView(View v, Context ctx, Cursor c) {
// example of formatting by position
//		if(c.getPosition()%2==1) {
//			view.setBackgroundColor(ctx.getResources().getColor(R.color.background_odd));
//		} else {
//			view.setBackgroundColor(ctx.getResources().getColor(R.color.background_even));
//		}
        TextView sppText = (TextView) v.findViewById(R.id.spp_descr_text);
        sppText.setText(c.getString(c.getColumnIndexOrThrow("MatchTxt")));
        if (c.getInt(c.getColumnIndexOrThrow("IsPlaceholder")) == 1) {
            v.setBackgroundColor(ctx.getResources().getColor(R.color.vn_color));
        }
    }
}