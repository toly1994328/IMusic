package com.toly1994.tolymusic.widget;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.MotionEvent;

/**
 * 作者：张风捷特烈<br/>
 * 时间：2018/11/1 0001:12:45<br/>
 * 邮箱：1981462002@qq.com<br/>
 * 说明：
 */
public class AlphaImageView extends android.support.v7.widget.AppCompatImageView {
    public AlphaImageView(Context context) {
        super(context);
    }

    public AlphaImageView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public AlphaImageView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) {
            ObjectAnimator alpha = ObjectAnimator.ofFloat(
                    this, "alpha", 1f, .1f, 1f).setDuration(300);
            alpha.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    if (listener != null) {
                        listener.onClick(AlphaImageView.this);
                    }
                }
            });
            alpha.start();
        }


        return true;
    }

    private OnClickListener listener;

    @Override
    public void setOnClickListener(@Nullable OnClickListener listener) {
        this.listener = listener;
    }

}
