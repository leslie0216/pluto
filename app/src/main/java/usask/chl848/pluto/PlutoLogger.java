package usask.chl848.pluto;

import android.os.Environment;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;

/**
 * Logger
 */
public final class PlutoLogger {
    private BufferedWriter m_bufferedWriter;

    private PlutoLogger(){
        String SUFFIX = ".txt";
        Calendar calendar = Calendar.getInstance();
        String year = String.valueOf(calendar.get(Calendar.YEAR));
        String month = String.valueOf(calendar.get(Calendar.MONTH) + 1);
        String date = String.valueOf(calendar.get(Calendar.DATE));
        String hour = String.valueOf(calendar.get(Calendar.HOUR_OF_DAY));
        String minute = String.valueOf(calendar.get(Calendar.MINUTE));
        String second = String.valueOf(calendar.get(Calendar.SECOND));
        String time = year+"-"+month+"-"+date+"-"+hour+"-"+minute+"-"+second;

        String path = Environment.getExternalStorageDirectory().getPath() + File.separator + "Pluto" + File.separator + "logs" + File.separator;
        String fileName =  "log" + "-" + time + SUFFIX;

        File targetFile = new File(path+fileName);

        File dirs = new File(targetFile.getParent());
        if (!dirs.exists())
            dirs.mkdirs();

        if (!targetFile.exists()) {
            try {
                if (!targetFile.createNewFile())
                {
                    //Toast.makeText(context, "Can not create log file!", Toast.LENGTH_SHORT).show();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            if (!targetFile.delete()) {
                //Toast.makeText(context, "Can not delete old log file!", Toast.LENGTH_SHORT).show();
            } else {
                try {
                    if (!targetFile.createNewFile())
                    {
                        //Toast.makeText(context, "Can not create log file!", Toast.LENGTH_SHORT).show();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        try {
            m_bufferedWriter = new BufferedWriter(new FileWriter(path+fileName, true));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static PlutoLogger instance = null;

    public static PlutoLogger Instance() {
        if (instance == null) {
            instance = new PlutoLogger();
        }

        return instance;
    }

    public void write(String str) {
        Log.d(MainActivity.TAG, str);
        try {
            m_bufferedWriter.write(System.getProperty("line.separator"));
            String time = getTime();
            m_bufferedWriter.write(time + "  " + str);
            m_bufferedWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
            try {
                m_bufferedWriter.flush();
                m_bufferedWriter.close();
            } catch (IOException e2) {
                e2.printStackTrace();
            }
        }
    }

    public void flush() {
        try {
            m_bufferedWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void close(){
        try {
            m_bufferedWriter.flush();
            m_bufferedWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getTime() {
        Calendar calendar = Calendar.getInstance();
        String year = String.valueOf(calendar.get(Calendar.YEAR));
        String month = String.valueOf(calendar.get(Calendar.MONTH) + 1);
        String date = String.valueOf(calendar.get(Calendar.DATE));
        String hour = String.valueOf(calendar.get(Calendar.HOUR_OF_DAY));
        String minute = String.valueOf(calendar.get(Calendar.MINUTE));
        String second = String.valueOf(calendar.get(Calendar.SECOND));
        String mills = String.valueOf(calendar.get(Calendar.MILLISECOND));
        return year+"-"+month+"-"+date+" "+hour+":"+minute+":"+second + "." + mills;
    }
}
