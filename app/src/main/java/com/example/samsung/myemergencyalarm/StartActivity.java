package com.example.samsung.myemergencyalarm;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;

public class StartActivity extends AppCompatActivity {

    @SuppressLint("HandlerLeak")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ActionBar actionBar = getSupportActionBar();
        // actionBar.hide();
        setContentView(R.layout.activity_start);

        Handler handler = new Handler() {
            public void handleMessage(Message msg){
                super.handleMessage(msg);
                startActivity(new Intent(StartActivity.this, NavigationActivity.class));
                finish();
            }
        };
        handler.sendEmptyMessageDelayed(0, 2000);

    }
}
