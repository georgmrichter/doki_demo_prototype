package de.georgrichter.vibrationdemoapp.ui.enablebluetooth;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class EnableBluetoothViewModel extends ViewModel {

    private MutableLiveData<String> mText;

    public EnableBluetoothViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("This is Enable Bluetooth fragment");
    }

    public LiveData<String> getText() {
        return mText;
    }
}