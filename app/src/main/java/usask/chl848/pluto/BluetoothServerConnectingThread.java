package usask.chl848.pluto;

import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;

import java.io.IOException;

/**
 * thread used to accept connection
 */
public class BluetoothServerConnectingThread extends Thread{
    private BluetoothServerData m_data;
    private BluetoothServerSocket m_serverSocket;

    public BluetoothServerConnectingThread(BluetoothServerData data) {
        m_data = data;
        initSocket();
    }

    public void initSocket () {
        try {
            m_serverSocket = m_data.getBluetoothAdapter().listenUsingRfcommWithServiceRecord("BTServer", BluetoothServerData.BLUETOOTH_UUID);
            //m_serverSocket = m_data.getBluetoothAdapter().listenUsingInsecureRfcommWithServiceRecord ("BTServer", BluetoothServerData.BLUETOOTH_UUID);
        } catch (IOException e) {
            m_serverSocket = null;
        }
    }
    @Override
    public void run() {
        // Keep listening until exception occurs or a socket is returned
        while (true) {
            BluetoothSocket socket;
            if (m_serverSocket != null) {
                try {
                    socket = m_serverSocket.accept();
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }
            }
            else {
                initSocket();
                continue;
            }

            if (socket != null) {
                m_data.startConnectedThread(socket);
            }
        }
    }

    public void cancel() {
        try {
            if (m_serverSocket != null) {
                m_serverSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
