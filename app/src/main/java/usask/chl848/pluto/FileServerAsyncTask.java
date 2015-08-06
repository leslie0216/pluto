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

            String path = Environment.getExternalStorageDirectory() + "/"
                    + m_context.getPackageName() + "/";
            String fileName = "wifip2pshared";
            String ext = ".jpg";

            File f = new File(path + fileName + ext);

            File dirs = new File(f.getParent());
            if (!dirs.exists())
                dirs.mkdirs();

            while (f.exists()) {
                fileName += "(1)";
                f = new File(path + fileName + ext);
            }
            boolean rt = f.createNewFile();
            if (!rt) {
                Log.d(MainActivity.TAG, "createNewFile failed " + f.toString());
            }
            Log.d(MainActivity.TAG, "server: copying files " + f.toString());
            InputStream inputstream = client.getInputStream();
            Utility.copyFile(inputstream, new FileOutputStream(f));
            serverSocket.close();
            Log.d(MainActivity.TAG, "server: copy files finished");
            ((MainActivity)m_context).disconnect();
            return f.getAbsolutePath();
        } catch (IOException e) {
            Log.e(MainActivity.TAG, "server: copy files error : " + e.getMessage());
            return null;
        }
    }

    @Override
    protected void onPostExecute(String result) {
        if (result != null) {
            ((MainActivity)m_context).stopProgressDialog();
            Intent intent = new Intent();
            intent.setAction(android.content.Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.parse("file://" + result), "image/*");
            m_context.startActivity(intent);
        }
    }

    @Override
    protected void onPreExecute() {
        ((MainActivity)m_context).showProgressDialog("", m_context.getResources().getString(R.string.receiving), true, false);
    }
}
