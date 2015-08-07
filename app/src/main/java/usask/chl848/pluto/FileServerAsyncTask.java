package usask.chl848.pluto;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Run in Server side for receiving file from client
 */
public class FileServerAsyncTask extends AsyncTask<Void, Void, String> {
    private Context m_context;

    public FileServerAsyncTask(Context context) {
        m_context = context;
    }

    @Override
    protected String doInBackground(Void... params) {
        try {
            ServerSocket serverSocket = new ServerSocket(8988);
            Log.d(MainActivity.TAG, "Server: Socket opened");
            Socket client = serverSocket.accept();
            Log.d(MainActivity.TAG, "Server: connection accepted");

            Log.d(MainActivity.TAG, "server: copying files ");

            InputStream inputstream = client.getInputStream();
            //Utility.copyFile(inputstream, new FileOutputStream(f));
            WiFiDirectObject wiFiDirectObject = null;
            ObjectInputStream is = new ObjectInputStream(inputstream);
            try {
                wiFiDirectObject = (WiFiDirectObject)is.readObject();
            } catch (ClassNotFoundException ex) {
                ex.printStackTrace();
            }

            String filePath = "";

            if (wiFiDirectObject != null) {
                String fileFullName = wiFiDirectObject.getFilename();
                String fileNameNoEx = Utility.getFileNameNoEx(fileFullName);
                String fileExt = Utility.getExtensionName(fileFullName);
                String dot = ".";

                String path = Environment.getExternalStorageDirectory() + "/"
                        + m_context.getPackageName() + "/";

                File f = new File(path + fileNameNoEx + dot + fileExt);

                File dirs = new File(f.getParent());
                if (!dirs.exists())
                    dirs.mkdirs();

                while (f.exists()) {
                    fileNameNoEx += "(1)";
                    f = new File(path + fileNameNoEx + dot + fileExt);
                }
                boolean rt = f.createNewFile();
                if (!rt) {
                    Log.d(MainActivity.TAG, "createNewFile failed " + f.toString());
                } else {
                    FileOutputStream fos = new FileOutputStream(f);
                    if (wiFiDirectObject.getFileContent() != null) {
                        fos.write(wiFiDirectObject.getFileContent());
                    }
                    fos.close();
                    filePath = f.getAbsolutePath();
                    Log.d(MainActivity.TAG, "server: file created : " + filePath);
                }
            } else {
                Log.d(MainActivity.TAG, "Server read file failed");
            }
            serverSocket.close();
            Log.d(MainActivity.TAG, "server: copy files finished");
            ((MainActivity)m_context).disconnect();
            return filePath;
        } catch (IOException e) {
            Log.e(MainActivity.TAG, "server: copy files error : " + e.getMessage());
            return null;
        }
    }

    @Override
    protected void onPostExecute(String result) {
        if (result != null) {
            ((MainActivity)m_context).stopProgressDialog();
            if (!result.isEmpty()) {
                Intent intent = new Intent();
                intent.setAction(android.content.Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.parse("file://" + result), "image/*");
                m_context.startActivity(intent);
            }
        }
    }

    @Override
    protected void onPreExecute() {
        ((MainActivity)m_context).showProgressDialog("", m_context.getResources().getString(R.string.receiving), true, false);
    }
}
