/**
 * 
 */
package com.vegnab.vegnab.database;

import android.graphics.Bitmap;
import android.provider.BaseColumns;

/**
 * @author rshory
 *
 */
public final class VNContract {
    // empty constructor to prevent accidental instantiation
    public VNContract() {}
    // inner class to define preferences
    public static abstract class Prefs {
        public static final String DEFAULT_PROJECT_ID = "Default_Project_Id";
        public static final String DEFAULT_PROJECT_NAME = "Default_Project_Name";
        public static final String DEFAULT_PLOTTYPE_ID = "Default_PlotType_Id";
        public static final String DEFAULT_PLOTTYPE_NAME = "Default_PlotType_Name";
        public static final String DEFAULT_NAMER_ID = "Default_Namer_Id";
        public static final String DEFAULT_NAMER_NAME = "Default_Namer_Name";
        public static final String CURRENT_VISIT_ID = "Current_Visit_Id";
        public static final String CURRENT_VISIT_NAME = "Current_Visit_Name";
        public static final String TARGET_ACCURACY_OF_VISIT_LOCATIONS = "Target_Accuracy_VisitLocs";
        public static final String TARGET_ACCURACY_OF_MAPPED_LOCATIONS = "Target_Accuracy_MappedLocs";
        public static final String UNIQUE_DEVICE_ID = "Unique_Device_Id";
        public static final String DEVICE_ID_SOURCE = "Device_Id_Source";
        public static final String SPECIES_LIST_DESCRIPTION = "Species_List_Description";
        public static final String LOCAL_SPECIES_LIST_DESCRIPTION = "LocalSpecies_List_Description";
        public static final String SPECIES_LIST_DOWNLOADED = "Species_List_Downloaded";
        public static final String VERIFY_VEG_ITEMS_PRESENCE = "VerifyVegItemsPresence";
        public static final String LATEST_VEG_ITEM_SAVE = "LatestVegItemSave";
        public static final String DEFAULT_IDENT_NAMER_ID = "DefaultIdentNamerId";
        public static final String DEFAULT_IDENT_REF_ID = "DefaultIdentRefId";
        public static final String DEFAULT_IDENT_METHOD_ID = "DefaultIdentMethodId";

    }
    // inner class to define loader IDs
    // putting them all together here helps avoid conflicts in various fragments
    public static abstract class Loaders {
        public static final int TEST_SQL = 0; // test loading from raw SQL
        // in New Visit
        public static final int PROJECTS = 1; // Loader Id for Projects
        public static final int PLOTTYPES = 2; // Loader Id for Plot Types
        public static final int PREV_VISITS = 3; // Loader ID for previous Visits
        public static final int VALID_DEL_PROJECTS = 4; // Loader Id for the list of Projects that are valid to delete
        // in Edit Project
        public static final int EXISTING_PROJCODES = 11; // to disallow duplicates
        public static final int PROJECT_TO_EDIT = 12; //
        // in Visit Header
        public static final int VISIT_TO_EDIT = 21; // the current Visit
        public static final int EXISTING_VISITS = 22; // Visits other than the current, to check duplicates
        public static final int NAMERS = 23; // all Namers, to choose from
        public static final int LOCATIONS = 24;
        public static final int VISIT_REF_LOCATION = 25; // the reference Location for this Visit
        // in Edit Namer
        public static final int NAMER_TO_EDIT = 31;
        public static final int EXISTING_NAMERS = 32; // Namers other than the current, to check duplicates
        // in Delete Namer
        public static final int VALID_DEL_NAMERS = 41; // Namers that are valid to delete
        // in Date Entry Container
        public static final int CURRENT_SUBPLOTS = 51; // Subplots for the current visit
        // in Veg Subplot
        public static final int CURRENT_SUBPLOT = 61; // Header info for the current subplot
        public static final int CURRENT_SUBPLOT_SPP = 71; // Veg species for the current subplot
        public static final int BASE_SUBPLOT = 1000; // Header info for the current subplot; base number, instances will increment
        public static final int BASE_SUBPLOT_SPP = 2000; // Veg species for the current subplot; base number, instances will increment
        // in Species Select
        public static final int SPP_MATCHES = 71; // Species that match the search string
        public static final int EXISTING_PLACEHOLDER_CODES = 72; // Placeholder codes already defined by this Namer

        // in Main Activity
        // in Edit VegItem
        public static final int VEGITEM_TO_EDIT = 81; // The current veg item
        public static final int VEG_ITEM_CONFIDENCE_LEVELS = 82; //
        public static final int VEG_ITEM_DUP_CODES = 83; // codes duplicated on the subplot?

        // in Edit Placeholder
        public static final int PLACEHOLDER_TO_EDIT = 91; // The current placeholder
        public static final int PLACEHOLDERS_EXISTING = 92; // The current placeholder
        public static final int PLACEHOLDER_PROJ_NAMER = 93; // projectID and NamerID from the Visit
        public static final int PLACEHOLDER_BACKSTORY = 94; // project, location, namer, etc., details for the placeholder
        public static final int PLACEHOLDER_HABITATS = 95; // Recall these as options to re-select
        public static final int PH_IDENT_NAMERS = 96; // For spinner
        public static final int PH_IDENT_REFS = 97; // For spinner
        public static final int PH_IDENT_METHODS = 98; // For spinner
        public static final int PH_IDENT_CONFIDENCS = 99; // For spinner

