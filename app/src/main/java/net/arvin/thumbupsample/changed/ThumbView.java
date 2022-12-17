package net.arvin.thumbupsample.changed;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.Keep;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.OvershootInterpolator;

import net.arvin.thumbupsample.R;

/**
 * Created by arvinljw on 17/10/25 14:52
 * Function：
 * Desc：
 */
public class ThumbView extends View {
    //圆圈颜色
    private static final int START_COLOR = Color.parseColor("#00e24d3d");
    private static final int END_COLOR = Color.parseColor("#88e24d3d");
    //缩放动画的时间
    private static final int SCALE_DURING = 150;
    //圆圈扩散动画的时间
    private static final int RADIUS_DURING = 250;

    private static final float SCALE_MIN = 0.9f;
    private static final float SCALE_MAX = 1f;

    private Bitmap mThumbUp;
    private Bitmap mShining;
    private Bitmap mThumbNormal;
    private Paint mBitmapPaint;
    private Runnable mCheckRunnable;

    private float mThumbWidth;
    private float mThumbHeight;
    private float mShiningWidth;
    private float mShiningHeight;

    private PointF mShiningPoint;
    private PointF mThumbPoint;
    private PointF mCirclePoint;

    private float mRadiusMax;
    private float mRadiusMin;
    private float mRadius;
    private Path mClipPath;
    private Paint mCirclePaint;

    private boolean mIsThumbUp;
    private long mLastStartTime;
    //上一个状态，1表示点赞，0表示未点赞
    private int mLastStatus;
    //点击的回调
    private ThumbUpClickListener mThumbUpClickListener;

    //被点击的次数
    private long mClickCount;
    private long mEndCount;
    private AnimatorSet mThumbUpAnim;
    private float mThumbNormalScale;
    private float mThumbUpScale;
    private float mShiningScale;

    public ThumbView(Context context) {
        this(context, null);
    }

