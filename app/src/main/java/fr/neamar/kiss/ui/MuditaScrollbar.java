package fr.neamar.kiss.ui;

import android.content.Context;
import android.database.DataSetObserver;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ListAdapter;
import android.widget.ListView;

/**
 * Mudita Mindful Design: Custom scrollbar for e-ink displays
 *
 * Based on MMD LazyColumnMMD scrollbar design:
 * - Small chevron arrows (16dp) at top and bottom
 * - White track with 1px black border
 * - Solid black thumb indicator
 * - No animations
 */
public class MuditaScrollbar extends View implements AbsListView.OnScrollListener {

    // MMD-inspired dimensions
    private static final int SCROLLBAR_WIDTH_DP = 32;  // Total width (increased)
    private static final int SCROLLBAR_PADDING_DP = 8; // Horizontal padding
    private static final int ARROW_SIZE_DP = 22;       // Chevron size (smaller)
    private static final int ARROW_PADDING_DP = 16;    // Padding around arrows (increased)
    private static final int TRACK_GAP_DP = 8;         // Gap between arrows and track
    private static final int TRACK_WIDTH_DP = 10;      // Track width (slightly wider)
    private static final int MIN_THUMB_HEIGHT_DP = 16;
    private static final int CORNER_RADIUS_DP = 5;     // Rounded corners

    private final Paint fillPaint;
    private final Paint strokePaint;
    private final Paint whitePaint;
    private final Paint arrowPaint;
    private final Paint arrowDisabledPaint;
    private final int scrollbarWidth;
    private final int scrollbarPadding;
    private final int arrowSize;
    private final int arrowPadding;
    private final int trackGap;
    private final int trackWidth;
    private final int minThumbHeight;
    private final int cornerRadius;

    private final Path upArrowPath;
    private final Path downArrowPath;
    private final RectF trackRect;

    private ListView attachedList;
    private ListAdapter attachedAdapter;
    private DataSetObserver adapterObserver;
    private boolean isScrollable = false;
    private boolean canScrollUp = false;
    private boolean canScrollDown = false;

    // Scroll position tracking
    private int firstVisibleItem = 0;
    private int visibleItemCount = 0;
    private int totalItemCount = 0;

    public MuditaScrollbar(Context context) {
        this(context, null);
    }

    public MuditaScrollbar(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MuditaScrollbar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        float density = context.getResources().getDisplayMetrics().density;
        scrollbarWidth = (int) (SCROLLBAR_WIDTH_DP * density);
        scrollbarPadding = (int) (SCROLLBAR_PADDING_DP * density);
        arrowSize = (int) (ARROW_SIZE_DP * density);
        arrowPadding = (int) (ARROW_PADDING_DP * density);
        trackGap = (int) (TRACK_GAP_DP * density);
        trackWidth = (int) (TRACK_WIDTH_DP * density);
        minThumbHeight = (int) (MIN_THUMB_HEIGHT_DP * density);
        cornerRadius = (int) (CORNER_RADIUS_DP * density);

        // Fill paint - solid black
        fillPaint = new Paint();
        fillPaint.setColor(0xFF000000);
        fillPaint.setStyle(Paint.Style.FILL);
        fillPaint.setAntiAlias(true);

        // Stroke paint - 1px black border
        strokePaint = new Paint();
        strokePaint.setColor(0xFF000000);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(1 * density);
        strokePaint.setAntiAlias(true);

        // White paint for track background
        whitePaint = new Paint();
        whitePaint.setColor(0xFFFFFFFF);
        whitePaint.setStyle(Paint.Style.FILL);
        whitePaint.setAntiAlias(true);

        // Arrow paint - filled with rounded corners
        float cornerEffect = 3 * density;  // Rounded corner radius
        arrowPaint = new Paint();
        arrowPaint.setColor(0xFF000000);
        arrowPaint.setStyle(Paint.Style.FILL);
        arrowPaint.setAntiAlias(true);
        arrowPaint.setPathEffect(new android.graphics.CornerPathEffect(cornerEffect));

        // Disabled arrow paint - gray filled with rounded corners
        arrowDisabledPaint = new Paint();
        arrowDisabledPaint.setColor(0xFFAAAAAA);
        arrowDisabledPaint.setStyle(Paint.Style.FILL);
        arrowDisabledPaint.setAntiAlias(true);
        arrowDisabledPaint.setPathEffect(new android.graphics.CornerPathEffect(cornerEffect));

        // Create paths
        upArrowPath = new Path();
        downArrowPath = new Path();
        trackRect = new RectF();

        // Enable touch events
        setClickable(true);
    }

