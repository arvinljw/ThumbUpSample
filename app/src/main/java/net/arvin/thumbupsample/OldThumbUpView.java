package net.arvin.thumbupsample;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.animation.OvershootInterpolator;

/**
 * Created by arvinljw on 17/10/13 12:15
 * Function：
 * Desc：仿即刻App点赞效果
 */
public class OldThumbUpView extends View implements View.OnClickListener {
    private static final float SCALE_MIN = 0.9f;
    private static final float SCALE_MAX = 1f;

    //图标大小
    private static final float THUMB_WIDTH = 20f;
    private static final float THUMB_HEIGHT = 20f;
    private static final float SHINING_WIDTH = 16f;
    private static final float SHINING_HEIGHT = 16f;

    private static final float TEXT_DEFAULT_SIZE = 15;

    //缩放动画的时间
    private static final int SCALE_DURING = 150;
    //圆圈扩散动画的时间
    private static final int RADIUS_DURING = 100;
    //圆圈颜色
    private static final int START_COLOR = Color.parseColor("#00e24d3d");
    private static final int END_COLOR = Color.parseColor("#88e24d3d");
    //文本颜色
    private static final int TEXT_DEFAULT_COLOR = Color.parseColor("#cccccc");
    private static final int TEXT_DEFAULT_END_COLOR = Color.parseColor("#00cccccc");

    private int dp_2;
    private int dp_8;

    //圆圈扩散的最小最大值，根据图标大小计算得出
    private float RADIUS_MIN;
    private float RADIUS_MAX;

    //文本的上下移动变化值
    private float OFFSET_MIN;
    private float OFFSET_MAX;

    //是否点赞
    private boolean isThumbUp;

    private Bitmap thumbUp;
    private Bitmap notThumbUp;
    private Bitmap shining;
    private float mScale;
    private float mRadius;
    private float mCircleX;
    private float mCircleY;
    private Path mClipPath;
    private Paint mBitmapPaint;
    private Paint mCirclePaint;

    private boolean isCanceled;
    private Animator currentAnim;

    private int count;
    private Paint mTextPaint;
    private float textStartX;
    private float textSize;
    private float drawablePadding;

    private String[] nums;//num[0]是不变的部分，nums[1]原来的部分，nums[2]变化后的部分
    private boolean toBigger;
    private float mOldOffsetY;
    private float mNewOffsetY;

    //为了保证居中绘制，这是绘制的起点坐标，减去这个值则为以原点为坐标开始绘制的
    private int startX;
    private int startY;

    private long lastClickTime;
    //点击的回调
    private ThumbUpClickListener thumbUpClickListener;

    public OldThumbUpView(Context context) {
        this(context, null);
    }

