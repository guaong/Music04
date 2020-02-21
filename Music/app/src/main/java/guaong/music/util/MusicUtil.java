package guaong.music.util;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.provider.MediaStore;

import java.util.ArrayList;
import java.util.List;

import guaong.music.entity.Music;

/**
 * Created by 关桐 on 2018/6/22.
 * 2020/1/9 再次使用
 */
public class MusicUtil {

    public static ArrayList<Music> scanMusicList(Context context) {
        final ArrayList<Music> musicList = new ArrayList<>();
        final ContentResolver contentResolver = context.getContentResolver();
        final Cursor cursor = contentResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                null, null, null, MediaStore.Audio.Media.DEFAULT_SORT_ORDER);
        if (cursor != null && cursor.moveToFirst()) {
            String title;
            String artist;
            long size;
            String uri;
            int id;
            long duration;
            String time;
            while (!cursor.isAfterLast()) {
                title = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE));
                artist = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST));
//                int flag = cutIndex(title);
//                if (flag != -1) {
//                    artist = title.substring(0, flag);
//                    title = title.substring(flag + 2, title.length());
//                }
                size = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE));
                uri = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA));
                id = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID));
                duration = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION));
                time = durationToString(duration);
                if (isLegal(title, artist, size, duration)) {
                    Music music = new Music();
                    music.setTitle(title);
                    music.setArtist(artist);
                    music.setUri(uri);
                    music.setDuration(duration);
                    music.setId(id);
                    music.setSize(size);
                    music.setTime(time);
                    musicList.add(music);
                }
                cursor.moveToNext();


            }
            cursor.close();
        }
        return musicList;
    }

    public static ArrayList<Music> selectMusicList(Context context){
        final ArrayList<Music> musicList = new ArrayList<>();
        MusicDBHelper musicDBHelper = new MusicDBHelper(context, "music", null,1);
        SQLiteDatabase db = musicDBHelper.getWritableDatabase();
        Cursor cursor = db.rawQuery("select * from Music", null);
        if(cursor != null && cursor.moveToFirst()){
            String title;
            String artist;
            long size;
            String uri;
            int id;
            long duration;
            String time;
            while (!cursor.isAfterLast()) {
                title = cursor.getString(cursor.getColumnIndex("title"));
                artist = cursor.getString(cursor.getColumnIndex("artist"));
                size = cursor.getLong(cursor.getColumnIndex("size"));
                uri = cursor.getString(cursor.getColumnIndex("uri"));
                id = cursor.getInt(cursor.getColumnIndex("id"));
                duration = cursor.getLong(cursor.getColumnIndex("duration"));
                time = cursor.getString(cursor.getColumnIndex("time"));
                Music music = new Music();
                music.setTitle(title);
                music.setArtist(artist);
                music.setUri(uri);
                music.setDuration(duration);
                music.setId(id);
                music.setSize(size);
                music.setTime(time);
                musicList.add(music);
                cursor.moveToNext();
            }
            cursor.close();
        }
        return musicList;
    }

    public static void insertMusicList(Context context, ArrayList<Music> musicList){
        MusicDBHelper musicDBHelper = new MusicDBHelper(context, "music", null,1);
        SQLiteDatabase db = musicDBHelper.getWritableDatabase();
        for (Music music:musicList
             ) {
            String sql = music.getId()+",'"+music.getTitle()+"','"+music.getArtist()
                    +"',"+music.getSize()+",'"+music.getUri()
                    +"',"+music.getDuration()+",'"+music.getTime()+"'";
            db.execSQL("insert into Music(id, title, artist, size, uri, duration, time)" +
                    "values(" +sql+
                    ")");
        }
        db.close();
        musicDBHelper.close();
    }

    public static void insertMusic(Context context, Music music){
        MusicDBHelper musicDBHelper = new MusicDBHelper(context, "music", null,1);
        SQLiteDatabase db = musicDBHelper.getWritableDatabase();
        String sql = music.getId()+",'"+music.getTitle()+"','"+music.getArtist()
                +"',"+music.getSize()+",'"+music.getUri()
                +"',"+music.getDuration()+",'"+music.getTime()+"'";
        db.execSQL("insert into Music(id, title, artist, size, uri, duration, time)" +
                "values(" +sql+
                ")");
        db.close();
        musicDBHelper.close();
    }

    public static void deleteMusic(Context context, Music music){
        MusicDBHelper musicDBHelper = new MusicDBHelper(context, "music", null,1);
        SQLiteDatabase db = musicDBHelper.getWritableDatabase();
        db.execSQL("delete from Music where id="+music.getId());
        db.close();
        musicDBHelper.close();
    }

    public static boolean haveMusic(List<Music> musicList) {
        return musicList.size() > 0;
    }

    public static String durationToString(long duration){
        String s = (int) ((duration / 1000) % 60) + "";
        if ((int) ((duration / 1000) % 60) < 10) {
            s = 0 + s;
        }
        return (int) (duration / 60000) + ":" + s;
    }

    private static int cutIndex(String str) {
        return str.indexOf("-");
    }

    private static boolean isLegal(String title, String artist, long size, long duration) {
        final boolean isLegal;
        isLegal = (!"".contains(title))
                && (size > 1024)
                && (!"".contains(artist))
                && (duration > 120000);
        return isLegal;
    }
}
