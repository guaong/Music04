package guaong.music.service;

import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import guaong.music.activity.MainActivity;
import guaong.music.entity.Music;
import guaong.music.util.WindowUtil;

import static guaong.music.config.Config.PLAY_LOOP;
import static guaong.music.config.Config.PLAY_RANDOM;
import static guaong.music.config.Config.PLAY_SINGLE;

public class MusicPlayerBinder extends Binder {

    private int mPlayOrder;

    // 当前音乐位置
    private int mCurrentPosition = 0;

    private MediaPlayer mMediaPlayer;

    private Context mContext;

    private ArrayList<Music> mMusicList;

    private Timer timingTimer;
    private Timer animationTimer;
    private Timer countDownTimer;

    private TimingTask timingTask;
    private TimerTask countDownTask;
    private boolean isCountDown = false;


    MusicPlayerBinder(Context context, MediaPlayer mediaPlayer,
                             ArrayList<Music> musicArrayList, Timer timingTimer, Timer animationTimer, Timer countDownTimer){
        this.timingTimer = timingTimer;
        this.animationTimer = animationTimer;
        this.countDownTimer = countDownTimer;
        mMediaPlayer = mediaPlayer;
        mContext = context;
        mMusicList = musicArrayList;
        initMediaPlayer();
        mPlayOrder = PLAY_LOOP;
        CommunicationTransitStation.sendDurationToAnimation(mMusicList.get(0).getDuration());
        initAnimationTimer();
    }

    /**
     * 初始化音乐播放器
     */
    private void initMediaPlayer(){
        final PlayCompleteListener playCompleteListener = new PlayCompleteListener();
        mMediaPlayer.setOnCompletionListener(playCompleteListener);
    }

    /**
     * 初始化WaterWaveView动画定时器
     */
    private void initAnimationTimer(){
        AnimationTask animationTask = new AnimationTask();
        // 定时向WaterView发送消息更新动画
        animationTimer.schedule(animationTask, 0, 100);
    }

    /**
     * 播放上一曲（activity中onTouch和音乐播放完成监听中使用）
     */
    public void playLast(){
        if (mCurrentPosition == 0) {
            mCurrentPosition = mMusicList.size();
        }
        mCurrentPosition = mCurrentPosition - 1;
        play(mCurrentPosition);
    }

    /**
     * 播放下一曲（activity中onTouch和音乐播放完成监听中使用）
     */
    public void playNext(){
        mCurrentPosition = (mCurrentPosition + 1) % mMusicList.size();
        play(mCurrentPosition);
    }

    /**
     * 预备下一曲
     */
    public void prepareNext(){
        mMediaPlayer.stop();
        if (mMusicList.size() > 0){
            try {
                mMediaPlayer.reset();
                mMediaPlayer.setDataSource(mContext, Uri.parse(mMusicList.get(mCurrentPosition).getUri()));
                mMediaPlayer.prepare();
                CommunicationTransitStation.sendCurrentTimeToAnimation(mMediaPlayer.getCurrentPosition());
                CommunicationTransitStation.sendStatusToAnimation(true);
                CommunicationTransitStation.sendDurationToAnimation(mMusicList.get(mCurrentPosition).getDuration());
            }catch (IOException e){
                e.printStackTrace();
            }
        }
    }

    /**
     * 播放当前曲目（activity中播放按钮监听使用）
     */
    public void playCurrent(){
        mMediaPlayer.start();
        CommunicationTransitStation.sendStatusToAnimation(false);
    }

    /**
     * 暂停当前曲目（activity中播放按钮监听使用）
     */
    public void pauseCurrent(){
        if (mMediaPlayer.isPlaying()){
            mMediaPlayer.pause();
            CommunicationTransitStation.sendStatusToAnimation(true);
        }
    }

