package de.georgrichter.vibrationdemoapp.ui;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Point;

import androidx.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.Function;

import de.georgrichter.vibrationdemoapp.ActionTriggeredArtPiece;
import de.georgrichter.vibrationdemoapp.BluetoothHandler;
import de.georgrichter.vibrationdemoapp.ContextProvider;
import de.georgrichter.vibrationdemoapp.Disposable;
import de.georgrichter.vibrationdemoapp.DistanceTriggeredArtPiece;
import de.georgrichter.vibrationdemoapp.R;
import de.georgrichter.vibrationdemoapp.packets.VibrationEffectPacket;
import de.georgrichter.vibrationdemoapp.util.InterpolationUtils;
import de.georgrichter.vibrationdemoapp.vibration.BluetoothRealtimePlaybackVibrator;
import de.georgrichter.vibrationdemoapp.vibration.BluetoothRealtimeVibrator;
import de.georgrichter.vibrationdemoapp.vibration.BluetoothVibrator;
import de.georgrichter.vibrationdemoapp.vibration.RangedVibrationProvider;
import de.georgrichter.vibrationdemoapp.vibration.VibrationPattern;
import de.georgrichter.vibrationdemoapp.vibration.VibrationProvider;

public class SharedState {

    public interface ContextConsumerCreator<T>{
        T create(ContextProvider contextProvider);
    }

    public static SharedState getInstance(){
        return INSTANCE;
    }

    private static final SharedState INSTANCE = new SharedState();

    public BluetoothHandler bluetoothHandler;
    public BluetoothDevice device;
    public final HashMap<String, ContextConsumerCreator<DistanceTriggeredArtPiece>> distArtPieces;
    public final HashMap<String, ContextConsumerCreator<ActionTriggeredArtPiece>> actionArtPieces;

    public float innerZoneDistance;
    public float outerZoneDistance;
    public float tooCloseDistance;
    public float vibrationRampRange;

    public int outerEnterRampMax;
    public int innerEnterRampMax;
    public int outerLeaveRampMax;
    public int innerLeaveRampMax;
    public int tooCloseRampMax;

    public RangedVibrationProvider outerEnterRamp;
    public RangedVibrationProvider innerEnterRamp;
    public RangedVibrationProvider outerLeaveRamp;
    public RangedVibrationProvider innerLeaveRamp;
    public RangedVibrationProvider tooCloseRamp;
    public VibrationProvider outerEnter;
    public VibrationProvider innerEnter;
    public VibrationProvider outerLeave;
    public VibrationProvider innerLeave;
    public VibrationProvider voiceEnd;
    public VibrationProvider enterSection;

    public int[] outerEnterPattern = new int[]{1, 123, 1};
    public int[] innerEnterPattern = new int[]{64, 64, 64};
    public int[] outerLeavePattern = new int[]{1, 123, 1};
    public int[] innerLeavePattern = new int[]{64, 64, 64};
    public int[] voiceEndPattern = new int[]{1, 1, 1};

    public int enterSectionDurationMs = 2000;
    public Function<Float, Float> enterSectionInterpolation
            = x -> x < 0.5f ?
            2 * x :
            2 * x - 1;

    public int connectionDurationMs = 2000;
    public Function<Float, Float> connectionInterpolation = InterpolationUtils.FromPoints(
            new InterpolationUtils.Vector2(0f, 0f),
            new InterpolationUtils.Vector2(0.33f, 0.5f),
            new InterpolationUtils.Vector2(0.66f, 0.5f),
            new InterpolationUtils.Vector2(1f, 0f)
    );

    private final ArrayList<Disposable> artPieces;
    private final ArrayList<Runnable> updateListeners;

    private SharedState(){
        artPieces = new ArrayList<>();
        distArtPieces = new HashMap<>();
        actionArtPieces = new HashMap<>();
        updateListeners = new ArrayList<>();
    }

    public void connectBluetooth(ContextProvider callingContext, String mac){
        if(bluetoothHandler != null && bluetoothHandler.isConnected()){
            if(!bluetoothHandler.getMac().equals(mac))
                bluetoothHandler.close();
            else return;
        }
        bluetoothHandler = new BluetoothHandler(callingContext.getContext(), mac);
        bluetoothHandler.connect(
                () -> {
                    new BluetoothRealtimePlaybackVibrator(callingContext, bluetoothHandler,
                            connectionInterpolation, connectionDurationMs).vibrate();
                },
                p -> { }, e -> { });
    }

    public void disconnectBluetooth(){
        bluetoothHandler.close();
        bluetoothHandler = null;
    }

    public void addUpdateListener(Runnable runnable){
        updateListeners.add(runnable);
    }

    public void removeUpdateListener(Runnable runnable){
        updateListeners.remove(runnable);
    }

