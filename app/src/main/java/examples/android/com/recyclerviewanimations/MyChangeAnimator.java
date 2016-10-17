package examples.android.com.recyclerviewanimations;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.support.annotation.NonNull;
import android.support.v4.util.ArrayMap;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

/**
 *
 * consider to use this: https://github.com/wasabeef/recyclerview-animators
 *
 *
 * Custom ItemAnimator that handles change animations. The default animator simple cross-
 * fades the old and new ViewHolder items. Our custom animation re-uses the same
 * ViewHolder and animates the contents of the views that it contains. The animation
 * consists of a color fade through black and a text rotation. The animation also handles
 * interruption, when a new change event happens on an item that is currently being
 * animated.
 */
class MyChangeAnimator extends DefaultItemAnimator {
    // stateless interpolator re-used for every change animation
    private AccelerateInterpolator mAccelerateInterpolator = new AccelerateInterpolator();
    private DecelerateInterpolator mDecelerateInterpolator = new DecelerateInterpolator();
    private ArgbEvaluator mColorEvaluator = new ArgbEvaluator();

    // Maps to hold running animators. These are used when running a new change
    // animation on an item that is already being animated. mRunningAnimators is
    // used to cancel the previous animation. mAnimatorMap is used to construct
    // the new change animation based on where the previous one was at when it
    // was interrupted.
    private ArrayMap<RecyclerView.ViewHolder, AnimatorInfo> mAnimatorMap = new ArrayMap<>();

    @Override
    public boolean canReuseUpdatedViewHolder(@NonNull RecyclerView.ViewHolder viewHolder) {
        // This allows our custom change animation on the contents of the holder instead
        // of the default behavior of replacing the viewHolder entirely
        return true;
    }

    @NonNull
    private ItemHolderInfo getItemHolderInfo(MyViewHolder viewHolder, ColorTextInfo info) {
        info.color = ((ColorDrawable) viewHolder.container.getBackground()).getColor();
        info.text = (String) viewHolder.getTextView().getText();
        return info;
    }

    @NonNull
    @Override
    public ItemHolderInfo recordPreLayoutInformation(@NonNull RecyclerView.State state,
                                                     @NonNull RecyclerView.ViewHolder viewHolder,
                                                     int changeFlags,
                                                     @NonNull List<Object> payloads) {
        ColorTextInfo info = (ColorTextInfo) super.recordPreLayoutInformation(state, viewHolder,
                changeFlags, payloads);
        return getItemHolderInfo((MyViewHolder) viewHolder, info);
    }

    @NonNull
    @Override
    public ItemHolderInfo recordPostLayoutInformation(@NonNull RecyclerView.State state,
                                                      @NonNull RecyclerView.ViewHolder viewHolder) {
        ColorTextInfo info = (ColorTextInfo) super.recordPostLayoutInformation(state, viewHolder);
        return getItemHolderInfo((MyViewHolder) viewHolder, info);
    }

    @Override
    public ItemHolderInfo obtainHolderInfo() {
        return new ColorTextInfo();
    }

    /**
     * Custom ItemHolderInfo class that holds color and text information used in
     * our custom change animation
     */
    private class ColorTextInfo extends ItemHolderInfo {
        int color;
        String text;
    }

    /**
     * Holds child animator objects for any change animation. Used when a new change
     * animation interrupts one already in progress; the new one is constructed to start
     * from where the previous one was at when the interruption occurred.
     */
    private class AnimatorInfo {
        Animator overallAnim;
        ObjectAnimator fadeToBlackAnim, fadeFromBlackAnim, oldTextRotator, newTextRotator;

        AnimatorInfo(Animator overallAnim,
                     ObjectAnimator fadeToBlackAnim, ObjectAnimator fadeFromBlackAnim,
                     ObjectAnimator oldTextRotator, ObjectAnimator newTextRotator) {
            this.overallAnim = overallAnim;
            this.fadeToBlackAnim = fadeToBlackAnim;
            this.fadeFromBlackAnim = fadeFromBlackAnim;
            this.oldTextRotator = oldTextRotator;
            this.newTextRotator = newTextRotator;
        }
    }

