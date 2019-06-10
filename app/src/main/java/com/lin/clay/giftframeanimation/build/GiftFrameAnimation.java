package com.lin.clay.giftframeanimation.build;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;

public final class GiftFrameAnimation {

    private Context mContext;
    /**
     * 缓存的图片
     */
    private final SparseArray<Bitmap> mBitmapCache;
    private SurfaceView mSurfaceView;
    private SurfaceHolder mSurfaceHolder;
    /**
     * 存储图片的所有路径
     */
    private List<String> mPathList;
    private MyCallBack mCallBack;
    /**
     * 用于绘制bitmap
     */
    private Matrix mDrawMatrix;
    private Paint mPaint;
    /**
     * 用来判断是否已经获取了需要的mDrawMatrix
     * 不需要重复获取
     */
    private int mLastFrameWidth = -1;
    private int mLastFrameHeight = -1;
    private int mLastFrameScaleType = -1;
    private int mLastSurfaceWidth;
    private int mLastSurfaceHeight;
    //图片的展现形式
    private int mScaleType = SCALE_TYPE_FIT_CENTER;
    //图片总数
    private int mTotalCount;
    //开始播放动画的图片第一帧
    private int mStartFramePositon = 0;
    //给定的帧动画的总时间
    private int mFrmeTime;
    //帧动画间隔是时间，单位毫秒
    private int mFrameInterval = 100;
    //图片缓存大小
    private int mCacheCount = 5;
    //是否支持inBitmap
    private boolean mSupportInBitmap = true;
    /**
     * in bitmap，避免频繁的GC
     */
    private Bitmap mInBitmap = null;
    /**
     * 作为一个标志位来标志是否应该初始化或者更新inBitmap，
     * 因为SurfaceView的双缓存机制，不能绘制完成直接就覆盖上一个bitmap
     * 此时surfaceView还没有post上一帧的数据，导致覆盖bitmap之后出现显示异常
     */
    private int mInBitmapFlag = 0;

    /**
     * 传入inBitmap时的decode参数
     */
    private BitmapFactory.Options mOptions;
    private Handler mDecodeHandler;
    //开始动画标志
    private final int START_ANIMATION = -1;
    //停止动画标志
    private final int STOP_ANIMATION = -2;
    //默认播放模式
    private int mPlayMode = MODE_ONCE;
    //动画只播放一次
    public static final int MODE_ONCE = 1;
    //动画重复播放
    public static final int MODE_INFINITE = 2;
    //动画播放状态监听
    private AnimationStateListener mAnimationStateListener;
    //异常退出监听
    private UnexceptedStopListener mUnexceptedListener;

    /**
     * 表示给定的matrix
     */
    private final int SCALE_TYPE_MATRIX = 0;
    /**
     * 完全拉伸，不保持原始图片比例，铺满
     */
    public static final int SCALE_TYPE_FIT_XY = 1;
    /**
     * 保持原始图片比例，整体拉伸图片至少填充满X或者Y轴的一个
     * 并最终依附在视图的上方或者左方
     */
    public static final int SCALE_TYPE_FIT_START = 2;
    /**
     * 保持原始图片比例，整体拉伸图片至少填充满X或者Y轴的一个
     * 并最终依附在视图的中心
     */
    public static final int SCALE_TYPE_FIT_CENTER = 3;
    /**
     * 保持原始图片比例，整体拉伸图片至少填充满X或者Y轴的一个
     * 并最终依附在视图的下方或者右方
     */
    public static final int SCALE_TYPE_FIT_END = 4;
    /**
     * 将图片置于视图中央，不缩放
     */
    public static final int SCALE_TYPE_CENTER = 5;
    /**
     * 整体缩放图片，保持原始比例，将图片置于视图中央，
     * 确保填充满整个视图，超出部分将会被裁剪
     */
    public static final int SCALE_TYPE_CENTER_CROP = 6;
    /**
     * 整体缩放图片，保持原始比例，将图片置于视图中央，
     * 确保X或者Y至少有一个填充满屏幕
     */
    public static final int SCALE_TYPE_CENTER_INSIDE = 7;

