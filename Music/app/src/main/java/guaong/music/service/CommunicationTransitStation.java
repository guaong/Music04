package guaong.music.service;

import android.os.Handler;
import android.os.Message;

import guaong.music.activity.MainActivity;
import guaong.music.components.WaterWaveView;
import guaong.music.util.MusicUtil;

/**
 * Created by 关桐 on 2018/6/22.
 * 信息中转站
 * 用于转发MusicPlayerBinder向WaterWaveView，各个Activity发送的各种消息
 */
public class CommunicationTransitStation {

    public static int TIMING_START = 1;
    public static int TIMING_STOP = 2;
    // 音乐播放完成
    public static int MUSIC_PLAY_COMPLETE = 1;

    /**
     * 向activity发送消息
     */
    static void sendMessageToActivity(){
        final Message message = new Message();
        message.arg1 = MUSIC_PLAY_COMPLETE;
        // 向activity发送消息，改变音乐信息
        MainActivity.musicInformationChangeHandler.sendMessage(message);
    }

    /**
     * 向MainActivity发送结束定时指令
     */
    static void sendTimingStopToMainActivity(Handler handler){
        final Message message = new Message();
        message.arg1 = TIMING_STOP;
        // 向activity发送消息，改变音乐信息
        handler.sendMessage(message);
    }

    /**
     * 向activity发送倒计时
     */
    static void sendCountDownToMainActivity(int countDown){
        final Message message = new Message();
        message.obj = MusicUtil.durationToString(countDown);
        MainActivity.countDownHandler.sendMessage(message);
    }

    /**
     * 向WaterWaveView发送消息
     * @param musicDuration 时长
     */
    static void sendDurationToAnimation(long musicDuration){
        sendMessageToAnimation(WaterWaveView.RESET_ANIMATION, musicDuration);
    }

    /**
     * 向WaterWaveView发送消息
     * @param b 是否停止动画
     */
    static void sendStatusToAnimation(boolean b){
        sendMessageToAnimation(WaterWaveView.CHANGE_ANIMATION, b);
    }

    /**
     * 向WaterView发送消息
     * @param currentTime 当前音乐时间
     */
    static void sendCurrentTimeToAnimation(int currentTime){
        sendMessageToAnimation(WaterWaveView.UPDATE_ANIMATION, currentTime);
    }

    /**
     * 发送消息到WaterView
     */
    private static void sendMessageToAnimation(int type, Object o){
        final Message message = new Message();
        message.arg1 = type;
        message.obj = o;
        WaterWaveView.animationHandler.sendMessage(message);
    }

}
