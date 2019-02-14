package com.smarttools.alertagps;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.view.View;

import com.github.angads25.toggle.LabeledSwitch;
import com.github.paolorotolo.appintro.AppIntro2;
import com.snatik.storage.Storage;

import java.io.File;

public class IntroActivity extends AppIntro2 {

    public static final int CODE_PERMISSION_REQUEST = 100;
    public static DialogHelper dialogHelper;
    private Fragment currFragment = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        addSlide(new FragmentIntro1());
        addSlide(new FragmentIntro2());
        addSlide(new FragmentIntro3());
        addSlide(new FragmentIntro4());
        addSlide(new FragmentIntro5());

        setSwipeLock(true);

        showSkipButton(false);

        getPager().setOffscreenPageLimit(4);

        dialogHelper = new DialogHelper(IntroActivity.this);
    }

    @Override
    public void onDonePressed(Fragment currentFragment) {

        super.onDonePressed(currentFragment);

        if (((LabeledSwitch)findViewById(R.id.toggleButtonGPS)).isOn()) {

            Storage storage = new Storage(getApplicationContext());
            storage.createFile(storage.getInternalFilesDirectory() + File.separator + MainActivity.STORAGE_NOT_FIRST_ACCESS, "{ok}");

            dialogHelper.showProgressDelayed(2000, new Runnable() {

                @Override
                public void run() {

                    Intent i = new Intent(IntroActivity.this, MainActivity.class);
                    startActivity(i);

                    finish();
                }
            });

        }
        else {

            IntroActivity.dialogHelper.showProgressDelayed(500, new Runnable() {

                @Override
                public void run() {

                    runOnUiThread(new Runnable() {

                        @Override
                        public void run() {

                            dialogHelper.showError("Você deve autorizar o uso do GPS pelo aplicativo.");
                        }
                    });
                }
            });
        }
    }


    @Override
    public void onSlideChanged(@Nullable Fragment oldFragment, @Nullable Fragment newFragment) {

        currFragment = newFragment;

        super.onSlideChanged(oldFragment, newFragment);
    }

    @Override
    public void onBackPressed() {

        super.onBackPressed();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CODE_PERMISSION_REQUEST) {

            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                //permissão concedida
            }
            else if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_DENIED) {

                ((LabeledSwitch)findViewById(R.id.toggleButtonGPS)).performClick();

                if (!ActivityCompat.shouldShowRequestPermissionRationale(IntroActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)) {

                    dialogHelper.showError("Vá para as configurações do sistema operacional para ativar a permissão de GPS.");
                }
            }
        }
    }
}