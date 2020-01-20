package com.example.autoresizededittext;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Build;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.SparseIntArray;
import android.util.TypedValue;
import android.widget.EditText;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

@SuppressLint("AppCompatCustomView")
public class AutoResizedEditText extends EditText {
    public AutoResizedEditText(Context context) {
        super(context);
        initialize();
    }

    public AutoResizedEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize();
    }

    public AutoResizedEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public AutoResizedEditText(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initialize();
    }

    private interface SizeTester {
        int onTestSize(int suggestedSize, RectF availableSpace);
    }

    private RectF mTextRect = new RectF();

    private RectF mAvailableSpaceRect;

    private SparseIntArray mTextCachedSizes;

    private TextPaint paint;

    private float maxTextSize;

    private float mSpacingMult = 1.0f;

    private static final int NO_LINE_LIMIT = -1;
    private int mMaxLines;

    private boolean mInitializedDimens;

    private void initialize() {
        paint = new TextPaint(getPaint());
        maxTextSize = getTextSize();
        mAvailableSpaceRect = new RectF();
        mTextCachedSizes = new SparseIntArray();
        if (mMaxLines == 0) {
            // no value was assigned during construction
            mMaxLines = NO_LINE_LIMIT;
        }
    }

    @Override
    public void setTextSize(float size) {
        maxTextSize = size;
        mTextCachedSizes.clear();
        adjustTextSize();
    }

    @Override
    public void setMaxLines(int maxLines) {
        super.setMaxLines(maxLines);
        mMaxLines = maxLines;
        adjustTextSize();
    }

    public int getMaxLines() {
        return mMaxLines;
    }

    @Override
    public void setSingleLine() {
        super.setSingleLine();
        mMaxLines = 1;
        adjustTextSize();
    }


    @Override
    public void setSingleLine(boolean singleLine) {
        super.setSingleLine(singleLine);
        if (singleLine) {
            mMaxLines = 1;
        } else {
            mMaxLines = NO_LINE_LIMIT;
        }
        adjustTextSize();
    }

    @Override
    public void setLines(int lines) {
        super.setLines(lines);
        mMaxLines = lines;
        adjustTextSize();
    }

    @Override
    public void setTextSize(int unit, float size) {
        Context c = getContext();
        Resources r;

        if (c == null)
            r = Resources.getSystem();
        else
            r = c.getResources();
        maxTextSize = TypedValue.applyDimension(unit, size,
                r.getDisplayMetrics());
        mTextCachedSizes.clear();
        adjustTextSize();
    }

    @Override
    public void setLineSpacing(float add, float mult) {
        super.setLineSpacing(add, mult);
        mSpacingMult = mult;
    }


    public void setMinTextSize(float mMinTextSize) {
        maxTextSize = mMinTextSize;
        adjustTextSize();
    }


    private void adjustTextSize() {
        if (!mInitializedDimens) {
            return;
        }
        //Minimum TextSize
        float minTextSize = 16;
        int startSize = (int) minTextSize;
        int heightLimit = getMeasuredHeight() - getCompoundPaddingBottom()
                - getCompoundPaddingTop();
        mAvailableSpaceRect.right = getMeasuredWidth() - getCompoundPaddingLeft()
                - getCompoundPaddingRight();
        mAvailableSpaceRect.bottom = heightLimit;
        super.setTextSize(
                TypedValue.COMPLEX_UNIT_PX,
                efficientTextSizeSearch(startSize, (int) maxTextSize,
                        sizeTester, mAvailableSpaceRect));
    }

    @Override
    public void setTypeface(@Nullable Typeface tf) {
        if (paint == null) {
            paint = new TextPaint(getPaint());
        }
        paint.setTypeface(tf);
        super.setTypeface(tf);
    }


    public SizeTester sizeTester = new SizeTester() {
        final RectF textRect = new RectF();

        @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
        @Override
        public int onTestSize(final int suggestedSize,
                              final RectF availableSPace) {

            TextPaint paint = new TextPaint();

            paint.setTextSize(suggestedSize);

            String text;
            if (!TextUtils.isEmpty(getHint())) {
                text = getHint().toString();
            } else {
                text = getText().toString();
            }

            textRect.bottom = paint.getFontSpacing();
            textRect.right = paint.measureText(text);
            textRect.offsetTo(0, 0);

            if (availableSPace.contains(textRect)) {
                return -1;
            } else {
                return 1;
            }
        }
    };

    private int efficientTextSizeSearch(int start, int end,
                                        SizeTester sizeTester, RectF availableSpace) {
        //   boolean mEnableSizeCache = true;
        int key = getText().toString().length();
        int size = mTextCachedSizes.get(key);
        if (size != 0) {
            return size;
        }
        size = binarySearch(start, end, sizeTester, availableSpace);
        mTextCachedSizes.put(key, size);
        return size;
    }


    private static int binarySearch(int start, int end, SizeTester sizeTester,
                                    RectF availableSpace) {
        int lastBest = start;
        int lo = start;
        int hi = end - 1;
        int mid;
        while (lo <= hi) {
            mid = (lo + hi) >>> 1;
            int midValCmp = sizeTester.onTestSize(mid, availableSpace);
            if (midValCmp < 0) {
                lastBest = lo;
                lo = mid + 1;
            } else if (midValCmp > 0) {
                hi = mid - 1;
                lastBest = hi;
            } else {
                return mid;
            }
        }
        return lastBest;

    }


    @Override
    protected void onTextChanged(final CharSequence text, final int start,
                                 final int before, final int after) {
        super.onTextChanged(text, start, before, after);
        adjustTextSize();
    }


    @Override
    protected void onSizeChanged(int width, int height, int oldWidth,
                                 int oldHeight) {
        mInitializedDimens = true;
        mTextCachedSizes.clear();
        super.onSizeChanged(width, height, oldWidth, oldHeight);
        if (width != oldWidth || height != oldHeight) {
            adjustTextSize();
        }
    }
}
