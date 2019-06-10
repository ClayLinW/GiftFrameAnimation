package com.lin.clay.giftframeanimation.build;

import android.view.SurfaceView;

public class GiftFrameAnimationBuilder implements IBuildGiftFragmentAnimation {

    private final GiftFrameAnimation mGiftFrameAnimation;

    public GiftFrameAnimationBuilder(SurfaceView surfaceView) {
        mGiftFrameAnimation = new GiftFrameAnimation(surfaceView);
    }

    @Override
    public void buildSupportInBitmap() {
        mGiftFrameAnimation.setSupportInBitmap(true);
    }

    @Override
    public void buildCacheCount() {
        mGiftFrameAnimation.setCacheCount(3);
    }

    @Override
    public void buildScaleType() {
        mGiftFrameAnimation.setScaleType(GiftFrameAnimation.SCALE_TYPE_CENTER_CROP);
    }

    @Override
    public void buildtRepeatMode() {
        mGiftFrameAnimation.setRepeatMode(GiftFrameAnimation.MODE_ONCE);
    }

    @Override
    public void buildFrameInterval() {
        mGiftFrameAnimation.setFrameInterval(50);
    }

    @Override
    public void buildMatrix() {
        // 给定绘制bitmap的matrix不能和设置ScaleType同时起作用
        //        mGiftFrameAnimation.setMatrix(null);
    }

    @Override
    public GiftFrameAnimation createGiftFragmentAnimation() {
        return mGiftFrameAnimation;
    }
}
