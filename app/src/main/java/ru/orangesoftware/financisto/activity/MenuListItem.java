package ru.orangesoftware.financisto.activity;

import static android.Manifest.permission.RECEIVE_SMS;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.DocumentsContract;
import android.widget.ListAdapter;
import android.widget.Toast;

import ru.orangesoftware.financisto.R;
import static ru.orangesoftware.financisto.activity.RequestPermission.isRequestingPermissions;
import ru.orangesoftware.financisto.bus.GreenRobotBus_;
import ru.orangesoftware.financisto.db.DatabaseAdapter;
import ru.orangesoftware.financisto.export.BackupExportTask;
import ru.orangesoftware.financisto.export.Export;
import ru.orangesoftware.financisto.export.csv.CsvExportOptions;
import ru.orangesoftware.financisto.export.csv.CsvExportTask;
import ru.orangesoftware.financisto.export.csv.CsvImportOptions;
import ru.orangesoftware.financisto.export.csv.CsvImportTask;
import ru.orangesoftware.financisto.export.qif.QifExportOptions;
import ru.orangesoftware.financisto.export.qif.QifExportTask;
import ru.orangesoftware.financisto.export.qif.QifImportOptions;
import ru.orangesoftware.financisto.export.qif.QifImportTask;
import ru.orangesoftware.financisto.utils.EntityEnum;
import ru.orangesoftware.financisto.utils.EnumUtils;
import static ru.orangesoftware.financisto.utils.EnumUtils.showPickOneDialog;

import androidx.fragment.app.Fragment;

import ru.orangesoftware.financisto.utils.ExecutableEntityEnum;
import ru.orangesoftware.financisto.utils.IntegrityFix;
import ru.orangesoftware.financisto.utils.SummaryEntityEnum;

public enum MenuListItem implements SummaryEntityEnum {