    public OldThumbUpView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public OldThumbUpView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.OldThumbUpView);
        count = typedArray.getInt(R.styleable.OldThumbUpView_o_tuv_count, 0);
        typedArray.recycle();
        init();
    }

    private void init() {
        initSize();
        mBitmapPaint = new Paint();
        mBitmapPaint.setAntiAlias(true);

        mCirclePaint = new Paint();
        mCirclePaint.setAntiAlias(true);
        mCirclePaint.setStyle(Paint.Style.STROKE);
        mCirclePaint.setStrokeWidth(dp_2);

        mTextPaint = new Paint();
        mTextPaint.setAntiAlias(true);
        mTextPaint.setTextSize(sp2px(textSize));
        mTextPaint.setColor(TEXT_DEFAULT_COLOR);

        thumbUp = BitmapFactory.decodeResource(getResources(), R.drawable.ic_messages_like_selected);
        notThumbUp = BitmapFactory.decodeResource(getResources(), R.drawable.ic_messages_like_unselected);
        shining = BitmapFactory.decodeResource(getResources(), R.drawable.ic_messages_like_selected_shining);

        mClipPath = new Path();
        mClipPath.addCircle(mCircleX, mCircleY, RADIUS_MAX, Path.Direction.CW);
        mCirclePaint.setColor(START_COLOR);

        setOnClickListener(this);
    }

    private void initSize() {
        dp_2 = dip2px(2);
        dp_8 = dip2px(8);
        mScale = 1;

        RADIUS_MIN = dp_8;//圆扩散的最小半径
        RADIUS_MAX = dip2px(16);//为了包住拇指和点，以中点为中心的最小半径，即扩散的最大半径

        mCircleX = dip2px(THUMB_WIDTH / 2);
        mCircleY = dip2px(18);//这个距离是拇指的中点的位置

        drawablePadding = dp_2 * 2;
        textStartX = dip2px(THUMB_WIDTH) + drawablePadding;
        textSize = TEXT_DEFAULT_SIZE;
        nums = new String[]{String.valueOf(count), "", ""};
        OFFSET_MIN = 0;
        OFFSET_MAX = 1.5f * sp2px(textSize);

    }

    private int dip2px(float dpValue) {
        final float scale = getContext().getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    private int sp2px(float spValue) {
        final float fontScale = getContext().getResources().getDisplayMetrics().scaledDensity;
        return (int) (spValue * fontScale + 0.5f);
    }

    //======自定义属性动画部分======
    public void setNotThumbUpScale(float scale) {
        mScale = scale;
        Matrix matrix = new Matrix();
        matrix.postScale(mScale, mScale);
        notThumbUp = BitmapFactory.decodeResource(getResources(), R.drawable.ic_messages_like_unselected);
        notThumbUp = Bitmap.createBitmap(notThumbUp, 0, 0, notThumbUp.getWidth(), notThumbUp.getHeight(),
                matrix, true);
        postInvalidate();
    }

    public float getNotThumbUpScale() {
        return mScale;
    }

    public void setThumbUpScale(float scale) {
        mScale = scale;
        Matrix matrix = new Matrix();
        matrix.postScale(mScale, mScale);
        thumbUp = BitmapFactory.decodeResource(getResources(), R.drawable.ic_messages_like_selected);
        thumbUp = Bitmap.createBitmap(thumbUp, 0, 0, thumbUp.getWidth(), thumbUp.getHeight(),
                matrix, true);
        postInvalidate();
    }

    public float getThumbUpScale() {
        return mScale;
    }

    public void setCircleScale(float radius) {
        mRadius = radius;
        mClipPath = new Path();
        mClipPath.addCircle(startX + mCircleX, startY + mCircleY, mRadius, Path.Direction.CW);

        float fraction = (RADIUS_MAX - radius) / (RADIUS_MAX - RADIUS_MIN);
        mCirclePaint.setColor((int) evaluate(fraction, START_COLOR, END_COLOR));

        postInvalidate();
    }

    public float getCircleScale() {
        return RADIUS_MAX;
    }

    public void setThumbUpClickListener(ThumbUpClickListener thumbUpClickListener) {
        this.thumbUpClickListener = thumbUpClickListener;
    }

    public void setTextOffsetY(float offsetY) {
        this.mOldOffsetY = offsetY;//变大是从[0,1]，变小是[0,-1]
        if (toBigger) {//从下到上[-1,0]
            this.mNewOffsetY = offsetY - OFFSET_MAX;
        } else {//从上到下[1,0]
            this.mNewOffsetY = OFFSET_MAX + offsetY;
        }
        postInvalidate();
    }

    public float getTextOffsetY() {
        return OFFSET_MIN;
    }
    //======自定义属性动画部分======

    public OldThumbUpView setCount(int count) {
        this.count = count;
        calculateChangeNum(0);
        requestLayout();
        return this;
    }

    public OldThumbUpView setThumbUp(boolean isThumbUp) {
        this.isThumbUp = isThumbUp;
        postInvalidate();
        return this;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(getWidth(widthMeasureSpec), getHeight(heightMeasureSpec));
    }

    private int getWidth(int measureSpec) {
        int result = 0;
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);

        switch (specMode) {
            case MeasureSpec.UNSPECIFIED:
                result = specSize;
                break;
            case MeasureSpec.AT_MOST:
                result = getContentWidth();
                break;
            case MeasureSpec.EXACTLY:
                result = specSize;
                result = Math.max(getContentWidth(), result);
                break;
        }
        return result;
    }

    private int getHeight(int measureSpec) {
        int result = 0;
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);

        switch (specMode) {
            case MeasureSpec.UNSPECIFIED:
                result = specSize;
                break;
            case MeasureSpec.AT_MOST:
                result = getContentHeight();
                break;
            case MeasureSpec.EXACTLY:
                result = specSize;
                result = Math.max(getContentHeight(), result);
                break;
        }
        return result;
    }

    private int getContentWidth() {
        int result;
        result = (int) (dip2px(THUMB_WIDTH) + drawablePadding + mTextPaint.measureText(String.valueOf(count)));
        result += getPaddingLeft() + getPaddingRight();
        return result;
    }

    private int getContentHeight() {
        int result;
        result = Math.max(sp2px(textSize), dip2px(THUMB_HEIGHT + SHINING_HEIGHT) - dp_8);
        result += getPaddingTop() + getPaddingBottom();
        return result;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        startX = (int) ((w - (dip2px(THUMB_WIDTH) + drawablePadding + mTextPaint.measureText(String.valueOf(count)))) / 2);
        startY = (h - Math.max(sp2px(textSize), dip2px(THUMB_HEIGHT + SHINING_HEIGHT) - dp_8)) / 2;

        mClipPath = new Path();
        mClipPath.addCircle(startX + mCircleX, startY + mCircleY, RADIUS_MAX, Path.Direction.CW);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Bundle data = new Bundle();
        data.putParcelable("superData", super.onSaveInstanceState());
        data.putInt("count", count);
        data.putBoolean("isThumbUp", isThumbUp);
        return data;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        Bundle data = (Bundle) state;
        Parcelable superData = data.getParcelable("superData");
        super.onRestoreInstanceState(superData);

        count = data.getInt("count", 0);
        isThumbUp = data.getBoolean("isThumbUp", false);

        init();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawIcon(canvas);
        drawText(canvas);
    }

    private void drawIcon(Canvas canvas) {
        if (isThumbUp) {
            if (mClipPath != null) {
                canvas.save();
                canvas.clipPath(mClipPath);
                canvas.drawBitmap(shining, startX + dp_2, startY, mBitmapPaint);
                canvas.restore();

                canvas.drawCircle(startX + mCircleX, startY + mCircleY, mRadius, mCirclePaint);
            } else {//为了保险，虽然正常情况mClipPath都不会为null
                canvas.drawBitmap(shining, startX + dp_2, startY, mBitmapPaint);
            }

            canvas.drawBitmap(thumbUp, startX, startY + dp_8, mBitmapPaint);
        } else {
            canvas.drawBitmap(notThumbUp, startX, startY + dp_8, mBitmapPaint);
        }
    }

    private void drawText(Canvas canvas) {
        Paint.FontMetricsInt fontMetrics = mTextPaint.getFontMetricsInt();
        float y = (dip2px(THUMB_HEIGHT + SHINING_HEIGHT) - fontMetrics.bottom - fontMetrics.top) / 2;

        mTextPaint.setColor(TEXT_DEFAULT_COLOR);
        canvas.drawText(String.valueOf(nums[0]), startX + textStartX, startY + y, mTextPaint);

        String text = String.valueOf(count);
        float textWidth = mTextPaint.measureText(text) / text.length();
        float fraction = (OFFSET_MAX - Math.abs(mOldOffsetY)) / (OFFSET_MAX - OFFSET_MIN);

        mTextPaint.setColor((Integer) evaluate(fraction, TEXT_DEFAULT_END_COLOR, TEXT_DEFAULT_COLOR));
        canvas.drawText(String.valueOf(nums[1]), startX + textStartX + textWidth * nums[0].length(), startY + y - mOldOffsetY, mTextPaint);

        mTextPaint.setColor((Integer) evaluate(fraction, TEXT_DEFAULT_COLOR, TEXT_DEFAULT_END_COLOR));
        canvas.drawText(String.valueOf(nums[2]), startX + textStartX + textWidth * nums[0].length(), startY + y - mNewOffsetY, mTextPaint);
    }

    /**
     * 计算不变，原来，和改变后各部分的数字
     * 这里是只针对加一和减一去计算的算法，因为直接设置的时候没有动画
     */
    private void calculateChangeNum(int change) {
        if (change == 0) {
            nums[0] = String.valueOf(count);
            nums[1] = "";
            nums[2] = "";
            return;
        }
        toBigger = change > 0;
        String oldNum = String.valueOf(count);
        String newNum = String.valueOf(count + change);

        int oldNumLen = oldNum.length();
        int newNumLen = newNum.length();

        if (oldNumLen != newNumLen) {
            nums[0] = "";
            nums[1] = oldNum;
            nums[2] = newNum;
        } else {
            for (int i = 0; i < oldNumLen; i++) {
                char oldC1 = oldNum.charAt(i);
                char newC1 = newNum.charAt(i);
                if (oldC1 != newC1) {
                    if (i == 0) {
                        nums[0] = "";
                    } else {
                        nums[0] = newNum.substring(0, i);
                    }
                    nums[1] = oldNum.substring(i);
                    nums[2] = newNum.substring(i);
                    break;
                }
            }
        }
    }

    @Override
    public void onClick(View v) {
        if (System.currentTimeMillis() - lastClickTime < SCALE_DURING + RADIUS_DURING) {
            return;
        }
        lastClickTime = System.currentTimeMillis();
        cancelAnim();
        if (isThumbUp) {
            calculateChangeNum(-1);
            count--;
            showThumbDownAnim();
        } else {
            calculateChangeNum(1);
            count++;
//            mClipPath = null;
            showThumbUpAnim();
        }
    }

    private void cancelAnim() {
        isCanceled = true;
        if (currentAnim != null) {
            currentAnim.cancel();
            currentAnim = null;
        }
    }

    private void showThumbUpAnim() {
        ObjectAnimator notThumbUpScale = ObjectAnimator.ofFloat(this, "notThumbUpScale", SCALE_MAX, SCALE_MIN);
        notThumbUpScale.setDuration(SCALE_DURING);
        notThumbUpScale.addListener(new ClickAnimatorListener() {
            @Override
            public void onAnimRealEnd(Animator animation) {
                isThumbUp = true;
            }
        });

        ObjectAnimator textOffsetY = ObjectAnimator.ofFloat(this, "textOffsetY", OFFSET_MIN, OFFSET_MAX);
        textOffsetY.setDuration(SCALE_DURING + RADIUS_DURING);

        ObjectAnimator thumbUpScale = ObjectAnimator.ofFloat(this, "thumbUpScale", SCALE_MIN, SCALE_MAX);
        thumbUpScale.setDuration(SCALE_DURING);
        thumbUpScale.setInterpolator(new OvershootInterpolator());

        ObjectAnimator circleScale = ObjectAnimator.ofFloat(this, "circleScale", RADIUS_MIN, RADIUS_MAX);
        thumbUpScale.setDuration(RADIUS_DURING);

        AnimatorSet set = new AnimatorSet();
        set.play(thumbUpScale).with(circleScale);
        set.play(textOffsetY).with(notThumbUpScale);
        set.play(thumbUpScale).after(notThumbUpScale);
        set.addListener(new ClickAnimatorListener() {
            @Override
            public void onAnimRealEnd(Animator animation) {
                Log.d("scaleOk", "thumbUp" + count);
                if (thumbUpClickListener != null) {
                    thumbUpClickListener.thumbUpFinish();
                }
            }

        });
        set.start();
        currentAnim = set;
    }

    private void showThumbDownAnim() {
        ObjectAnimator thumbUpScale = ObjectAnimator.ofFloat(this, "thumbUpScale", SCALE_MIN, SCALE_MAX);
        thumbUpScale.setDuration(SCALE_DURING);
        thumbUpScale.addListener(new ClickAnimatorListener() {
            @Override
            public void onAnimRealEnd(Animator animation) {
                isThumbUp = false;
            }
        });

        ObjectAnimator textOffsetY = ObjectAnimator.ofFloat(this, "textOffsetY", OFFSET_MIN, -OFFSET_MAX);
        textOffsetY.setDuration(SCALE_DURING + RADIUS_DURING);

        ObjectAnimator notThumbUpScale = ObjectAnimator.ofFloat(this, "notThumbUpScale", SCALE_MAX, SCALE_MAX);
        notThumbUpScale.setDuration(SCALE_DURING);

        AnimatorSet set = new AnimatorSet();
        set.play(thumbUpScale).with(textOffsetY);
        set.play(notThumbUpScale).after(thumbUpScale);
        set.addListener(new ClickAnimatorListener() {
            @Override
            public void onAnimRealEnd(Animator animation) {
                Log.d("scaleOk", "thumbDown" + count);
                if (thumbUpClickListener != null) {
                    thumbUpClickListener.thumbDownFinish();
                }
            }
        });
        set.start();

        currentAnim = set;
    }

    public interface ThumbUpClickListener {
        //点赞回调
        void thumbUpFinish();

        //取消回调
        void thumbDownFinish();
    }

    private abstract class ClickAnimatorListener extends AnimatorListenerAdapter {
        @Override
        public void onAnimationStart(Animator animation) {
            super.onAnimationStart(animation);
            isCanceled = false;
        }

        @Override
        public void onAnimationEnd(Animator animation) {
            super.onAnimationEnd(animation);
            if (!isCanceled) {
                onAnimRealEnd(animation);
            }
        }

        @Override
        public void onAnimationCancel(Animator animation) {
            super.onAnimationCancel(animation);
            isCanceled = true;
        }

        public abstract void onAnimRealEnd(Animator animation);
    }

    public Object evaluate(float fraction, Object startValue, Object endValue) {
        int startInt = (Integer) startValue;
        float startA = ((startInt >> 24) & 0xff) / 255.0f;
        float startR = ((startInt >> 16) & 0xff) / 255.0f;
        float startG = ((startInt >> 8) & 0xff) / 255.0f;
        float startB = (startInt & 0xff) / 255.0f;

        int endInt = (Integer) endValue;
        float endA = ((endInt >> 24) & 0xff) / 255.0f;
        float endR = ((endInt >> 16) & 0xff) / 255.0f;
        float endG = ((endInt >> 8) & 0xff) / 255.0f;
        float endB = (endInt & 0xff) / 255.0f;

        // convert from sRGB to linear
        startR = (float) Math.pow(startR, 2.2);
        startG = (float) Math.pow(startG, 2.2);
        startB = (float) Math.pow(startB, 2.2);

        endR = (float) Math.pow(endR, 2.2);
        endG = (float) Math.pow(endG, 2.2);
        endB = (float) Math.pow(endB, 2.2);

        // compute the interpolated color in linear space
        float a = startA + fraction * (endA - startA);
        float r = startR + fraction * (endR - startR);
        float g = startG + fraction * (endG - startG);
        float b = startB + fraction * (endB - startB);

        // convert back to sRGB in the [0..255] range
        a = a * 255.0f;
        r = (float) Math.pow(r, 1.0 / 2.2) * 255.0f;
        g = (float) Math.pow(g, 1.0 / 2.2) * 255.0f;
        b = (float) Math.pow(b, 1.0 / 2.2) * 255.0f;

        return Math.round(a) << 24 | Math.round(r) << 16 | Math.round(g) << 8 | Math.round(b);
    }
}
