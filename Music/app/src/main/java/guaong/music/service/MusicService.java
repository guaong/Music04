package guaong.music.service;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.IBinder;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Timer;

import guaong.music.entity.Music;

public class MusicService extends Service {
    private MusicPlayerBinder mPlayerBinder;

    private MediaPlayer mMediaPlayer;

    private Timer mTimingTimer = new Timer();

    private Timer mAnimationTimer = new Timer();

    private Timer mCountDownTimer = new Timer();

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mPlayerBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // 取得音乐列表
        ArrayList<Music> mMusicList = intent.getParcelableArrayListExtra("musicList");
        if (mMusicList != null && mMusicList.size() > 0){
            mMediaPlayer = MediaPlayer.create(getBaseContext(), Uri.parse(mMusicList.get(0).getUri()));
            mPlayerBinder = new MusicPlayerBinder(getBaseContext(), mMediaPlayer, mMusicList, mTimingTimer, mAnimationTimer, mCountDownTimer);
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        mTimingTimer.cancel();
        mAnimationTimer.cancel();
        mCountDownTimer.cancel();
        mMediaPlayer.stop();
    }
}
