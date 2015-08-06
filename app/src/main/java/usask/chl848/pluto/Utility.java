package usask.chl848.pluto;

import android.bluetooth.BluetoothSocket;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.LocalSocket;
import android.net.Uri;
import android.net.wifi.p2p.WifiP2pDevice;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;

/**
 * Utility functions
 */
public class Utility {

    public static boolean copyFile(InputStream inputStream, OutputStream out) {
        byte buf[] = new byte[1024];
        int len;
        try {
            while ((len = inputStream.read(buf)) != -1) {
                out.write(buf, 0, len);

            }
            out.close();
            inputStream.close();
        } catch (IOException e) {
            Log.d(MainActivity.TAG, e.toString());
            return false;
        }
        return true;
    }

    public static String getWifiDeviceStatus(int deviceStatus) {
        //Log.d(MainActivity.TAG, "Peer status : " + deviceStatus);
        switch (deviceStatus) {
            case WifiP2pDevice.AVAILABLE:
                return "available";
            case WifiP2pDevice.INVITED:
                return "Invited";
            case WifiP2pDevice.CONNECTED:
                return "Connected";
            case WifiP2pDevice.FAILED:
                return "Failed";
            case WifiP2pDevice.UNAVAILABLE:
                return "Unavailable";
            default:
                return "Unknown";
        }
    }

    public static String getRealFilePath( final Context context, final Uri uri ) {
        if ( null == uri ) return null;
        final String scheme = uri.getScheme();
        String data = null;
        if ( scheme == null )
            data = uri.getPath();
        else if ( ContentResolver.SCHEME_FILE.equals( scheme ) ) {
            data = uri.getPath();
        } else if ( ContentResolver.SCHEME_CONTENT.equals( scheme ) ) {
            Cursor cursor = context.getContentResolver().query( uri, new String[] { MediaStore.Images.ImageColumns.DATA }, null, null, null );
            if ( null != cursor ) {
                if ( cursor.moveToFirst() ) {
                    int index = cursor.getColumnIndex( MediaStore.Images.ImageColumns.DATA );
                    if ( index > -1 ) {
                        data = cursor.getString( index );
                    }
                }
                cursor.close();
            }
        }
        return data;
    }

    public static String getExtensionName(String filename) {
        if ((filename != null) && (filename.length() > 0)) {
            int dot = filename.lastIndexOf('.');
            if ((dot >-1) && (dot < (filename.length() - 1))) {
                return filename.substring(dot + 1);
            }
        }
        return filename;
    }

    public static String getFileName(String filename) {
        if ((filename != null) && (filename.length() > 0)) {
            int slash = filename.lastIndexOf('/');
            if ((slash >-1) && (slash < (filename.length() - 1))) {
                return filename.substring(slash + 1);
            }
        }
        return filename;
    }

    public static String getFileNameNoEx(String filename) {
        if ((filename != null) && (filename.length() > 0)) {
            int dot = filename.lastIndexOf('.');
            if ((dot >-1) && (dot < (filename.length()))) {
                return filename.substring(0, dot);
            }
        }
        return filename;
    }

    public static void cleanCloseFix(BluetoothSocket btSocket) throws IOException
    {
        synchronized(btSocket)
        {
            Field socketField = null;
            LocalSocket mSocket = null;
            try
            {
                socketField = btSocket.getClass().getDeclaredField("mSocket");
                socketField.setAccessible(true);

                mSocket = (LocalSocket)socketField.get(btSocket);
            }
            catch(Exception e)
            {
                Log.d(MainActivity.TAG, "Exception getting mSocket in cleanCloseFix(): " + e.toString());
            }

            if(mSocket != null)
            {
                mSocket.shutdownInput();
                mSocket.shutdownOutput();
                mSocket.close();

                mSocket = null;

                try { socketField.set(btSocket, mSocket); }
                catch(Exception e)
                {
                    Log.d(MainActivity.TAG, "Exception setting mSocket = null in cleanCloseFix(): " + e.toString());
                }
            }


            Field pfdField = null;
            ParcelFileDescriptor mPfd = null;
            try
            {
                pfdField = btSocket.getClass().getDeclaredField("mPfd");
                pfdField.setAccessible(true);

                mPfd = (ParcelFileDescriptor)pfdField.get(btSocket);
            }
            catch(Exception e)
            {
                Log.d(MainActivity.TAG, "Exception getting mPfd in cleanCloseFix(): " + e.toString());
            }

            if(mPfd != null)
            {
                mPfd.close();

                mPfd = null;

                try { pfdField.set(btSocket, mPfd); }
                catch(Exception e)
                {
                    Log.d(MainActivity.TAG, "Exception setting mPfd = null in cleanCloseFix(): " + e.toString());
                }
            }

        } //synchronized
    }
}