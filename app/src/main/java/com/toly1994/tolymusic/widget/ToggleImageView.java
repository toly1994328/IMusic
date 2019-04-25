package com.toly1994.tolymusic.widget;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import com.toly1994.tolymusic.R;

/**
 * 作者：张风捷特烈<br/>
 * 时间：2018/11/1 0001:12:45<br/>
 * 邮箱：1981462002@qq.com<br/>
 * 说明：
 */
public class ToggleImageView extends android.support.v7.widget.AppCompatImageView {

    private Drawable mOddSrc;
    private boolean isOddClick;//是否是奇数次点击
    private Drawable mEvenSrc;


    public ToggleImageView(Context context) {
        this(context,null);

    }

    public ToggleImageView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs,0);
    }

    public ToggleImageView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }


    /**
     * 初始化
     *
     * @param attrs 自定义属性
     */
    private void init(AttributeSet attrs) {
        TypedArray ta = attrs == null ? null : getContext()
                .obtainStyledAttributes(attrs, R.styleable.ToggleImageView);
        mOddSrc = ta.getDrawable(R.styleable.ToggleImageView_z_toggle_src);
        ta.recycle();//一定记得回收！！！
        mEvenSrc = getDrawable();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:

                break;
            case MotionEvent.ACTION_UP:
                ObjectAnimator alpha = ObjectAnimator.ofFloat(
                        this, "alpha", 1f, .1f, 1f).setDuration(300);
                alpha.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        //第一次点击--设置mOddSrc
                        if (mOnToggleListener != null) {
                            mOnToggleListener.click(ToggleImageView.this,isOddClick);
                        }
                    }
                });
                alpha.start();

        }

        return true;
    }

    public interface OnToggleListener {
        void click(View view, boolean isOdd);
    }

    private OnToggleListener mOnToggleListener;

    public void setOnToggleListener(OnToggleListener onToggleListener) {
        mOnToggleListener = onToggleListener;
    }
}
