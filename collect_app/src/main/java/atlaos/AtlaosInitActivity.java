package atlaos;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.os.Bundle;
import android.view.Window;

import androidx.annotation.Nullable;

import org.odk.collect.android.R;
import org.odk.collect.android.activities.SplashScreenActivity;
import org.odk.collect.android.listeners.PermissionListener;
import org.odk.collect.android.storage.StoragePathProvider;
import org.odk.collect.android.storage.StorageSubdirectory;
import org.odk.collect.android.utilities.PermissionUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import timber.log.Timber;


public class AtlaosInitActivity extends Activity {


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        PermissionUtils permissionUtils = new PermissionUtils(R.style.Theme_Collect_Dialog_PermissionAlert);
        permissionUtils.requestStoragePermissions(this, new PermissionListener() {
            @Override
            public void granted() {
                //We should now have acces to the sdcard
                launch();
            }

            @Override
            public void denied() {
                // The activity has to finish because ODK Collect cannot function without these permissions.
                finish();
            }
        });
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
    }


}