    /**
     * Custom change animation. Fade to black on the container background, then back
     * up to the new bg coolor. Meanwhile, the text rotates, switching along the way.
     * If a new change animation occurs on an item that is currently animating
     * a change, we stop the previous change and start the new one where the old
     * one left off.
     */
    @Override
    public boolean animateChange(@NonNull RecyclerView.ViewHolder oldHolder,
                                 @NonNull final RecyclerView.ViewHolder newHolder,
                                 @NonNull ItemHolderInfo preInfo, @NonNull ItemHolderInfo postInfo) {

        if (oldHolder != newHolder) {
            // use default behavior if not re-using view holders
            return super.animateChange(oldHolder, newHolder, preInfo, postInfo);
        }

        final MyViewHolder viewHolder = (MyViewHolder) newHolder;

        // Get the pre/post change values; these are what we are animating between
        ColorTextInfo oldInfo = (ColorTextInfo) preInfo;
        ColorTextInfo newInfo = (ColorTextInfo) postInfo;
        int oldColor = oldInfo.color;
        int newColor = newInfo.color;
        final String oldText = oldInfo.text;
        final String newText = newInfo.text;

        // These are the objects whose values will be animated
        LinearLayout newContainer = viewHolder.container;
        final TextView newTextView = viewHolder.getTextView();

        // Check to see if there's a change animation already running on this item
        AnimatorInfo runningInfo = mAnimatorMap.get(newHolder);
        long prevAnimPlayTime = 0;
        boolean firstHalf = false;
        if (runningInfo != null) {
            // The information we need to construct the new animators is whether we
            // are in the 'first half' (fading to black and rotating the old text out)
            // and how far we are in whichever half is running
            firstHalf = runningInfo.oldTextRotator != null &&
                    runningInfo.oldTextRotator.isRunning();
            prevAnimPlayTime = firstHalf ?
                    runningInfo.oldTextRotator.getCurrentPlayTime() :
                    runningInfo.newTextRotator.getCurrentPlayTime();
            // done with previous animation - cancel it
            runningInfo.overallAnim.cancel();
        }

        // Construct the fade to/from black animation
        ObjectAnimator fadeToBlack = null, fadeFromBlack;
        if (runningInfo == null || firstHalf) {
            // The first part of the animation fades to black. Skip this phase
            // if we're interrupting an animation that was already in the second phase.
            int startColor = oldColor;
            if (runningInfo != null) {
                startColor = (Integer) runningInfo.fadeToBlackAnim.getAnimatedValue();
            }
            fadeToBlack = ObjectAnimator.ofInt(newContainer, "backgroundColor",
                    startColor, Color.BLACK);
            fadeToBlack.setEvaluator(mColorEvaluator);
            if (runningInfo != null) {
                // Seek to appropriate time in new animator if we were already
                // running a previous animation
                fadeToBlack.setCurrentPlayTime(prevAnimPlayTime);
            }
        }

        // Second phase of animation fades from black to the new bg color
        fadeFromBlack = ObjectAnimator.ofInt(newContainer, "backgroundColor",
                Color.BLACK, newColor);
        fadeFromBlack.setEvaluator(mColorEvaluator);
        if (runningInfo != null && !firstHalf) {
            // Seek to appropriate time in new animator if we were already
            // running a previous animation
            fadeFromBlack.setCurrentPlayTime(prevAnimPlayTime);
        }

        // Set up an animation to play both the first (if non-null) and second phases
        AnimatorSet bgAnim = new AnimatorSet();
        if (fadeToBlack != null) {
            bgAnim.playSequentially(fadeToBlack, fadeFromBlack);
        } else {
            bgAnim.play(fadeFromBlack);
        }

        // The other part of the animation rotates the text, switching it to the
        // new value half-way through (when it is perpendicular to the user)
        ObjectAnimator oldTextRotate = null, newTextRotate;
        if (runningInfo == null || firstHalf) {
            // The first part of the animation rotates text to be perpendicular to user.
            // Skip this phase if we're interrupting an animation that was already
            // in the second phase.
            oldTextRotate = ObjectAnimator.ofFloat(newTextView, View.ROTATION_X, 0, 90);
            oldTextRotate.setInterpolator(mAccelerateInterpolator);
            if (runningInfo != null) {
                oldTextRotate.setCurrentPlayTime(prevAnimPlayTime);
            }
            oldTextRotate.addListener(new AnimatorListenerAdapter() {
                boolean mCanceled = false;

                @Override
                public void onAnimationStart(Animator animation) {
                    // text was changed as part of the item change notification. Change
                    // it back for the first phase of the animation
                    newTextView.setText(oldText);
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                    mCanceled = true;
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    if (!mCanceled) {
                        // Set it to the new text when the old rotator ends - this is when
                        // it is perpendicular to the user (thus making the switch
                        // invisible)
                        newTextView.setText(newText);
                    }
                }
            });
        }

        // Second half of text rotation rotates from perpendicular to 0
        newTextRotate = ObjectAnimator.ofFloat(newTextView, View.ROTATION_X, -90, 0);
        newTextRotate.setInterpolator(mDecelerateInterpolator);
        if (runningInfo != null && !firstHalf) {
            // If we're interrupting a previous second-phase animation, seek to that time
            newTextRotate.setCurrentPlayTime(prevAnimPlayTime);
        }

        // Choreograph first and second half. First half may be null if we interrupted
        // a second-phase animation
        AnimatorSet textAnim = new AnimatorSet();
        if (oldTextRotate != null) {
            textAnim.playSequentially(oldTextRotate, newTextRotate);
        } else {
            textAnim.play(newTextRotate);
        }

        // Choreograph both animations: color fading and text rotating
        AnimatorSet changeAnim = new AnimatorSet();
        changeAnim.playTogether(bgAnim, textAnim);
        changeAnim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                dispatchAnimationFinished(newHolder);
                mAnimatorMap.remove(newHolder);
            }
        });
        changeAnim.start();

        // Store info about this animation to be re-used if a succeeding change event
        // occurs while it's still running
        AnimatorInfo runningAnimInfo = new AnimatorInfo(changeAnim, fadeToBlack, fadeFromBlack,
                oldTextRotate, newTextRotate);
        mAnimatorMap.put(newHolder, runningAnimInfo);

        return true;
    }

    @Override
    public void endAnimation(RecyclerView.ViewHolder item) {
        super.endAnimation(item);
        if (!mAnimatorMap.isEmpty()) {
            final int numRunning = mAnimatorMap.size();
            for (int i = numRunning; i >= 0; i--) {
                if (item == mAnimatorMap.keyAt(i)) {
                    mAnimatorMap.valueAt(i).overallAnim.cancel();
                }
            }
        }
    }

    @Override
    public boolean isRunning() {
        return super.isRunning() || !mAnimatorMap.isEmpty();
    }

    @Override
    public void endAnimations() {
        super.endAnimations();
        if (!mAnimatorMap.isEmpty()) {
            final int numRunning = mAnimatorMap.size();
            for (int i = numRunning; i >= 0; i--) {
                mAnimatorMap.valueAt(i).overallAnim.cancel();
            }
        }
    }
}