    /**
     * 播放
     * @param current 传入需要播放的下一首的编号
     */
    public void play(int current){
        try {
            mMediaPlayer.stop();
            mMediaPlayer.reset();
            mMediaPlayer.setDataSource(mContext, Uri.parse(mMusicList.get(current).getUri()));
            mMediaPlayer.prepare();
            mMediaPlayer.start();
            // 告知WaterWaveView启动动画
            CommunicationTransitStation.sendStatusToAnimation(false);
            // 告知WaterWaveView当前音乐时间
            CommunicationTransitStation.sendDurationToAnimation(mMusicList.get(mCurrentPosition).getDuration());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取当前音乐信息（activity中修改TextView内容使用）
     */
    public Music getCurrentMusic(){
        return mMusicList.get(mCurrentPosition);
    }

    /**
     * 获取当前音乐位置（activity中音乐列表监听使用）
     */
    public int getCurrentPosition(){
        return mCurrentPosition;
    }

    /**
     * 设置当前音乐位置（activity音乐列表item点击使用）
     */
    public void setCurrentPosition(int position){
        mCurrentPosition = position;
    }

    /**
     * 获取所有音乐信息（音乐列表适配器使用）
     */
    public ArrayList<Music> getMusicList(){
        return mMusicList;
    }

    public void setMusicList(ArrayList<Music> musicList){
        mMusicList = musicList;
    }

    /**
     * 设置播放顺序
     */
    public void setPlayOrder(int playOrder) {
        mPlayOrder = playOrder;
    }

    /**
     * 改变播放进度
     */
    public void changePlayProgress(float offset, Context context){
        int musicTime = (int)mMusicList.get(mCurrentPosition).getDuration();
        int offsetTime = (int)(musicTime / WindowUtil.getWindowHeight(context) * offset);
        int time = mMediaPlayer.getCurrentPosition() + offsetTime;
        if (time > musicTime) time = musicTime;
        if (time < 0) time = 0;
        CommunicationTransitStation.sendCurrentTimeToAnimation(time);
        mMediaPlayer.seekTo(time);
    }

    /**
     * 重置播放进度为下一首歌的进度
     */
    public void resetPlayProgress(){
        CommunicationTransitStation.sendDurationToAnimation(mMusicList.get(mCurrentPosition).getDuration());
    }

    /**
     * 与resetPlayProgress不同
     */
    public void clearPlayProgress(){
        // 告知WaterWaveView启动动画
        CommunicationTransitStation.sendStatusToAnimation(true);
        // 告知WaterWaveView当前音乐时间
        CommunicationTransitStation.sendDurationToAnimation(mMusicList.get(mCurrentPosition).getDuration());
    }

    public boolean isPlaying(){
        return mMediaPlayer.isPlaying();
    }

    public boolean haveMusic(){
        return mMusicList.size() > 0;
    }

    public void startTiming(int time){
        if (isCountDown){
            timingTask.cancel();
            countDownTask.cancel();
        }
        timingTask = new TimingTask();
        timingTimer.schedule(timingTask, time);
        countDownTask = new CountDownTask(time);
        countDownTimer.schedule(countDownTask, 0, 1000);
        isCountDown = true;
    }

    public void cancelTiming(){
        if (isCountDown) {
            countDownTask.cancel();
            timingTask.cancel();
            isCountDown = false;
            CommunicationTransitStation.sendTimingStopToMainActivity(MainActivity.timingStopHandler);
        }
    }

    /**
     * 播放完成监听
     */
    private class PlayCompleteListener implements MediaPlayer.OnCompletionListener{

        @Override
        public void onCompletion(MediaPlayer mp) {
            switch (mPlayOrder){
                case PLAY_LOOP:
                    mCurrentPosition = mCurrentPosition % mMusicList.size() + 1;
                    if (mCurrentPosition == mMusicList.size())
                        mCurrentPosition = 0;
                    break;
                case PLAY_RANDOM:
                    mCurrentPosition = (int)(Math.random() * mMusicList.size());
                    break;
                case PLAY_SINGLE:break;
            }
            play(mCurrentPosition);
            // 发送消息给activity，改变TextView改变内容
            CommunicationTransitStation.sendMessageToActivity();
        }
    }

    class TimingTask extends TimerTask {

        @Override
        public void run() {
            cancelTiming();
        }
    }

    class AnimationTask extends TimerTask{
        @Override
        public void run() {
            if (mMediaPlayer.isPlaying()){
                CommunicationTransitStation.sendCurrentTimeToAnimation(mMediaPlayer.getCurrentPosition());
            }
        }
    }

    class CountDownTask extends TimerTask{
        private int time;
        CountDownTask(int time){this.time = time;}
        @Override
        public void run() {
            CommunicationTransitStation.sendCountDownToMainActivity(time-=1000);
        }
    }

}