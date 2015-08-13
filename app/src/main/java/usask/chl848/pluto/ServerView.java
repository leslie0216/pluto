package usask.chl848.pluto;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.DisplayMetrics;
import android.view.View;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Server View
 */
public class ServerView extends View{
    Paint m_paint;

    private String m_status;

    public class ClientInfo {
        float x;
        float y;
        float z;
        int color;

        boolean isSendingBall;
        String ballId;
        int ballColor;
        String name;
        String receiverAddress;

        boolean isBusy;

        String planetName;
    }
    private Map<String, ClientInfo> m_clients = new HashMap<>();

    private ArrayList<String> m_planetNames;

    public ServerView(Context context) {
        super(context);

        m_paint = new Paint();
        m_planetNames = new ArrayList<>();
        initPlanetId();
    }

    public void updateClientInfo(String senderName, String senderAddress, int color, float x, float y, float z, boolean isSendingBall, String ballId, int ballColor, String receiverAddress, boolean isBusy) {
        ClientInfo clientInfo = new ClientInfo();
        clientInfo.x = x;
        clientInfo.y = y;
        clientInfo.z = z;
        clientInfo.color = color;
        clientInfo.isSendingBall = isSendingBall;
        clientInfo.ballId = ballId;
        clientInfo.ballColor = ballColor;
        clientInfo.receiverAddress = receiverAddress;
        clientInfo.name = senderName;
        clientInfo.isBusy = isBusy;
        if (m_clients.containsKey(senderAddress)) {
            clientInfo.planetName = m_clients.get(senderAddress).planetName;
        } else {
            clientInfo.planetName = getAvailableName();
        }
        m_clients.put(senderAddress, clientInfo);
    }

    public String cookMessage() {
        JSONArray msg = new JSONArray();
        for(Map.Entry<String,ClientInfo> entry : m_clients.entrySet()){
            JSONObject record = new JSONObject();
            String strkey = entry.getKey();
            ClientInfo strval = entry.getValue();
            try {
                record.put("address", strkey);
                record.put("name", strval.name);
                record.put("color", strval.color);
                record.put("x", strval.x);
                record.put("y", strval.y);
                record.put("z", strval.z);
                record.put("isSendingBall", strval.isSendingBall);
                record.put("ballId", strval.ballId);
                record.put("ballColor", strval.ballColor);
                record.put("receiverAddress", strval.receiverAddress);
                record.put("isBusy", strval.isBusy);
                record.put("planetName", strval.planetName);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            msg.put(record);
        }

        return msg.toString();
    }

    public void setStatus(String status) {
        m_status = status;
    }

    @Override
    protected  void onDraw(Canvas canvas) {
        showConnections(canvas);
    }

    private void showConnections(Canvas canvas) {
        DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();

        m_paint.setTextSize(30);
        m_paint.setColor(Color.RED);

        float x = 0.05f;
        float y = 0.2f;

        for(Map.Entry<String,ClientInfo> entry : m_clients.entrySet()){
            //String strkey = entry.getKey();
            ClientInfo strval = entry.getValue();
            String output = strval.name + " - Angle : " + strval.z;
            if (strval.isSendingBall) {
                output += " Sending";
            }
            canvas.drawText(output, (int) (displayMetrics.widthPixels * x), (int) (displayMetrics.heightPixels * y), m_paint);
            y += 0.05f;
        }
    }

    private void showStatus(){
        ServerActivity sa = (ServerActivity)getContext();
        sa.updateStatus();
    }

    public void removeDevice(String id) {
        if (m_clients.containsKey(id)) {
            ClientInfo clientInfo = m_clients.get(id);
            String planetName = clientInfo.planetName;
            m_planetNames.add(planetName);
            m_clients.remove(id);

        }
    }

    private void initPlanetId() {
        m_planetNames.clear();
        m_planetNames.add("mercury");
        m_planetNames.add("venus");
        m_planetNames.add("mars");
        m_planetNames.add("jupiter");
        m_planetNames.add("saturn");
        m_planetNames.add("uranus");
        m_planetNames.add("neptune");
        m_planetNames.add("pluto");
    }

    private String getAvailableName() {
        if (m_planetNames.isEmpty()) {
            return "";
        }

        Random rnd = new Random();
        int index = rnd.nextInt(m_planetNames.size());
        String name = m_planetNames.get(index);
        m_planetNames.remove(index);
        return name;
    }
}
