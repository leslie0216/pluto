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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.URISyntaxException;

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
            PlutoLogger.Instance().write(e.toString());
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

    public static String getFailureReason(int reason) {
        String r = "";
        switch (reason) {
            case 0 :
                r = "Error";
                break;
            case 1:
                r = "P2p unsupported";
                break;
            case 2:
                r = "Busy";
                break;
        }

        return r;
    }

    public static String getPath(Context context, Uri uri) {
        if ("content".equalsIgnoreCase(uri.getScheme())) {
            String[] projection = { "_data" };
            Cursor cursor = null;

            try {
                cursor = context.getContentResolver().query(uri, projection, null, null, null);
                int column_index = cursor.getColumnIndexOrThrow("_data");
                if (cursor.moveToFirst()) {
                    return cursor.getString(column_index);
                }
            } catch (Exception e) {
                // Eat it
            }
        }
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
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

    public static String getFileName(String filePath) {
        if ((filePath != null) && (filePath.length() > 0)) {
            int slash = filePath.lastIndexOf('/');
            if ((slash >-1) && (slash < (filePath.length() - 1))) {
                return filePath.substring(slash + 1);
            }
        }
        return filePath;
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
                PlutoLogger.Instance().write("Utility::cleanCloseFix() - Exception getting mSocket in cleanCloseFix(): " + e.toString());
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
                    PlutoLogger.Instance().write("Utility::cleanCloseFix() - Exception setting mSocket = null in cleanCloseFix(): " + e.toString());
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
                PlutoLogger.Instance().write("Utility::cleanCloseFix() - Exception getting mPfd in cleanCloseFix(): " + e.toString());
            }

            if(mPfd != null)
            {
                mPfd.close();

                mPfd = null;

                try { pfdField.set(btSocket, mPfd); }
                catch(Exception e)
                {
                    PlutoLogger.Instance().write("Utility::cleanCloseFix() - Exception setting mPfd = null in cleanCloseFix(): " + e.toString());
                }
            }

        } //synchronized
    }

    public static byte[] serialize(Object obj) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ObjectOutputStream os = new ObjectOutputStream(out);
        os.writeObject(obj);
        return out.toByteArray();
    }

    public static Object deserialize(byte[] data) throws IOException, ClassNotFoundException {
        ByteArrayInputStream in = new ByteArrayInputStream(data);
        ObjectInputStream is = new ObjectInputStream(in);
        return is.readObject();
    }

    public static int getPlanetIdByName(String name) {
        int rt = R.drawable.earth;
        switch (name) {
            case "mercury":
                rt = R.drawable.mercury;
                break;
            case "venus":
                rt = R.drawable.venus;
                break;
            case "mars":
                rt = R.drawable.mars;
                break;
            case "jupiter":
                rt = R.drawable.jupiter;
                break;
            case "saturn":
                rt = R.drawable.saturn;
                break;
            case "uranus":
                rt = R.drawable.uranus;
                break;
            case "neptune":
                rt = R.drawable.neptune;
                break;
            case "pluto":
                rt = R.drawable.pluto;
                break;
        }

        return rt;
    }

    public static String readLine(InputStream inputStream) throws IOException {
        char buf[] = new char[256];
        int room = buf.length;
        int offset = 0;
        int c;
loop:   while (true) {
            switch (c = inputStream.read()) {
                case -1:
                case '\n':
                    break loop;
                default:
                    if (--room < 0) {
                        char[] lineBuffer = buf;
                        buf = new char[offset + 256];
                        room = buf.length - offset - 1;
                        System.arraycopy(lineBuffer, 0, buf, 0, offset);
                    }

                    buf[offset++] = (char)c;
                    break;
            }
        }
        if ((c==-1)&&(offset==0))
            return null;

        return String.copyValueOf(buf, 0, offset);
    }
}
