package de.georgrichter.vibrationdemoapp.ui.scanqr;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import de.georgrichter.vibrationdemoapp.databinding.FragmentEnableBluetoothBinding;
import de.georgrichter.vibrationdemoapp.databinding.FragmentScanQrBinding;
import de.georgrichter.vibrationdemoapp.ui.MainActivity;

public class ScanQRFragment extends Fragment {

    private ScanQRViewModel scanQRViewModel;
    public FragmentScanQrBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        scanQRViewModel =
                new ViewModelProvider(this).get(ScanQRViewModel.class);

        binding = FragmentScanQrBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        scanQRViewModel.getText().observe(getViewLifecycleOwner(), new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {

            }
        });

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}