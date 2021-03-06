package com.vegnab.vegnab;

import android.app.Activity;
import android.app.ActionBar;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.widget.Toolbar;
// android.app.DialogFragment; // maybe use this instead
import android.text.InputFilter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.vegnab.vegnab.database.VNContract;
import com.vegnab.vegnab.database.VNContract.LDebug;
import com.vegnab.vegnab.util.InputFilterMinMaxFloat;

public class LocManualEntryDialog extends DialogFragment {
    private int mValidationLevel = VNContract.Validation.CRITICAL;
    private static final String LOG_TAG = LocManualEntryDialog.class.getSimpleName();
    private TextView mManualLatitude, mManualLongitude, mManualAccuracy;
    private double mLatitude, mLongitude;
    private float mAccuracy;

    public interface LocManualEntryListener {
        // methods that must be implemented in the container Activity
        void onLocManualEntry(DialogFragment dialog, Bundle args);
    }
    // Use this instance of the interface to deliver action events
    LocManualEntryListener mLocManualEntryCallback; // declare the interface

    final static String ARG_TOOLBAR_HEADER = "hdrStr";

    static LocManualEntryDialog newInstance(Bundle args) {
        LocManualEntryDialog f = new LocManualEntryDialog();
        f.setArguments(args);
        return f;
    }