    public void updateValues(ContextProvider cp){
        setSharedState(cp.getContext());
        outerEnter = new BluetoothVibrator(
                cp, bluetoothHandler, outerEnterPattern);
        innerEnter = new BluetoothVibrator(
                cp, bluetoothHandler, innerEnterPattern);
        outerLeave = new BluetoothVibrator(
                cp, bluetoothHandler, outerLeavePattern);
        innerLeave = new BluetoothVibrator(
                cp, bluetoothHandler, innerLeavePattern);
        voiceEnd = new BluetoothVibrator(
                cp, bluetoothHandler, voiceEndPattern);
        enterSection = new BluetoothRealtimePlaybackVibrator(cp,
                bluetoothHandler,
                enterSectionInterpolation,
                enterSectionDurationMs);

        outerEnterRamp = new BluetoothRealtimeVibrator(cp, bluetoothHandler, outerEnterRampMax);
        innerEnterRamp = new BluetoothRealtimeVibrator(cp, bluetoothHandler, innerEnterRampMax);
        outerLeaveRamp = new BluetoothRealtimeVibrator(cp, bluetoothHandler, outerLeaveRampMax);
        innerLeaveRamp = new BluetoothRealtimeVibrator(cp, bluetoothHandler, innerLeaveRampMax);
        tooCloseRamp = new BluetoothRealtimeVibrator(cp, bluetoothHandler, tooCloseRampMax);
        initArtPieces();
        updateListeners.forEach(Runnable::run);
    }

    public void initArtPieces(){
        //artPieces.forEach(Disposable::dispose);
        distArtPieces.clear();
        distArtPieces.put("Wasserfall (Jacob Isaackszoon van Ruisdael)",
                c -> getDistanceTriggered(c,
                        R.raw.waterfall_ambient, R.raw.waterfall_voice));
        distArtPieces.put("Strand (Claude Monet)",
                c -> getDistanceTriggered(c,
                        R.raw.beach_ambient, R.raw.beach_voice));
        actionArtPieces.clear();
        actionArtPieces.put("Wasserfall (Jacob Isaackszoon van Ruisdael)",
                c -> getActionTriggered(c, R.raw.waterfall_ambient, R.raw.waterfall_voice,
                        outerEnter, innerEnter));
        actionArtPieces.put("Strand (Claude Monet)",
                c -> getActionTriggered(c, R.raw.waterfall_ambient, R.raw.waterfall_voice,
                        outerEnter, innerEnter));
    }

    public void setSharedState(Context context){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedState sharedState = SharedState.getInstance();
        sharedState.innerZoneDistance = getFloat(sharedPreferences,"innerZoneDistance", 2);
        sharedState.outerZoneDistance = getFloat(sharedPreferences,"outerZoneDistance", 4);
        sharedState.tooCloseDistance = getFloat(sharedPreferences, "tooCloseDistance", 0.5f);
        sharedState.vibrationRampRange = getFloat(sharedPreferences, "vibrationRampRange", 0.5f);

        sharedState.outerEnterRampMax = getInt(sharedPreferences, "outerEnterRampMax", 255);
        sharedState.innerEnterRampMax = getInt(sharedPreferences, "innerEnterRampMax", 255);
        sharedState.outerLeaveRampMax = getInt(sharedPreferences, "outerLeaveRampMax", 126);
        sharedState.innerLeaveRampMax = getInt(sharedPreferences, "innerLeaveRampMax", 126);
        sharedState.tooCloseRampMax = getInt(sharedPreferences, "tooCloseRampMax", 255);

        sharedState.outerEnterPattern = getIntArray(sharedPreferences, "outerEnterPattern", "1,123,1");
        sharedState.innerEnterPattern = getIntArray(sharedPreferences, "innerEnterPattern", "12");
        sharedState.outerEnterPattern = getIntArray(sharedPreferences, "outerLeavePattern", "1,123,1");
        sharedState.innerLeavePattern = getIntArray(sharedPreferences, "innerLeavePattern", "12");
        sharedState.voiceEndPattern = getIntArray(sharedPreferences, "voiceEndPattern", "1,1,1");
    }

    private static float getFloat(SharedPreferences sharedPreferences, String key, float defVal){
        return Float.parseFloat(sharedPreferences.getString(key, "" + defVal));
    }

    private static int getInt(SharedPreferences sharedPreferences, String key, int defVal){
        return Integer.parseInt(sharedPreferences.getString(key, "" + defVal));
    }

    private static int[] getIntArray(SharedPreferences sharedPreferences, String setting, String defVal){
        String raw = sharedPreferences.getString(setting, defVal);
        String[] parts = raw.split(",");
        int[] arr = new int[parts.length];
        for (int i = 0; i < parts.length; i++){
            arr[i] = Integer.parseInt(parts[i]);
        }
        return arr;
    }

    private DistanceTriggeredArtPiece getDistanceTriggered(ContextProvider contextProvider,
                                                           int ambientID,
                                                           int voiceID){
        DistanceTriggeredArtPiece artPiece = new DistanceTriggeredArtPiece(contextProvider,
                ambientID, voiceID, true);
        artPieces.add(artPiece);
        return artPiece;
    }

    private ActionTriggeredArtPiece getActionTriggered(ContextProvider context,
                                                       int ambientSoundId,
                                                       int voiceSoundId,
                                                       VibrationProvider ambientVibrationProvider,
                                                       VibrationProvider voiceVibrationProvider){
        ActionTriggeredArtPiece artPiece = new ActionTriggeredArtPiece(context, ambientSoundId,
                voiceSoundId, ambientVibrationProvider, voiceVibrationProvider);
        artPieces.add(artPiece);
        return artPiece;
    }
}
