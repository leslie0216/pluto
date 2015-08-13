package usask.chl848.pluto;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

/**
 * bluetooth client data
 */
public class BluetoothClientData {
    public MainActivity m_activity;
    private BluetoothClientConnectingThread m_clientThread = null;
    private BluetoothClientConnectedThread m_connectedThread = null;

    private BluetoothAdapter m_bluetoothAdapter = null;
    public BluetoothDevice m_device = null;

    public static final UUID BLUETOOTH_UUID = UUID.fromString("8bb345b0-712a-400a-8f47-6a4bda472638");
    private ArrayList<String> m_messageList = new ArrayList<>();

    private boolean m_isConnected;

    private BroadcastReceiver m_receiver;
    private final IntentFilter m_intentFilter = new IntentFilter();
    private boolean m_isRecevierRegistered;

    BluetoothClientData(MainActivity activity) {
        m_activity = activity;
    }

    void init() {
        //showMessageOnMainView("Not Connected");
        m_isConnected = false;

        m_isRecevierRegistered = false;

        m_bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if(m_bluetoothAdapter != null){  //Device support Bluetooth
            if(!m_bluetoothAdapter.isEnabled()){
                Intent intent=new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                m_activity.startActivityForResult(intent, MainActivity.REQUEST_ENABLE_BLUETOOTH);
            }
            else {
                setupThread();
            }
        }
        else{   //Device does not support Bluetooth
            m_activity.showToast(m_activity.getResources().getString(R.string.bluetoothNotSupport));
        }

        m_intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
        m_intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
    }

    public void registerBluetoothReceiver() {
        m_receiver = new BluetoothBroadcastReceiver(this);
        m_activity.registerReceiver(m_receiver, m_intentFilter);
        m_isRecevierRegistered = true;
    }

    public void unRegisterBluetoothReceiver() {
        if (m_isRecevierRegistered) {
            m_activity.unregisterReceiver(m_receiver);
            m_isRecevierRegistered = false;
        }
    }

    public boolean getIsRecevierRegistered() {
        return m_isRecevierRegistered;
    }

    public void stopDiscovery() {
        if (getDevice() == null) {
            m_activity.showToast(m_activity.getResources().getString(R.string.bluetoothSeverNotFound));
        }

        unRegisterBluetoothReceiver();
    }

    public void setDevice(BluetoothDevice device) {
        m_device = device;
    }

    public BluetoothDevice getDevice() {
        return m_device;
    }

    public void setupThread(){
        findPairedDevices();
        if (getDevice() == null) {
            //m_activity.showToast(m_activity.getResources().getString(R.string.bluetoothSeverNotFound));
            if (!m_bluetoothAdapter.isDiscovering()) {
                //m_bluetoothAdapter.cancelDiscovery();
                PlutoLogger.Instance().write("BluetoothClientData::setupThread() - Start searching Bluetooth server");
                registerBluetoothReceiver();
                if (!m_bluetoothAdapter.startDiscovery()) {
                    m_activity.showToast(m_activity.getResources().getString(R.string.bluetoothSeverNotFound));
                    unRegisterBluetoothReceiver();
                }
            }
            //registerBluetoothReceiver();
        } else {
            setupClientThread();
        }
    }

    public void setupClientThread() {
        if (m_clientThread == null) {
            m_clientThread = new BluetoothClientConnectingThread(this);
            m_clientThread.start();
        }
    }

    public void stopThreads() {
        if(m_bluetoothAdapter!=null&&m_bluetoothAdapter.isDiscovering()){
            m_bluetoothAdapter.cancelDiscovery();
        }
        if (m_clientThread != null) {
            m_clientThread.cancel();
            m_clientThread = null;
        }
        if (m_connectedThread != null) {
            m_connectedThread.cancel();
            m_connectedThread = null;
        }
    }

    public void findPairedDevices() {
        Set<BluetoothDevice> pairedDevices = m_bluetoothAdapter.getBondedDevices();
        // If there are paired devices
        if (pairedDevices.size() > 0) {
            // Loop through paired devices
            for (BluetoothDevice device : pairedDevices) {
                // Add the name and address to an array adapter to show in a ListView
                if (device.getName().contains("btserver"))
                {
                    PlutoLogger.Instance().write("BluetoothClientData::findPairedDevices() - Bluetooth server found in paired devices");
                    setDevice(device);
                    break;
                }
                //mArrayAdapter.add(device.getName() + "\n" + device.getAddress());
            }
        }
    }

    public void cancelDiscovery() {
        m_bluetoothAdapter.cancelDiscovery();
    }

    public void addMessage(String msg) {
        m_messageList.add(msg);
    }

    public void sendMessage(){
        if (m_messageList.size() != 0) {
            String msg = m_messageList.get(0);
            m_messageList.remove(0);
            if (m_connectedThread != null) {
                m_connectedThread.write(msg);
            }
        }
    }

    public boolean isMessageListEmpty() {
        return m_messageList.isEmpty();
    }

    public void setIsConnected(boolean isConnected) {
        m_isConnected = isConnected;
    }
    public boolean isConnected(){
        return m_isConnected;
    }

    public void startConnectedThread(BluetoothSocket socket) {
        m_connectedThread = new BluetoothClientConnectedThread(this, socket);
        m_connectedThread.start();
    }

    public void receiveBTMessage(String msg){
        try {
            JSONArray jsonArray = new JSONArray(msg);

            int len = jsonArray.length();

            ArrayList<String> address = new ArrayList<>();

            for (int i=0; i<len; ++i) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);

                final String senderName = jsonObject.getString("name");
                String senderAddress = jsonObject.getString("address");
                int senderColor = jsonObject.getInt("color");
                float senderX = (float) jsonObject.getDouble("x");
                float senderY = (float) jsonObject.getDouble("y");
                float senderZ = (float) jsonObject.getDouble("z");
                boolean isBusy = jsonObject.getBoolean("isBusy");
                String planetName = jsonObject.getString("planetName");

                if (m_activity.m_clientView != null) {
                    m_activity.m_clientView.updateRemotePhone(senderName, senderAddress, senderColor, senderX, senderY, senderZ, isBusy, planetName);
                }

                boolean isSendingBall = jsonObject.getBoolean("isSendingBall");
                if (isSendingBall) {
                    String receiverAddress = jsonObject.getString("receiverAddress");
                    if (receiverAddress.equalsIgnoreCase(m_activity.m_wifiDirectData.getDevice().deviceAddress)) {
                        //enable discovering...
                        PlutoLogger.Instance().write("BluetoothClientData::receiveBTMessage() - This device has been set as wifi server, enable discovery");
                        m_activity.enableWiFiDirectDiscovery();
                    }
                }

                address.add(senderAddress);
            }

            ArrayList<ClientView.RemotePhoneInfo> remotePhoneInfos = m_activity.m_clientView.getRemotePhones();
            ArrayList<ClientView.RemotePhoneInfo> lostPhoneInfos = new ArrayList<>();
            for (ClientView.RemotePhoneInfo phoneInfo : remotePhoneInfos) {
                if (!address.contains(phoneInfo.m_macAddress)) {
                    lostPhoneInfos.add(phoneInfo);
                }
            }

            if (!lostPhoneInfos.isEmpty()) {
                m_activity.m_clientView.removePhones(lostPhoneInfos);
            }
        }catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
