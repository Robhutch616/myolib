/*
 * darken's Myo lib
 * Matthias Urhahn (matthias.urhahn@rwth-aachen.de)
 * mHealth - Uniklinik RWTH-Aachen.
 */
package eu.darken.myolib.exampleapp;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eu.darken.myolib.BaseMyo;
import eu.darken.myolib.Myo;
import eu.darken.myolib.MyoCmds;
import eu.darken.myolib.MyoConnector;
import eu.darken.myolib.MyoInfo;
import eu.darken.myolib.msgs.MyoMsg;
import eu.darken.myolib.tools.Logy;

/**
 * Test class for various library and Myo functions.
 * Tries to display all found Myo devices with all their available data.
 */
public class DebugFragment extends Fragment implements BaseMyo.ConnectionListener {
    private static final String TAG = "MyoLib:DebugFragment";
    private Map<Myo, MyoInfoView> mMyoViewMap = new HashMap<>();
    private ViewGroup mContainer;
    private boolean mScanning = false;
    private MyoConnector mMyoConnector;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Logy.sLoglevel = Logy.VERBOSE;
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View layout = inflater.inflate(R.layout.fragment_debug, container, false);
        mContainer = (ViewGroup) layout.findViewById(R.id.container);
        return layout;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        mMyoConnector = new MyoConnector(getActivity());
        super.onActivityCreated(savedInstanceState);
    }

    private MyoConnector.ScannerCallback mScannerCallback = new MyoConnector.ScannerCallback() {
        @Override
        public void onScanFinished(final List<Myo> myos) {
            if (mContainer.getHandler() == null)
                return;
            Logy.d(TAG, "MYOS:" + myos.size());
            mContainer.post(new Runnable() {
                @Override
                public void run() {
                    if (getView() == null)
                        return;
                    for (final Myo myo : myos) {
                        if (!mMyoViewMap.containsKey(myo)) {
                            MyoInfoView infoView = (MyoInfoView) LayoutInflater.from(getActivity()).inflate(R.layout.view_myoinfo, mContainer, false);
                            mMyoViewMap.put(myo, infoView);
                            myo.addConnectionListener(DebugFragment.this);
                            myo.connect();
                            myo.setConnectionSpeed(BaseMyo.ConnectionSpeed.HIGH);
                            myo.writeSleepMode(MyoCmds.SleepMode.NEVER, null);
                            myo.writeMode(MyoCmds.EmgMode.NONE, MyoCmds.ImuMode.EVENTS, MyoCmds.ClassifierMode.ENABLED, null);
                            myo.writeUnlock(MyoCmds.UnlockType.HOLD, null);
                            myo.readInfo(new Myo.ReadMyoInfoCallback() {
                                @Override
                                public void onReadMyoInfo(Myo myo, MyoMsg msg, MyoInfo myoInfo) {

                                }
                            });
                            infoView.setMyo(myo);
                            mContainer.addView(infoView);
                        }
                    }
                    if (mScanning) {
                        mMyoConnector.scan(2000, mScannerCallback);
                    }
                }
            });

        }
    };


    @Override
    public void onResume() {
        mScanning = true;
        mMyoConnector.scan(2000, mScannerCallback);
        super.onResume();
    }

    @Override
    public void onPause() {
        mScanning = false;
        for (Myo myo : mMyoViewMap.keySet()) {
            myo.removeConnectionListener(this);
            myo.setConnectionSpeed(BaseMyo.ConnectionSpeed.BALANCED);
            myo.writeSleepMode(MyoCmds.SleepMode.NORMAL, null);
            myo.writeMode(MyoCmds.EmgMode.NONE, MyoCmds.ImuMode.NONE, MyoCmds.ClassifierMode.DISABLED, null);
            myo.disconnect();
        }
        mContainer.removeAllViews();
        mMyoViewMap.clear();
        super.onPause();
    }

    @Override
    public void onConnectionStateChanged(final BaseMyo myo, BaseMyo.ConnectionState state) {
        if (getView() == null)
            return;
        if (state == BaseMyo.ConnectionState.DISCONNECTED) {
            getView().post(new Runnable() {
                @Override
                public void run() {
                    mContainer.removeView(mMyoViewMap.get(myo));
                }
            });
        }
    }
}

