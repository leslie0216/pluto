package usask.chl848.pluto;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.net.Uri;
import android.net.wifi.p2p.WifiP2pDevice;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;
import java.util.UUID;

/**
 * client view
 */
public class ClientView extends View {

    Paint m_paint;
    private String m_name;
    private String m_macAddress;
    private int m_color;

    private class RotationVector {
        float m_x;
        float m_y;
        float m_z;
    }

    private Queue<RotationVector> m_rotationVectorQueue;
    private static final int m_filterSize = 15;
    private static final int m_textSize = 70;
    private static final int m_messageTextSize = 50;
    private static final int m_textStrokeWidth = 2;
    private static final int m_boundaryStrokeWidth = 10;

    private String m_message;

    Bitmap m_pic;
    Bitmap m_earth;

    public class RemotePhoneInfo {
        String m_name;
        String m_macAddress;
        int m_color;
        float m_x;
        float m_y;
        float m_z;
        boolean m_isBusy;
        Bitmap m_planet;
    }

    private ArrayList<RemotePhoneInfo> m_remotePhones;
    private float m_remotePhoneRadius;

    private class Ball {
        public int m_ballColor;
        public float m_ballX;
        public float m_ballY;
        public boolean m_isTouched;
        public String m_id;
        public String m_fileName;
        public String m_fileExt;
        public String m_filePath;
        public Uri m_fileUri;
    }

    private ArrayList<Ball> m_balls;
    private int m_touchedBallId;
    private String m_sentBallId;

    private float m_ballRadius;
    private float m_ballBornX;
    private float m_ballBornY;

    private float m_localCoordinateCenterX;
    private float m_localCoordinateCenterY;
    private float m_localCoordinateRadius;

    private boolean m_isAccurate = true;

    private boolean m_showRemoteNames;
    final Handler handler = new Handler();
    Runnable mLongPressed = new Runnable() {
        @Override
        public void run() {
            setShowRemoteNames(true);
            invalidate();
        }
    };

    public ClientView (Context context) {
        super(context);

        m_paint = new Paint();
        m_rotationVectorQueue = new LinkedList<>();
        m_remotePhones = new ArrayList<>();

        setBackgroundColor(Color.TRANSPARENT);
        //setBackgroundResource(R.drawable.bg);
        m_pic = BitmapFactory.decodeResource(this.getResources(), R.drawable.uparrow);
        m_earth = BitmapFactory.decodeResource(this.getResources(), R.drawable.earth);

        m_message = getResources().getString(R.string.noMsg);

        updateThisDevice();

        Random rnd = new Random();
        m_color = Color.argb(255, rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256));

        m_touchedBallId = -1;
        m_sentBallId = "";
        m_balls = new ArrayList<>();

        DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();
        m_ballRadius = displayMetrics.widthPixels * 0.08f;
        m_ballBornX = displayMetrics.widthPixels * 0.5f;
        m_ballBornY = displayMetrics.heightPixels * 0.75f - m_ballRadius * 2.0f;

        m_localCoordinateCenterX = displayMetrics.widthPixels * 0.5f;
        m_localCoordinateCenterY = displayMetrics.heightPixels * 0.5f;
        m_localCoordinateRadius = displayMetrics.widthPixels * 0.5f;

        m_remotePhoneRadius = displayMetrics.widthPixels * 0.05f;

