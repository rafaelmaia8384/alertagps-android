package com.smarttools.alertagps;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.github.angads25.toggle.LabeledSwitch;
import com.github.angads25.toggle.interfaces.OnToggledListener;
import com.github.paolorotolo.appintro.ISlidePolicy;

public class FragmentIntro5 extends Fragment implements ISlidePolicy {

    private boolean canContinue = true;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        return inflater.inflate(R.layout.fragment_intro5, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

        super.onViewCreated(view, savedInstanceState);

        ((LabeledSwitch)getActivity().findViewById(R.id.toggleButtonGPS)).setOnToggledListener(new OnToggledListener() {

            @Override
            public void onSwitched(LabeledSwitch labeledSwitch, boolean isOn) {

                if (isOn) {

                    checkPermission();
                }
            }
        });
    }

    public void checkPermission() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, IntroActivity.CODE_PERMISSION_REQUEST);
            }
        }
    }

    @Override
    public boolean isPolicyRespected() {

        return canContinue;
    }

    @Override
    public void onUserIllegallyRequestedNextPage() {

    }
}