    public ThumbView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ThumbView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        init();
    }

    private void init() {
        initBitmapInfo();

        mCirclePaint = new Paint();
        mCirclePaint.setAntiAlias(true);
        mCirclePaint.setStyle(Paint.Style.STROKE);
        mCirclePaint.setStrokeWidth(TuvUtils.dip2px(getContext(), 2));

        mCirclePoint = new PointF();
        mCirclePoint.x = mThumbPoint.x + mThumbWidth / 2;
        mCirclePoint.y = mThumbPoint.y + mThumbHeight / 2;

        mRadiusMax = Math.max(mCirclePoint.x - getPaddingLeft(), mCirclePoint.y - getPaddingTop());
        mRadiusMin = TuvUtils.dip2px(getContext(), 8);//这个值是根据点击效果调整得到的
        mClipPath = new Path();
        mClipPath.addCircle(mCirclePoint.x, mCirclePoint.y, mRadiusMax, Path.Direction.CW);
    }

    private void initBitmapInfo() {
        mBitmapPaint = new Paint();
        mBitmapPaint.setAntiAlias(true);

        resetBitmap();

        mThumbWidth = mThumbUp.getWidth();
        mThumbHeight = mThumbUp.getHeight();

        mShiningWidth = mShining.getWidth();
        mShiningHeight = mShining.getHeight();

        mShiningPoint = new PointF();
        mThumbPoint = new PointF();
        //这个相对位置是在布局中试出来的
        mShiningPoint.x = getPaddingLeft() + TuvUtils.dip2px(getContext(), 2);
        mShiningPoint.y = getPaddingTop();
        mThumbPoint.x = getPaddingLeft();
        mThumbPoint.y = getPaddingTop() + TuvUtils.dip2px(getContext(), 8);

        mThumbNormalScale = 1f;
        mThumbUpScale = 1f;
        mShiningScale = 1f;
    }

    private void resetBitmap() {
        mThumbUp = BitmapFactory.decodeResource(getResources(), R.drawable.ic_messages_like_selected);
        mThumbNormal = BitmapFactory.decodeResource(getResources(), R.drawable.ic_messages_like_unselected);
        mShining = BitmapFactory.decodeResource(getResources(), R.drawable.ic_messages_like_selected_shining);
    }

    public void setIsThumbUp(boolean isThumbUp) {
        this.mIsThumbUp = isThumbUp;
        mClickCount = mIsThumbUp ? 1 : 0;
        mEndCount = mClickCount;
        invalidate();
    }

    public boolean isThumbUp() {
        return mIsThumbUp;
    }

    public void setThumbUpClickListener(ThumbUpClickListener thumbUpClickListener) {
        this.mThumbUpClickListener = thumbUpClickListener;
    }

    public PointF getCirclePoint() {
        return mCirclePoint;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(TuvUtils.getDefaultSize(widthMeasureSpec, getContentWidth() + getPaddingLeft() + getPaddingRight()),
                TuvUtils.getDefaultSize(heightMeasureSpec, getContentHeight() + getPaddingTop() + getPaddingBottom()));
    }

    private int getContentWidth() {
        float minLeft = Math.min(mShiningPoint.x, mThumbPoint.x);
        float maxRight = Math.max(mShiningPoint.x + mShiningWidth, mThumbPoint.x + mThumbWidth);
        return (int) (maxRight - minLeft);
    }

    private int getContentHeight() {
        float minTop = Math.min(mShiningPoint.y, mThumbPoint.y);
        float maxBottom = Math.max(mShiningPoint.y + mShiningHeight, mThumbPoint.y + mThumbHeight);
        return (int) (maxBottom - minTop);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Bundle data = new Bundle();
        data.putParcelable("superData", super.onSaveInstanceState());
        data.putBoolean("isThumbUp", mIsThumbUp);
        return data;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        Bundle data = (Bundle) state;
        Parcelable superData = data.getParcelable("superData");
        super.onRestoreInstanceState(superData);

        mIsThumbUp = data.getBoolean("isThumbUp", false);

        init();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mIsThumbUp) {
            if (mClipPath != null) {
                canvas.save();
                canvas.clipPath(mClipPath);
                canvas.scale(mShiningScale, mShiningScale);
                canvas.drawBitmap(mShining, mShiningPoint.x, mShiningPoint.y, mBitmapPaint);
                canvas.restore();
                canvas.drawCircle(mCirclePoint.x, mCirclePoint.y, mRadius, mCirclePaint);
            }
            canvas.save();
            canvas.scale(mThumbUpScale, mThumbUpScale);
            canvas.drawBitmap(mThumbUp, mThumbPoint.x, mThumbPoint.y, mBitmapPaint);
        } else {
            canvas.save();
            canvas.scale(mThumbNormalScale, mThumbNormalScale);
            canvas.drawBitmap(mThumbNormal, mThumbPoint.x, mThumbPoint.y, mBitmapPaint);
        }
        canvas.restore();
    }

    public void startAnim() {
        mClickCount++;
        boolean isFastAnim = false;
        long currentTimeMillis = System.currentTimeMillis();
        if (currentTimeMillis - mLastStartTime < 300) {
            isFastAnim = true;
        }
        mLastStartTime = currentTimeMillis;
        if (mLastStatus == 0 && isFastAnim) {
            startFastAnim();
            return;
        }
        mLastStatus = mIsThumbUp ? 1 : 0;
        if (mIsThumbUp) {
            startThumbDownAnim();
        } else {
            startThumbUpAnim();
        }
        mEndCount = mClickCount;
    }

    private void startFastAnim() {
        if (mCheckRunnable != null) {
            removeCallbacks(mCheckRunnable);
        }
        ObjectAnimator thumbUpScale = ObjectAnimator.ofFloat(this, "thumbUpScale", SCALE_MIN, SCALE_MAX);
        thumbUpScale.setDuration(SCALE_DURING);
        thumbUpScale.setInterpolator(new OvershootInterpolator());

        ObjectAnimator circleScale = ObjectAnimator.ofFloat(this, "circleScale", mRadiusMin, mRadiusMax);
        circleScale.setDuration(RADIUS_DURING);

        AnimatorSet set = new AnimatorSet();
        set.play(thumbUpScale).with(circleScale);
        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                mEndCount++;
                if (mClickCount != mEndCount) {
                    return;
                }
                if (mCheckRunnable == null) {
                    mCheckRunnable = getCheckRunnable();
                }
                postDelayed(mCheckRunnable, 100);

            }
        });
        set.start();
    }

    private Runnable getCheckRunnable() {
        return new Runnable() {
            @Override
            public void run() {
                if (mClickCount % 2 == 0) {
                    startThumbDownAnim();
                } else {
                    if (mThumbUpClickListener != null) {
                        mThumbUpClickListener.thumbUpFinish();
                    }
                }
            }
        };
    }

    private void startThumbDownAnim() {
        ObjectAnimator thumbUpScale = ObjectAnimator.ofFloat(this, "thumbUpScale", SCALE_MIN, SCALE_MAX);
        thumbUpScale.setDuration(SCALE_DURING);
        thumbUpScale.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                mIsThumbUp = false;
                setNotThumbUpScale(SCALE_MAX);
                if (mThumbUpClickListener != null) {
                    mThumbUpClickListener.thumbDownFinish();
                }
            }
        });
        thumbUpScale.start();
    }

    private void startThumbUpAnim() {
        ObjectAnimator notThumbUpScale = ObjectAnimator.ofFloat(this, "notThumbUpScale", SCALE_MAX, SCALE_MIN);
        notThumbUpScale.setDuration(SCALE_DURING);
        notThumbUpScale.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                mIsThumbUp = true;
            }
        });

        ObjectAnimator thumbUpScale = ObjectAnimator.ofFloat(this, "thumbUpScale", SCALE_MIN, SCALE_MAX);
        thumbUpScale.setDuration(SCALE_DURING);
        thumbUpScale.setInterpolator(new OvershootInterpolator());

        ObjectAnimator circleScale = ObjectAnimator.ofFloat(this, "circleScale", mRadiusMin, mRadiusMax);
        circleScale.setDuration(RADIUS_DURING);

        mThumbUpAnim = new AnimatorSet();
        mThumbUpAnim.play(thumbUpScale).with(circleScale).after(notThumbUpScale);
        mThumbUpAnim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                mThumbUpAnim = null;
                if (mThumbUpClickListener != null) {
                    mThumbUpClickListener.thumbUpFinish();
                }
            }
        });
        mThumbUpAnim.start();
    }

    @Keep
    private void setNotThumbUpScale(float scale) {
        mThumbNormalScale = scale;
        invalidate();
    }

    @Keep
    private void setThumbUpScale(float scale) {
        mThumbUpScale = scale;
        invalidate();
    }

    @Keep
    private void setShiningScale(float scale) {
        mShiningScale = scale;
        invalidate();
    }

    @Keep
    private void setCircleScale(float radius) {
        mRadius = radius;
        mClipPath.reset();
        mClipPath.addCircle(mCirclePoint.x, mCirclePoint.y, mRadius, Path.Direction.CW);
        float fraction = (mRadiusMax - radius) / (mRadiusMax - mRadiusMin);
        mCirclePaint.setColor((int) TuvUtils.evaluate(fraction, START_COLOR, END_COLOR));
        invalidate();
    }

    public interface ThumbUpClickListener {
        //点赞回调
        void thumbUpFinish();

        //取消回调
        void thumbDownFinish();
    }

}
