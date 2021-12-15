package de.georgrichter.vibrationdemoapp.ui.scanqr;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class ScanQRViewModel extends ViewModel {

    private MutableLiveData<String> mText;

    public ScanQRViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("This is Scan QR fragment");
    }

    public LiveData<String> getText() {
        return mText;
    }
}