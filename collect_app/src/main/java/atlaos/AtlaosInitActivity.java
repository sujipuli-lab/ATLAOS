package atlaos;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import org.odk.collect.android.activities.SplashScreenActivity;
import org.odk.collect.android.storage.StoragePathProvider;
import org.odk.collect.android.storage.StorageSubdirectory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import timber.log.Timber;

public class AtlaosInitActivity extends Activity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            String version = pInfo.versionName;
            int verCode = pInfo.versionCode;
            SharedPreferences initVersionPref = getSharedPreferences("InitVersion", Context.MODE_PRIVATE);

            if (verCode != initVersionPref.getInt("installedVersion", -1)) {

                reinitforms();
            }
            Intent intent = new Intent(this, SplashScreenActivity.class);
            startActivity(intent);

        } catch (Exception e) {

            Timber.e(e, "could not manage init");
        }
    }

    private void reinitforms() throws Exception {
        //
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        StoragePathProvider spp = new StoragePathProvider();
        File formsDir = new File(spp.getDirPath(StorageSubdirectory.FORMS));
        if (!formsDir.exists()) {
            formsDir.mkdirs();
        }
        //InputStream in = getResources().openRawResource(R.raw.atlas);
        for (String form : getAssets().list("forms")) {
            Timber.d("found :" + form);
            String formPath = "forms" + File.separator + form;
            Timber.d("importing form :" + formPath);
            InputStream in = getAssets().open(formPath);
            FileOutputStream out = new FileOutputStream(new File(formsDir, form));
            byte[] buff = new byte[1024];
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

