package guaong.music.activity;

import android.Manifest;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import guaong.music.config.ColorConfig;
import guaong.music.service.CommunicationTransitStation;
import guaong.music.service.MusicPlayerBinder;
import guaong.music.service.MusicService;
import guaong.music.components.WaterWaveView;
import guaong.music.config.Config;
import guaong.music.util.MusicUtil;
import guaong.music.R;
import guaong.music.components.MarqueeTextView;
import guaong.music.entity.Music;

public class MainActivity extends AppCompatActivity {

    private ImageButton listBtn;
    private ImageButton settingsBtn;
    private ImageButton playOrderBtn;
    private MarqueeTextView nameText;
    private MarqueeTextView authorText;
    private WaterWaveView waterWaveView;
    private Button timingBtn;
    private ConstraintLayout mainLayout;

    private MusicPlayerBinder musicPlayerBinder;
    private ServiceConnection serviceConnection;
    private Intent serviceIntent;

    private ArrayList<Music> musicList;

    private int[] playStatusRes = {R.drawable.loop, R.drawable.singlecycle, R.drawable.random};

    private float offsetX;
    private float offsetY;
    private float currentY;
    private int playStatus = Config.PLAY_LOOP;
    private boolean isStart = false;
    private boolean isFirstBack = true;
    private boolean haveMusic = false;
    private int timingDuration = 0;

