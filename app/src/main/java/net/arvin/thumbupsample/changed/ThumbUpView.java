package net.arvin.thumbupsample.changed;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import net.arvin.thumbupsample.R;

/**
 * Created by arvinljw on 17/10/25 14:57
 * Function：
 * Desc：
 */
@SuppressWarnings("ALL")
public class ThumbUpView extends LinearLayout implements View.OnClickListener {
    public static final float DEFAULT_DRAWABLE_PADDING = 4f;
    private ThumbView mThumbView;
    private CountView mCountView;

    private float mDrawablePadding;
    private int mTextColor;
    private int mCount;
    private float mTextSize;
    private boolean mIsThumbUp;
    private int mTopMargin;
    private boolean mNeedChangeChildView;

    public ThumbUpView(Context context) {
        this(context, null);
    }

    public ThumbUpView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ThumbUpView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.ThumbUpView);
        mDrawablePadding = typedArray.getDimension(R.styleable.ThumbUpView_tuv_drawable_padding, TuvUtils.dip2px(context, DEFAULT_DRAWABLE_PADDING));
        mCount = typedArray.getInt(R.styleable.ThumbUpView_tuv_count, 0);
        mTextColor = typedArray.getColor(R.styleable.ThumbUpView_tuv_text_color, Color.parseColor(CountView.DEFAULT_TEXT_COLOR));
        mTextSize = typedArray.getDimension(R.styleable.ThumbUpView_tuv_drawable_padding, TuvUtils.sp2px(context, CountView.DEFAULT_TEXT_SIZE));
        mIsThumbUp = typedArray.getBoolean(R.styleable.ThumbUpView_tuv_isThumbUp, false);
        typedArray.recycle();
        init();
    }

    private void init() {
        removeAllViews();
        setClipChildren(false);
        setOrientation(LinearLayout.HORIZONTAL);

        addThumbView();

        addCountView();

        //把设置的padding分解到子view，否则对超出view范围的动画显示不全
        setPadding(0, 0, 0, 0, false);
        setOnClickListener(this);
    }

    public ThumbUpView setCount(int mCount) {
        this.mCount = mCount;
        mCountView.setCount(mCount);
        return this;
    }

    public ThumbUpView setTextColor(int mTextColor) {
        this.mTextColor = mTextColor;
        mCountView.setTextColor(mCount);
        return this;
    }

    public ThumbUpView setTextSize(float mTextSize) {
        this.mTextSize = mTextSize;
        mCountView.setTextSize(mCount);
        return this;
    }

    public ThumbUpView setThumbUp(boolean isThumbUp) {
        this.mIsThumbUp = isThumbUp;
        mThumbView.setIsThumbUp(mIsThumbUp);
        return this;
    }

    public void setThumbUpClickListener(ThumbView.ThumbUpClickListener listener){
        mThumbView.setThumbUpClickListener(listener);
    }

    @Override
    public void setPadding(int left, int top, int right, int bottom) {
        if (mNeedChangeChildView) {
            resetThumbParams();
            resetCountViewParams();
            mNeedChangeChildView = false;
        } else {
            super.setPadding(left, top, right, bottom);
        }
    }

    private void resetThumbParams() {
        LayoutParams params = (LayoutParams) mThumbView.getLayoutParams();
        if (mTopMargin < 0) {
            params.topMargin = mTopMargin;//设置这个距离是为了文字与拇指居中显示
        }
        params.leftMargin = getPaddingLeft();
        params.topMargin += getPaddingTop();
        params.bottomMargin = getPaddingBottom();
        mThumbView.setLayoutParams(params);
    }

    private void resetCountViewParams() {
        LayoutParams params = (LayoutParams) mCountView.getLayoutParams();
        if (mTopMargin > 0) {
            params.topMargin = mTopMargin;//设置这个距离是为了文字与拇指居中显示
        }
        params.leftMargin = (int) mDrawablePadding;
        params.topMargin += getPaddingTop();
        params.bottomMargin = getPaddingBottom();
        params.rightMargin = getPaddingRight();
        mCountView.setLayoutParams(params);
    }

    @SuppressWarnings("SameParameterValue")
    public void setPadding(int left, int top, int right, int bottom, boolean needChange) {
        this.mNeedChangeChildView = needChange;
        setPadding(left, top, right, bottom);
    }

    private void addThumbView() {
        mThumbView = new ThumbView(getContext());
        mThumbView.setIsThumbUp(mIsThumbUp);
        TuvPoint circlePoint = mThumbView.getCirclePoint();
        mTopMargin = (int) (circlePoint.y - mTextSize / 2);
        addView(mThumbView, getThumbParams());
    }

    private void addCountView() {
        mCountView = new CountView(getContext());
        mCountView.setTextColor(mTextColor);
        mCountView.setTextSize(mTextSize);
        mCountView.setCount(mCount);

        addView(mCountView, getCountParams());
    }

    private LayoutParams getThumbParams() {
        LayoutParams params = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        if (mTopMargin < 0) {
            params.topMargin = mTopMargin;//设置这个距离是为了文字与拇指居中显示
        }
        params.leftMargin = getPaddingLeft();
        params.topMargin += getPaddingTop();
        params.bottomMargin = getPaddingBottom();
        return params;
    }

    private LayoutParams getCountParams() {
        LayoutParams params = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        if (mTopMargin > 0) {
            params.topMargin = mTopMargin;//设置这个距离是为了文字与拇指居中显示
        }
        params.leftMargin = (int) mDrawablePadding;
        params.topMargin += getPaddingTop();
        params.bottomMargin = getPaddingBottom();
        params.rightMargin = getPaddingRight();
        return params;
    }

    @Override
    public void onClick(View v) {
        mIsThumbUp = !mIsThumbUp;
        if (mIsThumbUp) {
            mCountView.calculateChangeNum(1);
        } else {
            mCountView.calculateChangeNum(-1);
        }
        mThumbView.startAnim();
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Bundle data = new Bundle();
        data.putParcelable("superData", super.onSaveInstanceState());
        data.putInt("count", mCount);
        data.putFloat("textSize", mTextSize);
        data.putInt("textColor", mTextColor);
        data.putBoolean("isThumbUp", mIsThumbUp);
        data.putFloat("drawablePadding", mDrawablePadding);
        return data;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        Bundle data = (Bundle) state;
        Parcelable superData = data.getParcelable("superData");
        super.onRestoreInstanceState(superData);
        mCount = data.getInt("count");
        mTextSize = data.getFloat("textSize", TuvUtils.sp2px(getContext(), CountView.DEFAULT_TEXT_SIZE));
        mTextColor = data.getInt("textColor", Color.parseColor(CountView.DEFAULT_TEXT_COLOR));
        mIsThumbUp = data.getBoolean("isThumbUp", false);
        mDrawablePadding = data.getFloat("drawablePadding", TuvUtils.sp2px(getContext(), DEFAULT_DRAWABLE_PADDING));
        init();
    }
}
