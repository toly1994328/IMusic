package com.toly1994.tolymusic.fragment;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import com.toly1994.tolymusic.R;
import com.toly1994.tolymusic.app.domain.Song;
import com.toly1994.tolymusic.app.utils.ImageUtils;

public class PlayingSongCoverFragemnt extends Fragment {
    private View view;
    private ImageView iv_big_cover;
    private Song item;

    public PlayingSongCoverFragemnt() {
    }

    public PlayingSongCoverFragemnt(Song item) {
        this.item = item;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (view == null) {
            view = View.inflate(getActivity(), R.layout.fragment_playing_song_message, null);
        }
        setViewComponent();
        return view;
    }

    private void setViewComponent() {
        iv_big_cover = view.findViewById(R.id.iv_big_cover);
        setTopContent();
    }

    private void setTopContent() {
        Bitmap defaultCover = BitmapFactory.decodeResource(getResources(),
                R.drawable.default_music_icon);
        // 设置内容
        if (item != null) {
            Bitmap bitmap = item.getAlbum().getCover();
            if (bitmap == null) {
                bitmap = ImageUtils.getArtwork(getActivity(),
                        item.getTitle(), item.getSongId(), item.getAlbum()
                                .getAlbumId(), true);
                if (bitmap == null) {
                    bitmap = defaultCover;
                }
                item.getAlbum().setCover(bitmap);
            }
            iv_big_cover.setImageBitmap(bitmap);
        } else {
            iv_big_cover.setImageBitmap(defaultCover);
        }
    }
}
