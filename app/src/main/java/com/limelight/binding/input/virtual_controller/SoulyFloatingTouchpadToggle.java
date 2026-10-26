package com.limelight.binding.input.virtual_controller;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

import com.limelight.preferences.PreferenceConfiguration;

/** Small grey DS4 touchpad toggle. Shares the existing OSC move/resize editor. */
public final class SoulyFloatingTouchpadToggle extends View {
    private static final String PREFS = "soultemis_ds4_floating_button_layout";
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private VirtualController editor;
    private Runnable onToggle;
    private boolean enabled = true;
    private boolean editing;
    private float downX, downY;
    private int originalLeft, originalTop, originalWidth, originalHeight;

    public SoulyFloatingTouchpadToggle(Context context) {
        super(context);
        setClickable(true);
        setContentDescription("Toggle DS4 touchpad");
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }

    private int dp(int amount) {
        return Math.round(amount * getResources().getDisplayMetrics().density);
    }

    public void setEditorController(VirtualController controller) {
        editor = controller;
        post(this::restorePlacement);
        refreshOpacity();
    }

    public void setToggleAction(Runnable action) {
        onToggle = action;
    }

    public void setTouchpadEnabled(boolean enabled) {
        this.enabled = enabled;
        setContentDescription(enabled ? "Disable DS4 touchpad" : "Enable DS4 touchpad");
        invalidate();
    }

    public void refreshOpacity() {
        int opacity = PreferenceConfiguration.readPreferences(getContext()).oscOpacity;
        setAlpha(Math.max(0.05f, Math.min(1.0f, opacity / 100.0f)));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        final float w = getWidth();
        final float h = getHeight();
        final float scale = Math.min(w, h);
        final float cx = w / 2f;
        final float cy = h / 2f;
        final float edge = Math.max(dp(1), scale * 0.045f);
        final float radius = scale * 0.17f;

        // Grey, semi-transparent rounded-square button; no text or blue accents.
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(enabled ? 0xB53B3B3B : 0x97404040);
        canvas.drawRoundRect(new RectF(1, 1, w - 1, h - 1), radius, radius, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(edge);
        paint.setColor(enabled ? 0xFFD3D3D3 : 0xFF949494);
        canvas.drawRoundRect(new RectF(edge, edge, w - edge, h - edge), radius, radius, paint);

        // Minimal touchpad symbol: rounded rectangle and simple fingertip.
        paint.setStrokeWidth(Math.max(dp(1), scale * 0.046f));
        final float touchLeft = cx - scale * 0.26f;
        final float touchTop = cy - scale * 0.19f;
        final float touchRight = cx + scale * 0.26f;
        final float touchBottom = cy + scale * 0.19f;
        canvas.drawRoundRect(new RectF(touchLeft, touchTop, touchRight, touchBottom),
                scale * 0.06f, scale * 0.06f, paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(enabled ? 0xFFF0F0F0 : 0xFFADADAD);
        canvas.drawCircle(cx + scale * 0.04f, cy - scale * 0.045f, scale * 0.083f, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Math.max(dp(1), scale * 0.05f));
        paint.setStrokeCap(Paint.Cap.ROUND);
        // One short line representing the finger contacting the touchpad.
        canvas.drawLine(cx + scale * 0.04f, cy + scale * 0.035f,
                cx + scale * 0.04f, cy + scale * 0.20f, paint);
        paint.setStrokeCap(Paint.Cap.BUTT);

        if (!enabled) {
            paint.setStrokeWidth(Math.max(dp(1), scale * 0.055f));
            paint.setColor(0xFFF0F0F0);
            canvas.drawLine(cx - scale * 0.29f, cy + scale * 0.27f,
                    cx + scale * 0.29f, cy - scale * 0.27f, paint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (editor == null) return super.onTouchEvent(event);
        VirtualController.ControllerMode mode = editor.getControllerMode();
        int action = event.getActionMasked();
        if (mode == VirtualController.ControllerMode.Active && !editing) {
            if (action == MotionEvent.ACTION_UP) {
                performClick();
                return true;
            }
            return true;
        }
        if (action == MotionEvent.ACTION_DOWN) {
            editing = true;
            FrameLayout.LayoutParams p = (FrameLayout.LayoutParams) getLayoutParams();
            downX = event.getRawX();
            downY = event.getRawY();
            originalLeft = p.leftMargin;
            originalTop = p.topMargin;
            originalWidth = p.width;
            originalHeight = p.height;
            return true;
        }
        if (!editing) return true;
        if (action == MotionEvent.ACTION_MOVE) {
            if (!(getParent() instanceof FrameLayout)) return true;
            FrameLayout parent = (FrameLayout) getParent();
            FrameLayout.LayoutParams p = (FrameLayout.LayoutParams) getLayoutParams();
            int dx = Math.round(event.getRawX() - downX);
            int dy = Math.round(event.getRawY() - downY);
            if (mode == VirtualController.ControllerMode.MoveButtons) {
                p.leftMargin = clamp(originalLeft + dx, 0, Math.max(0, parent.getWidth() - p.width));
                p.topMargin = clamp(originalTop + dy, 0, Math.max(0, parent.getHeight() - p.height));
                setLayoutParams(p);
            } else if (mode == VirtualController.ControllerMode.ResizeButtons) {
                p.width = clamp(originalWidth + dx, dp(30), Math.max(dp(30), parent.getWidth() - p.leftMargin));
                p.height = clamp(originalHeight + dy, dp(30), Math.max(dp(30), parent.getHeight() - p.topMargin));
                setLayoutParams(p);
            }
            return true;
        }
        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            editing = false;
            savePlacement();
            return true;
        }
        return true;
    }

    @Override
    public boolean performClick() {
        super.performClick();
        if (onToggle != null) onToggle.run();
        return true;
    }

    private static int clamp(int v, int low, int high) {
        return Math.max(low, Math.min(high, v));
    }

    private void savePlacement() {
        if (!(getParent() instanceof FrameLayout)) return;
        FrameLayout parent = (FrameLayout) getParent();
        if (parent.getWidth() <= 0 || parent.getHeight() <= 0) return;
        FrameLayout.LayoutParams p = (FrameLayout.LayoutParams) getLayoutParams();
        getContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putBoolean("saved", true)
                .putFloat("x", (float) p.leftMargin / parent.getWidth())
                .putFloat("y", (float) p.topMargin / parent.getHeight())
                .putFloat("w", (float) p.width / parent.getWidth())
                .putFloat("h", (float) p.height / parent.getHeight())
                .apply();
    }

    private void restorePlacement() {
        if (!(getParent() instanceof FrameLayout)) return;
        FrameLayout parent = (FrameLayout) getParent();
        if (parent.getWidth() <= 0 || parent.getHeight() <= 0) return;
        SharedPreferences p = getContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (!p.getBoolean("saved", false)) return;
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) getLayoutParams();
        lp.gravity = Gravity.TOP | Gravity.LEFT;
        lp.width = clamp(Math.round(p.getFloat("w", 0.05f) * parent.getWidth()), dp(30), parent.getWidth());
        lp.height = clamp(Math.round(p.getFloat("h", 0.08f) * parent.getHeight()), dp(30), parent.getHeight());
        lp.leftMargin = clamp(Math.round(p.getFloat("x", 0.85f) * parent.getWidth()),
                0, Math.max(0, parent.getWidth() - lp.width));
        lp.topMargin = clamp(Math.round(p.getFloat("y", 0.04f) * parent.getHeight()),
                0, Math.max(0, parent.getHeight() - lp.height));
        setLayoutParams(lp);
    }
}
