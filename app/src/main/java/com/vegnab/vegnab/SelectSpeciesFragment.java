package com.vegnab.vegnab;

import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.vegnab.vegnab.contentprovider.ContentProvider_VegNab;
import com.vegnab.vegnab.database.VNContract.Loaders;
import com.vegnab.vegnab.database.VNContract.LDebug;
import com.vegnab.vegnab.database.VNContract.VegcodeSources;
import com.vegnab.vegnab.database.VNContract.VNRegex;
import com.vegnab.vegnab.database.VNContract.VNConstraints;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;

import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class SelectSpeciesFragment extends ListFragment 
        implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String LOG_TAG = SelectSpeciesFragment.class.getSimpleName();
    final static String ARG_VISIT_ID = "visId";
    final static String ARG_SUBPLOT_TYPE_ID = "sbpId";
    final static String ARG_PRESENCE_ONLY_SUBPLOT = "presenceOnly";
    final static String ARG_PICK_PLACEHOLDER = "pickPlaceholder";
    final static String ARG_PROJECT_ID = "projectId";
    final static String ARG_NAMER_ID = "namerId";
    final static String ARG_SEARCH_TEXT = "search_text";

    long mCurVisitRecId = 0;
    long mCurSubplotTypeRecId = 0;
    boolean mPresenceOnly = true;
    boolean mPickPlaceholder = false;
    long mProjectId = 0;
    long mNamerId = 0;
    Cursor mSppMatchCursor;
    ContentValues mValues = new ContentValues();

    SelSppItemAdapter mSppResultsAdapter;
    TextView mViewForEmptyList;

    // declare an interface the container Activity must implement
    public interface OnEditPlaceholderListener {
        // methods that must be implemented in the container Activity
        void onEditPlaceholder(Bundle args);
    }
    OnEditPlaceholderListener mEditPlaceholderCallback; // declare the interface

    public interface OnPlaceholderRequestListener {
        // methods that must be implemented in the container Activity
        void onRequestGenerateExistingPlaceholders(Bundle args);
        long onRequestGetCountOfExistingPlaceholders();
        boolean onRequestMatchCheckOfExistingPlaceholders(String ph);
    }
    OnPlaceholderRequestListener mPlaceholderRequestListener;

    long mRowCt;
    String mStSearch = "", mStMatch = "", mPhCodeFixed = "";
    EditText mViewSearchChars;
    CheckBox mViewPickPhs;
