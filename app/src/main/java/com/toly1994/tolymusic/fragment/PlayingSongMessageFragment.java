package com.toly1994.tolymusic.fragment;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.graphics.Palette;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.toly1994.tolymusic.R;
import com.toly1994.tolymusic.app.domain.Song;
import com.toly1994.tolymusic.app.utils.DensityUtils;
import com.toly1994.tolymusic.app.utils.ImageUtils;

/**
 * 暂停时切换中间出现的fragment
 */

@SuppressWarnings("deprecation")
public class PlayingSongMessageFragment extends Fragment {
    private Song item;
    private View view;
    private TextView tv_album, tv_artist;

    public PlayingSongMessageFragment(Song item) {
        this.item = item;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (view == null) {
            view = View.inflate(getActivity(), R.layout.fragment_song_message, null);
        }
        setViewComponent();
        return view;
    }

    private void setViewComponent() {
        tv_album = view.findViewById(R.id.tv_album);
        tv_artist = view.findViewById(R.id.tv_artist);
        // 设置宽高
        WindowManager windowManager = getActivity().getWindowManager();
        int width = windowManager.getDefaultDisplay().getWidth();
        int height = DensityUtils.dp2px(getActivity(), 150);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(width,
                height);
        LinearLayout root = (LinearLayout) view;
        root.setLayoutParams(params);
        setMessage();
    }

    private void setMessage() {
        String album = null, artist = null;
        Bitmap cover = null;
        if (item != null) {
            album = item.getAlbum().getAlbumName();
            artist = item.getAlbum().getArtist().getSingerName();
            cover = item.getAlbum().getCover();
            if (cover == null) {
                cover = ImageUtils.getArtwork(getActivity(),
                        item.getTitle(), item.getSongId(), item.getAlbum()
                                .getAlbumId(), true);
                item.getAlbum().setCover(cover);
            }
        } else {
            album = getString(R.string.dont_have_playing_song);
            artist = getString(R.string.dont_have_playing_song);
            cover = BitmapFactory.decodeResource(getResources(), R.drawable.default_music_icon);
        }
        tv_album.setText(album);
        tv_artist.setText(artist);
        setSongMessageBgColor(cover);
    }

    private void setSongMessageBgColor(final Bitmap bitmap) {
        Palette.generateAsync(bitmap, palette -> {
            Palette.Swatch vibrant = palette.getVibrantSwatch();
            int themeRgb;
            if (vibrant == null) {
                // 分析不出颜色,默认颜色
                themeRgb = Color.parseColor("#76C2AF");
            } else {
                themeRgb = vibrant.getRgb();
            }
            view.setBackgroundColor(themeRgb);
        });
    }
}
