package usask.chl848.pluto;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.TextView;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Run in Server side for receiving file from client
 */
public class FileServerAsyncTask extends AsyncTask<Void, Integer, String> {
    private Context m_context;

    public FileServerAsyncTask(Context context) {
        m_context = context;
    }

    @Override
    protected String doInBackground(Void... params) {
        try {
            ServerSocket serverSocket = new ServerSocket(8988);
            PlutoLogger.Instance().write("FileServerAsyncTask::doInBackground() - Server: Socket opened");
            Socket socket = serverSocket.accept();
            PlutoLogger.Instance().write("FileServerAsyncTask::doInBackground() - Server: connection accepted");

            String filePath = "";

            InputStream inputStream = socket.getInputStream();
            String head = Utility.readLine(inputStream);
            PlutoLogger.Instance().write("FileServerAsyncTask::doInBackground() - server: get file head : " + head);
            if (head != null) {
                String[] items = head.split(";");
                String fileLen = items[0].substring(items[0].indexOf("=") + 1);
                String fileName = items[1].substring(items[1].indexOf("=")+1);

                // create file
                String fileFullName = fileName;
                String fileNameNoEx = Utility.getFileNameNoEx(fileFullName);
                String fileExt = Utility.getExtensionName(fileFullName);
                String dot = ".";

                String path = Environment.getExternalStorageDirectory() + File.separator
                        + "pluto" + File.separator;

                File file = new File(path + fileNameNoEx + dot + fileExt);

                File dirs = new File(file.getParent());
                if (!dirs.exists())
                    dirs.mkdirs();

                while (file.exists()) {
                    fileNameNoEx += "(1)";
                    file = new File(path + fileNameNoEx + dot + fileExt);
                }
                boolean rt = file.createNewFile();
                if (!rt) {
                    PlutoLogger.Instance().write("FileServerAsyncTask::doInBackground() - createNewFile failed " + file.toString());
                }

                OutputStream outputStream = socket.getOutputStream();
                String response = "result="+rt+"\n";
                outputStream.write(response.getBytes());

                if (rt) {
                    PlutoLogger.Instance().write("FileServerAsyncTask::doInBackground() - server: copying files");
                    byte[] buffer = new byte[524288];
                    int bytesRead = 0;
                    int progress = bytesRead;
                    Utility.progressValue = progress;
                    FileOutputStream fos = new FileOutputStream(file);
                    BufferedOutputStream bos = new BufferedOutputStream(fos);
                    publishProgress(progress, Integer.parseInt(fileLen));
                    while (true){
                        bytesRead = inputStream.read(buffer, 0, buffer.length);
                        if (bytesRead == -1) {
                            break;
                        }
                        bos.write(buffer, 0, bytesRead);
                        bos.flush();

                        progress += bytesRead;
                        publishProgress(progress, Integer.parseInt(fileLen));
                    }
                    filePath = file.getAbsolutePath();
                    bos.close();
                    fos.close();
                }
            }

            inputStream.close();
            socket.close();
            serverSocket.close();
            PlutoLogger.Instance().write("FileServerAsyncTask::doInBackground() - server: copy files finished");
            ((MainActivity)m_context).disconnect();
            return filePath;
        } catch (IOException e) {
            e.printStackTrace();
            PlutoLogger.Instance().write("FileServerAsyncTask::doInBackground() - server: copy files error : " + e.getMessage());
            ((MainActivity)m_context).disconnect();
            return null;
        }
    }

    @Override
    protected void onPostExecute(String result) {
        if (result != null) {
            ((MainActivity)m_context).stopProgressDialog();
            if (!result.isEmpty()) {
                ((MainActivity)m_context).viewFile(result);
            }
        }
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        if (values[0] == 0) {
            ((MainActivity)m_context).showProgressDialog("", m_context.getResources().getString(R.string.receiving), false, false, null, false, values[1]);
            //((MainActivity)m_context).setProgressDialogMaxValue(values[1]);
        }

        Utility.progressValue = values[0];
        ((MainActivity)m_context).updateProgressDialog();
    }


    @Override
    protected void onPreExecute() {

    }
}
