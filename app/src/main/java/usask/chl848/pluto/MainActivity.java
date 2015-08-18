package usask.chl848.pluto;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.provider.Settings;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.io.File;


public class MainActivity extends Activity {
    protected static final int CHOOSE_FILE_RESULT_CODE = 20;
    protected static final int REQUEST_ENABLE_BLUETOOTH = 21;
    public static final String REQUEST_DISCONNECT_ACTION = "usask.chl848.pluto.DISCONNECT_ACTION";
    public static final String REQUEST_REMOVE_BALL_ACTION = "usask.chl848.pluto.REMOVE_BALL__ACTION";
    public static final String REQUEST_UPDATE_PROGRESS = "usask.chl848.pluto.UPDATE_PROGRESS";

    public static final String TAG = "USaskPluto";

    public ClientView m_clientView;

    private String m_receivedFile;

    /**
     * Wifi Direct
     * */
    public WifiDirectData m_wifiDirectData;

    private ProgressDialog m_progressDialog = null;

    private boolean m_isBusy;
    private boolean m_isInvited;

    /**
     * Bluetooth
     * */
    public BluetoothClientData m_bluetoothData;

    /**
     * Sensor
     * */
    public SensorData m_sensorData;

    long m_startTime = 0;
    Handler m_timerHandler = new Handler();
    Runnable m_timerRunnable = new Runnable() {
        @Override
        public void run() {
            if (m_bluetoothData.isMessageListEmpty()) {
                m_clientView.sendRotation();
            }
            m_clientView.invalidate();
            m_bluetoothData.sendMessage();
            m_timerHandler.postDelayed(this, 200);
        }
    };

    public void showProgressDialog(CharSequence title,CharSequence message, boolean indeterminate, boolean cancelable, boolean isRing, int maxValue) {
        stopProgressDialog();
        //m_progressDialog = ProgressDialog.show(this, title, message, indeterminate, cancelable);
        if (isRing) {
            m_progressDialog = newRingProgressDialog(this, title, message, indeterminate, cancelable, null);
        } else {
            m_progressDialog = newBarProgressDialog(this, title, message, indeterminate, cancelable, null, maxValue);
        }
    }

    public void stopProgressDialog() {
        if (m_progressDialog != null && m_progressDialog.isShowing()) {
            m_progressDialog.dismiss();
            Utility.progressValue = 0;
        }
    }

    private ProgressDialog newRingProgressDialog(Context context, CharSequence title, CharSequence message, boolean indeterminate, boolean cancelable,  DialogInterface.OnCancelListener cancelListener) {
        ProgressDialog dialog = new ProgressDialog(context, ProgressDialog.THEME_HOLO_DARK);
        dialog.setTitle(title);
        dialog.setMessage(message);
        dialog.setIndeterminate(indeterminate);
        dialog.setCancelable(cancelable);
        dialog.setOnCancelListener(cancelListener);
        dialog.show();

        return  dialog;
    }

    private ProgressDialog newBarProgressDialog(Context context, CharSequence title, CharSequence message, boolean indeterminate, boolean cancelable,  DialogInterface.OnCancelListener cancelListener, int maxValue) {
        ProgressDialog dialog = new ProgressDialog(context, ProgressDialog.THEME_HOLO_DARK);
        dialog.setTitle(title);
        dialog.setMessage(message);
        dialog.setIndeterminate(indeterminate);
        dialog.setCancelable(cancelable);
        dialog.setOnCancelListener(cancelListener);
        dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        dialog.setProgress(0);
        dialog.setMax(maxValue);
        dialog.show();

        return  dialog;
    }

    public void updateProgressDialog() {
        if (m_progressDialog.isShowing()) {
            m_progressDialog.setProgress(Utility.progressValue);
        }
    }

    public int getProgressDialogMaxValue() {
        if (m_progressDialog != null) {
            return m_progressDialog.getMax();
        }

        return -1;
    }

