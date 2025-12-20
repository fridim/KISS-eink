package fr.neamar.kiss.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.ViewConfiguration;
import android.widget.ListView;

/**
 * ListView subclass optimized for e-ink displays.
 * - Blocks touch events when requested
 * - Uses paged scrolling instead of smooth scrolling (better for e-ink refresh)
 * - No animations, instant page jumps only
 */
public class BlockableListView extends ListView {
    private boolean touchEventsBlocked = false;

    // For detecting swipe gestures
    private float startY;
    private float startX;
    private boolean isScrolling = false;
    private VelocityTracker velocityTracker;
    private int touchSlop;
    private int minimumFlingVelocity;

    public BlockableListView(Context context) {
        super(context);
        init(context);
    }

    public BlockableListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public BlockableListView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        // Disable overscroll glow effect (not needed for e-ink)
        setOverScrollMode(OVER_SCROLL_NEVER);

        ViewConfiguration vc = ViewConfiguration.get(context);
        touchSlop = vc.getScaledTouchSlop();
        minimumFlingVelocity = vc.getScaledMinimumFlingVelocity();
    }

    /**
     * Jump to the next page of items (no animation)
     */
    public void pageDown() {
        int visibleItemCount = getLastVisiblePosition() - getFirstVisiblePosition();
        if (visibleItemCount <= 0) visibleItemCount = 1;
        int targetPosition = Math.min(getFirstVisiblePosition() + visibleItemCount, getCount() - 1);
        setSelectionFromTop(targetPosition, 0);
    }

    /**
     * Jump to the previous page of items (no animation)
     */
    public void pageUp() {
        int visibleItemCount = getLastVisiblePosition() - getFirstVisiblePosition();
        if (visibleItemCount <= 0) visibleItemCount = 1;
        int targetPosition = Math.max(getFirstVisiblePosition() - visibleItemCount, 0);
        setSelectionFromTop(targetPosition, 0);
    }

    /**
     * Prevent this ListView from receiving any new touch events
     */
    public void blockTouchEvents() {
        this.touchEventsBlocked = true;
    }

    /**
     * Stop preventing this ListView from receiving touch events
     */
    public void unblockTouchEvents() {
        this.touchEventsBlocked = false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (this.touchEventsBlocked) {
            return true;
        }

        final int action = ev.getActionMasked();

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                startY = ev.getY();
                startX = ev.getX();
                isScrolling = false;

                if (velocityTracker == null) {
                    velocityTracker = VelocityTracker.obtain();
                } else {
                    velocityTracker.clear();
                }
                velocityTracker.addMovement(ev);

                // Allow click handling
                return super.onTouchEvent(ev);

            case MotionEvent.ACTION_MOVE:
                if (velocityTracker != null) {
                    velocityTracker.addMovement(ev);
                }

                float deltaY = ev.getY() - startY;
                float deltaX = ev.getX() - startX;

                // Check if this is a vertical scroll
                if (!isScrolling && Math.abs(deltaY) > touchSlop && Math.abs(deltaY) > Math.abs(deltaX)) {
                    isScrolling = true;
                    // Cancel any pending click
                    MotionEvent cancelEvent = MotionEvent.obtain(ev);
                    cancelEvent.setAction(MotionEvent.ACTION_CANCEL);
                    super.onTouchEvent(cancelEvent);
                    cancelEvent.recycle();
                }

                // Don't allow continuous scrolling - we only do page jumps
                if (isScrolling) {
                    return true; // Consume the event but don't scroll
                }
                return super.onTouchEvent(ev);

            case MotionEvent.ACTION_UP:
                if (velocityTracker != null) {
                    velocityTracker.addMovement(ev);
                    velocityTracker.computeCurrentVelocity(1000);
                    float velocityY = velocityTracker.getYVelocity();

                    float totalDeltaY = ev.getY() - startY;

                    // Check for fling or significant swipe
                    if (isScrolling) {
                        if (Math.abs(velocityY) > minimumFlingVelocity || Math.abs(totalDeltaY) > getHeight() / 4) {
                            if (totalDeltaY < 0) {
                                // Swiped up - next page
                                pageDown();
                            } else {
                                // Swiped down - previous page
                                pageUp();
                            }
                        }
                        velocityTracker.recycle();
                        velocityTracker = null;
                        isScrolling = false;
                        return true;
                    }

                    velocityTracker.recycle();
                    velocityTracker = null;
                }
                isScrolling = false;
                return super.onTouchEvent(ev);

            case MotionEvent.ACTION_CANCEL:
                if (velocityTracker != null) {
                    velocityTracker.recycle();
                    velocityTracker = null;
                }
                isScrolling = false;
                return super.onTouchEvent(ev);
        }

        return super.onTouchEvent(ev);
    }

    @Override
    public boolean performClick() {
        return this.touchEventsBlocked || super.performClick();
    }

    /**
     * Override fling to do nothing - we use page jumps instead
     */
    @Override
    public void fling(int velocityY) {
        // Disabled for e-ink - no smooth fling
    }

    /**
     * Disable smooth scrolling - use instant jumps for e-ink
     */
    @Override
    public void smoothScrollToPosition(int position) {
        setSelection(position);
    }

    @Override
    public void smoothScrollBy(int distance, int duration) {
        // Instant scroll instead of smooth
        if (distance != 0) {
            scrollListBy(distance);
        }
    }

    @Override
    public void smoothScrollToPositionFromTop(int position, int offset, int duration) {
        setSelectionFromTop(position, offset);
    }
}