    public static MusicInformationChangeHandler musicInformationChangeHandler;
    public static TimingStopHandler timingStopHandler;
    public static CountDownHandler countDownHandler;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getPermissions();
        initViews();
        initHandlers();
        initMusic();
        conService();
        startService();
    }

    private void getPermissions(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            int haveReadPermission = checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE);
            int haveWritePermission = checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            if (haveReadPermission != PackageManager.PERMISSION_GRANTED
                    || haveWritePermission != PackageManager.PERMISSION_GRANTED) {
                String[] permissions = {Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE};
                requestPermissions(permissions, Config.REQUEST_CODE);
            }
        }
    }

    private void initViews(){
        listBtn = findViewById(R.id.listBtn);
        settingsBtn = findViewById(R.id.settingsBtn);
        playOrderBtn = findViewById(R.id.playOrderBtn);
        nameText = findViewById(R.id.musicNameText);
        authorText = findViewById(R.id.musicAuthorText);
        waterWaveView = findViewById(R.id.waterWaveView);
        timingBtn = findViewById(R.id.timingBtn);
        mainLayout = findViewById(R.id.mainLayout);
        listBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (haveMusic){
                    toListActivity();
                }
            }
        });
        settingsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
        playOrderBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setNextPlayStatus(v);
            }
        });
        timingBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                showTimingDialog();
            }
        });
        setViewsColor();
    }

    private void initHandlers(){
        musicInformationChangeHandler = new MusicInformationChangeHandler(this);
        timingStopHandler = new TimingStopHandler(this);
        countDownHandler = new CountDownHandler(this);
    }

    private void initMusic(){
        musicList = MusicUtil.selectMusicList(this);
        if (haveMusic = MusicUtil.haveMusic(musicList)){ //判断数据库中是否有数据
            Music music = musicList.get(0);
            nameText.setText(music.getTitle());
            authorText.setText(music.getArtist());
        }else{
            //当数据库没有数据，写入数据库
            musicList = MusicUtil.scanMusicList(this);
            if (haveMusic = MusicUtil.haveMusic(musicList)){
                MusicUtil.insertMusicList(this, musicList);
                Music music = musicList.get(0);
                nameText.setText(music.getTitle());
                authorText.setText(music.getArtist());
            }
        }
    }

    private void conService(){
        serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                musicPlayerBinder = (MusicPlayerBinder) service;
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
            }
        };
    }

    private void startService(){
        serviceIntent = new Intent(MainActivity.this, MusicService.class);
        serviceIntent.putParcelableArrayListExtra("musicList", musicList);
        startService(serviceIntent);
        bindService(serviceIntent, serviceConnection, BIND_AUTO_CREATE);
    }

    private void setNextPlayStatus (View v){
        playStatus = playStatus % 3 + 1;
        int res = playStatusRes[playStatus - 1];
        v.setBackgroundResource(res);
        musicPlayerBinder.setPlayOrder(playStatus);
    }

    private void toListActivity(){
        Intent intent = new Intent(MainActivity.this, ListActivity.class);
        intent.putParcelableArrayListExtra("musicList", musicList);
        intent.putExtra("currentMusic", musicPlayerBinder.getCurrentPosition());
        startActivity(intent);
    }

    private void showTimingDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        String[] time = {"关闭定时", "10分钟", "15分钟", "30分钟", "1小时", "1.5小时", "2小时"};
        builder.setSingleChoiceItems(time, timingDuration, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                int time = 0;
                switch (which){
                    case 0:time = 0;break;
                    case 1:time = 600000;break;
                    case 2:time = 900000;break;
                    case 3:time = 1800000;break;
                    case 4:time = 3600000;break;
                    case 5:time = 5400000;break;
                    case 6:time = 7200000;break;
                }
                timingDuration = which;
                if (which != 0){
                    musicPlayerBinder.startTiming(time);
                }else{
                    musicPlayerBinder.cancelTiming();
                }
                dialog.dismiss();
            }
        })
                .setTitle("定时")
                .setCancelable(false)
                .show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == Config.REQUEST_CODE){
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED){

            }else {
                Toast.makeText(this, "no permission", Toast.LENGTH_SHORT).show();
            }
        }else{
            Toast.makeText(this, "no permission", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (haveMusic){
            switch (event.getAction()){
                case MotionEvent.ACTION_UP:
                    offsetX = offsetX - event.getRawX();
                    offsetY = offsetY - event.getRawY();
                    //根据偏移量执行不同行为
                    if (haveMusic) {
                        executeTouchEvent(offsetX, offsetY);
                    }
                    break;
                case MotionEvent.ACTION_DOWN:
                    offsetX = event.getRawX();
                    offsetY = event.getRawY();
                    currentY = offsetY;
                    break;
                case MotionEvent.ACTION_MOVE:
                    boolean isHorizontal = Math.abs((offsetY - event.getY()) / (offsetX - event.getX())) <= 2f;
                    if (!isHorizontal && haveMusic) {
                        musicPlayerBinder.pauseCurrent();
                        musicPlayerBinder.changePlayProgress(currentY - event.getY(), this);
                        currentY = event.getY();
                    }
            }
        }
        return true;
    }

    private void executeTouchEvent(float offsetX, float offsetY){
        if (Math.abs(offsetX) > 100 || Math.abs(offsetY) > 100){ //滑动事件
            boolean isHorizontal = Math.abs(offsetY / offsetX) <= 0.5f;
            if (isHorizontal){ //水平
                boolean isToLeft = offsetX > 0;
                if (isToLeft){ //左划
                    musicPlayerBinder.playLast();
                }else{ //右划
                    musicPlayerBinder.playNext();
                }
                changeMusicInfo();
            }else {
                musicPlayerBinder.playCurrent();
            }
        }else if(Math.abs(offsetX) < 50 && Math.abs(offsetY) < 50){ //点击事件
            if (isStart){
                isStart = false;
                musicPlayerBinder.pauseCurrent();
            }else {
                isStart = true;
                musicPlayerBinder.playCurrent();
            }
        }
    }

    private void changeMusicInfo(){
        Music music = musicPlayerBinder.getCurrentMusic();
        nameText.setText(music.getTitle());
        authorText.setText(music.getArtist());
    }

    @Override
    protected void onRestart() {
        refreshViews();
        super.onRestart();
    }

    private void refreshViews(){
        musicList = musicPlayerBinder.getMusicList();
        if (musicPlayerBinder.haveMusic()){
            Music music = musicPlayerBinder.getCurrentMusic();
            nameText.setText(music.getTitle());
            authorText.setText(music.getArtist());
            if (musicPlayerBinder.isPlaying()){
                musicPlayerBinder.resetPlayProgress();
            }else {
                musicPlayerBinder.clearPlayProgress();
                isStart = false;
            }
        }else{
            nameText.setText(R.string.music_name);
            authorText.setText(R.string.music_author);
            haveMusic = false;
        }
    }

    @Override
    public void onBackPressed() {
        exitApp();
    }

    private void exitApp(){
        final Timer timer = new Timer();
        if (isFirstBack){
            Toast.makeText(this, "再按一次退出", Toast.LENGTH_SHORT).show();
            isFirstBack = false;
            TimerTask timerTask = new TimerTask() {
                @Override
                public void run() {
                    isFirstBack = true;
                }
            };
            timer.schedule(timerTask, 2000);
        }else {
            unbindService(serviceConnection);
            stopService(serviceIntent);
            finish();
            System.exit(0);
        }
    }

    public static class MusicInformationChangeHandler extends Handler {

        private WeakReference<MainActivity> mWeakReference;

        MusicInformationChangeHandler(MainActivity activity) {
            mWeakReference = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            if (msg.arg1 == CommunicationTransitStation.MUSIC_PLAY_COMPLETE) {
                mWeakReference.get().changeMusicInfo();
            }
        }
    }

    public static class TimingStopHandler extends Handler{

        private WeakReference<MainActivity> mWeakReference;

        TimingStopHandler(MainActivity activity) {
            mWeakReference = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            if (msg.arg1 == CommunicationTransitStation.TIMING_STOP){
                // 告知server停止播放音乐
                mWeakReference.get().musicPlayerBinder.pauseCurrent();
                mWeakReference.get().isStart = false;
                mWeakReference.get().timingBtn.setText("");
                mWeakReference.get().timingBtn.setBackgroundResource(R.drawable.timing_icon);
            }
        }
    }

    public static class CountDownHandler extends Handler{

        private WeakReference<MainActivity> mWeakReference;

        CountDownHandler(MainActivity activity){
            mWeakReference = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            mWeakReference.get().timingBtn.setBackgroundResource(R.drawable.style_button_border);
            String s = (String)msg.obj;
            mWeakReference.get().timingBtn.setText(s);
        }
    }

    private void setViewsColor(){
        waterWaveView.setColor(ColorConfig.waterColor);
        mainLayout.setBackgroundColor(ColorConfig.backgroundColor);
    }
}
