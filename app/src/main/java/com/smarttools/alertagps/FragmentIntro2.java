package com.smarttools.alertagps;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.github.angads25.toggle.LabeledSwitch;
import com.github.angads25.toggle.interfaces.OnToggledListener;
import com.github.paolorotolo.appintro.ISlidePolicy;

public class FragmentIntro2 extends Fragment implements ISlidePolicy {

    private boolean canContinue = true;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        return inflater.inflate(R.layout.fragment_intro2, container, false);
    }

    @Override
    public boolean isPolicyRespected() {

        return canContinue;
    }

    @Override
    public void onUserIllegallyRequestedNextPage() {

    }
}