        // in Placeholder Pictures Grid
        public static final int PLACEHOLDER_OF_PIX = 101; // The current placeholder
        public static final int PLACEHOLDER_PIX = 102; // Pictures for the current placeholder

        // in Configurable Edit Dialog
        public static final int ITEMS = 110; // all Items, to choose from
        public static final int ITEM_TO_EDIT = 111;
        public static final int EXISTING_ITEMS = 112; // Items other than the current, to check duplicates


    }

    // inner class to define Tags
    // putting them all together here helps use across fragments
    public static abstract class Tags {
        public static final String SPINNER_FIRST_USE = "FirstTime"; // flag to catch and ignore erroneous first firing
        public static final String NEW_VISIT = "NewVisit";
        public static final String VISIT_HEADER = "VisitHeader";
        public static final String TEST_WEBVIEW = "TestWebview";
        public static final String WEBVIEW_TUTORIAL = "WebviewTutorial";
        public static final String WEBVIEW_PLOT_TYPES = "WebviewPlotTypes";
        public static final String WEBVIEW_REGIONAL_LISTS = "WebviewSppLists";
        public static final String DATA_SCREENS_CONTAINER = "DataScreensContainer";
        public static final String VEG_SUBPLOT = "VegSubplot";
        public static final String SELECT_SPECIES = "SelectSpecies";
        public static final String EDIT_PLACEHOLDER = "EditPlaceholder";
        public static final String PLACEHOLDER_PIX_GRID = "PlaceholderPixGrid";
        public static final String PLACEHOLDER_PIC_DETAIL = "PlaceholderPicDetail";

    }
    // inner class to define Veg Item record sources
    // putting them all together here allows consistent usage throughout
    public static abstract class VegcodeSources {
        public static final int REGIONAL_LIST = 0; // standard NRCS codes from the regional list, first time entered
        public static final int PREVIOUSLY_FOUND = 1; // NRCS code, species previously entered
        public static final int PLACE_HOLDERS = 2; // user-created codes for species not known at the time
        public static final int SPECIAL_CODES = 3; // e.g. "no veg" (no vegetation on this subplot)
    }

    // inner class to define Placeholder screen actions
    // putting them all together here allows consistent usage throughout
    public static abstract class PhActions {
        public static final int GO_TO_PICTURES = 0; // go to the show/take/edit photos screen
        public static final int SAVE = 1; // attempt to force-save the Placeholder information
        public static final int CANCEL = 2; // cancel creating the Placeholder
    }

    // inner class to define Validation levels
    // putting them all together here allows consistent usage throughout
    public static abstract class Validation {
        public static final int SILENT = 0; // usually no notice
        public static final int QUIET = 1; // usually a Toast
        public static final int CRITICAL = 2; // usually a message dialog
    }

    // inner class to define regular expressions
    // putting them all together here allows consistent usage throughout
    public static abstract class VNRegex {
        public static final String NRCS_CODE = "[a-zA-Z]{3,5}[0-9]*|2[a-zA-Z]{1,4}";
        // disallow 3 to 5 letters, alone or followed by numbers
        // disallow any with numerals trailing 3-5 letters, though never saw real codes with more than 2 digits here
        // also disallow codes like "2FDA" (forb dicot annual) some agencies use for general ids

    }

    public static abstract class VNConstraints {
        public static final int PLACEHOLDER_MAX_LENGTH = 10; // maximum allowed length for a Placeholder code
    }

    // class for use in grid view
    public class VNGridImageItem {
        private Bitmap image;
        private String title;

        public VNGridImageItem(Bitmap image, String title) {
            super();
            this.image = image;
            this.title = title;
        }
        public Bitmap getImage() {
            return image;
        }
        public void setImage(Bitmap image) {
            this.image = image;
        }
        public String getTitle() {
            return title;
        }
        public void setTitle(String title) {
            this.title = title;
        }
    }

    // inner classes to define tables
    public static abstract class Project implements BaseColumns {
        public static final String TABLE_NAME = "Projects";
        public static final String COLUMN_NAME_PROJCODE = "ProjCode";
        public static final int COLUMN_SIZE_PROJCODE = 10;
        public static final String COLUMN_NAME_DESCRIPTION = "DESCRIPTION";
        public static final String COLUMN_NAME_CONTEXT = "Context";
        public static final String COLUMN_NAME_CAVEATS = "Caveats";
        public static final String COLUMN_NAME_CONTACTPERSON = "ContactPerson";
        public static final String COLUMN_NAME_STARTDATE = "StartDate";
        public static final String COLUMN_NAME_ENDDATE = "EndDate";
        public static final String SQL_CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + "("
                + _ID + " INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL UNIQUE,"
                + COLUMN_NAME_PROJCODE + " VARCHAR(" + COLUMN_SIZE_PROJCODE + ") NOT NULL UNIQUE,"
                + COLUMN_NAME_DESCRIPTION + " TEXT,"
                + COLUMN_NAME_CONTEXT + " TEXT,"
                + COLUMN_NAME_CAVEATS + " TEXT,"
                + COLUMN_NAME_CONTACTPERSON + " TEXT,"
                + COLUMN_NAME_STARTDATE + " DATE DEFAULT (DATETIME('now')),"
                + COLUMN_NAME_ENDDATE + " DATE"
                + ");";
        public static final String SQL_DROP_TABLE = "DROP TABLE IF EXISTS " + TABLE_NAME + ";";
        public static final String SQL_ASSURE_CONTENTS = "";



    }
}
