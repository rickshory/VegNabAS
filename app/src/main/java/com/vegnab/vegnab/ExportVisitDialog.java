package com.vegnab.vegnab;

import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.vegnab.vegnab.database.VNContract.LDebug;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class ExportVisitDialog extends DialogFragment implements android.view.View.OnClickListener {
    private static final String LOG_TAG = ExportVisitDialog.class.getSimpleName();

    public interface ExportVisitListener {
        void onExportVisitRequest(Bundle paramsBundle);
    }
    ExportVisitListener mExpVisListener;

    private long mVisToExportRecId = 0; // zero default means new or not specified yet
    private String mVisExportVisName = null;
    private String mVisExportFileName = null;
    private RadioGroup mResolvePhRadioGp;
    private RadioButton mResolvePhs;
    private RadioButton mUsePhsAsIs;

    private TextView mTxtHeader, mTxtFileNameToExport;
    SimpleDateFormat mTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
    Button mBtnCancel, mBtnExport;

    static ExportVisitDialog newInstance(Bundle args) {
        ExportVisitDialog f = new ExportVisitDialog();
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            mExpVisListener = (ExportVisitListener) getActivity();
           if (LDebug.ON) Log.d(LOG_TAG, "(ExportVisitListener) getActivity()");
        } catch (ClassCastException e) {
            throw new ClassCastException("Main Activity must implement ExportVisitListener interface");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup rootView, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_export_visit, rootView);
        mResolvePhRadioGp = (RadioGroup) view.findViewById(R.id.radio_group_opts_resolve_phs);
        mResolvePhRadioGp.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId) {
                    case R.id.radio_resolve_phs:
                       if (LDebug.ON) Log.d(LOG_TAG, "R.id.radio_resolve_phs");
                        break;
                    case R.id.radio_phs_asis:
                       if (LDebug.ON) Log.d(LOG_TAG, "R.id.radio_phs_asis");
                        break;
                    default:
                       if (LDebug.ON) Log.d(LOG_TAG, "neither radio button");
                        break;
                }
            }
        });

        mResolvePhs = (RadioButton) view.findViewById(R.id.radio_resolve_phs);
        mUsePhsAsIs = (RadioButton) view.findViewById(R.id.radio_phs_asis);

//        mTxtHeader = (TextView) view.findViewById(R.id.lbl_export_visit);
        mTxtFileNameToExport = (TextView) view.findViewById(R.id.lbl_export_visit_filename);

        mBtnCancel = (Button) view.findViewById(R.id.export_visit_cancel_button);
        mBtnCancel.setOnClickListener(this);
        mBtnExport = (Button) view.findViewById(R.id.export_visit_export_button);
        mBtnExport.setOnClickListener(this);

        getDialog().setTitle(R.string.export_visit_dlg_title_pre);
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        // during startup, check if arguments are passed to the fragment
        // this is where to do this because the layout has been applied
        // to the fragment
        Bundle args = getArguments();

        if (args != null) {
            mVisToExportRecId = args.getLong(MainVNActivity.ARG_VISIT_TO_EXPORT_ID);
            mVisExportVisName = args.getString(MainVNActivity.ARG_VISIT_TO_EXPORT_NAME);
            mVisExportFileName = args.getString(MainVNActivity.ARG_VISIT_TO_EXPORT_FILENAME);
            getDialog().setTitle(getText(R.string.export_visit_dlg_title_pre)
                    + " \"" + mVisExportVisName + "\"");
            mTxtFileNameToExport.setText(mVisExportFileName);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.export_visit_cancel_button:
                this.dismiss();
                break;

            case R.id.export_visit_export_button:
                Bundle expArgs = new Bundle();
                // for testing, send same args as received
                // in future, allow user to override
                expArgs.putLong(MainVNActivity.ARG_VISIT_TO_EXPORT_ID, mVisToExportRecId);
                expArgs.putString(MainVNActivity.ARG_VISIT_TO_EXPORT_NAME, mVisExportVisName);
                expArgs.putString(MainVNActivity.ARG_VISIT_TO_EXPORT_FILENAME, mVisExportFileName);
                // flag whether to resolve Placeholders
                boolean resolvePh = true;
                switch (mResolvePhRadioGp.getCheckedRadioButtonId()) {
                    case R.id.radio_resolve_phs:
                        resolvePh = true;
                       if (LDebug.ON) Log.d(LOG_TAG, "resolvePh radio button selected: R.id.radio_resolve_phs");
                        break;
                    case R.id.radio_phs_asis:
                        resolvePh = false;
                       if (LDebug.ON) Log.d(LOG_TAG, "resolvePh radio button selected: R.id.radio_phs_asis");
                        break;
                    default:
                        resolvePh = true;
                       if (LDebug.ON) Log.d(LOG_TAG, "resolvePh radio button selected: neither radio button");
                        break;
                }
                expArgs.putBoolean(MainVNActivity.ARG_RESOLVE_PLACEHOLDERS, resolvePh);
                // put any other parameters in, such as
                // format of output, etc.
                mExpVisListener.onExportVisitRequest(expArgs);
                this.dismiss();
                break;
        }
    }
}
