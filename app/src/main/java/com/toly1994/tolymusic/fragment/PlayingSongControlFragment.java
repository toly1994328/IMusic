package com.toly1994.tolymusic.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import com.toly1994.tolymusic.R;
import com.toly1994.tolymusic.app.domain.Song;
import com.toly1994.tolymusic.app.utils.DensityUtils;
import com.toly1994.tolymusic.app.utils.MTextUtils;
import com.toly1994.tolymusic.four.activity.PlayMusicActivity;
import com.toly1994.tolymusic.four.service.PlayingService;

import java.util.Timer;
import java.util.TimerTask;

public class PlayingSongControlFragment extends Fragment implements OnClickListener {
    private static final PlayingSongControlFragment pFragment = new PlayingSongControlFragment();
    private static Song playingSong;
    private View view;
    private SeekBar sbar_song;
    private ImageView iv_last_music, iv_pause_music, iv_next_music;
    private TextView tv_now_progress, tv_song_duration, tv_song_name, tv_singer_name;
    private MediaPlayer player;
    private Timer timer;
    private OperateFinishReceiver finishReceiver;

    private PlayingSongControlFragment() {
    }

    ;

    public static PlayingSongControlFragment getInstance(Song playingSong) {
        PlayingSongControlFragment.playingSong = playingSong;
        return pFragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (view == null) {
            view = View.inflate(getActivity(), R.layout.fragment_song_control, null);
        }

        // 开始间隔1秒更新
        timer = new Timer();

        setViewComponent();
        return view;
    }

    @Override
    public void onStart() {
        if (finishReceiver == null) {
            finishReceiver = new OperateFinishReceiver();
            IntentFilter filter = new IntentFilter(PlayingService.OPERATE_FINISH);
            getActivity().registerReceiver(finishReceiver, filter);
        }

        // 更新进度
        initSeekBar();
        super.onStart();
    }

    @Override
    public void onStop() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        super.onStop();
    }

    @Override
    public void onDestroy() {

        if (finishReceiver != null) {
            getActivity().unregisterReceiver(finishReceiver);
            finishReceiver = null;
        }
        super.onDestroy();
    }

    private static final String TAG = "PlayingSongControlFragm";

    private void setViewComponent() {
        sbar_song = view.findViewById(R.id.sbar_song);
        iv_last_music = view.findViewById(R.id.iv_last_music);
        iv_pause_music = view.findViewById(R.id.iv_pause_music);
        iv_next_music = view.findViewById(R.id.iv_next_music);
        tv_now_progress = view.findViewById(R.id.tv_now_progress);
        tv_song_duration = view.findViewById(R.id.tv_song_duration);
        tv_song_name = view.findViewById(R.id.tv_song_name);
        tv_singer_name = view.findViewById(R.id.tv_singer_name);
        // 设置宽高
        WindowManager windowManager = getActivity().getWindowManager();
        int width = windowManager.getDefaultDisplay().getWidth();
        int height = DensityUtils.dp2px(getActivity(), 150);
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(width,
                height);
        RelativeLayout root = (RelativeLayout) view;
        root.setLayoutParams(params);
        // 开启进度条更新
        iv_pause_music.setOnClickListener(this);
        iv_last_music.setOnClickListener(this);
        iv_next_music.setOnClickListener(this);

        player = PlayingService.mediaPlayer;
        tv_song_duration.setText(MTextUtils.long2Minute(player.getDuration()));
        tv_song_name.setText(playingSong.getTitle());
        tv_singer_name.setText(playingSong.getAlbum().getArtist().getSingerName());

        TimerTask task = new TimerTask() {
            public void run() {
                sbar_song.setProgress(PlayingService.mediaPlayer.getCurrentPosition());
            }
        };
        timer.schedule(task, 0, 1000);
    }

    /**
     * 控制seekbar更新
     */
    private void initSeekBar() {
        // 清空上一次的进度
        sbar_song.setProgress(0);
        sbar_song.setMax(PlayingService.mediaPlayer.getDuration());
        // 设置监听器
        sbar_song.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // 进度条改变,歌曲进度也改变
                player.seekTo(seekBar.getProgress());
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,
                                          boolean fromUser) {
                tv_now_progress.setText(MTextUtils.long2Minute(seekBar.getProgress()));
            }
        });
    }


    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.iv_pause_music:
                Intent send = new Intent(getActivity(), PlayingService.class);
                send.putExtra("action", PlayingService.INTENT_PAUSE_MUSIC);
                getActivity().startService(send);
                break;
            case R.id.iv_last_music:
                Intent otherSend = new Intent(getActivity(), PlayingService.class);
                otherSend.putExtra("action", PlayingService.INTENT_LAST_MUSIC);
                getActivity().startService(otherSend);
                break;
            case R.id.iv_next_music:
                Intent otherSend2 = new Intent(getActivity(), PlayingService.class);
                otherSend2.putExtra("action", PlayingService.INTENT_NEXT_MUSIC);
                getActivity().startService(otherSend2);
                break;
        }

    }

    /**
     * 接收播放服务操作(上一首,下一首)完成的广播
     *
     * @author lbRoNG
     */
    private class OperateFinishReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String intent_type = intent.getStringExtra("intent_type");
            // 服务内对歌曲的操作完成
            if (PlayingService.INTENT_PLAYLIST_START_MUSIC.equals(intent_type)) {
                initSeekBar();
            } else if (PlayingService.INTENT_LAST_MUSIC.equals(intent_type)) {
                initSeekBar();
            } else if (PlayingService.INTENT_NEXT_MUSIC.equals(intent_type)) {
                initSeekBar();
            } else if (PlayingService.INTENT_NEXT_RANDOM_MUSIC.equals(intent_type)) {
                initSeekBar();
            } else if (PlayingService.INTENT_NEXT_LOOP_MUSIC.equals(intent_type)) {
                initSeekBar();
            }
        }
    }
}
