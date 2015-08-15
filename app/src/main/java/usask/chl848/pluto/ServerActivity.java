package usask.chl848.pluto;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;


public class ServerActivity extends Activity{

    protected static final int REQUEST_ENABLE_BLUETOOTH = 21;

    private boolean m_isOn;

    private BluetoothServerData m_bluetoothData;

    public ServerView m_serverView;

    long startTime = 0;

    Handler timerHandler = new Handler();
    Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            m_serverView.invalidate();
            if (m_bluetoothData != null && m_bluetoothData.isMessageListEmpty()) {
                m_bluetoothData.addMessage(m_serverView.cookMessage());
            }

            if (m_bluetoothData != null) {
                m_bluetoothData.sendMessage();
            }
            timerHandler.postDelayed(this, 200);
        }
    };

    public void scheduleUpdateStatus(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateStatus();
            }
        });
    }

    public void updateStatus(){
        TextView v = (TextView)this.findViewById(R.id.txt_status_cnt);
        int count = 0;
        if (m_bluetoothData != null) {
            count = m_bluetoothData.getConnectedThreadCount();
        }
        v.setText(getResources().getString(R.string.connectedCnt) + " : " + count);
    }

    public void updateServerName() {
        TextView v = (TextView)this.findViewById(R.id.txt_server_name);
        v.setText(getResources().getString(R.string.serverName) + " : " + m_bluetoothData.getServerName());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server);

        m_bluetoothData = new BluetoothServerData(this);

        updateStatus();

        Button btn=(Button)this.findViewById(R.id.btn_switch);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Button btn=(Button)ServerActivity.this.findViewById(R.id.btn_switch);
                if (!m_isOn) {
                    ServerActivity.this.m_bluetoothData.init();
                    m_isOn = true;
                    btn.setText("Stop Server");
                } else {
                    ServerActivity.this.m_bluetoothData.stopThreads();
                    m_isOn = false;
                    btn.setText("Start Server");
                }
            }
        });

        m_serverView = new ServerView(this);

        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        this.addContentView(m_serverView, new LinearLayout.LayoutParams(displayMetrics.widthPixels, (int) (displayMetrics.heightPixels * 0.7f)));

        setServerIsOn(false);

        startTime = System.currentTimeMillis();
        timerHandler.postDelayed(timerRunnable, 0);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_server, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        if (getServerIsOn()) {
            m_bluetoothData.setupThread();
        }
        super.onResume();
    }

    @Override
    protected void onRestart() {
        if (getServerIsOn()) {
            m_bluetoothData.setupThread();
        }
        super.onRestart();
    }

    @Override
    protected void onDestroy() {
        setServerIsOn(false);
        m_bluetoothData.stopThreads();
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
        new AlertDialog.Builder(ServerActivity.this, AlertDialog.THEME_HOLO_DARK).setTitle(getResources().getString(R.string.warningTitle)).setMessage(getResources().getString(R.string.warningMsg)).setPositiveButton(getResources().getString(R.string.yes), new DialogInterface.OnClickListener() {
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BLUETOOTH) {
            if (resultCode == RESULT_OK) {
                m_bluetoothData.setupThread();
            }
            else {
                showToast(getResources().getString(R.string.bluetoothEnableFailed));
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    public void showToast(String message)
    {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    public void setServerIsOn(boolean isOn) {
        m_isOn = isOn;
    }

    public boolean getServerIsOn() {
        return  m_isOn;
    }
}
