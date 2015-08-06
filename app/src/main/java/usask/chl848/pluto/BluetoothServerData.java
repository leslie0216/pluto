package usask.chl848.pluto;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.UUID;

/**
 * bluetooth client data
 */
public class BluetoothServerData {
    public ServerActivity m_activity;
    private BluetoothServerConnectingThread m_serverThread = null;
    private ArrayList<BluetoothServerConnectedThread> m_connectedThreadList = new ArrayList<>();

    public static final UUID BLUETOOTH_UUID = UUID.fromString("8bb345b0-712a-400a-8f47-6a4bda472638");
    private ArrayList<String> m_messageList = new ArrayList<>();

    private BluetoothAdapter m_bluetoothAdapter = null;

    private String m_name;

    public BluetoothServerData(ServerActivity activity) {
        m_activity = activity;
    }

    public void init(){
        m_activity.scheduleUpdateStatus();

        m_bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if(m_bluetoothAdapter != null){  //Device support Bluetooth
            if(!m_bluetoothAdapter.isEnabled()){
                Intent intent=new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                m_activity.startActivityForResult(intent, ServerActivity.REQUEST_ENABLE_BLUETOOTH);
            }
            else {
                setupThread();
            }

            m_name = m_bluetoothAdapter.getName();
            m_activity.updateServerName();
        }
        else{   //Device does not support Bluetooth

            m_activity.showToast(m_activity.getResources().getString(R.string.bluetoothNotSupport));
        }
    }

    public void setupThread(){
        if (m_serverThread == null) {
            m_serverThread = new BluetoothServerConnectingThread(this);
            m_serverThread.start();
        }
    }

    public void stopThreads() {
        if(m_bluetoothAdapter!=null&&m_bluetoothAdapter.isDiscovering()){
            m_bluetoothAdapter.cancelDiscovery();
        }

        if (m_serverThread != null) {
            m_serverThread.cancel();
            m_serverThread = null;
        }

        int size = m_connectedThreadList.size();
        if (size != 0) {
            for (int i = 0; i<size; ++i) {
                BluetoothServerConnectedThread ct = m_connectedThreadList.get(i);
                if (ct != null) {
                    ct.m_isStoppedByServer = true;
                    ct.cancel();
                }
            }
        }
        m_connectedThreadList.clear();

        m_activity.scheduleUpdateStatus();

        m_activity.setServerIsOn(false);
    }

    BluetoothAdapter getBluetoothAdapter(){
        return m_bluetoothAdapter;
    }

    public void startConnectedThread(BluetoothSocket socket) {
        BluetoothServerConnectedThread connectedThread = new BluetoothServerConnectedThread(this, socket);
        connectedThread.start();
        m_connectedThreadList.add(connectedThread);
        m_activity.scheduleUpdateStatus();
    }

    public void removeConnectedThread(BluetoothServerConnectedThread thread) {
        m_connectedThreadList.remove(thread);
        m_activity.scheduleUpdateStatus();
    }

    public int getConnectedThreadCount() {
        return m_connectedThreadList.size();
    }

    public void addMessage(String msg) {
        m_messageList.add(msg);
    }

    public void sendMessage(){
        if (m_messageList.size() != 0) {
            String msg = m_messageList.get(0);
            m_messageList.remove(0);
            if (!m_connectedThreadList.isEmpty()) {
                int size = m_connectedThreadList.size();

                for (int i = 0; i<size; ++i) {
                    m_connectedThreadList.get(i).write(msg);
                }
            }
        }
    }

    public boolean isMessageListEmpty() {
        return m_messageList.isEmpty();
    }

    public String receiveBTMessage(String msg){
        String rt = "";
        try {
            JSONObject jsonObject = new JSONObject(msg);

            String senderName = jsonObject.getString("name");
            String senderAddress = jsonObject.getString("address");
            float senderX = (float) jsonObject.getDouble("x");
            float senderY = (float) jsonObject.getDouble("y");
            float senderZ = (float) jsonObject.getDouble("z");
            int color = jsonObject.getInt("color");
            boolean isBusy = jsonObject.getBoolean("isBusy");
            String receiverAddress = "";
            String ballId = "";
            int ballColor = 0;

            boolean isSendingBall = jsonObject.getBoolean("isSendingBall");
            if (isSendingBall) {
                receiverAddress = jsonObject.getString("receiverAddress");
                ballId = jsonObject.getString("ballId");
                ballColor = jsonObject.getInt("ballColor");
            }

            m_activity.m_serverView.updateClientInfo(senderName, senderAddress, color, senderX, senderY, senderZ, isSendingBall, ballId, ballColor, receiverAddress, isBusy);

            rt = senderAddress;

        }catch (JSONException e) {
            e.printStackTrace();
        }

        return rt;
    }

    public String getServerName() {
        return m_name;
    }
}