    public void setProgressDialogMaxValue(int value) {
        if (m_progressDialog != null) {
            m_progressDialog.setMax(value);
        }
    }

    public void setClientViewMessage(String message) {
        if (m_clientView != null) {
            m_clientView.setMessage(message);
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        m_wifiDirectData = new WifiDirectData(this);
        m_wifiDirectData.init();

        Button selectBtn = new Button(this);
        selectBtn.setText(getResources().getString(R.string.selectFile));
        selectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity.this.fileTypeSelectDialog();
            }
        });

        RelativeLayout relativeLayout = new RelativeLayout(this);

        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        relativeLayout.addView(selectBtn, layoutParams);

        m_clientView = new ClientView(this);

        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        this.addContentView(m_clientView, new LinearLayout.LayoutParams(displayMetrics.widthPixels, displayMetrics.heightPixels));

        this.addContentView(relativeLayout, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        m_sensorData = new SensorData(this);
        m_sensorData.registerSensors();

        m_startTime = System.currentTimeMillis();
        m_timerHandler.postDelayed(m_timerRunnable, 0);

        m_bluetoothData = new BluetoothClientData(this);
        m_bluetoothData.init();

        m_isBusy = false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.atn_direct_enable) {
            startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        m_wifiDirectData.registerWifiDirectReceiver();
        m_bluetoothData.setupThread();
        m_sensorData.registerSensors();
        if (m_bluetoothData.getIsRecevierRegistered()) {
            m_bluetoothData.registerBluetoothReceiver();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        m_wifiDirectData.unRegisterWifiDirectReceiver();
        m_sensorData.unRegisterSensors();
        m_bluetoothData.unRegisterBluetoothReceiver();
    }

    @Override
    protected void onRestart() {
        m_bluetoothData.setupThread();
        m_sensorData.registerSensors();
        super.onRestart();
    }

    @Override
    protected void onDestroy() {
        m_bluetoothData.stopThreads();
        PlutoLogger.Instance().close();
        super.onDestroy();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            exit();
            return true;
        } else {
            return super.onKeyDown(keyCode, event);
        }
    }

    private void exit() {
        new AlertDialog.Builder(MainActivity.this, AlertDialog.THEME_HOLO_DARK).setTitle(getResources().getString(R.string.exitTitle)).setMessage(getResources().getString(R.string.exitMsg)).setPositiveButton(getResources().getString(R.string.yes), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
                System.exit(0);
            }
        }).setNegativeButton(getResources().getString(R.string.no), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //
            }
        }).show();
    }

    public void sendFile() {
        if (!m_wifiDirectData.getFilePath().isEmpty()) {
            File f = new File(m_wifiDirectData.getFilePath());
            showProgressDialog("", getResources().getString(R.string.sending), false, false, false,(int)f.length());
            PlutoLogger.Instance().write("MainActivity::sendFile() - filePath : " + m_wifiDirectData.getFilePath());
            Intent serviceIntent = new Intent(this, FileTransferService.class);
            serviceIntent.setAction(FileTransferService.ACTION_SEND_FILE);
            serviceIntent.putExtra(FileTransferService.EXTRAS_FILE_PATH, m_wifiDirectData.getFilePath());
            serviceIntent.putExtra(FileTransferService.EXTRAS_GROUP_OWNER_ADDRESS, m_wifiDirectData.getWifiP2pInfo().groupOwnerAddress.getHostAddress());
            serviceIntent.putExtra(FileTransferService.EXTRAS_GROUP_OWNER_PORT, 8988);
            startService(serviceIntent);
        }
    }

    public void removeBall() {
        m_wifiDirectData.setFilePath("");
        m_clientView.removeBall();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CHOOSE_FILE_RESULT_CODE && resultCode == RESULT_OK) {
            Uri uri = data.getData();
            PlutoLogger.Instance().write("MainActivity::onActivityResult(), choose file: " + uri);
            m_clientView.addBall(uri);
        } else if (requestCode == REQUEST_ENABLE_BLUETOOTH) {
            PlutoLogger.Instance().write("MainActivity::onActivityResult(), enable bt : " + resultCode);
            if (resultCode == RESULT_OK) {
                m_bluetoothData.setupThread();
            }
            else {
                showToast(getResources().getString(R.string.bluetoothEnableFailed));
            }
        }
    }

    public void updateThisDevice() {
        m_clientView.updateThisDevice();
    }

    public void connect() {
        m_wifiDirectData.connect();
    }

    public void disconnect() {
        stopProgressDialog();
        m_wifiDirectData.disconnect();
    }

    public void showToast(String message)
    {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    public void startWifiDirectConnection(String remoteAddress) {
        if (m_wifiDirectData.getIsWifiP2pEnabled()) {
            PlutoLogger.Instance().write("MainActivity::startWifiDirectConnection(), client start wifi connection, remoteAddr : " + remoteAddress);
            m_wifiDirectData.setRemoteDeviceAddress(remoteAddress);
            setIsInvited(false);

            showProgressDialog("", getResources().getString(R.string.finding) + " : " + m_wifiDirectData.getRemoteDeviceAddress(), true, false, true, 0);
            m_wifiDirectData.discoverPeers();
        } else {
            showToast(getResources().getString(R.string.p2p_off_warning));
            setIsBusy(false);
        }
    }

    public void enableWiFiDirectDiscovery() {
        if (m_wifiDirectData.getIsWifiP2pEnabled()) {
            PlutoLogger.Instance().write("MainActivity::enableWiFiDirectDiscovery(), server start wifi connection");
            m_wifiDirectData.setRemoteDeviceAddress("");
            m_wifiDirectData.discoverPeers();
        } else {
            showToast(getResources().getString(R.string.p2p_off_warning));
        }
    }

    public boolean getIsBusy() {
        return m_isBusy;
    }

    public void setIsBusy(boolean isBusy) {
        m_isBusy = isBusy;
    }

    public boolean getIsInvited() {
        return m_isInvited;
    }

    public void setIsInvited(boolean isInvited) {
        m_isInvited = isInvited;
    }

    public void viewFile(String file) {
        m_receivedFile = file;
        String fileName = Utility.getFileName(file);
        new AlertDialog.Builder(MainActivity.this, AlertDialog.THEME_HOLO_DARK).setTitle(getResources().getString(R.string.fileReceived)).setMessage("\"" +fileName + "\"" + " " + getResources().getString(R.string.viewFileAlert)).setPositiveButton(getResources().getString(R.string.yes), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent();
                intent.setAction(android.content.Intent.ACTION_VIEW);
                //String ext = MimeTypeMap.getFileExtensionFromUrl(result);

                String ext = Utility.getExtensionName(MainActivity.this.m_receivedFile);
                if (!ext.isEmpty()) {
                    String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);
                    if (!mimeType.isEmpty()) {
                        intent.setDataAndType(Uri.parse("file://" + MainActivity.this.m_receivedFile), mimeType);
                        startActivity(intent);
                    } else {
                        MainActivity.this.showToast("Can not view such type of file");
                    }

                }
            }
        }).setNegativeButton(getResources().getString(R.string.no), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //
            }
        }).show();
    }

    public void fileTypeSelectDialog() {
        AlertDialog.Builder ab = new AlertDialog.Builder(MainActivity.this, AlertDialog.THEME_HOLO_DARK);
        ab.setMessage(getResources().getString(R.string.fileChooseMsg));
        ab.setPositiveButton(getResources().getString(R.string.fileTypeOther), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });

        ab.setNeutralButton(getResources().getString(R.string.fileTypeVideo), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("video/*");
                intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(intent, CHOOSE_FILE_RESULT_CODE);
            }
        });

        ab.setNegativeButton(getResources().getString(R.string.fileTypeImage), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(intent, CHOOSE_FILE_RESULT_CODE);
            }
        });

        ab.show();
    }
}
