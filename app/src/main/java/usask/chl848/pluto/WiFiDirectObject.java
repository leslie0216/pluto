package usask.chl848.pluto;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;

/**
 * object for transferring between devices
 */
public class WiFiDirectObject implements Serializable {
    private String m_filename;
    private byte[] m_fileContent;
    private long m_fileLength;

    public WiFiDirectObject() {

    }

    public void init(String filename, String path) {
        m_filename = filename;
        File file = new File(path);
        m_fileLength = file.length();

        FileInputStream inputStream = null;
        try {
            inputStream = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            PlutoLogger.Instance().write(e.toString());
        }

        m_fileContent = new byte[(int)m_fileLength];

        try {
            inputStream.read(m_fileContent);
        } catch (IOException e) {
            PlutoLogger.Instance().write(e.toString());
        }
    }

    public String getFilename() {
        return m_filename;
    }

    public long getFileLength() {
        return m_fileLength;
    }

    public byte[] getFileContent() {
        return m_fileContent;
    }
}
