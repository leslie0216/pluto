package usask.chl848.pluto;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 * data of wifi direct
 */
public class WifiDirectData implements WifiP2pManager.ChannelListener, WifiP2pManager.ConnectionInfoListener, WifiP2pManager.PeerListListener{
    private static final int SEARCHING_TIMEOUT = 15000;

    private MainActivity m_activity;

    private WifiP2pDevice m_device;
    private String m_remote_device_address;

    private final IntentFilter m_intentFilter = new IntentFilter();
    private WifiP2pManager.Channel m_channel;
    private WifiP2pManager m_wifiP2pManager;
    private BroadcastReceiver m_receiver;
    private WifiP2pInfo m_info;
    private String m_fileUri;

    private boolean m_isWifiP2pEnabled = false;
    private boolean m_retryChannel = false;

    private boolean m_isDiscovering = false;

    Handler m_timerDiscoveryHandler = new Handler();
    Runnable m_timerDiscoveryRunnable = new Runnable() {
        @Override
        public void run() {
            m_activity.showToast(m_activity.getResources().getString(R.string.peerNotFound));
            m_activity.stopProgressDialog();
            m_activity.setIsBusy(false);

            m_wifiP2pManager.stopPeerDiscovery(m_channel, new WifiP2pManager.ActionListener() {

                @Override
                public void onSuccess() {
                    // WiFiDirectBroadcastReceiver will notify us. Ignore for now.
                }

                @Override
                public void onFailure(int reason) {

                }
            });

        }
    };

    public WifiDirectData(MainActivity activity) {
        m_activity = activity;
    }

    public void init() {
        m_intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        m_intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        m_intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        m_intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        m_intentFilter.addAction(WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION);
        m_intentFilter.addAction(MainActivity.REQUEST_DISCONNECT_ACTION);

        m_wifiP2pManager = (WifiP2pManager)m_activity.getSystemService(Context.WIFI_P2P_SERVICE);
        m_channel = m_wifiP2pManager.initialize(m_activity, m_activity.getMainLooper(), this);
        m_fileUri = "";
    }

    public void setIsWifiP2pEnabled(boolean isWifiP2pEnabled) {
        m_isWifiP2pEnabled = isWifiP2pEnabled;
    }

    public boolean getIsWifiP2pEnabled() {
        return m_isWifiP2pEnabled;
    }

    public void setIsDiscovering(boolean isDiscovering){
        m_isDiscovering = isDiscovering;
    }

    public boolean getIsDiscovering() {
        return m_isDiscovering;
    }

    public void setRemoteDeviceAddress(String address) {
        m_remote_device_address = address;
    }

    public String getRemoteDeviceAddress() {
        return m_remote_device_address;
    }

    public void setFileUri(String fileUri) {
        m_fileUri = fileUri;
    }

    public String getFileUri() {
        return m_fileUri;
    }

    public void setWifiP2pInfo(WifiP2pInfo info) {
        m_info = info;
    }

    public WifiP2pInfo getWifiP2pInfo() {
        return m_info;
    }

    public void setDevice(WifiP2pDevice device) {
        m_device = device;
    }

    public WifiP2pDevice getDevice() {
        return m_device;
    }

    public void registerWifiDirectReceiver() {
        m_receiver = new WiFiDirectBroadcastReceiver(m_wifiP2pManager, m_channel, m_activity);
        m_activity.registerReceiver(m_receiver, m_intentFilter);
    }

    public void unRegisterWifiDirectReceiver() {
        m_activity.unregisterReceiver(m_receiver);
    }

    public void discoverPeers() {
        m_wifiP2pManager.discoverPeers(m_channel, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                Toast.makeText(m_activity, "Discovery Initiated",
                        Toast.LENGTH_SHORT).show();
                m_timerDiscoveryHandler.postDelayed(m_timerDiscoveryRunnable, SEARCHING_TIMEOUT);
            }

            @Override
            public void onFailure(int reasonCode) {
                Toast.makeText(m_activity, "Discovery Failed : " + reasonCode,
                        Toast.LENGTH_SHORT).show();
                m_activity.setIsBusy(false);
            }
        });
    }

    public void connect() {
        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = m_remote_device_address;
        config.wps.setup = WpsInfo.PBC;
        config.groupOwnerIntent = 0;
        m_wifiP2pManager.connect(m_channel, config, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                // WiFiDirectBroadcastReceiver will notify us. Ignore for now.
                m_remote_device_address = "";
            }

            @Override
            public void onFailure(int reason) {
                Toast.makeText(m_activity, "Connect failed. Retry." + reason, Toast.LENGTH_SHORT).show();
                m_activity.setIsBusy(false);
            }
        });
    }

    public void disconnect() {
        Log.d(MainActivity.TAG, "Disconnect()");
        m_wifiP2pManager.removeGroup(m_channel, new WifiP2pManager.ActionListener() {

            @Override
            public void onFailure(int reasonCode) {
                Log.d(MainActivity.TAG, "Disconnect failed. Reason :" + reasonCode);

            }

            @Override
            public void onSuccess() {

            }

        });
    }

    @Override
    public void onChannelDisconnected() {
        // we will try once more
        if (m_wifiP2pManager != null && !m_retryChannel) {
            Toast.makeText(m_activity, "Channel lost. Trying again", Toast.LENGTH_LONG).show();
            //resetData();
            m_retryChannel = true;
            m_wifiP2pManager.initialize(m_activity, m_activity.getMainLooper(), this);
        } else {
            Toast.makeText(m_activity,
                    "Severe! Channel is probably lost premanently. Try Disable/Re-Enable P2P.",
                    Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public  void onConnectionInfoAvailable(final WifiP2pInfo info) {
        m_activity.stopProgressDialog();
        setWifiP2pInfo(info);

        if (info.groupFormed) {
            if (info.isGroupOwner) {
                Log.d(MainActivity.TAG, "onConnectionInfoAvailable - isGroupOwner = true");
                new FileServerAsyncTask(m_activity).execute();
            } else {
                Log.d(MainActivity.TAG, "onConnectionInfoAvailable - isGroupOwner = false");
                m_activity.sendFile();
            }
        }
    }

    @Override
    public void onPeersAvailable(WifiP2pDeviceList peerList) {
        m_timerDiscoveryHandler.removeCallbacks(m_timerDiscoveryRunnable);
        List<WifiP2pDevice> deviceList = new ArrayList<>();
        deviceList.addAll(peerList.getDeviceList());

        for (WifiP2pDevice device : deviceList) {
            Log.d(MainActivity.TAG, "devices found : " + device.deviceName);
            if (device.deviceAddress.equals(getRemoteDeviceAddress())) {
                m_activity.stopProgressDialog();
                m_activity.showProgressDialog("", m_activity.getResources().getString(R.string.connectingTo) + " " + getRemoteDeviceAddress(), true, false);
                Log.d(MainActivity.TAG, "target device found");
                m_activity.showToast("target device found");
                connect();
                break;
            }
        }

        if (deviceList.isEmpty()) {
            Toast.makeText(m_activity, "No devices found", Toast.LENGTH_SHORT).show();
            Log.d(MainActivity.TAG, "No devices found");
        }
    }
}
