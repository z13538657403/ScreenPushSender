package com.ckzn.jrtptest;

import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity implements View.OnClickListener
{
    private static final int REQUEST_CODE = 1;
    private MediaProjectionManager mMediaProjectionManager;
    private Button mButton;
    private Button mStartButton;
    private Intent mPushScreenIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
    }

    private void initView()
    {
        mButton = (Button) findViewById(R.id.record_screen_btn);
        mStartButton = (Button) findViewById(R.id.start_play_btn);
        mButton.setOnClickListener(this);
        mStartButton.setOnClickListener(this);
        mMediaProjectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
    }

    @Override
    public void onClick(View view)
    {
        switch (view.getId())
        {
            case R.id.record_screen_btn:
                Intent recordIntent = null;
                recordIntent = mMediaProjectionManager.createScreenCaptureIntent();
                startActivityForResult(recordIntent , REQUEST_CODE);
                break;
            case R.id.start_play_btn:
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (requestCode == REQUEST_CODE && resultCode == RESULT_OK)
        {
            mPushScreenIntent = new Intent(MainActivity.this , PushService.class);
            mPushScreenIntent.putExtra("data", data);
            startService(mPushScreenIntent);
            mButton.setText(R.string.stop_record_text);
            Toast.makeText(this , "recording" , Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        stopService(mPushScreenIntent);
    }
}
