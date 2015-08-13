package usask.chl848.pluto;

import android.bluetooth.BluetoothSocket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * thread used to exchange data with bluetooth clients
 */
public class BluetoothServerConnectedThread extends Thread{
    private BluetoothServerData m_data;

    private BluetoothSocket m_socket;

    private InputStream m_inStream;
    private OutputStream m_outStream;

    String m_deviceAddress;
    boolean m_isStoppedByServer;
    public BluetoothServerConnectedThread(BluetoothServerData data, BluetoothSocket socket) {
        try {
            m_data = data;
            m_socket = socket;
            m_inStream = m_socket.getInputStream();
            m_outStream = m_socket.getOutputStream();
            m_deviceAddress = "";
            m_isStoppedByServer = false;
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    @Override
    public void run() {

        byte[] buffer = new byte[1024];
        int bytes;

        while (true) {
            try {
                // Read from the InputStream
                if( m_inStream != null && (bytes = m_inStream.read(buffer)) > 0 )
                {
                    byte[] buf_data = new byte[bytes];
                    for(int i=0; i<bytes; i++)
                    {
                        buf_data[i] = buffer[i];
                    }
                    String msg = new String(buf_data);
                    String address = m_data.receiveBTMessage(msg);
                    if (m_deviceAddress.equalsIgnoreCase("")) {
                        m_deviceAddress = address;
                    }
                }
            } catch (IOException e) {
                cancel();
                if (!m_isStoppedByServer) {
                    m_data.removeConnectedThread(this);
                }
                m_data.m_activity.m_serverView.removeDevice(m_deviceAddress);
                break;
            }
        }
    }

    public void write(String msg) {
        try {
            if (m_socket.isConnected() &&m_outStream != null) {
                m_outStream.write(msg.getBytes());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void cancel() {
        synchronized (this) {
            try {
                if (m_inStream != null) {
                    m_inStream.close();
                    m_inStream = null;
                }
                if (m_outStream != null) {
                    m_outStream.flush();
                    m_outStream.close();
                    m_outStream = null;
                }
                if (m_socket != null) {
                    Utility.cleanCloseFix(m_socket);
                    m_socket.close();
                    m_socket = null;
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
