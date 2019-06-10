package com.lin.clay.giftframeanimation;

import android.animation.ValueAnimator;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.SurfaceView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.lin.clay.giftframeanimation.build.GiftFragmentAnimationDirector;
import com.lin.clay.giftframeanimation.build.GiftFrameAnimation;
import com.lin.clay.giftframeanimation.build.GiftFrameAnimationBuilder;
import com.lin.clay.statusbarutils.StatusBarUtil;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private TextView tvPlayFlower;
    private TextView tvPlayStarrySky;
    private FrameLayout flFrameAnim;
    private SurfaceView mSurfaceView;
    private GiftFrameAnimation mGiftFrameAnimation;
    private int mAnimationTime;
    private ValueAnimator mAnimator;
    private static Handler mHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        StatusBarUtil.setSocialityStatusBar(this);
        initView();
        initData();
    }

    private void initView() {
        tvPlayFlower = findViewById(R.id.tv_playFlower);
        tvPlayStarrySky = findViewById(R.id.tv_playStarrySky);
        flFrameAnim = findViewById(R.id.fl_frameGiftAnim);
        mSurfaceView = findViewById(R.id.sv_main);
    }

    private void initData() {
        tvPlayFlower.setOnClickListener(this);
        tvPlayStarrySky.setOnClickListener(this);
        mSurfaceView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mGiftFrameAnimation != null) {
                    mGiftFrameAnimation.stop();
                }
            }
        });
        GiftFragmentAnimationDirector director = new GiftFragmentAnimationDirector();
        mGiftFrameAnimation = director.createGiftFrameAnimation(new GiftFrameAnimationBuilder(mSurfaceView));
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tv_playFlower:
                playFrameGift("1000");
                break;
            case R.id.tv_playStarrySky:
                playFrameGift("1001");
                break;
        }
    }

    /**
     * 播放动画
     *
     * @param giftId
     */
    public void playFrameGift(String giftId) {
        mGiftFrameAnimation.setAnimationStateListener(new GiftFrameAnimation.AnimationStateListener() {
            @Override
            public void onStart() {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        flFrameAnim.setVisibility(View.VISIBLE);
                        mSurfaceView.setVisibility(View.VISIBLE);

                        int time = mAnimationTime;
                        int count = time / 200;
                        float[] floats = new float[count];
                        floats[0] = 0;
                        for (int i = 0; i < count - 2; i++) {
                            floats[i + 1] = 1;
                        }
                        floats[count - 1] = 0;

                        mAnimator = ValueAnimator.ofFloat(floats);
                        mAnimator.setDuration(time + 200);
                        mAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                            @Override
                            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                                float value = (float) valueAnimator.getAnimatedValue();
                                flFrameAnim.setAlpha(value);
                            }
                        });
                        mAnimator.start();
                    }
                });
            }

            @Override
            public void onFinish() {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        flFrameAnim.setVisibility(View.GONE);
                        mSurfaceView.setVisibility(View.GONE);
                        MediaManager.stop();
                        if (mAnimator != null) {
                            mAnimator.cancel();
                        }
                    }
                });
            }

            @Override
            public void giftFileNotExists() {
                //                Toast.makeText(MainActivity.this,"文件不存在，需要下载动画", Toast.LENGTH_SHORT).show();
                Toast.makeText(MainActivity.this, "加载动画失败(查看下是否把项目目录的礼物资源文件放到手机的SDCard中或者查看是否已授权读写权限)", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void giftFrameType(String musicPath, int mFrmeTime, String backgroundColor) {
                if (musicPath != null) {
                    MediaManager.playSound(musicPath, null);
                }
                mAnimationTime = mFrmeTime;
                flFrameAnim.setBackgroundColor(Color.parseColor(backgroundColor));
            }
        });

        mGiftFrameAnimation.start(giftId, 0);
    }

    @Override
    protected void onPause() {
        super.onPause();
        MediaManager.pause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        MediaManager.resume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        MediaManager.release();
        if (mGiftFrameAnimation != null) {
            mGiftFrameAnimation.stop();
        }
    }
}