    MENU_PREFERENCES(R.string.preferences, R.string.preferences_summary, R.drawable.drawer_action_preferences) {
        @Override
        public void call(Fragment fragment) {
            fragment.startActivityForResult(new Intent(fragment.getContext(), PreferencesActivity.class), ACTIVITY_CHANGE_PREFERENCES);
        }
    },
    MENU_ENTITIES(R.string.entities, R.string.entities_summary, R.drawable.drawer_action_entities) {
        @Override
        public void call(Fragment fragment) {
            final MenuEntities[] entities = MenuEntities.values();
            ListAdapter adapter = EnumUtils.createEntityEnumAdapter(fragment.getContext(), entities);
            final AlertDialog d = new AlertDialog.Builder(fragment.getContext())
                    .setAdapter(adapter, (dialog, which) -> {
                        dialog.dismiss();
                        MenuEntities e = entities[which];
                        if (e.getPermissions() == null
                                || !isRequestingPermissions(fragment.getContext(), e.getPermissions())) {
                            fragment.startActivity(new Intent(fragment.getContext(), e.getActivityClass()));
                        }
                    })
                    .create();
            d.setTitle(R.string.entities);
            d.show();
        }
    },
    MENU_BACKUP(R.string.backup_database, R.string.backup_database_summary, R.drawable.actionbar_db_backup) {
        @Override
        public void call(Fragment fragment) {
            ProgressDialog d = ProgressDialog.show(fragment.getContext(), null, fragment.getContext().getString(R.string.backup_database_inprogress), true);
            new BackupExportTask(fragment.getContext(), d, true).execute();
        }
    },
    MENU_RESTORE(R.string.restore_database, R.string.restore_database_summary, R.drawable.actionbar_db_restore) {
        @Override
        public void call(Fragment fragment) {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, Export.getBackupFolder(fragment.getContext()));
            fragment.startActivityForResult(intent, ACTIVITY_RESTORE_DATABASE);
        }
    },
    GOOGLE_DRIVE_BACKUP(R.string.backup_database_online_google_drive, R.string.backup_database_online_google_drive_summary, R.drawable.actionbar_google_drive) {
        @Override
        public void call(Fragment fragment) {
            GreenRobotBus_.getInstance_(fragment.getContext()).post(new MenuListFragment.StartDriveBackup());
        }
    },
    GOOGLE_DRIVE_RESTORE(R.string.restore_database_online_google_drive, R.string.restore_database_online_google_drive_summary, R.drawable.actionbar_google_drive) {
        @Override
        public void call(Fragment fragment) {
            GreenRobotBus_.getInstance_(fragment.getContext()).post(new MenuListFragment.StartDriveRestore());
        }
    },
    DROPBOX_BACKUP(R.string.backup_database_online_dropbox, R.string.backup_database_online_dropbox_summary, R.drawable.actionbar_dropbox) {
        @Override
        public void call(Fragment fragment) {
            GreenRobotBus_.getInstance_(fragment.getContext()).post(new MenuListFragment.StartDropboxBackup());
        }
    },
    DROPBOX_RESTORE(R.string.restore_database_online_dropbox, R.string.restore_database_online_dropbox_summary, R.drawable.actionbar_dropbox) {
        @Override
        public void call(Fragment fragment) {
            GreenRobotBus_.getInstance_(fragment.getContext()).post(new MenuListFragment.StartDropboxRestore());
        }
    },
    MENU_BACKUP_TO(R.string.backup_database_to, R.string.backup_database_to_summary, R.drawable.actionbar_share) {
        @Override
        public void call(Fragment fragment) {
            ProgressDialog d = ProgressDialog.show(fragment.getContext(), null, fragment.getString(R.string.backup_database_inprogress), true);
            final BackupExportTask t = new BackupExportTask(fragment.getContext(), d, false);
            t.setShowResultMessage(false);
            t.setListener(result -> {
                Uri backupFileUri = t.backupFileUri;
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.putExtra(Intent.EXTRA_STREAM, backupFileUri);
                intent.setType(Export.BACKUP_MIME_TYPE);
                fragment.startActivity(Intent.createChooser(intent, fragment.getString(R.string.backup_database_to_title)));
            });
            t.execute((Uri[]) null);
        }
    },
    MENU_IMPORT_EXPORT(R.string.import_export, R.string.import_export_summary, R.drawable.actionbar_export) {
        @Override
        public void call(Fragment fragment) {
            showPickOneDialog(fragment.getContext(), R.string.import_export, ImportExportEntities.values(), fragment);
        }
    },
    MENU_MASS_OP(R.string.mass_operations, R.string.mass_operations_summary, R.drawable.ic_menu_agenda) {
        @Override
        public void call(Fragment fragment) {
            fragment.startActivity(new Intent(fragment.getContext(), MassOpActivity.class));
        }
    },
    MENU_SCHEDULED_TRANSACTIONS(R.string.scheduled_transactions, R.string.scheduled_transactions_summary, R.drawable.actionbar_calendar) {
        @Override
        public void call(Fragment fragment) {
            fragment.startActivity(new Intent(fragment.getContext(), ScheduledListActivity.class));
        }
    },
    MENU_PLANNER(R.string.planner, R.string.planner_summary, R.drawable.actionbar_calendar) {
        @Override
        public void call(Fragment fragment) {
            fragment.startActivity(new Intent(fragment.getContext(), PlannerActivity.class));
        }
    },
    MENU_PERMISSIONS(R.string.permissions, R.string.permissions_summary, R.drawable.ic_tab_about) {
        @Override
        public void call(Fragment fragment) {
            RequestPermissionActivity_.intent(fragment.getContext()).start();
        }
    },
    MENU_INTEGRITY_FIX(R.string.integrity_fix, R.string.integrity_fix_summary, R.drawable.actionbar_flash) {
        @Override
        public void call(Fragment fragment) {
            new IntegrityFixTask(fragment.getContext()).execute();
        }
    },
    MENU_DONATE(R.string.donate, R.string.donate_summary, R.drawable.actionbar_donate) {
        @Override
        public void call(Fragment fragment) {
            try {
                Intent browserIntent = new Intent("android.intent.action.VIEW",
                        Uri.parse("market://search?q=pname:ru.orangesoftware.financisto.support"));
                fragment.startActivity(browserIntent);
            } catch (Exception ex) {
                //eventually market is not available
                Toast.makeText(fragment.getContext(), R.string.donate_error, Toast.LENGTH_LONG).show();
            }
        }

    },
    MENU_ABOUT(R.string.about, R.string.about_summary, R.drawable.ic_action_info) {
        @Override
        public void call(Fragment fragment) {
            fragment.startActivity(new Intent(fragment.getContext(), AboutActivity.class));
        }
    };

    public final int titleId;
    public final int summaryId;
    public final int iconId;

    MenuListItem(int titleId, int summaryId, int iconId) {
        this.titleId = titleId;
        this.summaryId = summaryId;
        this.iconId = iconId;
    }

    @Override
    public int getTitleId() {
        return titleId;
    }

    @Override
    public int getSummaryId() {
        return summaryId;
    }

    @Override
    public int getIconId() {
        return iconId;
    }

    public static final int ACTIVITY_CSV_EXPORT = 2;
    public static final int ACTIVITY_QIF_EXPORT = 3;
    public static final int ACTIVITY_CSV_IMPORT = 4;
    public static final int ACTIVITY_QIF_IMPORT = 5;
    public static final int ACTIVITY_CHANGE_PREFERENCES = 6;
    public static final int ACTIVITY_RESTORE_DATABASE = 7;

    public abstract void call(Fragment fragment);

    private enum MenuEntities implements EntityEnum {

        CURRENCIES(R.string.currencies, R.drawable.ic_action_money, CurrencyListActivity.class),
        EXCHANGE_RATES(R.string.exchange_rates, R.drawable.ic_action_line_chart, ExchangeRatesListActivity.class),
        CATEGORIES(R.string.categories, R.drawable.ic_action_category, CategoryListActivity2.class),
        SMS_TEMPLATES(R.string.sms_templates, R.drawable.ic_action_sms, SmsDragListActivity.class, RECEIVE_SMS),
        PAYEES(R.string.payees, R.drawable.ic_action_users, PayeeListActivity.class),
        PROJECTS(R.string.projects, R.drawable.ic_action_gear, ProjectListActivity.class),
        LOCATIONS(R.string.locations, R.drawable.ic_action_location_2, LocationsListActivity.class);

        private final int titleId;
        private final int iconId;
        private final Class<?> actitivyClass;
        private final String[] permissions;

        MenuEntities(int titleId, int iconId, Class<?> activityClass) {
            this(titleId, iconId, activityClass, (String[]) null);
        }

        MenuEntities(int titleId, int iconId, Class<?> activityClass, String... permissions) {
            this.titleId = titleId;
            this.iconId = iconId;
            this.actitivyClass = activityClass;
            this.permissions = permissions;
        }

        @Override
        public int getTitleId() {
            return titleId;
        }

        @Override
        public int getIconId() {
            return iconId;
        }

        public Class<?> getActivityClass() {
            return actitivyClass;
        }

        public String[] getPermissions() {
            return permissions;
        }
    }

    private enum ImportExportEntities implements ExecutableEntityEnum<Fragment> {

        CSV_EXPORT(R.string.csv_export, R.drawable.backup_csv) {
            @Override
            public void execute(Fragment fragment) {
                Intent intent = new Intent(fragment.getContext(), CsvExportActivity.class);
                fragment.startActivityForResult(intent, ACTIVITY_CSV_EXPORT);
            }
        },
        CSV_IMPORT(R.string.csv_import, R.drawable.backup_csv) {
            @Override
            public void execute(Fragment fragment) {
                Intent intent = new Intent(fragment.getContext(), CsvImportActivity.class);
                fragment.startActivityForResult(intent, ACTIVITY_CSV_IMPORT);
            }
        },
        QIF_EXPORT(R.string.qif_export, R.drawable.backup_qif) {
            @Override
            public void execute(Fragment fragment) {
                Intent intent = new Intent(fragment.getContext(), QifExportActivity.class);
                fragment.startActivityForResult(intent, ACTIVITY_QIF_EXPORT);
            }
        },
        QIF_IMPORT(R.string.qif_import, R.drawable.backup_qif) {
            @Override
            public void execute(Fragment fragment) {
                Intent intent = new Intent(fragment.getContext(), QifImportActivity.class);
                fragment.startActivityForResult(intent, ACTIVITY_QIF_IMPORT);
            }
        };

        private final int titleId;
        private final int iconId;

        ImportExportEntities(int titleId, int iconId) {
            this.titleId = titleId;
            this.iconId = iconId;
        }

        @Override
        public int getTitleId() {
            return titleId;
        }

        @Override
        public int getIconId() {
            return iconId;
        }

    }

    public static void doCsvExport(Activity activity, CsvExportOptions options) {
        ProgressDialog progressDialog = ProgressDialog.show(activity, null, activity.getString(R.string.csv_export_inprogress), true);
        new CsvExportTask(activity, progressDialog, options).execute();
    }

    public static void doCsvImport(Activity activity, CsvImportOptions options) {
        ProgressDialog progressDialog = ProgressDialog.show(activity, null, activity.getString(R.string.csv_import_inprogress), true);
        new CsvImportTask(activity, progressDialog, options).execute();
    }

    public static void doQifExport(Activity activity, QifExportOptions options) {
        ProgressDialog progressDialog = ProgressDialog.show(activity, null, activity.getString(R.string.qif_export_inprogress), true);
        new QifExportTask(activity, progressDialog, options).execute();
    }

    public static void doQifImport(Activity activity, QifImportOptions options) {
        ProgressDialog progressDialog = ProgressDialog.show(activity, null, activity.getString(R.string.qif_import_inprogress), true);
        new QifImportTask(activity, progressDialog, options).execute();
    }

    private static class IntegrityFixTask extends AsyncTask<Void, Void, Void> {

        private final Context context;
        private ProgressDialog progressDialog;

        IntegrityFixTask(Context context) {
            this.context = context;
        }

        @Override
        protected void onPreExecute() {
            progressDialog = ProgressDialog.show(context, null, context.getString(R.string.integrity_fix_in_progress), true);
            progressDialog.show();
        }

        @Override
        protected void onPostExecute(Void o) {
            if (context instanceof MainActivity) {
                ((MainActivity) context).refreshCurrentTab();
            }
            progressDialog.dismiss();
        }

        @Override
        protected Void doInBackground(Void... objects) {
            DatabaseAdapter db = new DatabaseAdapter(context);
            new IntegrityFix(db).fix();
            return null;
        }
    }

}