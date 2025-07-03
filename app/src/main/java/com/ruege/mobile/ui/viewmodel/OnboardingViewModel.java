package com.ruege.mobile.ui.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class OnboardingViewModel extends ViewModel {

    private final MutableLiveData<Boolean> isToolbarOnboardingFinished = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> isHomeOnboardingFinished = new MutableLiveData<>(false);

    public LiveData<Boolean> isToolbarOnboardingFinished() {
        return isToolbarOnboardingFinished;
    }

    public void setToolbarOnboardingFinished(boolean finished) {
        isToolbarOnboardingFinished.setValue(finished);
    }

    public LiveData<Boolean> isHomeOnboardingFinished() {
        return isHomeOnboardingFinished;
    }

    public void setHomeOnboardingFinished(boolean finished) {
        isHomeOnboardingFinished.setValue(finished);
    }
} 