        setShowRemoteNames(false);
    }

    public void updateThisDevice() {
        WifiP2pDevice device = ((MainActivity)getContext()).m_wifiDirectData.getDevice();
        if (device != null) {
            m_name = device.deviceName;
            m_macAddress = device.deviceAddress;

            ((MainActivity) getContext()).setTitle(m_name + " : " + m_macAddress);
        }
    }

    private void setShowRemoteNames(boolean show) {
        m_showRemoteNames = show;
    }

    private boolean getShowRemoteNames() {
        return m_showRemoteNames;
    }

    @Override
    protected  void onDraw(Canvas canvas) {
        showAccuracy(canvas);
        showArrow(canvas);
        showBoundary(canvas);
        showLocalCircleCoordinate(canvas);
        showMessage(canvas);
        showRotationVector(canvas);
        //showBalls(canvas);
        showDiscoveryStatus(canvas);
        showBusy(canvas);
        showDeviceStatus(canvas);
        showEarths(canvas);
    }

    public void showDeviceStatus(Canvas canvas) {
        WifiP2pDevice device = ((MainActivity)getContext()).m_wifiDirectData.getDevice();
        if (device != null) {
            m_paint.setTextSize(m_messageTextSize);
            m_paint.setColor(Color.RED);
            m_paint.setStyle(Paint.Style.FILL_AND_STROKE);
            DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();

            String state = "Status : " + Utility.getWifiDeviceStatus(device.status);

            canvas.drawText(state, (int) (displayMetrics.widthPixels * 0.7), (int) (displayMetrics.heightPixels * 0.15), m_paint);
        }
    }

    public void showDiscoveryStatus(Canvas canvas) {
        m_paint.setTextSize(m_messageTextSize);
        m_paint.setColor(Color.RED);
        m_paint.setStyle(Paint.Style.FILL_AND_STROKE);
        DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();

        String state = "Scan : idle";
        if (((MainActivity)getContext()).m_wifiDirectData.getIsDiscovering()) {
            state = "Scan : Discovering";
        }

        canvas.drawText(state, (int) (displayMetrics.widthPixels * 0.7), (int) (displayMetrics.heightPixels * 0.1), m_paint);
    }

    public void showBusy(Canvas canvas) {
        m_paint.setTextSize(m_messageTextSize);
        m_paint.setColor(Color.RED);
        m_paint.setStyle(Paint.Style.FILL_AND_STROKE);
        DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();

        String state = "free";
        if (((MainActivity)getContext()).getIsBusy()) {
            state = "busy";
        }

        canvas.drawText(state, (int) (displayMetrics.widthPixels * 0.7), (int) (displayMetrics.heightPixels * 0.2), m_paint);
    }

    public void showAccuracy(Canvas canvas) {
        if (!m_isAccurate) {
            m_paint.setTextSize(m_messageTextSize);
            m_paint.setColor(Color.RED);
            m_paint.setStyle(Paint.Style.FILL_AND_STROKE);

            DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();

            String output = getResources().getString(R.string.inaccurate);
            canvas.drawText(output, (int) (displayMetrics.widthPixels * 0.05), (int) (displayMetrics.heightPixels * 0.1), m_paint);
        }
    }

    public void showArrow(Canvas canvas) {
        DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();

        float picWidth = m_pic.getScaledWidth(displayMetrics);
        float picHeight = m_pic.getScaledHeight(displayMetrics);

        float left = displayMetrics.widthPixels * 0.5f - picWidth * 0.25f;
        float top = displayMetrics.heightPixels * 0.02f;
        float right = displayMetrics.widthPixels * 0.5f + picWidth * 0.25f;
        float bottom = displayMetrics.heightPixels * 0.01f + picHeight*0.3f;
        RectF disRect = new RectF(left, top, right, bottom );
        canvas.drawBitmap(m_pic, null, disRect, m_paint);
    }

    public void showBoundary(Canvas canvas) {
        m_paint.setColor(Color.RED);
        m_paint.setStrokeWidth(m_boundaryStrokeWidth);
        m_paint.setStyle(Paint.Style.FILL_AND_STROKE);

        DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();
        canvas.drawLine(0, displayMetrics.heightPixels * 0.75f, displayMetrics.widthPixels, displayMetrics.heightPixels * 0.75f, m_paint);
    }

    public void showLocalCircleCoordinate(Canvas canvas) {
        MainActivity ma = (MainActivity)getContext();

        DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();

        float left = 0.0f;
        float top = displayMetrics.heightPixels * 0.5f - m_localCoordinateRadius;
        float right = displayMetrics.widthPixels;
        float bottom = displayMetrics.heightPixels * 0.5f + m_localCoordinateRadius;
        RectF disRect = new RectF(left, top, right, bottom);

        m_paint.setColor(Color.BLUE);
        m_paint.setStyle(Paint.Style.STROKE);
        m_paint.setStrokeWidth(m_boundaryStrokeWidth);
        canvas.drawArc(disRect, 160.0f, 220.0f, false, m_paint);

        if (ma.m_bluetoothData.isConnected()) {
            showRemotePhones(canvas);
        }
    }

    public void showRemotePhones(Canvas canvas) {
        if (!m_remotePhones.isEmpty()) {
            // copy it for avoiding concurrent modification exception
            ArrayList<RemotePhoneInfo> remotePhones = new ArrayList<>();
            remotePhones.addAll(m_remotePhones);

            for (RemotePhoneInfo info : remotePhones) {
                float angle_remote = calculateRemoteAngleInLocalCoordinate(info.m_z);
                float pointX_remote = m_localCoordinateCenterX + m_localCoordinateRadius * (float) Math.cos(Math.toRadians(angle_remote));
                float pointY_remote = m_localCoordinateCenterY - m_localCoordinateRadius * (float) Math.sin(Math.toRadians(angle_remote));
                //m_paint.setColor(info.m_color);
                //m_paint.setStyle(Paint.Style.FILL_AND_STROKE);
                //canvas.drawCircle(pointX_remote, pointY_remote, m_remotePhoneRadius, m_paint);

                float left = pointX_remote - m_ballRadius;
                float top = pointY_remote - m_ballRadius;
                float right = pointX_remote + m_ballRadius;
                float bottom = pointY_remote + m_ballRadius;
                RectF disRect = new RectF(left, top, right, bottom );
                canvas.drawBitmap(info.m_planet, null, disRect, m_paint);


                if (getShowRemoteNames()) {
                    m_paint.setTextSize(m_textSize);
                    m_paint.setStrokeWidth(m_textStrokeWidth);
                    m_paint.setStyle(Paint.Style.FILL_AND_STROKE);
                    m_paint.setColor(info.m_color);
                    float textX = pointX_remote - m_remotePhoneRadius;
                    float textY = pointY_remote - m_remotePhoneRadius * 1.5f;
                    if (info.m_name.length() > 5) {
                        textX = pointX_remote - m_remotePhoneRadius * 2.0f;
                    }
                    canvas.drawText(info.m_name, textX, textY, m_paint);
                }
            }
        }
    }

    public void showBalls(Canvas canvas) {
        if (!m_balls.isEmpty()) {
            for (Ball ball : m_balls) {
                m_paint.setColor(ball.m_ballColor);
                m_paint.setStyle(Paint.Style.FILL_AND_STROKE);
                canvas.drawCircle(ball.m_ballX, ball.m_ballY, m_ballRadius, m_paint);

                /**
                 * experiment begin
                 */

                m_paint.setStrokeWidth(m_textStrokeWidth);
                m_paint.setTextSize(m_textSize);
                float textX = ball.m_ballX - m_ballRadius;
                float textY = ball.m_ballY - m_ballRadius;
                if (ball.m_fileName.length() > 5) {
                    textX = ball.m_ballX - m_ballRadius * 2.0f;
                }
                canvas.drawText(ball.m_fileName, textX, textY, m_paint);
                /**
                 * experiment end
                 */
            }
        }
    }

    public void showEarths(Canvas canvas) {

        if (!m_balls.isEmpty()) {
            for (Ball ball : m_balls) {
                //m_paint.setColor(ball.m_ballColor);
                //m_paint.setStyle(Paint.Style.FILL_AND_STROKE);
                //Bitmap earth = BitmapFactory.decodeResource(this.getResources(), R.drawable.earth);

                //DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();
                //float picWidth = m_pic.getScaledWidth(displayMetrics);
                //float picHeight = m_pic.getScaledHeight(displayMetrics);

                float left = ball.m_ballX - m_ballRadius;
                float top = ball.m_ballY - m_ballRadius;
                float right = ball.m_ballX + m_ballRadius;
                float bottom = ball.m_ballY + m_ballRadius;
                RectF disRect = new RectF(left, top, right, bottom );
                canvas.drawBitmap(m_earth, null, disRect, m_paint);

                //canvas.drawCircle(ball.m_ballX, ball.m_ballY, m_ballRadius, m_paint);

                /**
                 * experiment begin
                 */

                m_paint.setStrokeWidth(m_textStrokeWidth);
                m_paint.setTextSize(m_textSize);
                float textX = ball.m_ballX - m_ballRadius;
                float textY = ball.m_ballY - m_ballRadius;
                if (ball.m_fileName.length() > 5) {
                    textX = ball.m_ballX - m_ballRadius * 2.0f;
                }
                canvas.drawText(ball.m_fileName, textX, textY, m_paint);
                /**
                 * experiment end
                 */
            }
        }
    }

    public void showMessage(Canvas canvas) {
        m_paint.setTextSize(m_messageTextSize);
        m_paint.setColor(Color.GREEN);
        m_paint.setStrokeWidth(m_textStrokeWidth);
        m_paint.setStyle(Paint.Style.FILL_AND_STROKE);
        DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();
        canvas.drawText(m_message, displayMetrics.widthPixels * 0.3f, displayMetrics.heightPixels * 0.8f, m_paint);
    }

    public void showRotationVector(Canvas canvas) {
        m_paint.setTextSize(m_messageTextSize);
        m_paint.setColor(Color.RED);
        m_paint.setStyle(Paint.Style.FILL_AND_STROKE);

        DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();

        RotationVector rotationVector = getRotationVector();

        //String output = "Z = " + String.format("%.3f",rotationVector.m_z) + " X = " + String.format("%.3f", rotationVector.m_x) + " Y = " + String.format("%.3f", rotationVector.m_y);
        String output = "Azimuth = " + String.format("%.3f", rotationVector.m_z);
        canvas.drawText(output, (int) (displayMetrics.widthPixels * 0.3), (int) (displayMetrics.heightPixels * 0.85), m_paint);
    }

    private float calculateRemoteAngleInLocalCoordinate(float raw_angle) {
        float angle = 90.0f - getRotationVector().m_z + 180.0f;
        float angle_remote = 90.0f - raw_angle + 180.0f;

        float included_angle = Math.abs(angle - angle_remote);
        boolean greaterThan180 = false;

        if (included_angle > 180.0f) {
            included_angle = 360.0f - included_angle;
            greaterThan180 = true;
        }

        float new_remote_angle;

        float intersect_angle = (180.0f - included_angle) / 2.0f;

        if (angle > angle_remote) {
            if (!greaterThan180) {
                new_remote_angle = 90.0f + intersect_angle;
            } else {
                new_remote_angle = 90.0f - intersect_angle;
            }
        } else {
            if (!greaterThan180) {
                new_remote_angle = 90.0f - intersect_angle;
            } else {
                new_remote_angle = 90.0f + intersect_angle;
            }
        }

        return new_remote_angle;
    }

    public void setRotation(float[] values) {
        RotationVector rotationVector = new RotationVector();
        rotationVector.m_x = values[1];
        rotationVector.m_y = values[2];
        rotationVector.m_z = values[0];

        int size = m_rotationVectorQueue.size();
        if (size >= m_filterSize) {
            m_rotationVectorQueue.poll();
        }

        m_rotationVectorQueue.offer(rotationVector);
        this.invalidate();
    }

    public void setIsAccurate (boolean isAccurate) {
        m_isAccurate = isAccurate;
    }

    private RotationVector getRotationVector() {
        RotationVector rotationVector = new RotationVector();
        float x, y, z;
        x = y = z = 0.0f;
        int size = m_rotationVectorQueue.size();
        for (RotationVector rv : m_rotationVectorQueue) {
            x += rv.m_x;
            y += rv.m_y;
            z += rv.m_z;
        }
        rotationVector.m_x = x/size;
        rotationVector.m_y = y/size;
        rotationVector.m_z = z/size;

        return rotationVector;
    }

    public void setMessage (String msg) {
        m_message = msg;
    }

    public void updateRemotePhone(String name, String macAddress, int color, float x, float y, float z, boolean isBusy, String planetName){
        if (macAddress.isEmpty() || macAddress.equalsIgnoreCase(m_macAddress)) {
            return;
        }

        int size = m_remotePhones.size();
        boolean isFound = false;
        for (int i = 0; i<size; ++i) {
            RemotePhoneInfo info = m_remotePhones.get(i);
            if (info.m_macAddress.equalsIgnoreCase(macAddress)) {
                info.m_name = name;
                info.m_color = color;
                info.m_x = x;
                info.m_y = y;
                info.m_z = z;
                info.m_isBusy = isBusy;

                isFound = true;
                break;
            }
        }

        if (!isFound) {
            RemotePhoneInfo info = new RemotePhoneInfo();
            info.m_name = name;
            info.m_macAddress = macAddress;
            info.m_color = color;
            info.m_x = x;
            info.m_y = y;
            info.m_z = z;
            info.m_isBusy = isBusy;
            int planetId = Utility.getPlanetIdByName(planetName);
            Bitmap planet = BitmapFactory.decodeResource(this.getResources(), planetId);
            info.m_planet = planet;

            m_remotePhones.add(info);
        }
    }

    public ArrayList<RemotePhoneInfo> getRemotePhones() {
        return m_remotePhones;
    }

    public void removePhones(ArrayList<RemotePhoneInfo> phoneInfos) {
        m_remotePhones.removeAll(phoneInfos);
    }

    public void clearRemotePhoneInfo() {
        m_remotePhones.clear();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int eventaction = event.getAction();

        float X = event.getX();
        float Y = event.getY();
        float touchRadius = event.getTouchMajor();

        int ballCount = m_balls.size();
        switch (eventaction) {
            case MotionEvent.ACTION_DOWN:
                m_touchedBallId = -1;
                for (int i = 0; i < ballCount; ++i){
                    Ball ball = m_balls.get(i);
                    ball.m_isTouched = false;

                    double dist;
                    dist = Math.sqrt(Math.pow((X - ball.m_ballX), 2) + Math.pow((Y - ball.m_ballY), 2));
                    if (dist <= (touchRadius + m_ballRadius)) {
                        ball.m_isTouched = true;
                        m_touchedBallId = i;

                        boolean isOverlap = false;
                        for (int j = 0; j < ballCount; ++j) {
                            if (j != m_touchedBallId) {
                                Ball ball2 = m_balls.get(j);

                                double dist2 = Math.sqrt(Math.pow((X - ball2.m_ballX), 2) + Math.pow((Y - ball2.m_ballY), 2));
                                if (dist2 <= m_ballRadius * 2) {
                                    isOverlap = true;
                                }
                            }
                        }

                        if (!isOverlap && !isBoundary(X, Y)) {
                            ball.m_ballX = X;
                            ball.m_ballY = Y;
                            this.invalidate();
                        }
                    }

                    if (m_touchedBallId > -1)
                    {
                        break;
                    }
                }

                if (m_touchedBallId == -1) {

                    boolean show = false;

                    for (RemotePhoneInfo remotePhone : m_remotePhones) {
                        float angle_remote = calculateRemoteAngleInLocalCoordinate(remotePhone.m_z);
                        float pointX_remote = m_localCoordinateCenterX + m_localCoordinateRadius * (float) Math.cos(Math.toRadians(angle_remote));
                        float pointY_remote = m_localCoordinateCenterY - m_localCoordinateRadius * (float) Math.sin(Math.toRadians(angle_remote));

                        double dist = Math.sqrt(Math.pow((X - pointX_remote),2) + Math.pow((Y - pointY_remote), 2));

                        if (dist <= (touchRadius + m_remotePhoneRadius)) {
                            show = true;
                            break;
                        }
                    }

                    if (show) {
                        handler.postDelayed(mLongPressed, 500);
                    }
                }

                break;
            case MotionEvent.ACTION_MOVE:
                if (getShowRemoteNames()) {
                    boolean show = false;

                    for (RemotePhoneInfo remotePhone : m_remotePhones) {
                        float angle_remote = calculateRemoteAngleInLocalCoordinate(remotePhone.m_z);
                        float pointX_remote = m_localCoordinateCenterX + m_localCoordinateRadius * (float) Math.cos(Math.toRadians(angle_remote));
                        float pointY_remote = m_localCoordinateCenterY - m_localCoordinateRadius * (float) Math.sin(Math.toRadians(angle_remote));

                        double dist = Math.sqrt(Math.pow((X - pointX_remote),2) + Math.pow((Y - pointY_remote), 2));

                        if (dist <= (touchRadius + m_remotePhoneRadius)) {
                            show = true;
                            break;
                        }
                    }

                    if (!show) {
                        handler.removeCallbacks(mLongPressed);
                        setShowRemoteNames(false);
                        invalidate();
                    }
                }

                if (m_touchedBallId > -1) {
                    Ball ball = m_balls.get(m_touchedBallId);
                    if (ball.m_isTouched) {
                        boolean isOverlap = false;

                        for (int j = 0; j < ballCount; ++j) {
                            if (j != m_touchedBallId) {
                                Ball ball2 = m_balls.get(j);

                                double dist = Math.sqrt(Math.pow((X - ball2.m_ballX), 2) + Math.pow((Y - ball2.m_ballY), 2));
                                if (dist <= m_ballRadius * 2) {
                                    isOverlap = true;
                                }
                            }
                        }

                        if (!isOverlap & !isBoundary(X, Y)) {
                            ball.m_ballX = X;
                            ball.m_ballY = Y;
                            this.invalidate();
                        }
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                handler.removeCallbacks(mLongPressed);
                if (getShowRemoteNames()) {
                    setShowRemoteNames(false);
                    invalidate();
                }

                if (m_touchedBallId > -1) {
                    Ball ball = m_balls.get(m_touchedBallId);
                    if (ball.m_isTouched) {
                        boolean isOverlap = false;

                        for (int j = 0; j < ballCount; ++j) {
                            if (j != m_touchedBallId) {
                                Ball ball2 = m_balls.get(j);

                                double dist = Math.sqrt(Math.pow((X - ball2.m_ballX), 2) + Math.pow((Y - ball2.m_ballY), 2));
                                if (dist <= m_ballRadius * 2) {
                                    isOverlap = true;
                                }
                            }
                        }

                        if (!isOverlap) {
                            String address = isSending(ball.m_ballX, ball.m_ballY);
                            if (!address.isEmpty()) {
                                if (isBusy(address)){
                                    ((MainActivity)getContext()).showToast(getResources().getString(R.string.targetIsBusy));
                                } else {
                                    sendBall(ball, address);
                                }
                            }
                        }
                    }
                }

                for (Ball ball : m_balls) {
                    ball.m_isTouched = false;
                }
                break;
        }

        return  true;
    }

    private boolean isBoundary(float x, float y) {
        boolean rt = false;
        DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();

        while (true) {
            if (y > m_localCoordinateCenterY) {
                // check bottom
                if ((y + m_ballRadius) >= (displayMetrics.heightPixels * 0.75f)) {
                    rt = true;
                    break;
                }

                // check left
                if (x - m_ballRadius <= 0.0f) {
                    rt = true;
                    break;
                }

                // check right
                if (x + m_ballRadius >= displayMetrics.widthPixels) {
                    rt = true;
                    break;
                }
            } else {
                //check top
                double dist = Math.sqrt(Math.pow((x - m_localCoordinateCenterX), 2) + Math.pow((y - m_localCoordinateCenterY), 2));
                if (dist + m_ballRadius >= m_localCoordinateRadius) {
                    rt = true;
                }
                break;
            }
            break;
        }

        return rt;
    }

    private String isSending(float x, float y) {
        String receiverAddress = "";
        float rate = 10000.0f;
        if (!m_remotePhones.isEmpty()) {
            for (RemotePhoneInfo remotePhoneInfo : m_remotePhones) {
                float angle_remote = calculateRemoteAngleInLocalCoordinate(remotePhoneInfo.m_z);
                float pointX_remote = m_localCoordinateCenterX + m_localCoordinateRadius * (float) Math.cos(Math.toRadians(angle_remote));
                float pointY_remote = m_localCoordinateCenterY - m_localCoordinateRadius * (float) Math.sin(Math.toRadians(angle_remote));

                double dist = Math.sqrt(Math.pow((x - pointX_remote), 2) + Math.pow((y - pointY_remote), 2));
                if (dist < (m_remotePhoneRadius + m_ballRadius)){
                    if (dist < rate) {
                        receiverAddress = remotePhoneInfo.m_macAddress;
                        rate = (float)dist;
                    }
                }
            }
        }

        return receiverAddress;
    }

    private boolean isBusy(String address) {
        boolean rt = false;
        for (RemotePhoneInfo remotePhoneInfo : m_remotePhones) {
            if (remotePhoneInfo.m_macAddress.equals(address)){
                if (remotePhoneInfo.m_isBusy) {
                    rt = true;
                }
                break;
            }
        }

        return rt;
    }

    public void addBall(Uri fileUri) {
        //String filePath = Utility.getRealFilePath(getContext(), fileUri);
        String filePath = FileUtils.getPath(getContext(), fileUri);
        PlutoLogger.Instance().write("ClietView::addBall(), choose file: " + filePath);
        //String path = Utility.getPath(getContext(), fileUri);
        Ball ball = new Ball();
        Random rnd = new Random();
        ball.m_ballColor = Color.argb(255, rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256));
        ball.m_ballX = m_ballBornX;
        ball.m_ballY = m_ballBornY;
        ball.m_isTouched = false;
        ball.m_id = UUID.randomUUID().toString();
        ball.m_fileName = Utility.getFileName(filePath);
        ball.m_fileExt = Utility.getExtensionName(filePath);
        ball.m_filePath = filePath;
        ball.m_fileUri = fileUri;
        m_balls.add(ball);

        this.invalidate();
    }

    public  void removeBall() {
        for (Ball ball : m_balls) {
            if (ball.m_id.equalsIgnoreCase(m_sentBallId)) {
                m_balls.remove(ball);
                m_touchedBallId = -1;
                break;
            }
        }

        this.invalidate();
    }

    public void sendBall(Ball ball, String receiverAddress ) {
        JSONObject jsonObject = new JSONObject();
        RotationVector rotationVector = getRotationVector();
        try {
            jsonObject.put("ballId", ball.m_id);
            jsonObject.put("ballColor", ball.m_ballColor);
            jsonObject.put("receiverAddress", receiverAddress);
            jsonObject.put("isSendingBall", true);
            jsonObject.put("name", m_name);
            jsonObject.put("address", m_macAddress);
            jsonObject.put("z", rotationVector.m_z);
            jsonObject.put("x", rotationVector.m_z);
            jsonObject.put("y", rotationVector.m_z);
            jsonObject.put("color", m_color);
            boolean isBusy = true;
            MainActivity ma = (MainActivity)getContext();
            if (ma != null) {
                ma.setIsBusy(true);
                isBusy = ma.getIsBusy();
            }
            jsonObject.put("isBusy", isBusy);
        } catch (JSONException e){
            e.printStackTrace();
        }

        MainActivity ma = (MainActivity)getContext();
        if (ma != null) {
            ma.m_bluetoothData.addMessage(jsonObject.toString());
            //ma.m_bluetoothData.sendMessage();
            ma.m_wifiDirectData.setFilePath(ball.m_filePath);
            m_sentBallId = ball.m_id;
            ma.startWifiDirectConnection(receiverAddress);
        }
    }


    public void sendRotation(){
        JSONObject msg = new JSONObject();
        RotationVector rotationVector = getRotationVector();
        try {
            msg.put("name", m_name);
            msg.put("address", m_macAddress);
            msg.put("z", rotationVector.m_z);
            msg.put("x", rotationVector.m_x);
            msg.put("y", rotationVector.m_y);
            msg.put("color", m_color);
            msg.put("isSendingBall", false);
            boolean isBusy = true;
            MainActivity ma = (MainActivity)getContext();
            if (ma != null) {
                isBusy = ma.getIsBusy();
            }
            msg.put("isBusy", isBusy);
        } catch (JSONException e){
            e.printStackTrace();
        }

        MainActivity ma = (MainActivity)getContext();
        if (ma != null) {
            ma.m_bluetoothData.addMessage(msg.toString());
        }
    }
}
