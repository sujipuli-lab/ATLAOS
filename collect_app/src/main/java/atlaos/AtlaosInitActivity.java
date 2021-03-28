package atlaos;

import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.os.Bundle;
import android.view.Window;

import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

import org.odk.collect.android.R;
import org.odk.collect.android.activities.SplashScreenActivity;
import org.odk.collect.android.gdrive.GoogleAccountNotSetDialog;
import org.odk.collect.android.gdrive.GoogleAccountsManager;
import org.odk.collect.android.injection.DaggerUtils;
import org.odk.collect.android.listeners.PermissionListener;
import org.odk.collect.android.preferences.AdminKeys;
import org.odk.collect.android.preferences.AdminSharedPreferences;
import org.odk.collect.android.preferences.GeneralKeys;
import org.odk.collect.android.preferences.GeneralSharedPreferences;
import org.odk.collect.android.storage.StoragePathProvider;
import org.odk.collect.android.storage.StorageSubdirectory;
import org.odk.collect.android.utilities.PermissionUtils;
import org.odk.collect.android.utilities.PlayServicesChecker;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import javax.inject.Inject;

import timber.log.Timber;

import static org.odk.collect.android.preferences.AdminPreferencesActivity.ADMIN_PREFERENCES;
import static org.odk.collect.android.preferences.GeneralKeys.KEY_SELECTED_GOOGLE_ACCOUNT;

public class AtlaosInitActivity extends Activity {


    private static final int REQUEST_ACCOUNT_PICKER = 1000;
    @Inject
    GoogleAccountsManager accountsManager;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DaggerUtils.getComponent(this).inject(this);

        //      GoogleAccountNotSetDialog.show(this);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        PermissionUtils permissionUtils = new PermissionUtils(R.style.Theme_Collect_Dialog_PermissionAlert);
        permissionUtils.requestStoragePermissions(this, new PermissionListener() {
            @Override
            public void granted() {
                //We should now have acces to the sdcard, checking for user configuration
                checkaccount();
            }

            @Override
            public void denied() {
                // The activity has to finish because ODK Collect cannot function without these permissions.
                finish();
            }
        });
    }

    public void checkaccount() {
        //Checking if an account is already set
        String account = (String) GeneralSharedPreferences.getInstance().get(KEY_SELECTED_GOOGLE_ACCOUNT);
        if ("".equals(account)) {
            addAcountAccess();
        } else {
            launch();
        }

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_ACCOUNT_PICKER:
                if (resultCode == RESULT_OK && data != null && data.getExtras() != null) {
                    String accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                    accountsManager.selectAccount(accountName);
                    //   selectedGoogleAccountPreference.setSummary(accountName);
                    launch();
                }
                //       allowClickSelectedGoogleAccountPreference = true;
                break;
        }
    }

    private void launch() {
        try {
            // Getting the installed app version
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            String version = pInfo.versionName;
            int verCode = pInfo.versionCode;
            SharedPreferences initVersionPref = getSharedPreferences("InitVersion", Context.MODE_PRIVATE);
            if (verCode != initVersionPref.getInt("installedVersion", -1)) {
                //First Launch of new version, redeploying forls
                reinitforms();
                initVersionPref.edit().putInt("installedVersion", verCode).commit();
            }
            // Launching the norml ODK activity
            finish();
            startActivity(new Intent(this, SplashScreenActivity.class));

        } catch (Exception e) {
            Timber.e(e, "could not manage init");
        }
    }

    private void reinitforms() throws Exception {
        // Getting the default storage folder and making sure the folders for the forms exist
        StoragePathProvider spp = new StoragePathProvider();
        File formsDir = new File(spp.getDirPath(StorageSubdirectory.FORMS));
        if (!formsDir.exists()) {
            formsDir.mkdirs();
        }
        // pushing all forms from src/asset/forms to the forms folder of odk
        for (String form : getAssets().list("forms")) {
            Timber.d("found :" + form);
            String formPath = "forms" + File.separator + form;
            Timber.d("importing form :" + formPath);
            InputStream in = getAssets().open(formPath);
            FileOutputStream out = new FileOutputStream(new File(formsDir, form));
            byte[] buff = new byte[10240];
            int read = 0;

            try {
                while ((read = in.read(buff)) > 0) {
                    out.write(buff, 0, read);
                }
            } finally {
                in.close();
                out.close();
            }
        }
        //Setting default preferences
        SharedPreferences sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(GeneralKeys.KEY_PROTOCOL, getString(R.string.protocol_google_sheets));
        String[] imageEntryValues = getResources().getStringArray(R.array.image_size_entry_values);
        editor.putString(GeneralKeys.KEY_IMAGE_SIZE, imageEntryValues[2]);
        editor.apply();
        editor = getSharedPreferences(ADMIN_PREFERENCES, MODE_PRIVATE).edit();
        editor.putString(AdminKeys.KEY_ADMIN_PW, "1234");
        editor.apply();

        AdminSharedPreferences.getInstance().save(AdminKeys.KEY_GET_BLANK, false);
        AdminSharedPreferences.getInstance().save(AdminKeys.KEY_DELETE_SAVED, false);
        AdminSharedPreferences.getInstance().save(AdminKeys.KEY_QR_CODE_SCANNER, false);
        GoogleAccountNotSetDialog.show(this);
    }

    private void addAcountAccess() {

        if (new PlayServicesChecker().isGooglePlayServicesAvailable(this)) {

            new PermissionUtils(R.style.Theme_Collect_Dialog_PermissionAlert).requestGetAccountsPermission(this, new PermissionListener() {
                @Override
                public void granted() {
                    Intent intent = accountsManager.getAccountChooserIntent();
                    startActivityForResult(intent, REQUEST_ACCOUNT_PICKER);
                }

                @Override
                public void denied() {
                }
            });
        } else {
            new PlayServicesChecker().showGooglePlayServicesAvailabilityErrorDialog(this);
        }
    }


}
