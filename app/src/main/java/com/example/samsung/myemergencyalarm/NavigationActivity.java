package com.example.samsung.myemergencyalarm;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.Vibrator;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

public class NavigationActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private AudioReader audioReader;
    private int sampleRate = 8000;
    private int inputBlockSize = 256;
    private int sampleDecimate = 1;
    private boolean isStart=false;
    private TextView dBt;
    private Button dBbtn;
    private int curDB=0;

    NotificationManager nm;
    Notification.Builder builder;
    Intent push;
    PendingIntent fullScreenPendingIntent;

    //화면깨우기 변수
    private static PowerManager.WakeLock sCpuWakeLock;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        // Button setting = (Button) findViewById(R.id.setting);
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        PowerManager pm = (PowerManager) getBaseContext().getSystemService(Context.POWER_SERVICE);
        sCpuWakeLock = pm.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK |
                        PowerManager.ACQUIRE_CAUSES_WAKEUP |
                        PowerManager.ON_AFTER_RELEASE, "hi");

        //팝업공지
        builder = new Notification.Builder(this);
        builder.setSmallIcon(R.mipmap.ic_launcher);
        //builder.setTicker("Test1"); //** 이 부분은 확인 필요
        builder.setWhen(System.currentTimeMillis());
        //builder.setContentTitle("Sound generation"); //** 큰 텍스트로 표시
        builder.setAutoCancel(true);
        builder.setPriority(Notification.PRIORITY_MAX); //** MAX 나 HIGH로 줘야 가능함

        nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        //** Intent와 PendingIntent를 추가해 주는 것으로 헤드업 알림이 가능
        //** 없을 경우 이전 버전의 Notification과 동일
        push = new Intent();
        push.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        //push.setClass(this, MainActivity.class);

        fullScreenPendingIntent = PendingIntent.getActivity(this, 0, push, PendingIntent.FLAG_CANCEL_CURRENT);
        builder.setFullScreenIntent(fullScreenPendingIntent, true);
        //** 여기까지 헤드업 알림을 사용하기 위한 필수 조건!

        //  mouthImage = (ImageView)findViewById(R.id.mounthHolder);
        //   mouthImage.setKeepScreenOn(true);
        final Vibrator vibrator = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);

        if(Build.VERSION.SDK_INT>=23){
            if(checkSelfPermission((Manifest.permission.RECORD_AUDIO))== PackageManager.PERMISSION_GRANTED){

            }else{
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO},1);
            }
        }

        audioReader = new AudioReader();
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        //dBt = (TextView)findViewById(R.id.dBt);
        final TextView tView = (TextView) findViewById(R.id.textView);

        //dBbtn = (Button)findViewById(R.id.Btn);
        Switch sButton = (Switch)findViewById(R.id.switch1);

        //Set a CheckedChange Listener for Switch Button
        sButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){
            @Override
            public void onCheckedChanged(CompoundButton cb, boolean on){
                if(on)
                {
                    //Do something when Switch button is on/checked
                    tView.setTextColor(Color.parseColor("#ff0000"));
                    tView.setText("Connected");

                    if(!isStart){
                        doStart(cb);
                        //dBbtn.setText("Stop");
                        isStart=true;
                    }
                }
                else
                {
                    //Do something when Switch is off/unchecked
                    tView.setTextColor(Color.parseColor("#000000"));
                    tView.setText("Disconnected");

                    doStop(cb);
                    //dBbtn.setText("Start");
                    isStart=false;
                }
            }
        });


    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.navigation, menu);
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
            Intent i = new Intent(NavigationActivity.this, SettingActivity.class);
            startActivity(i);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.about_hassib) {
            // Handle the camera action
        } else if (id == R.id.contact_us) {

        } else if (id == R.id.share) {

        } else if (id == R.id.rating) {
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }


    public void doStart(final View v)
    {
        audioReader.startReader(sampleRate, inputBlockSize * sampleDecimate, new AudioReader.Listener()
        {
            @Override
            public final void onReadComplete(int dB)
            {
                receiveDecibel(dB);
                curDB=dB;
                handler.sendEmptyMessage(0);
            }

            @Override
            public void onReadError(int error)
            {
                handler.sendEmptyMessage(1);
            }
        });
    }

    private void receiveDecibel(final int dB)
    {
        Log.e("###", dB+" dB");
    }

    public void doStop(View v) {
        audioReader.stopReader();
    }

    Handler handler = new Handler(){
        public void handleMessage(Message msg){
            final Vibrator vibrator = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);

            if(msg.what==0){
                //dBt.setText(String.valueOf(curDB)+"dB");
                //long[] pattern = {100,300,100,700,300,2000};
                //vibrator.vibrate(pattern, 0);

                if(curDB > 0 && curDB <= 75){
                    //mouthImage.setImageResource(R.drawable.mouth4);
                    //vibrator.vibrate(1000);
                }else
                if(curDB > 75 && curDB <= 80){
                    //mouthImage.setImageResource(R.drawable.mouth3);

                    //진동 울리기
                    vibrator.vibrate(1000);

                    //잠든화면 깨우기
                    sCpuWakeLock.acquire();

                    //공지 띄우기
                    nm.notify(123456, builder.build());

                    //다이어로그 창 띄우기
                    Context mContext = getApplicationContext();
                    LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(LAYOUT_INFLATER_SERVICE);

                    //R.layout.dialog는 xml 파일명이고  R.id.popup은 보여줄 레이아웃 아이디
                    View layout = inflater.inflate(R.layout.dialog2,(ViewGroup) findViewById(R.id.popup));
                    AlertDialog.Builder aDialog = new AlertDialog.Builder(NavigationActivity.this);

                    //aDialog.setTitle("Sound generation"); //타이틀바 제목
                    aDialog.setView(layout); //dialog.xml 파일을 뷰로 셋팅
                    //다이어로그 창 여기까지!

                    //그냥 닫기버튼을 위한 부분
                    aDialog.setNegativeButton("close", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    });
                    //팝업창 생성
                    AlertDialog ad = aDialog.create();
                    ad.show();//보여줌!

                    // audioReader.stopReader();
                    // dBbtn.setText("Start");
                    // isStart=false;
                }else
                if(curDB > 80 && curDB <= 90){
                    vibrator.vibrate(2000);

                    //잠든화면 깨우기
                    sCpuWakeLock.acquire();

                    //공지 띄우기
                    nm.notify(123456, builder.build());

                    //다이어로그 창 띄우기
                    Context mContext = getApplicationContext();
                    LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(LAYOUT_INFLATER_SERVICE);

                    //R.layout.dialog는 xml 파일명이고  R.id.popup은 보여줄 레이아웃 아이디
                    View layout = inflater.inflate(R.layout.dialog3,(ViewGroup) findViewById(R.id.popup));
                    AlertDialog.Builder aDialog = new AlertDialog.Builder(NavigationActivity.this);

                    aDialog.setTitle("Sound generation"); //타이틀바 제목
                    aDialog.setView(layout); //dialog.xml 파일을 뷰로 셋팅
                    //다이어로그 창 여기까지!

                    //그냥 닫기버튼을 위한 부분
                    aDialog.setNegativeButton("close", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    });
                    //팝업창 생성
                    AlertDialog ad = aDialog.create();
                    ad.show();//보여줌!

                    // audioReader.stopReader();
                    // dBbtn.setText("Start");
                    // isStart=false;
                }
            }else{
                //dBt.setText("ERRdB");
            }
        }
    };
}
