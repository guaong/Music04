package guaong.music.activity;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import guaong.music.service.MusicPlayerBinder;
import guaong.music.service.MusicService;
import guaong.music.R;
import guaong.music.config.ColorConfig;
import guaong.music.entity.Music;
import guaong.music.util.MusicUtil;

public class ListActivity extends AppCompatActivity {

    private ImageButton backBtn;
    private ImageButton addBtn;
    private ImageButton checkBtn;
    private ImageButton deleteBtn;
    private ImageButton cancelBtn;
    private MusicListAdapter musicListAdapter;
    private RelativeLayout listLayout;
    private RecyclerView listRecyclerView;

    private MusicPlayerBinder musicPlayerBinder;
    private ServiceConnection serviceConnection;

    private ArrayList<Music> musicList;
    private ArrayList<View> itemList = new ArrayList<>();

    private int currentMusic;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);
        receiveIntent();
        conService();
        bindService();
        initViews();
    }

    private void receiveIntent(){
        Intent intent = getIntent();
        musicList = intent.getParcelableArrayListExtra("musicList");
        currentMusic = intent.getIntExtra("currentMusic", 0);
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

    private void bindService(){
        Intent intent = new Intent(ListActivity.this, MusicService.class);
        bindService(intent, serviceConnection, BIND_AUTO_CREATE);
    }

    private void initViews(){
        listRecyclerView = findViewById(R.id.listRecycler);
        listRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        musicListAdapter = new MusicListAdapter(musicList);
        listRecyclerView.setAdapter(musicListAdapter);
        backBtn = findViewById(R.id.backBtn);
        checkBtn = findViewById(R.id.checkBtn);
        addBtn = findViewById(R.id.addBtn);
        deleteBtn = findViewById(R.id.deleteBtn);
        cancelBtn = findViewById(R.id.cancelBtn);
        listLayout = findViewById(R.id.listLayout);
        backBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        checkBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                deleteMusic(v);
            }
        });
        addBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {

            }
        });
        cancelBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
               cancelDelete(v);
            }
        });
        deleteBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                v.setVisibility(View.GONE);
                addBtn.setVisibility(View.GONE);
                checkBtn.setVisibility(View.VISIBLE);
                cancelBtn.setVisibility(View.VISIBLE);
                for (View view:itemList
                     ) {
                    view.setVisibility(View.VISIBLE);
                }
            }
        });
        setViewsColor();
    }

    private void cancelDelete(View v){
        v.setVisibility(View.GONE);
        addBtn.setVisibility(View.VISIBLE);
        checkBtn.setVisibility(View.GONE);
        deleteBtn.setVisibility(View.VISIBLE);
        for (View view:itemList
        ) {
            CheckBox checkBox = (CheckBox)view;
            checkBox.setChecked(false);
            checkBox.setVisibility(View.GONE);
        }
    }

    private void deleteMusic(View v){
        v.setVisibility(View.GONE);
        addBtn.setVisibility(View.VISIBLE);
        cancelBtn.setVisibility(View.GONE);
        deleteBtn.setVisibility(View.VISIBLE);
        Iterator<View> iterator = itemList.iterator();
        while (iterator.hasNext()){
            CheckBox c = (CheckBox)iterator.next();
            if (c.isChecked()){
                c.setChecked(false);
                int k = itemList.indexOf(c);
                MusicUtil.deleteMusic(ListActivity.this, musicList.get(k));
                musicList.remove(k);
                musicPlayerBinder.setMusicList(musicList);
                if (musicPlayerBinder.getCurrentPosition() == itemList.indexOf(c)){
                    musicPlayerBinder.prepareNext();
                }
                iterator.remove();
            }
            c.setVisibility(View.GONE);
        }
        musicListAdapter.notifyDataSetChanged();
    }

    class MusicListAdapter extends RecyclerView.Adapter<MusicListAdapter.MusicListHolder> {

        private List<Music> musicList;

        MusicListAdapter(List<Music> list) {
            musicList = list;
        }


        @Override
        public MusicListHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            final View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_list_child, parent, false);
            return new MusicListHolder(view);
        }

        @Override
        public void onBindViewHolder(MusicListHolder holder, int position) {
            holder.authorText.setText(musicList.get(position).getArtist());
            holder.nameText.setText(musicList.get(position).getTitle());
            holder.timeText.setText(musicList.get(position).getTime());
            holder.layout.setOnClickListener(new ItemClickListener(position));
            if (position == currentMusic) {
                setTextColor(holder, ColorConfig.waterColor);
            } else {
                setTextColor(holder, ColorConfig.PAINT_COLOR);
            }
            itemList.add(holder.checkBox);
        }

        @Override
        public int getItemCount() {
            return musicList.size();
        }

        private void setTextColor(MusicListHolder holder, int color){
            holder.authorText.setTextColor(color);
            holder.nameText.setTextColor(color);
            holder.timeText.setTextColor(color);
        }

        class MusicListHolder extends RecyclerView.ViewHolder {

            private TextView nameText;
            private TextView authorText;
            private TextView timeText;
            private RelativeLayout layout;
            private CheckBox checkBox;

            MusicListHolder(View itemView) {
                super(itemView);
                nameText = itemView.findViewById(R.id.itemNameText);
                authorText = itemView.findViewById(R.id.itemAuthorText);
                timeText = itemView.findViewById(R.id.itemTimeText);
                layout = itemView.findViewById(R.id.listChildLayout);
                checkBox = itemView.findViewById(R.id.itemChecked);
//                nameText.setTypeface(mTypeface);
//                authorText.setTypeface(mTypeface);
//                timeText.setTypeface(mTypeface);
            }
        }

    }

    /**
     * recycler的item点击
     */
    class ItemClickListener implements View.OnClickListener {

        int mPosition;

        ItemClickListener(int position) {
            mPosition = position;
        }

        @Override
        public void onClick(View v) {
            musicPlayerBinder.play(mPosition);
            currentMusic = mPosition;
            musicPlayerBinder.setCurrentPosition(currentMusic);
            musicListAdapter.notifyDataSetChanged();
        }
    }

    @Override
    protected void onPause() {
        unbindService(serviceConnection);
        super.onPause();
    }

    private void setViewsColor(){
        listLayout.setBackgroundColor(ColorConfig.backgroundColor);
    }
}