    public GiftFrameAnimation(SurfaceView surfaceView) {
        this.mSurfaceView = surfaceView;
        this.mSurfaceHolder = surfaceView.getHolder();
        mCallBack = new MyCallBack();
        mSurfaceHolder.setFormat(PixelFormat.TRANSLUCENT);
        mSurfaceView.setZOrderOnTop(true);
        mSurfaceHolder.addCallback(mCallBack);
        mBitmapCache = new SparseArray<>();
        mContext = surfaceView.getContext();
        mDrawMatrix = new Matrix();
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
    }

    @IntDef({MODE_INFINITE, MODE_ONCE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface RepeatMode {
    }

    @IntDef({SCALE_TYPE_FIT_XY, SCALE_TYPE_FIT_START, SCALE_TYPE_FIT_CENTER, SCALE_TYPE_FIT_END,
            SCALE_TYPE_CENTER, SCALE_TYPE_CENTER_CROP, SCALE_TYPE_CENTER_INSIDE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface ScaleType {

    }

    /**
     * 设置是否支持inBitmap，支持inBitmap会非常显著的改善内存抖动的问题
     * 因为存在bitmap复用的问题，当设置支持inBitmap时，请务必保证帧动画
     * 所有的图片分辨率和颜色位数完全一致。默认为true。
     *
     * @param support
     */
    public void setSupportInBitmap(boolean support) {
        this.mSupportInBitmap = support;
    }

    /**
     * 设置帧动画动画之间的间隔时间
     * 如果给定了总时间mFrameTime不为0，则这个设置无效，要按照计算得出
     *
     * @param time 单位是毫秒
     */
    public void setFrameInterval(int time) {
        this.mFrameInterval = time;
    }

    /**
     * 给定绘制bitmap的matrix不能和设置ScaleType同时起作用
     *
     * @param matrix 绘制bitmap时应用的matrix
     */
    public void setMatrix(@NonNull Matrix matrix) {
        mDrawMatrix = matrix;
        mScaleType = SCALE_TYPE_MATRIX;
    }

    /**
     * 设置图片的展现形式
     *
     * @param type
     */
    public void setScaleType(@ScaleType int type) {
        this.mScaleType = type;
    }

    /**
     * 设置缓存图片个数
     *
     * @param count
     */
    public void setCacheCount(int count) {
        this.mCacheCount = count;
    }

    /**
     * 设置图片播放模式，播放一次或者重复播放
     *
     * @param mode
     */
    public void setRepeatMode(@RepeatMode int mode) {
        this.mPlayMode = mode;
    }

    /**
     * 启动动画，判断动画是否存在，获取动画文件路径,然后从指定位置开始播放
     *
     * @param giftId
     * @param startFramePositon
     */
    public void start(String giftId, int startFramePositon) {
        if (mCallBack.isDrawing) {
            stop();
        }
        mPathList = getPathList(giftId);
        if (mPathList == null || mPathList.size() == 0) {
            return;
        }
        mTotalCount = mPathList.size();
        //缓存图片个数不能超过总图片数
        if (mCacheCount > mTotalCount) {
            mCacheCount = mTotalCount;
        }
        mStartFramePositon = startFramePositon;
        if (mStartFramePositon >= mTotalCount) {
            mStartFramePositon = 0;
        }
        startDecodeThread();
    }

    /**
     * 通过File资源转换pathList
     *
     * @return
     */
    private List<String> getPathList(String giftId) {
        List<String> list = new ArrayList<>();
        GiftModel giftModel = GainGiftUtils.getGiftModel(giftId);
        if (giftModel == null) {
            if (mAnimationStateListener != null) {
                mAnimationStateListener.giftFileNotExists();
            }
            return list;
        }
        String backgroundColor = giftModel.backgroundColor;
        String musicPath = giftModel.backgroundMusic;
        list = giftModel.imageArray;
        int size = list.size();
        mFrmeTime = giftModel.playTimeMillisecond;
        if (mFrmeTime == 0) {
            mFrmeTime = mFrameInterval * size;
            // 为了避免帧动画的播放跟背景音乐时长不一致，故禁掉音乐播放
            musicPath = null;
        } else {
            //根据json文件中给定的播放时间计算每帧图片之间的播放间隔时间
            mFrameInterval = mFrmeTime / size;
        }
        if (mAnimationStateListener != null) {
            mAnimationStateListener.giftFrameType(musicPath, mFrmeTime, backgroundColor);
        }
        return list;
    }

    public void stop() {
        if (!isDrawing()) {
            return;
        }
        mCallBack.stopAnim();
    }

    /**
     * 根据ScaleType配置绘制bitmap的Matrix
     *
     * @param bitmap
     */
    private void configureDrawMatrix(Bitmap bitmap) {
        final int srcWidth = bitmap.getWidth();
        final int srcHeight = bitmap.getHeight();
        final int dstWidth = mSurfaceView.getWidth();
        final int dstHeight = mSurfaceView.getHeight();
        final boolean nothingChanged = srcWidth == mLastFrameWidth
                && srcHeight == mLastFrameHeight
                && mLastFrameScaleType == mScaleType
                && mLastSurfaceWidth == dstWidth
                && mLastSurfaceHeight == dstHeight;
        if (nothingChanged) {
            return;
        }
        mLastFrameScaleType = mScaleType;
        mLastFrameWidth = srcWidth;
        mLastFrameHeight = srcHeight;
        mLastSurfaceWidth = dstWidth;
        mLastSurfaceHeight = dstHeight;
        if (mScaleType == SCALE_TYPE_MATRIX) {
            return;
        } else if (mScaleType == SCALE_TYPE_CENTER) {
            mDrawMatrix.setTranslate(
                    Math.round((dstWidth - srcWidth) * 0.5f),
                    Math.round((dstHeight - srcHeight) * 0.5f));
        } else if (mScaleType == SCALE_TYPE_CENTER_CROP) {
            float scale;
            float dx = 0, dy = 0;
            //按照高缩放
            if (dstHeight * srcWidth > dstWidth * srcHeight) {
                scale = (float) dstHeight / (float) srcHeight;
                dx = (dstWidth - srcWidth * scale) * 0.5f;
            } else {
                scale = (float) dstWidth / (float) srcWidth;
                dy = (dstHeight - srcHeight * scale) * 0.5f;
            }
            mDrawMatrix.setScale(scale, scale);
            mDrawMatrix.postTranslate(dx, dy);
        } else if (mScaleType == SCALE_TYPE_CENTER_INSIDE) {
            float scale;
            float dx;
            float dy;
            //小于dst时不缩放
            if (srcWidth <= dstWidth && srcHeight <= dstHeight) {
                scale = 1.0f;
            } else {
                scale = Math.min((float) dstWidth / (float) srcWidth,
                        (float) dstHeight / (float) srcHeight);
            }
            dx = Math.round((dstWidth - srcWidth * scale) * 0.5f);
            dy = Math.round((dstHeight - srcHeight * scale) * 0.5f);

            mDrawMatrix.setScale(scale, scale);
            mDrawMatrix.postTranslate(dx, dy);
        } else {
            RectF srcRect = new RectF(0, 0, bitmap.getWidth(), bitmap.getHeight());
            RectF dstRect = new RectF(0, 0, mSurfaceView.getWidth(), mSurfaceView.getHeight());
            mDrawMatrix.setRectToRect(srcRect, dstRect, MATRIX_SCALE_ARRAY[mScaleType - 1]);
        }
    }

    private final Matrix.ScaleToFit[] MATRIX_SCALE_ARRAY = {
            Matrix.ScaleToFit.FILL,
            Matrix.ScaleToFit.START,
            Matrix.ScaleToFit.CENTER,
            Matrix.ScaleToFit.END
    };

    public boolean isDrawing() {
        return mCallBack.isDrawing;
    }

    private class MyCallBack implements SurfaceHolder.Callback {
        private Canvas mCanvas;
        private int position;
        private boolean isDrawing = false;
        private Thread drawThread;

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            if (isDrawing) {
                stopAnim();
                if (mUnexceptedListener != null) {
                    mUnexceptedListener.onUnexceptedStop(getCorrectPosition());
                }
            }
        }

        /**
         * 绘制
         */
        private void drawBitmap() {
            //当循环播放时，获取真实的position
            if (mPlayMode == MODE_INFINITE && position >= mTotalCount) {
                position = position % mTotalCount;
            }
            if (position >= mTotalCount) {
                mDecodeHandler.sendEmptyMessage(STOP_ANIMATION);
                clearSurface();
                return;
            }
            if (mBitmapCache.get(position, null) == null) {
                stopAnim();
                return;
            }
            final Bitmap currentBitmap = mBitmapCache.get(position);
            mDecodeHandler.sendEmptyMessage(position);
            mCanvas = mSurfaceHolder.lockCanvas();
            if (mCanvas == null) {
                return;
            }
            mCanvas.setDrawFilter(new PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG));
            mCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
            configureDrawMatrix(currentBitmap);
            mCanvas.drawBitmap(currentBitmap, mDrawMatrix, mPaint);
            mSurfaceHolder.unlockCanvasAndPost(mCanvas);
            position++;
        }

        private void clearSurface() {
            try {
                mCanvas = mSurfaceHolder.lockCanvas();
                if (mCanvas != null) {
                    mCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                    mSurfaceHolder.unlockCanvasAndPost(mCanvas);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void startAnim() {
            if (mAnimationStateListener != null) {
                mAnimationStateListener.onStart();
            }
            isDrawing = true;
            position = mStartFramePositon;
            //绘制线程
            drawThread = new Thread() {
                @Override
                public void run() {
                    super.run();
                    while (isDrawing) {
                        try {
                            long now = System.currentTimeMillis();
                            drawBitmap();
                            //控制两帧之间的间隔
                            sleep(mFrameInterval - (System.currentTimeMillis() - now) > 0 ? mFrameInterval - (System.currentTimeMillis() - now) : 0);
                        } catch (InterruptedException e1) {
                            e1.printStackTrace();
                        }
                    }
                }
            };
            drawThread.start();
        }

        private int getCorrectPosition() {
            if (mPlayMode == MODE_INFINITE && position >= mTotalCount) {
                return position % mTotalCount;
            }
            return position;
        }

        private void stopAnim() {
            if (!isDrawing) {
                return;
            }
            isDrawing = false;
            position = 0;
            mBitmapCache.clear();
            clearSurface();
            if (mDecodeHandler != null) {
                mDecodeHandler.sendEmptyMessage(STOP_ANIMATION);
            }
            if (drawThread != null) {
                drawThread.interrupt();
            }
            if (mAnimationStateListener != null) {
                mAnimationStateListener.onFinish();
            }
            mInBitmapFlag = 0;
            mInBitmap = null;
        }
    }

    /**
     * decode线程
     */
    private void startDecodeThread() {
        new Thread() {
            @Override
            public void run() {
                super.run();
                Looper.prepare();
                mDecodeHandler = new Handler(Looper.myLooper()) {
                    @Override
                    public void handleMessage(Message msg) {
                        super.handleMessage(msg);
                        if (msg.what == STOP_ANIMATION) {
                            decodeBitmap(STOP_ANIMATION);
                            getLooper().quit();
                            return;
                        }
                        decodeBitmap(msg.what);
                    }
                };
                decodeBitmap(START_ANIMATION);
                Looper.loop();
            }
        }.start();
    }

    /**
     * 根据不同指令 进行不同操作，
     * 根据position的位置来缓存position后指定数量的图片
     *
     * @param position 小于0时，为handler发出的命令. 大于0时为当前帧
     */
    private void decodeBitmap(int position) {
        if (position == START_ANIMATION) {
            //初始化存储
            if (mSupportInBitmap) {
                mOptions = new BitmapFactory.Options();
                mOptions.inMutable = true;
                mOptions.inSampleSize = 1;
            }
            for (int i = mStartFramePositon; i < mCacheCount + mStartFramePositon; i++) {
                int putPosition = i;
                if (putPosition > mTotalCount - 1) {
                    putPosition = putPosition % mTotalCount;
                }
                mBitmapCache.put(putPosition, decodeBitmapReal(mPathList.get(putPosition)));
            }
            mCallBack.startAnim();
        } else if (position == STOP_ANIMATION) {
            mCallBack.stopAnim();
        } else if (mPlayMode == MODE_ONCE) {
            if (position + mCacheCount <= mTotalCount - 1) {
                //由于surface的双缓冲，不能直接复用上一帧的bitmap，因为上一帧的bitmap可能还没有post
                writeInBitmap(position);
                mBitmapCache.put(position + mCacheCount, decodeBitmapReal(mPathList.get(position + mCacheCount)));
            }
            //循环播放
        } else if (mPlayMode == MODE_INFINITE) {
            //由于surface的双缓冲，不能直接复用上一帧的bitmap，上一帧的bitmap可能还没有post
            writeInBitmap(position);
            //播放到尾部时，取mod
            if (position + mCacheCount > mTotalCount - 1) {
                mBitmapCache.put((position + mCacheCount) % mTotalCount, decodeBitmapReal(mPathList.get((position + mCacheCount) % mTotalCount)));
            } else {
                mBitmapCache.put(position + mCacheCount, decodeBitmapReal(mPathList.get(position + mCacheCount)));
            }
        }
    }

    /**
     * 更新inBitmap
     *
     * @param position
     */
    private void writeInBitmap(int position) {
        if (!mSupportInBitmap) {
            mBitmapCache.remove(position);
            return;
        }
        mInBitmapFlag++;
        if (mInBitmapFlag > 1) {
            int writePosition = position - 2;
            //得到正确的position
            if (writePosition < 0) {
                writePosition = mTotalCount + writePosition;
            }
            mInBitmap = mBitmapCache.get(writePosition);
            mBitmapCache.remove(writePosition);
        }
    }

    /**
     * 根据不同的情况，选择不同的加载方式
     *
     * @param path
     * @return
     */
    private Bitmap decodeBitmapReal(String path) {
        if (mInBitmap != null) {
            mOptions.inBitmap = mInBitmap;
        }
        return BitmapFactory.decodeFile(path, mOptions);
    }

    /**
     * 设置动画开始和结束监听
     *
     * @param animationStateListener
     */
    public void setAnimationStateListener(AnimationStateListener animationStateListener) {
        this.mAnimationStateListener = animationStateListener;
    }

    /**
     * Animation状态监听
     */
    public interface AnimationStateListener {
        /**
         * 动画开始
         */
        void onStart();

        /**
         * 动画结束
         */
        void onFinish();

        /**
         * 动画不存在
         */
        void giftFileNotExists();

        /**
         * 帧动画
         *
         * @param musicPath
         * @param mFrmeTime
         * @param backgroundColor
         */
        void giftFrameType(String musicPath, int mFrmeTime, String backgroundColor);
    }

    /**
     * 异常停止监听
     *
     * @param unexceptedStopListener
     */
    public void setUnexceptedStopListener(UnexceptedStopListener unexceptedStopListener) {
        this.mUnexceptedListener = unexceptedStopListener;
    }

    /**
     * 异常停止监听
     */
    public interface UnexceptedStopListener {
        /**
         * 异常停止时触发，比如home键被按下，直接锁屏，旋转屏幕等
         * 记录此位置后，最后可以通过自己去扩展来恢复动画
         *
         * @param position 异常停止时，帧动画播放的位置
         */
        void onUnexceptedStop(int position);
    }

}
