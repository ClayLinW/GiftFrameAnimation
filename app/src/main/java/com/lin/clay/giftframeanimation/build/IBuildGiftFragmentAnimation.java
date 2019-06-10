package com.lin.clay.giftframeanimation.build;

public interface IBuildGiftFragmentAnimation {
    void buildSupportInBitmap();

    void buildCacheCount();

    void buildScaleType();

    void buildtRepeatMode();

    void buildFrameInterval();

    void buildMatrix();

    GiftFrameAnimation createGiftFragmentAnimation();
}