//	ListView mSppItemsList;
    TextWatcher sppCodeTextWatcher = new TextWatcher() {
        @Override
        public void afterTextChanged(Editable s) {
            mPickPlaceholder = false; // pop out of this mode on any keystroke
            mViewPickPhs.setChecked(false);
            // use this method; test length of string; e.g. 'count' of other methods does not give this length
            //Log.d(LOG_TAG, "afterTextChanged, s: '" + s.toString() + "'");
           if (LDebug.ON) Log.d(LOG_TAG, "afterTextChanged, s: '" + s.toString() + "', length: " + s.length());
            mStSearch = s.toString();
            if (mStSearch.trim().length() == 0) {
                mViewForEmptyList.setText(
                        getActivity().getResources().getString(R.string.sel_spp_search_msg_empty_list));
                mSppResultsAdapter.swapCursor(null);
            } else {
                mViewForEmptyList.setText(
                        getActivity().getResources().getString(R.string.sel_spp_search_msg_not_finished));
                mSppResultsAdapter.swapCursor(null);
                getLoaderManager().restartLoader(Loaders.SPP_MATCHES, null, SelectSpeciesFragment.this);
            }
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            // the 'count' characters beginning at 'start' are about to be replaced by new text with length 'after'
            //Log.d(LOG_TAG, "beforeTextChanged, s: '" + s.toString() + "', start: " + start + ", count: " + count + ", after: " + after);
            //
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            // the 'count' characters beginning at 'start' have just replaced old text that had length 'before'
            //Log.d(LOG_TAG, "onTextChanged, s: '" + s.toString() + "', start: " + start + ", before: " + before + ", count: " + count);

        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        // if the activity was re-created (e.g. from a screen rotate)
        // restore the previous screen, remembered by onSaveInstanceState()
        // This is mostly needed in fixed-pane layouts
        if (savedInstanceState != null) {
            // restore search text and any search options
            mCurVisitRecId = savedInstanceState.getLong(ARG_VISIT_ID);
            mCurSubplotTypeRecId = savedInstanceState.getLong(ARG_SUBPLOT_TYPE_ID);
            mPresenceOnly = savedInstanceState.getBoolean(ARG_PRESENCE_ONLY_SUBPLOT);
            mPickPlaceholder = savedInstanceState.getBoolean(ARG_PICK_PLACEHOLDER);
            mStSearch = savedInstanceState.getString(ARG_SEARCH_TEXT);
            mProjectId = savedInstanceState.getLong(ARG_PROJECT_ID);
            mNamerId = savedInstanceState.getLong(ARG_NAMER_ID);
        }
        // inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_sel_species, container, false);
        mViewSearchChars = (EditText) rootView.findViewById(R.id.txt_search_chars);
        mViewPickPhs = (CheckBox) rootView.findViewById(R.id.ck_pick_ph);

        mViewSearchChars.addTextChangedListener(sppCodeTextWatcher);
        registerForContextMenu(mViewSearchChars); // enable long-press

        mViewForEmptyList = (TextView) rootView.findViewById(android.R.id.empty);

        // use query to return 'MatchTxt', concatenated from code and description; more reading room
        mSppResultsAdapter = new SelSppItemAdapter(getActivity(),
                R.layout.list_spp_search_item, null, 0);
        setListAdapter(mSppResultsAdapter);
        getLoaderManager().initLoader(Loaders.SPP_MATCHES, null, this);

        return rootView;
    }

        @Override
    public void onStart() {
        super.onStart();
        // during startup, check if arguments are passed to the fragment
        // this is where to do this because the layout has been applied
        // to the fragment
        Bundle args = getArguments();

        if (args != null) {
            mCurVisitRecId = args.getLong(ARG_VISIT_ID);
            mCurSubplotTypeRecId = args.getLong(ARG_SUBPLOT_TYPE_ID);
            mPresenceOnly = args.getBoolean(ARG_PRESENCE_ONLY_SUBPLOT);
            mPickPlaceholder = args.getBoolean(ARG_PICK_PLACEHOLDER);
            mStSearch = args.getString(ARG_SEARCH_TEXT, "");
            mProjectId = args.getLong(ARG_PROJECT_ID);
            mNamerId = args.getLong(ARG_NAMER_ID);
        }
        // get the ProjectID and NamerID for this Visit
        getLoaderManager().initLoader(Loaders.VISIT_INFO, null, this);
        // when above is done, will call fetchExistingPlaceholders();

        mViewSearchChars.requestFocus();

        mViewSearchChars.postDelayed(new Runnable() {
            @Override
            public void run() {
                // update mViewSearchChars if a newly created placeholder code was remotely edited
                if (mPhCodeFixed.length() > 0) {
                    mViewSearchChars.setText(mPhCodeFixed);
                    mPhCodeFixed = "";
                }
                InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(mViewSearchChars, 0);
            }
        }, 50);

        // set up the list to receive long-press
        registerForContextMenu(getListView());
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        // assure the container activity has implemented the callback interfaces
        if (context instanceof Activity) {
            Activity a = (Activity) context;
            try {
                mEditPlaceholderCallback = (OnEditPlaceholderListener) a;
            } catch (ClassCastException e) {
                throw new ClassCastException(a.toString() + " must implement OnEditPlaceholderListener");
            }
            try {
                mPlaceholderRequestListener = (OnPlaceholderRequestListener) a;
            } catch (ClassCastException e) {
                throw new ClassCastException(a.toString() + " must implement OnPlaceholderRequestListener");
            }
        }
    }

    @Override
    public void onPause(){
        if (LDebug.ON) Log.d(LOG_TAG, "in 'onPause'; mPickPlaceholder "
                + (mPickPlaceholder ? "true" : "false") + "; mStSearch: '" + mStSearch + "'" );
        super.onPause();
        if (LDebug.ON) Log.d(LOG_TAG, "in 'onPause' after super; mPickPlaceholder "
                + (mPickPlaceholder ? "true" : "false") + "; mStSearch: '" + mStSearch + "'" );
    }

    @Override
    public void onResume(){
        if (LDebug.ON) Log.d(LOG_TAG, "in 'onResume'; mPickPlaceholder "
                + (mPickPlaceholder ? "true" : "false") + "; mStSearch: '" + mStSearch + "'" );
        super.onResume();
        mStSearch = mViewSearchChars.getText().toString();
        mPickPlaceholder = mViewPickPhs.isChecked();
        if (LDebug.ON) Log.d(LOG_TAG, "in 'onResume' after super and getText; mPickPlaceholder "
                + (mPickPlaceholder ? "true" : "false") + "; mStSearch: '" + mStSearch + "'" );
        refreshMatchList(); // if Placeholders were IDd, show changes
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // save the current search text and any options
        outState.putString(ARG_SEARCH_TEXT, mStSearch);
        outState.putLong(ARG_VISIT_ID, mCurVisitRecId);
        outState.putBoolean(ARG_PICK_PLACEHOLDER, mPickPlaceholder);
        outState.putLong(ARG_SUBPLOT_TYPE_ID, mCurSubplotTypeRecId);
        outState.putBoolean(ARG_PRESENCE_ONLY_SUBPLOT, mPresenceOnly);
        outState.putLong(ARG_PROJECT_ID, mProjectId);
        outState.putLong(ARG_NAMER_ID, mNamerId);
    }

    @Override
    public void onListItemClick(ListView l, View v, int pos, long id) {
//        Toast.makeText(this.getActivity(), "Clicked position " + pos + ", id " + id, Toast.LENGTH_SHORT).show();
        // check if selected code is in mVegCodesAlreadyOnSubplot
//    	getListView().getItemAtPosition(pos).toString(); // not useful, gets cursor wrapper
        mSppMatchCursor.moveToPosition(pos);
// available fields: _id, Code, Genus, Species, SubsppVar, Vernacular, MatchTxt, SubListOrder, IsPlaceholder
        String vegCode = mSppMatchCursor.getString(
                mSppMatchCursor.getColumnIndexOrThrow("Code"));
       if (LDebug.ON) Log.d(LOG_TAG, "mSppMatchCursor, pos = " + pos + " SppCode: " + vegCode);
        String vegDescr = mSppMatchCursor.getString(
                mSppMatchCursor.getColumnIndexOrThrow("MatchTxt"));
        String vegGenus = mSppMatchCursor.getString(
                mSppMatchCursor.getColumnIndexOrThrow("Genus"));
        String vegSpecies = mSppMatchCursor.getString(
                mSppMatchCursor.getColumnIndexOrThrow("Species"));
        String vegSubsppVar = mSppMatchCursor.getString(
                mSppMatchCursor.getColumnIndexOrThrow("SubsppVar"));
        String vegVernacular = mSppMatchCursor.getString(
                mSppMatchCursor.getColumnIndexOrThrow("Vernacular"));
        int vegSubListOrder = mSppMatchCursor.getInt(
                mSppMatchCursor.getColumnIndexOrThrow("SubListOrder"));
        int vegIsPlaceholder = mSppMatchCursor.getInt(
                mSppMatchCursor.getColumnIndexOrThrow("IsPlaceholder"));

       if (LDebug.ON) Log.d(LOG_TAG, "about to dispatch 'EditSppItemDialog' dialog to create new record");
        Bundle args = new Bundle();
        args.putLong(EditSppItemDialog.VEG_ITEM_REC_ID, 0); // don't need this, default is in class
        args.putLong(EditSppItemDialog.CUR_VISIT_REC_ID, mCurVisitRecId);
        args.putLong(EditSppItemDialog.CUR_SUBPLOT_REC_ID, mCurSubplotTypeRecId);
        if (vegIsPlaceholder == 1) {
            args.putInt(EditSppItemDialog.REC_SOURCE, VegcodeSources.PLACE_HOLDERS);
        } else {
            args.putInt(EditSppItemDialog.REC_SOURCE, VegcodeSources.REGIONAL_LIST);
        }
        args.putLong(EditSppItemDialog.SOURCE_REC_ID, id);
        args.putBoolean(EditSppItemDialog.PRESENCE_ONLY, mPresenceOnly);
        // streamline this, get directly from cursor
        args.putString(EditSppItemDialog.VEG_CODE, vegCode);
        args.putString(EditSppItemDialog.VEG_DESCR, vegDescr);
        args.putString(EditSppItemDialog.VEG_GENUS, vegGenus);
        args.putString(EditSppItemDialog.VEG_SPECIES, vegSpecies);
        args.putString(EditSppItemDialog.VEG_SUBSPP_VAR, vegSubsppVar);
        args.putString(EditSppItemDialog.VEG_VERNACULAR, vegVernacular);
        args.putInt(EditSppItemDialog.VEG_SUB_LIST_ORDER, vegSubListOrder);
        args.putInt(EditSppItemDialog.VEG_IS_PLACEHOLDER, vegIsPlaceholder);

        EditSppItemDialog newVegItemDlg = EditSppItemDialog.newInstance(args);

        newVegItemDlg.show(getFragmentManager(), "frg_new_veg_item");
    }

    // create context menus
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
       ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getActivity().getMenuInflater();
        switch (v.getId()) {
        case R.id.txt_search_chars:
            inflater.inflate(R.menu.context_sel_spp_search_chars, menu);
            if (mStSearch.trim().length() == 0) {
                // can't add placeholder if no text yet to use
                menu.removeItem(R.id.sel_spp_search_add_placeholder);
            }
            if (mPlaceholderRequestListener
                    .onRequestGetCountOfExistingPlaceholders() == 0) {
                // if no placeholders, don't show option to pick from them
                menu.removeItem(R.id.sel_spp_search_pick_placeholder);
            }
            break;
		case android.R.id.list:
            inflater.inflate(R.menu.context_sel_spp_list_items, menu);
            // try to remove items not relevant to the selection
            AdapterView.AdapterContextMenuInfo info;
            try {
                // Casts the incoming data object into the type for AdapterView objects.
                info = (AdapterView.AdapterContextMenuInfo) menuInfo;
            } catch (ClassCastException e) {
                if (LDebug.ON) Log.d(LOG_TAG, "bad menuInfo", e); // if the menu object can't be cast
                break;
            }
            mSppMatchCursor.moveToPosition(info.position);
            int isPlaceHolder = mSppMatchCursor.getInt(
                    mSppMatchCursor.getColumnIndexOrThrow("IsPlaceholder"));
            if (isPlaceHolder == 0) {
                // if not a Placeholder, the 'edit Placeholder' option does not apply
                menu.removeItem(R.id.sel_spp_list_item_edit_ph);
            }
            int subListOrder = mSppMatchCursor.getInt(
                    mSppMatchCursor.getColumnIndexOrThrow("SubListOrder"));
            if ((isPlaceHolder == 1) || (subListOrder > 2)) {
                // a Placeholder, or a defined species not previously found
                // option to forget its top relevance does not apply
                menu.removeItem(R.id.sel_spp_list_item_forget);
            }
			break;
        }
    }

    // This is executed when the user selects an option
    @Override
    public boolean onContextItemSelected(MenuItem item) {
//    AdapterViewCompat.AdapterContextMenuInfo info = (AdapterViewCompat.AdapterContextMenuInfo) item.getMenuInfo();
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        if (info == null) {
           if (LDebug.ON) Log.d(LOG_TAG, "onContextItemSelected info is null");
        } else {
           if (LDebug.ON) Log.d(LOG_TAG, "onContextItemSelected info: " + info.toString());
        }
        Context c = getActivity();
        UnderConstrDialog notYetDlg = new UnderConstrDialog();
        HelpUnderConstrDialog hlpDlg = new HelpUnderConstrDialog();
        ConfigurableMsgDialog flexHlpDlg = new ConfigurableMsgDialog();
        String helpTitle, helpMessage;
        Bundle phArgs = new Bundle();
        int itemIsPlaceholder;


        // get an Analytics event tracker
        Tracker headerContextTracker = ((VNApplication) getActivity().getApplication()).getTracker(VNApplication.TrackerName.APP_TRACKER);

        switch (item.getItemId()) {

            case R.id.sel_spp_search_add_placeholder:
               if (LDebug.ON) Log.d(LOG_TAG, "'Create Placeholder' selected");
                headerContextTracker.send(new HitBuilders.EventBuilder()
                        .setCategory("Species Select Event")
                        .setAction("Context Menu")
                        .setLabel("Create Placeholder")
                        .setValue(1)
                        .build());
                // add or edit Placeholder
                String phCode, wholeContents = mStSearch.trim();
                Boolean wasShortened;
                if (wholeContents.length() <= VNConstraints.PLACEHOLDER_MAX_LENGTH) {
                  phCode = wholeContents;
                    wasShortened = false;
                } else {
                    phCode = wholeContents.substring(0, VNConstraints.PLACEHOLDER_MAX_LENGTH).trim();
                    wasShortened = true;
                }

//                Toast.makeText(this.getActivity(), "Placeholder code '" + phCode + "'", Toast.LENGTH_SHORT).show();
                if (phCode.length() < 3) {
//                    Toast.makeText(this.getActivity(), "Placeholder codes must be at least 3 characters long.", Toast.LENGTH_SHORT).show();
                    Toast.makeText(this.getActivity(), c.getResources().getString(R.string.placeholder_validate_code_short), Toast.LENGTH_SHORT).show();
                    return true;
                }

                if (phCode.matches(VNRegex.NRCS_CODE)) { // see VNContract for details
//                    Toast.makeText(this.getActivity(), "Placeholder can\'t be like an NRCS code.", Toast.LENGTH_SHORT).show();
                    Toast.makeText(this.getActivity(), c.getResources().getString(R.string.placeholder_validate_code_bad), Toast.LENGTH_SHORT).show();
                    return true;
                }

                if (mPlaceholderRequestListener
                        .onRequestMatchCheckOfExistingPlaceholders(phCode)) {
                    Toast.makeText(this.getActivity(),
                            c.getResources().getString(R.string.placeholder_validate_code_dup),
                            Toast.LENGTH_SHORT).show();
                    return true;
                }

                phArgs.putLong(EditPlaceholderFragment.ARG_PLACEHOLDER_ID, 0); // new, may not be needed
                phArgs.putLong(EditPlaceholderFragment.ARG_PH_PROJECT_ID, mProjectId);
                phArgs.putLong(EditPlaceholderFragment.ARG_PH_NAMER_ID, mNamerId);
                phArgs.putString(EditPlaceholderFragment.ARG_PLACEHOLDER_CODE, phCode);
                phArgs.putBoolean(EditPlaceholderFragment.ARG_CODE_WAS_SHORTENED, wasShortened);
                mEditPlaceholderCallback.onEditPlaceholder(phArgs);
                return true;

            case R.id.sel_spp_search_pick_placeholder:
                headerContextTracker.send(new HitBuilders.EventBuilder()
                        .setCategory("Species Select Event")
                        .setAction("Context Menu")
                        .setLabel("Pick Placeholder")
                        .setValue(1)
                        .build());
                mPickPlaceholder = true;
                mViewPickPhs.setChecked(true);
                getLoaderManager().restartLoader(Loaders.SPP_MATCHES, null, SelectSpeciesFragment.this);
                return true;

            case R.id.sel_spp_search_help:
               if (LDebug.ON) Log.d(LOG_TAG, "'Search Chars Help' selected");
                headerContextTracker.send(new HitBuilders.EventBuilder()
                        .setCategory("Species Select Event")
                        .setAction("Context Menu")
                        .setLabel("Search Chars Help")
                        .setValue(1)
                        .build());
                // Search Characters help
                helpTitle = c.getResources().getString(R.string.sel_spp_help_search_title);
                helpMessage = c.getResources().getString(R.string.sel_spp_help_search_text);
                flexHlpDlg = ConfigurableMsgDialog.newInstance(helpTitle, helpMessage);
                flexHlpDlg.show(getFragmentManager(), "frg_help_search_chars");
                return true;

            case R.id.sel_spp_list_item_forget:
               if (LDebug.ON) Log.d(LOG_TAG, "Spp list item 'Forget Species' selected");
                headerContextTracker.send(new HitBuilders.EventBuilder()
                        .setCategory("Species Select Event")
                        .setAction("Context Menu")
                        .setLabel("List Item Forget Species")
                        .setValue(1)
                        .build());
                // Forget remembered species
                if (info == null) {
                    Toast.makeText(getActivity(),
                            c.getResources().getString(R.string.sel_spp_list_ctx_forget_not_spp),
                            Toast.LENGTH_SHORT).show();
                    return true;
                }
                mSppMatchCursor.moveToPosition(info.position);
                itemIsPlaceholder = mSppMatchCursor.getInt(
                        mSppMatchCursor.getColumnIndexOrThrow("IsPlaceholder"));
                if (itemIsPlaceholder == 1) {
                    Toast.makeText(getActivity(),
                            c.getResources().getString(R.string.sel_spp_list_ctx_forget_not_spp),
                            Toast.LENGTH_SHORT).show();
                    return true;
                }
                forgetSppMatch(info.id);
                return true;

            case R.id.sel_spp_list_item_edit_ph:
               if (LDebug.ON) Log.d(LOG_TAG, "Spp list item 'Edit Placeholder' selected");
                headerContextTracker.send(new HitBuilders.EventBuilder()
                        .setCategory("Species Select Event")
                        .setAction("Context Menu")
                        .setLabel("List Item Edit Placeholder")
                        .setValue(1)
                        .build());
//                // Search Characters help
//                helpTitle = "Edit";
//                helpMessage = "Edit Placeholder tapped";
//                flexHlpDlg = ConfigurableMsgDialog.newInstance(helpTitle, helpMessage);
//                flexHlpDlg.show(getFragmentManager(), "frg_spp_item_edit_ph");

                // Edit placeholder
                if (info == null) {
                    Toast.makeText(getActivity(),
                            c.getResources().getString(R.string.sel_spp_list_ctx_edit_ph_not),
                            Toast.LENGTH_SHORT).show();
                    return true;
                }
                mSppMatchCursor.moveToPosition(info.position);
                itemIsPlaceholder = mSppMatchCursor.getInt(
                        mSppMatchCursor.getColumnIndexOrThrow("IsPlaceholder"));
                if (itemIsPlaceholder != 1) {
                    Toast.makeText(getActivity(),
                            c.getResources().getString(R.string.sel_spp_list_ctx_edit_ph_not),
                            Toast.LENGTH_SHORT).show();
                    return true;
                }
                phArgs.putLong(EditPlaceholderFragment.ARG_PLACEHOLDER_ID, info.id);
                phArgs.putString(EditPlaceholderFragment.ARG_PLACEHOLDER_CODE, mSppMatchCursor.getString(
                        mSppMatchCursor.getColumnIndexOrThrow("Code")));
                phArgs.putBoolean(EditPlaceholderFragment.ARG_CODE_WAS_SHORTENED, false);
                mEditPlaceholderCallback.onEditPlaceholder(phArgs);
                return true;

            case R.id.sel_spp_list_item_help:
               if (LDebug.ON) Log.d(LOG_TAG, "Spp list item 'Help' selected");
                headerContextTracker.send(new HitBuilders.EventBuilder()
                        .setCategory("Species Select Event")
                        .setAction("Context Menu")
                        .setLabel("List Item Help")
                        .setValue(1)
                        .build());
                // Search Characters help
                helpTitle = c.getResources().getString(R.string.sel_spp_help_list_item_title);
                helpMessage = c.getResources().getString(R.string.sel_spp_help_list_item_text);
                flexHlpDlg = ConfigurableMsgDialog.newInstance(helpTitle, helpMessage);
                flexHlpDlg.show(getFragmentManager(), "frg_spp_item_help");
                return true;

            default:
                return super.onContextItemSelected(item);
       } // end of Switch
    }

    public void forgetSppMatch(long sppRecId) {
        Context c = getActivity();
       if (LDebug.ON) Log.d(LOG_TAG, "About to forget Species, record id=" + sppRecId);
        Uri uri, sUri = Uri.withAppendedPath(ContentProvider_VegNab.CONTENT_URI, "species");
        uri = ContentUris.withAppendedId(sUri, sppRecId);
        mValues.clear();
        mValues.put("HasBeenFound", 0);
        ContentResolver rs = c.getContentResolver();
        int numUpdated = rs.update(uri, mValues, null, null);
       if (LDebug.ON) Log.d(LOG_TAG, "Updated species to HasBeenFound=false; numUpdated: " + numUpdated);
        Toast.makeText(getActivity(),
                c.getResources().getString(R.string.sel_spp_list_ctx_forget_spp_done),
                Toast.LENGTH_LONG).show();
        refreshMatchList();
    }

    public void refreshMatchList() {
        // use after edit/delete
        getLoaderManager().restartLoader(Loaders.SPP_MATCHES, null, this);
    }

    public void finishPlaceholder(EditPlaceholderFragment edPh) {
        // this is mainly for fixing text that was sent to become a Placeholder, and
        // the text was edited in the Placeholder fragment
        if (!(edPh == null)) {
            mPhCodeFixed = edPh.mPlaceholderCode;
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // This is called when a new Loader needs to be created.
        // switch out based on id
        CursorLoader cl = null;
        Uri baseUri;
        String select = null; // default for all-columns, unless re-assigned or overridden by raw SQL
        String[] params = null;
        switch (id) {

        case Loaders.VISIT_INFO:
            baseUri = ContentProvider_VegNab.SQL_URI;
            select = "SELECT Visits.ProjID, Visits.NamerID "
                    + "FROM Visits "
                    + "WHERE Visits._id = ?;";
            cl = new CursorLoader(getActivity(), baseUri, null, select,
                    new String[] { "" + mCurVisitRecId }, null);
            break;

        case Loaders.SPP_MATCHES:
            baseUri = ContentProvider_VegNab.SQL_URI;
            if (mPickPlaceholder) { // show only the Placeholders, most recent first
                select = "SELECT _id, PlaceHolderCode AS Code, '' AS Genus, '' AS Species, "
                        + "'' AS SubsppVar, Description AS Vernacular, "
                        + "PlaceHolderCode || ': ' || Description || "
                        + "IFNULL((' = ' || IdSppCode || (IFNULL((': ' || IdSppDescription), ''))), '') "
                        + "AS MatchTxt, "
                        + "1 AS SubListOrder, "
                        + "1 AS IsPlaceholder, "
                        + "CASE WHEN IFNULL(IdSppCode, 0) = 0 THEN 0 ELSE 1 END AS IsIdentified "
                        + "FROM PlaceHolders "
                        + "WHERE ProjID=? AND PlaceHolders.NamerID=? "
                        + "ORDER BY TimeFirstInput DESC;";
                params = new String[] {"" + mProjectId, "" + mNamerId };
            } else {
                if (mStSearch.trim().length() == 0) {
                    select = "SELECT _id, Code, Genus, Species, SubsppVar, Vernacular, "
                            + "Code || ': ' || Genus AS MatchTxt FROM NRCSSpp "
                            + "WHERE Code LIKE '';";  // dummy query that gets no records
                    // leave params = null;
                } else if (mStSearch.trim().length() < 3) {
                    // match only previously found species and previously defined placeholders
                    // and only by code
                    select = "SELECT _id, Code, Genus, Species, SubsppVar, Vernacular, "
                            + "Code || ': ' || Genus || "
                            + "(CASE WHEN LENGTH(Species)>0 THEN (' ' || Species) ELSE '' END) || "
                            + "(CASE WHEN LENGTH(SubsppVar)>0 THEN (' ' || SubsppVar) ELSE '' END) || "
                            + "(CASE WHEN LENGTH(Vernacular)>0 THEN (', ' || Vernacular) ELSE '' END) "
                            + "AS MatchTxt, 1 AS SubListOrder, 0 AS IsPlaceholder, 1 AS IsIdentified "
                            + "FROM NRCSSpp "
                            + "WHERE Code LIKE ? AND HasBeenFound = 1 "
                            + "UNION SELECT _id, PlaceHolderCode AS Code, '' AS Genus, '' AS Species, "
                            + "'' AS SubsppVar, Description AS Vernacular, "
                            + "PlaceHolderCode || ': ' || Description "
                            + "|| IFNULL((' = ' || IdSppCode || (IFNULL((': ' || IdSppDescription), ''))), '') "
                            + "AS MatchTxt, "
                            + "1 AS SubListOrder, 1 AS IsPlaceholder, "
                            + "CASE WHEN IFNULL(IdSppCode, 0) = 0 THEN 0 ELSE 1 END AS IsIdentified "
                            + "FROM PlaceHolders "
                            + "WHERE Code Like ? "
                            + "AND ProjID=? AND PlaceHolders.NamerID=? "
                            + "ORDER BY SubListOrder, Code;";
                    params = new String[] {mStSearch + "%", mStSearch + "%", "" + mProjectId, "" + mNamerId };
                } else {
                    // For FTS matching, if the input has spaces, do an exact match on items
                    // before the last space and a prefix match on the last item
                    // Items within the criteria are AND-ed together by default, the space being the 'AND'.
                    mStMatch = mStSearch.trim() + "*";
                    select = "SELECT _id, PlaceHolderCode AS Code, '' AS Genus, '' AS Species, "
                            + "'' AS SubsppVar, Description AS Vernacular, "
                            + "PlaceHolderCode || ': ' || Description "
                            + "|| IFNULL((' = ' || IdSppCode || (IFNULL((': ' || IdSppDescription), ''))), '') "
                            + "AS MatchTxt, "
                            + "1 AS SubListOrder, 1 AS IsPlaceholder, "
                            + "CASE WHEN IFNULL(IdSppCode, 0) = 0 THEN 0 ELSE 1 END AS IsIdentified "
                            + "FROM PlaceHolders "
                            + "WHERE Code Like ? "
                            + "AND ProjID=? AND PlaceHolders.NamerID=? "
                            + "UNION SELECT docid, Code, Genus, Species, SubsppVar, Vernacular, "
                            + "Code || ': ' || Genus || "
                            + "(CASE WHEN LENGTH(Species)>0 THEN (' ' || Species) ELSE '' END) || "
                            + "(CASE WHEN LENGTH(SubsppVar)>0 THEN (' ' || SubsppVar) ELSE '' END) || "
                            + "(CASE WHEN LENGTH(Vernacular)>0 THEN (', ' || Vernacular) ELSE '' END) "
                            + "AS MatchTxt, 2 AS SubListOrder, 0 AS IsPlaceholder, 1 AS IsIdentified "
                            + "FROM NRCSSpp_fts "
                            + "WHERE NRCSSpp_fts MATCH ? "
                            + "AND HasBeenFound = 1 "
                            + "UNION SELECT _id, PlaceHolderCode AS Code, '' AS Genus, '' AS Species, "
                            + "'' AS SubsppVar, Description AS Vernacular, "
                            + "PlaceHolderCode || ': ' || Description "
                            + "|| IFNULL((' = ' || IdSppCode || (IFNULL((': ' || IdSppDescription), ''))), '') "
                            + "AS MatchTxt, "
                            + "2 AS SubListOrder, 1 AS IsPlaceholder, "
                            + "CASE WHEN IFNULL(IdSppCode, 0) = 0 THEN 0 ELSE 1 END AS IsIdentified "
                            + "FROM PlaceHolders "
                            + "WHERE Description Like ? "
                            + "AND ProjID=? AND PlaceHolders.NamerID=? "
                            + "UNION SELECT docid, Code, Genus, Species, SubsppVar, Vernacular, "
                            + "Code || ': ' || Genus || "
                            + "(CASE WHEN LENGTH(Species)>0 THEN (' ' || Species) ELSE '' END) || "
                            + "(CASE WHEN LENGTH(SubsppVar)>0 THEN (' ' || SubsppVar) ELSE '' END) || "
                            + "(CASE WHEN LENGTH(Vernacular)>0 THEN (', ' || Vernacular) ELSE '' END) "
                            + "AS MatchTxt, 4 AS SubListOrder, 0 AS IsPlaceholder, 1 AS IsIdentified "
                            + "FROM NRCSSpp_fts "
                            + "WHERE NRCSSpp_fts MATCH ? "
                            + "AND Local = 1 AND HasBeenFound = 0 "
                            + "UNION SELECT docid, Code, Genus, Species, SubsppVar, Vernacular, "
                            + "Code || ': ' || Genus || "
                            + "(CASE WHEN LENGTH(Species)>0 THEN (' ' || Species) ELSE '' END) || "
                            + "(CASE WHEN LENGTH(SubsppVar)>0 THEN (' ' || SubsppVar) ELSE '' END) || "
                            + "(CASE WHEN LENGTH(Vernacular)>0 THEN (', ' || Vernacular) ELSE '' END) "
                            + "AS MatchTxt, 6 AS SubListOrder, 0 AS IsPlaceholder, 1 AS IsIdentified "
                            + "FROM NRCSSpp_fts "
                            + "WHERE NRCSSpp_fts MATCH ? "
                            + "AND Local = 0 AND HasBeenFound = 0 "
                            + "ORDER BY SubListOrder, Code;";
                    params = new String[] {mStSearch + "%", "" + mProjectId, "" + mNamerId,
                            mStMatch, "%" + mStSearch + "%", "" + mProjectId, "" + mNamerId,
                            mStMatch, mStMatch };
                }
            }

            cl = new CursorLoader(getActivity(), baseUri,
                    null, select, params, null);
            break;

        }
        return cl;

    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor finishedCursor) {
        // there will be various loaders, switch them out here
        mRowCt = finishedCursor.getCount();
        switch (loader.getId()) {

            case Loaders.VISIT_INFO:
                if (finishedCursor.moveToFirst()) {
                    mProjectId = finishedCursor.getLong(finishedCursor.getColumnIndexOrThrow("ProjID"));
                    mNamerId = finishedCursor.getLong(finishedCursor.getColumnIndexOrThrow("NamerID"));
                    // now that NamerID is valid, get existing Placeholders to disallow duplicates
                    // they will exist as a hashmap in the Main activity
                    Bundle args = new Bundle();
                    args.putLong(MainVNActivity.ARG_PH_PROJ_ID, mProjectId);
                    args.putLong(MainVNActivity.ARG_PH_NAMER_ID, mNamerId);
                    mPlaceholderRequestListener
                            .onRequestGenerateExistingPlaceholders(args);
                }
                break;

            case Loaders.SPP_MATCHES:
            mSppResultsAdapter.swapCursor(finishedCursor);
            mSppMatchCursor = finishedCursor;
            if ((mPickPlaceholder) && (mRowCt == 0)) {
                        /*    <string name="sel_spp_search_msg_empty_list">(matches will appear here)</string>
    <string name="sel_spp_search_msg_not_finished">(working)</string>
    <string name="sel_spp_search_msg_no_matches">(no matches)</string>*/
                Toast.makeText(getActivity(),
                        getActivity().getResources().getString(R.string.sel_spp_pick_placeholder_none),
                        Toast.LENGTH_SHORT).show();
            }
            if ((mRowCt == 0) && (mStSearch.trim().length() != 0)) {
                mViewForEmptyList.setText(
                        getActivity().getResources().getString(R.string.sel_spp_search_msg_no_matches));
            }
            break;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // This is called when the last Cursor provided to onLoadFinished()
        // is about to be closed. Need to make sure it is no longer is use.
        switch (loader.getId()) {
            case Loaders.VISIT_INFO:
                break;
        case Loaders.SPP_MATCHES:
            mSppResultsAdapter.swapCursor(null);
            break;
        }
    }
}