    // Test to make sure implemented:
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // assure the container activity has implemented the callback interface
        try {
            mLocManualEntryCallback = (LocManualEntryListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException (activity.toString()
                + " must implement LocManualEntryListener");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup rootView, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_loc_manual_entry, rootView);
        Toolbar toolbar = (Toolbar) view.findViewById(R.id.loc_manl_entry_toolbar);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    getDialog().onBackPressed();
                }
            });
            toolbar.setTitle(this.getArguments().getString(ARG_TOOLBAR_HEADER));
        }
        Button  saveButton = (Button) view.findViewById(R.id.btn_manl_loc_save);
        saveButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (validateManualLocValues()) {
                    Toast.makeText(getContext(),
                            "Validated OK",
                            Toast.LENGTH_LONG).show();
                    try { // can fail with null pointer exception if fragment is gone
                        Bundle args = new Bundle();
                        // send args generalized for VisitHeaderFragment
                        args.putDouble(VisitHeaderFragment.ARG_LOC_LATITUDE, mLatitude);
                        args.putDouble(VisitHeaderFragment.ARG_LOC_LONGITUDE, mLongitude);
                        args.putFloat(VisitHeaderFragment.ARG_LOC_ACCURACY, mAccuracy);
                        args.putString(VisitHeaderFragment.ARG_LOC_ACC_SOURCE, "User supplied");
                        args.putString(VisitHeaderFragment.ARG_LOC_PROVIDER, "Manual entry");

                        // We send the dialog only to dismiss it in the activity. Can we dismiss it here?
                        mLocManualEntryCallback.onLocManualEntry(LocManualEntryDialog.this, args);
                    } catch (Exception e) {
                        // ignore; if fails, will not update with manual entry
                    }
                } else {
                    Toast.makeText(getContext(),
                            "Did not validate",
                            Toast.LENGTH_LONG).show();
                }
            }
        });
        mManualLatitude = (EditText) view.findViewById(R.id.txt_manual_latitude);
        mManualLongitude = (EditText) view.findViewById(R.id.txt_manual_longitude);
        mManualAccuracy = (EditText) view.findViewById(R.id.txt_manual_accuracy);
        Bundle a = this.getArguments();
        if (a.containsKey(VisitHeaderFragment.ARG_LOC_LATITUDE))
            mManualLatitude.setText(""
                + a.getDouble(VisitHeaderFragment.ARG_LOC_LATITUDE));
        if (a.containsKey(VisitHeaderFragment.ARG_LOC_LONGITUDE))
            mManualLongitude.setText(""
                + a.getDouble(VisitHeaderFragment.ARG_LOC_LONGITUDE));
        if (a.containsKey(VisitHeaderFragment.ARG_LOC_ACCURACY)) {
            float Ac = a.getFloat(VisitHeaderFragment.ARG_LOC_ACCURACY);
            if (Ac == 0.0) {
                mManualAccuracy.setText("");
            } else {
                mManualAccuracy.setText("" + Ac);
            }
        }
        return view;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Remove dialog title
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        return dialog;
    }

    @Override
    public void onStart() {
        super.onStart();
        // during startup, check if arguments are passed to the fragment
        // this is where to do this because the layout has been applied
        // to the fragment
        Bundle args = getArguments();
        setupUI();
    }

    void setupUI() {
    }

    @Override
    public void onCancel (DialogInterface dialog) {
        //
        if (LDebug.ON) Log.d(LOG_TAG, "Verification in onCancel");

// maybe implement a listener
//        mSettingsListener.onSettingsComplete(LocManualEntryDialog.this);

    }

    private boolean validateManualLocValues() {
        // validate all user-accessible items
        Context c = getActivity();
        String stringProblem;
        String errTitle = c.getResources().getString(R.string.vis_hdr_validate_generic_title);
        ConfigurableMsgDialog flexErrDlg;

        double Lat, Lon;
        float Ac;
        // verify numeric Latitude, Longitude & Accuracy
        // validate Latitude
        String stringLat = mManualLatitude.getText().toString().trim();
        if (stringLat.length() == 0) {
            if (LDebug.ON) Log.d(LOG_TAG, "Latitude is length zero");
            if (mValidationLevel > VNContract.Validation.SILENT) {
                stringProblem = c.getResources().getString(R.string.loc_manual_entry_msg_no_latitude);
                if (mValidationLevel == VNContract.Validation.QUIET) {
                    Toast.makeText(this.getActivity(),
                            stringProblem,
                            Toast.LENGTH_LONG).show();
                }
                if (mValidationLevel == VNContract.Validation.CRITICAL) {
                    flexErrDlg = ConfigurableMsgDialog.newInstance(errTitle, stringProblem);
                    flexErrDlg.show(getFragmentManager(), "frg_err_latitude_out_of_range");
                    mManualLatitude.requestFocus();
                }
            } // end of validation not silent
            return false; // end of Lat length zero
        } else {
            try {
                Lat = Double.parseDouble(stringLat);
                if ((Lat < -90) || (Lat > 90)) { // latitude can only be +- 90 degrees
                    if (LDebug.ON) Log.d(LOG_TAG, "Latitude is out of range");
                    if (mValidationLevel > VNContract.Validation.SILENT) {
                        stringProblem = c.getResources().getString(R.string.loc_manual_entry_msg_latitude_bad);
                        if (mValidationLevel == VNContract.Validation.QUIET) {
                            Toast.makeText(this.getActivity(),
                                    stringProblem,
                                    Toast.LENGTH_LONG).show();
                        }
                        if (mValidationLevel == VNContract.Validation.CRITICAL) {
                            flexErrDlg = ConfigurableMsgDialog.newInstance(errTitle, stringProblem);
                            flexErrDlg.show(getFragmentManager(), "frg_err_latitude_out_of_range");
                            mManualLatitude.requestFocus();
                        }
                    } // end of validation not silent
                    return false; // end of Lat out of range
                }
            } catch(NumberFormatException e) {
                if (LDebug.ON) Log.d(LOG_TAG, "Latitude is not a valid number");
                if (mValidationLevel > VNContract.Validation.SILENT) {
                    stringProblem = c.getResources().getString(R.string.loc_manual_entry_msg_latitude_bad);
                    if (mValidationLevel == VNContract.Validation.QUIET) {
                        Toast.makeText(this.getActivity(),
                                stringProblem,
                                Toast.LENGTH_LONG).show();
                    }
                    if (mValidationLevel == VNContract.Validation.CRITICAL) {
                        flexErrDlg = ConfigurableMsgDialog.newInstance(errTitle, stringProblem);
                        flexErrDlg.show(getFragmentManager(), "frg_err_latitude_out_of_range");
                        mManualLatitude.requestFocus();
                    }
                } // end of validation not silent
                return false; // end of Lat invalid number
            }
        } // end of validate Latitude

        // validate Longitude
        String stringLon = mManualLongitude.getText().toString().trim();
        if (stringLon.length() == 0) {
            if (LDebug.ON) Log.d(LOG_TAG, "Longitude is length zero");
            if (mValidationLevel > VNContract.Validation.SILENT) {
                stringProblem = c.getResources().getString(R.string.loc_manual_entry_msg_no_longitude);
                if (mValidationLevel == VNContract.Validation.QUIET) {
                    Toast.makeText(this.getActivity(),
                            stringProblem,
                            Toast.LENGTH_LONG).show();
                }
                if (mValidationLevel == VNContract.Validation.CRITICAL) {
                    flexErrDlg = ConfigurableMsgDialog.newInstance(errTitle, stringProblem);
                    flexErrDlg.show(getFragmentManager(), "frg_err_longitude_out_of_range");
                    mManualLongitude.requestFocus();
                }
            } // end of validation not silent
            return false; // end of Lon length zero
        } else {
            try {
                Lon = Double.parseDouble(stringLon);
                if ((Lon < -180) || (Lon > 180)) { // Longitude can only be +-180 degrees
                    if (LDebug.ON) Log.d(LOG_TAG, "Longitude is out of range");
                    if (mValidationLevel > VNContract.Validation.SILENT) {
                        stringProblem = c.getResources().getString(R.string.loc_manual_entry_msg_longitude_bad);
                        if (mValidationLevel == VNContract.Validation.QUIET) {
                            Toast.makeText(this.getActivity(),
                                    stringProblem,
                                    Toast.LENGTH_LONG).show();
                        }
                        if (mValidationLevel == VNContract.Validation.CRITICAL) {
                            flexErrDlg = ConfigurableMsgDialog.newInstance(errTitle, stringProblem);
                            flexErrDlg.show(getFragmentManager(), "frg_err_longitude_out_of_range");
                            mManualLongitude.requestFocus();
                        }
                    } // end of validation not silent
                    return false; // end of Lon out of range
                }
            } catch(NumberFormatException e) {
                if (LDebug.ON) Log.d(LOG_TAG, "Longitude is not a valid number");
                if (mValidationLevel > VNContract.Validation.SILENT) {
                    stringProblem = c.getResources().getString(R.string.loc_manual_entry_msg_longitude_bad);
                    if (mValidationLevel == VNContract.Validation.QUIET) {
                        Toast.makeText(this.getActivity(),
                                stringProblem,
                                Toast.LENGTH_LONG).show();
                    }
                    if (mValidationLevel == VNContract.Validation.CRITICAL) {
                        flexErrDlg = ConfigurableMsgDialog.newInstance(errTitle, stringProblem);
                        flexErrDlg.show(getFragmentManager(), "frg_err_longitude_out_of_range");
                        mManualLongitude.requestFocus();
                    }
                } // end of validation not silent
                return false; // end of Lon invalid number
            }
        } // end of verify Longitude

        // validate Accuracy
        String stringAc = mManualAccuracy.getText().toString().trim();
        if (stringAc.length() == 0) { // valid to not put in any accuracy
            if (LDebug.ON) Log.d(LOG_TAG, "Accuracy is length zero");
            Ac = 0; // use default which means no accuracy given
        } else {
            try {
                Ac = Float.parseFloat(stringAc);
/*
                if ((Ac > 2000)) { // maybe limit accuracy
                    if (LDebug.ON) Log.d(LOG_TAG, "Accuracy input is > 2000");
                    if (mValidationLevel > VNContract.Validation.SILENT) {
                        stringProblem = c.getResources().getString(R.string.loc_manual_entry_msg_accuracy_bad);
                        if (mValidationLevel == VNContract.Validation.QUIET) {
                            Toast.makeText(this.getActivity(),
                                    stringProblem,
                                    Toast.LENGTH_LONG).show();
                        }
                        if (mValidationLevel == VNContract.Validation.CRITICAL) {
                            flexErrDlg = ConfigurableMsgDialog.newInstance(errTitle, stringProblem);
                            flexErrDlg.show(getFragmentManager(), "frg_err_accuracy_out_of_range");
                            mManualLatitude.requestFocus();
                        }
                    } // end of validation not silent
                    return false; // end of Ac out of range
                }
*/
            } catch(NumberFormatException e) {
                if (LDebug.ON) Log.d(LOG_TAG, "Accuracy is not a valid number");
                if (mValidationLevel > VNContract.Validation.SILENT) {
                    stringProblem = c.getResources().getString(R.string.loc_manual_entry_msg_accuracy_bad);
                    if (mValidationLevel == VNContract.Validation.QUIET) {
                        Toast.makeText(this.getActivity(),
                                stringProblem,
                                Toast.LENGTH_LONG).show();
                    }
                    if (mValidationLevel == VNContract.Validation.CRITICAL) {
                        flexErrDlg = ConfigurableMsgDialog.newInstance(errTitle, stringProblem);
                        flexErrDlg.show(getFragmentManager(), "frg_err_accuracy_out_of_range");
                        mManualAccuracy.requestFocus();
                    }
                } // end of validation not silent
                return false; // end of Ac invalid number
            }
        } // end of validate Accuracy

        mLatitude = Lat;
        mLongitude = Lon;
        mAccuracy = Ac;
        mManualLatitude.setText("" + Lat);
        mManualLongitude.setText("" + Lon);
        if (Ac == 0.0)
            mManualAccuracy.setText("");
        else
            mManualAccuracy.setText("" + Ac);

        return true;
    } // end of validation
}
