package usask.chl848.pluto;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;

/**
 * BroadcastReceiver for distributing the events sent by wifi
 */
public class WiFiDirectBroadcastReceiver extends BroadcastReceiver {

    private WifiP2pManager m_manager;
    private WifiP2pManager.Channel m_channel;
    private MainActivity m_activity;

    public WiFiDirectBroadcastReceiver(WifiP2pManager manager, WifiP2pManager.Channel channel, MainActivity activity) {
        super();
        this.m_manager = manager;
        this.m_channel = channel;
        this.m_activity = activity;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {

            // UI update to indicate wifi p2p status.
            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
            if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                // Wifi Direct mode is enabled
                m_activity.m_wifiDirectData.setIsWifiP2pEnabled(true);
                Log.d(MainActivity.TAG, "P2P state changed - enable");
            } else {
                m_activity.m_wifiDirectData.setIsWifiP2pEnabled(false);
                Log.d(MainActivity.TAG, "P2P state changed - disable");
                //m_activity.resetData();
            }
        } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {

            // request available peers from the wifi p2p manager. This is an
            // asynchronous call and the calling activity is notified with a
            // callback on PeerListListener.onPeersAvailable()
            Log.d(MainActivity.TAG, "P2P peers changed");
            if (m_manager != null) {
                m_manager.requestPeers(m_channel, m_activity.m_wifiDirectData);
            }
        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {

            if (m_manager == null) {
                return;
            }

            NetworkInfo networkInfo = (NetworkInfo) intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);

            Log.d(MainActivity.TAG, "connection changed, isConnected = " + networkInfo.isConnected());

            if (networkInfo.isConnected()) {

                // we are connected with the other device, request connection
                // info to find group owner IP

                //DeviceDetailFragment fragment = (DeviceDetailFragment) m_activity.getFragmentManager().findFragmentById(R.id.frag_detail);
                m_manager.requestConnectionInfo(m_channel, m_activity.m_wifiDirectData);
            } else {
                // It's a disconnect
                m_activity.setIsBusy(false);
                m_activity.stopProgressDialog();
            }
        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {

            m_activity.m_wifiDirectData.setDevice((WifiP2pDevice) intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE));
            m_activity.updateThisDevice();
        } else if (MainActivity.REQUEST_DISCONNECT_ACTION.equals(action)) {
            Log.d(MainActivity.TAG, "User request disconnection");
            m_activity.disconnect();
        } else if (WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION.equals(action)) {
            int state = intent.getIntExtra(WifiP2pManager.EXTRA_DISCOVERY_STATE, 0);
            m_activity.m_wifiDirectData.setIsDiscovering(state == WifiP2pManager.WIFI_P2P_DISCOVERY_STARTED);
        }
    }
}