    /**
     * Attach this scrollbar to any ListView
     */
    public void attachToListView(ListView listView) {
        // Clean up previous attachment
        if (attachedList != null) {
            attachedList.setOnScrollListener(null);
        }
        if (attachedAdapter != null && adapterObserver != null) {
            attachedAdapter.unregisterDataSetObserver(adapterObserver);
        }

        attachedList = listView;

        if (attachedList != null) {
            attachedList.setOnScrollListener(this);
            // Disable the built-in scrollbars
            attachedList.setVerticalScrollBarEnabled(false);
            attachedList.setFastScrollEnabled(false);

            // Create observer for adapter data changes
            adapterObserver = new DataSetObserver() {
                @Override
                public void onChanged() {
                    post(() -> refresh());
                }

                @Override
                public void onInvalidated() {
                    post(() -> refresh());
                }
            };

            // Register observer if adapter exists
            attachedAdapter = attachedList.getAdapter();
            if (attachedAdapter != null) {
                attachedAdapter.registerDataSetObserver(adapterObserver);
            }

            // Recheck scrollability after layout is complete
            attachedList.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
                post(() -> updateScrollability());
            });
        }

        updateScrollability();
    }

    private void updateScrollability() {
        if (attachedList == null || attachedList.getAdapter() == null) {
            isScrollable = false;
            canScrollUp = false;
            canScrollDown = false;
            setVisibility(GONE);
            return;
        }

        int itemCount = attachedList.getAdapter().getCount();
        if (itemCount == 0) {
            isScrollable = false;
            canScrollUp = false;
            canScrollDown = false;
            setVisibility(GONE);
            return;
        }

        // Check if all content is visible by examining actual child positions
        int firstPos = attachedList.getFirstVisiblePosition();
        int lastPos = attachedList.getLastVisiblePosition();
        int childCount = attachedList.getChildCount();

        if (childCount == 0) {
            isScrollable = false;
            canScrollUp = false;
            canScrollDown = false;
            setVisibility(GONE);
            return;
        }

        // Check if first item is fully visible at position 0
        boolean firstFullyVisible = false;
        if (firstPos == 0) {
            View firstChild = attachedList.getChildAt(0);
            if (firstChild != null) {
                firstFullyVisible = firstChild.getTop() >= attachedList.getPaddingTop();
            }
        }

        // Check if last item is fully visible at last position
        boolean lastFullyVisible = false;
        if (lastPos == itemCount - 1) {
            View lastChild = attachedList.getChildAt(childCount - 1);
            if (lastChild != null) {
                int listBottomEdge = attachedList.getHeight() - attachedList.getPaddingBottom();
                lastFullyVisible = lastChild.getBottom() <= listBottomEdge;
            }
        }

        // Only scrollable if not all content is visible
        isScrollable = !(firstFullyVisible && lastFullyVisible);

        // Determine if we can scroll up (not at top) or down (not at bottom)
        canScrollUp = !firstFullyVisible;
        canScrollDown = !lastFullyVisible;

        setVisibility(isScrollable ? VISIBLE : GONE);
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        this.firstVisibleItem = firstVisibleItem;
        this.visibleItemCount = visibleItemCount;
        this.totalItemCount = totalItemCount;

        updateScrollability();

        if (isScrollable) {
            invalidate();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isScrollable || attachedList == null) {
            return super.onTouchEvent(event);
        }

        float y = event.getY();
        int height = getHeight();
        int arrowAreaHeight = arrowSize + arrowPadding * 2;

        if (event.getAction() == MotionEvent.ACTION_UP) {
            if (y < arrowAreaHeight) {
                // Tap on up arrow - page up
                pageUp();
                return true;
            } else if (y > height - arrowAreaHeight) {
                // Tap on down arrow - page down
                pageDown();
                return true;
            }
        }

        return super.onTouchEvent(event);
    }

    /**
     * Scroll up by one page
     */
    private void pageUp() {
        if (attachedList == null) return;

        if (attachedList instanceof BlockableListView) {
            ((BlockableListView) attachedList).pageUp();
        } else {
            // Generic ListView page up
            int visibleCount = attachedList.getLastVisiblePosition() - attachedList.getFirstVisiblePosition();
            if (visibleCount <= 0) visibleCount = 1;
            int targetPos = Math.max(attachedList.getFirstVisiblePosition() - visibleCount, 0);
            attachedList.setSelectionFromTop(targetPos, 0);
        }
    }

    /**
     * Scroll down by one page
     */
    private void pageDown() {
        if (attachedList == null) return;

        if (attachedList instanceof BlockableListView) {
            ((BlockableListView) attachedList).pageDown();
        } else {
            // Generic ListView page down
            int visibleCount = attachedList.getLastVisiblePosition() - attachedList.getFirstVisiblePosition();
            if (visibleCount <= 0) visibleCount = 1;
            int targetPos = Math.min(attachedList.getFirstVisiblePosition() + visibleCount, attachedList.getCount() - 1);
            attachedList.setSelectionFromTop(targetPos, 0);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (!isScrollable || totalItemCount == 0) {
            return;
        }

        int height = getHeight();
        int width = getWidth();
        int centerX = width / 2;

        int arrowAreaHeight = arrowSize + arrowPadding * 2;

        // === Draw Up Arrow (small chevron) - gray if can't scroll up ===
        int upArrowCenterY = arrowPadding + arrowSize / 2;
        drawChevronUp(canvas, centerX, upArrowCenterY, canScrollUp);

        // === Draw Down Arrow (small chevron) - gray if can't scroll down ===
        int downArrowCenterY = height - arrowPadding - arrowSize / 2;
        drawChevronDown(canvas, centerX, downArrowCenterY, canScrollDown);

        // === Draw Track between arrows (with extra gap) ===
        int trackTop = arrowAreaHeight + trackGap;
        int trackBottom = height - arrowAreaHeight - trackGap;
        int trackHeight = trackBottom - trackTop;

        if (trackHeight > minThumbHeight) {
            int trackLeft = centerX - trackWidth / 2;
            int trackRight = centerX + trackWidth / 2;

            // Draw white background with rounded corners
            trackRect.set(trackLeft, trackTop, trackRight, trackBottom);
            canvas.drawRoundRect(trackRect, cornerRadius, cornerRadius, whitePaint);

            // Draw 1px black border
            canvas.drawRoundRect(trackRect, cornerRadius, cornerRadius, strokePaint);

            // Calculate thumb size and position
            float scrollRatio = (float) visibleItemCount / totalItemCount;
            int thumbHeight = Math.max((int) (trackHeight * scrollRatio), minThumbHeight);

            // Calculate thumb position
            int scrollableRange = totalItemCount - visibleItemCount;
            float scrollPosition = scrollableRange > 0 ? (float) firstVisibleItem / scrollableRange : 0;
            int thumbTravel = trackHeight - thumbHeight;
            int thumbTop = trackTop + (int) (thumbTravel * scrollPosition);

            // Draw solid black thumb with rounded corners
            trackRect.set(trackLeft, thumbTop, trackRight, thumbTop + thumbHeight);
            canvas.drawRoundRect(trackRect, cornerRadius, cornerRadius, fillPaint);
        }
    }

    private void drawChevronUp(Canvas canvas, int centerX, int centerY, boolean enabled) {
        // Flat, wide triangle - width is larger than height
        int halfWidth = arrowSize / 2;
        int halfHeight = arrowSize / 4;  // Flatter shape

        upArrowPath.reset();
        // Filled triangle pointing up with rounded corners via CornerPathEffect
        upArrowPath.moveTo(centerX - halfWidth, centerY + halfHeight);
        upArrowPath.lineTo(centerX, centerY - halfHeight);
        upArrowPath.lineTo(centerX + halfWidth, centerY + halfHeight);
        upArrowPath.close();

        canvas.drawPath(upArrowPath, enabled ? arrowPaint : arrowDisabledPaint);
    }

    private void drawChevronDown(Canvas canvas, int centerX, int centerY, boolean enabled) {
        // Flat, wide triangle - width is larger than height
        int halfWidth = arrowSize / 2;
        int halfHeight = arrowSize / 4;  // Flatter shape

        downArrowPath.reset();
        // Filled triangle pointing down with rounded corners via CornerPathEffect
        downArrowPath.moveTo(centerX - halfWidth, centerY - halfHeight);
        downArrowPath.lineTo(centerX, centerY + halfHeight);
        downArrowPath.lineTo(centerX + halfWidth, centerY - halfHeight);
        downArrowPath.close();

        canvas.drawPath(downArrowPath, enabled ? arrowPaint : arrowDisabledPaint);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int desiredWidth = scrollbarWidth + scrollbarPadding * 2;
        int width = resolveSize(desiredWidth, widthMeasureSpec);
        int height = resolveSize(0, heightMeasureSpec);
        setMeasuredDimension(width, height);
    }

    public void refresh() {
        if (attachedList != null && attachedList.getAdapter() != null) {
            totalItemCount = attachedList.getAdapter().getCount();
            visibleItemCount = attachedList.getLastVisiblePosition() - attachedList.getFirstVisiblePosition() + 1;
            firstVisibleItem = attachedList.getFirstVisiblePosition();
            updateScrollability();
            invalidate();
        }
    }
}
