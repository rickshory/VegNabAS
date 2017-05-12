package com.vegnab.vegnab;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.widget.ResourceCursorAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.vegnab.vegnab.database.VNContract.LDebug;

import java.io.File;
import java.util.ArrayList;

public class PhPixGridArrayAdapter extends ArrayAdapter {
        //implements AdapterView.OnItemClickListener
    private static final String LOG_TAG = PhPixGridArrayAdapter.class.getSimpleName();

    private LayoutInflater mInflater;

    public PhPixGridArrayAdapter(Context ctx, int layout, ArrayList data) {
        super(ctx, layout, data);
        mInflater = (LayoutInflater) ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    // following commented-out techniques may give smoother scrolling, but code at bottom is OK for now
//	private Context context;
//	private int layoutResourceId;
//	private ArrayList data = new ArrayList();
//

//	public GridViewAdapter(Context context, int layoutResourceId, ArrayList data) {
//		super(context, layoutResourceId, data);
//		this.layoutResourceId = layoutResourceId;
//		this.context = context;
//		this.data = data;
//	}
//
    /*        public View getView(int position,View ConvertView,ViewGroup parent)
        {
            LayoutInflater inflater=(LayoutInflater)c.getSystemService(LAYOUT_INFLATER_SERVICE);
            ConvertView =inflater.inflate(R.layout.forgridmain, parent, false);
            ImageView iv1=(ImageView) ConvertView.findViewById(R.id.imageView111);
            TextView tv=(TextView)ConvertView.findViewById(R.id.textViewgd1);
            RelativeLayout rl=(RelativeLayout)ConvertView.findViewById(R.id.relalypout);
            rl.setBackgroundColor(Color.TRANSPARENT);
            iv1.setImageURI(Uri.parse("android.resource://"+getPackageName()+"/drawable/"+img[position]));
            tv.setText(p[position]);
            gv.setOnItemClickListener(ga);
            return ConvertView;

        }*/    // break the following apart to use with cursor instead of array
//	@Override
//	public View getView(int position, View convertView, ViewGroup parent) {
//		View row = convertView;
//		ViewHolder holder = null;
//
    // adapt the following for newView
//		if (row == null) {
//			LayoutInflater inflater = ((Activity) context).getLayoutInflater();
//			row = inflater.inflate(layoutResourceId, parent, false);
//			holder = new ViewHolder();
//			holder.imageTitle = (TextView) row.findViewById(R.id.phGridItemText);
//			holder.image = (ImageView) row.findViewById(R.id.phGridItemImage);
//			row.setTag(holder);
//		} else {
    // adapt the following for bindView
//			holder = (ViewHolder) row.getTag();
//		}
//
//		VNContract.VNGridImageItem item = data.get(position);
//		holder.imageTitle.setText(item.getTitle());
//		holder.image.setImageBitmap(item.getImage());
//		return row;
//	}
//
//	static class ViewHolder {
//		TextView imageTitle;
//		ImageView image;
//	}

    static class ViewHolder {
        ImageView image;
        TextView note;
    }

    @Override
    public void bindView(View v, Context ctx, Cursor c) {
        // available fields: PhotoPath, PlaceHolderID, PhotoTimeStamp, PhotoNotes, PhotoURL
        TextView phGridCellText = (TextView) v.findViewById(R.id.phGridItemText);
        String note = c.getString(c.getColumnIndexOrThrow("PhotoNotes"));
        if (note == null) {
            note = "(no note)";
        }
        phGridCellText.setText(note);

        ImageView phGridCellImage = (ImageView) v.findViewById(R.id.phGridItemImage);
        String path = c.getString(c.getColumnIndexOrThrow("PhotoPath"));
        File imgFile = new  File(path);
        if (imgFile.exists()) {
            // There isn't enough memory to open up more than a few camera photos
            // so pre-scale the target bitmap into which the file is decoded
            // Get the size of the ImageView
            int targetW = phGridCellImage.getWidth();
            int targetH = phGridCellImage.getHeight();
           if (LDebug.ON) Log.d(LOG_TAG, "grid cell image Ht " + targetH + ", Wd " + targetW);
            // for testing, manually override
            targetW = 100;
            targetH = 100;
            // Get the size of the image
            BitmapFactory.Options bmOptions = new BitmapFactory.Options();
            bmOptions.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(path, bmOptions);
            int photoW = bmOptions.outWidth;
            int photoH = bmOptions.outHeight;
            // determine the aspect ratio
            int scaleFactor = 1;
            if ((targetW > 0) || (targetH > 0)) {
                scaleFactor = Math.min(photoW/targetW, photoH/targetH);
            }
            // Set bitmap options to scale the image decode target
            bmOptions.inJustDecodeBounds = false;
            bmOptions.inSampleSize = scaleFactor;
            // bmOptions.inPurgeable = true;
            // Decode the JPEG file into a Bitmap
           if (LDebug.ON) Log.d(LOG_TAG, "Scale Factor " + scaleFactor + ", About to decode: " + path);
            Bitmap bitmap = BitmapFactory.decodeFile(path, bmOptions);
           if (LDebug.ON) Log.d(LOG_TAG, "bitmap Ht " + bitmap.getHeight() + ", width " + bitmap.getWidth());

            // associate the Bitmap to the ImageView
            phGridCellImage.setImageBitmap(bitmap);

//            Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
//            phGridCellImage.setImageBitmap(myBitmap);
//            phGridCellImage.setAdjustViewBounds(true);
        } else {
            // set bitmap to a not-found icon
        }
    }

    /*
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position,
                            long id) {
// above is from original example
    //Item click listener for pictures grid
    final AdapterView.OnItemClickListener mPixGrid_ItemClickListener = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            Context c = getActivity();
//            Toast.makeText(getActivity(), "Item Clicked: " + position + ", id=" + id, Toast.LENGTH_SHORT).show();
            mPixMatchCursor.moveToPosition(position);
            String path = mPixMatchCursor.getString(mPixMatchCursor.getColumnIndexOrThrow("PhotoPath"));
//            Toast.makeText(getActivity(), "" + path, Toast.LENGTH_SHORT).show();
            Uri uri = getImageContentUri(c, path);
//            Toast.makeText(getActivity(), "" + uri.toString(), Toast.LENGTH_SHORT).show();
            if (uri == null) {
                Toast.makeText(c, c.getResources().getString(R.string.ph_pix_grid_pic_no_file),
                        Toast.LENGTH_SHORT).show();
            } else {
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_VIEW);
                intent.setDataAndType(uri, "image/*");
                startActivity(intent);
            }
        }
    };
// below is from original example
        Intent trans=new Intent(MainActivity.this,Listed.class);
        trans.putExtra("first",p[arg2]);
        startActivity(trans);
    }
*/
}
