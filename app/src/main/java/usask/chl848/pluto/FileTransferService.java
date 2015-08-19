package usask.chl848.pluto;

import android.app.IntentService;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Run in Client side for transferring file to server
 */
public class FileTransferService extends IntentService {

    private static final int SOCKET_TIMEOUT = 5000;
    public static final String ACTION_SEND_FILE = "usask.chl848.pluto.SEND_FILE";
    public static final String EXTRAS_FILE_PATH = "file_url";
    public static final String EXTRAS_GROUP_OWNER_ADDRESS = "go_host";
    public static final String EXTRAS_GROUP_OWNER_PORT = "go_port";

    public FileTransferService(String name) {
        super(name);
    }

    public FileTransferService() {
        super("FileTransferService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent.getAction().equals(ACTION_SEND_FILE)) {
            String filePath = intent.getExtras().getString(EXTRAS_FILE_PATH);
            String host = intent.getExtras().getString(EXTRAS_GROUP_OWNER_ADDRESS);
            Socket socket = new Socket();
            int port = intent.getExtras().getInt(EXTRAS_GROUP_OWNER_PORT);

            try {
                PlutoLogger.Instance().write("FileTransferService::onHandleIntent() - Opening client socket - ");
                PlutoLogger.Instance().write("FileTransferService::onHandleIntent() - host - " + host);
                PlutoLogger.Instance().write("FileTransferService::onHandleIntent() - port - " + port);
                socket.bind(null);
                socket.connect((new InetSocketAddress(host, port)), SOCKET_TIMEOUT);
                PlutoLogger.Instance().write("FileTransferService::onHandleIntent() - Client socket - " + socket.isConnected());

                String fileName = Utility.getFileName(filePath);
                File file = new File(filePath);
                long fileLen = file.length();
                String head = "filelen="+fileLen+";filename="+fileName+"\n";
                OutputStream outputStream = socket.getOutputStream();
                PlutoLogger.Instance().write("FileTransferService::onHandleIntent() - Client send head : " + head);
                outputStream.write(head.getBytes());

                InputStream inputStream = socket.getInputStream();
                String response = Utility.readLine(inputStream);
                PlutoLogger.Instance().write("FileTransferService::onHandleIntent() - Client received response : " + response);
                if (response != null) {
                    String rt = response.substring(response.indexOf("=") + 1);
                    if (rt.equalsIgnoreCase("true")) {
                        PlutoLogger.Instance().write("FileTransferService::onHandleIntent() - Client sending file");

                        byte[] buffer = new byte[524288];//500k
                        FileInputStream fis = new FileInputStream(file);
                        BufferedInputStream bis = new BufferedInputStream(fis);
                        int bytesRead = 0;
                        int progress = bytesRead;
                        Utility.progressValue = progress;
                        while (true) {
                            bytesRead = bis.read(buffer, 0, buffer.length);
                            if (bytesRead == -1) {
                                break;
                            }
                            outputStream.write(buffer, 0, bytesRead);
                            outputStream.flush();
                            progress += bytesRead;
                            Utility.progressValue = progress;

                            Intent updateIntent = new Intent(MainActivity.REQUEST_UPDATE_PROGRESS);
                            sendBroadcast(updateIntent);
                        }
                        PlutoLogger.Instance().write("FileTransferService::onHandleIntent() - Client: Data written done");
                        fis.close();
                        bis.close();
                    }
                }

                inputStream.close();
                outputStream.close();
                socket.close();

                Intent i = new Intent(MainActivity.REQUEST_REMOVE_BALL_ACTION);
                sendBroadcast(i);
            } catch (IOException e) {
                PlutoLogger.Instance().write("FileTransferService::onHandleIntent() - Client: Data write error : " + e.getMessage());
                //disconnect wifi direct
                Intent i = new Intent(MainActivity.REQUEST_DISCONNECT_ACTION);
                sendBroadcast(i);
            } finally {
                if (socket != null) {
                    if (socket.isConnected()) {
                        try {
                            socket.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }
}