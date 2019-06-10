package com.lin.clay.giftframeanimation.build;

public class GiftFragmentAnimationDirector {
    public GiftFrameAnimation createGiftFrameAnimation(IBuildGiftFragmentAnimation iBuild) {
        iBuild.buildSupportInBitmap();
        iBuild.buildScaleType();
        iBuild.buildtRepeatMode();
        iBuild.buildCacheCount();
        iBuild.buildFrameInterval();
        iBuild.buildMatrix();
        return iBuild.createGiftFragmentAnimation();
    }
}
