package com.limelight.binding.input.virtual_controller;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.os.Handler;
import android.os.Looper;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.limelight.binding.input.ControllerHandler;
import com.limelight.preferences.PreferenceConfiguration;

/** Soultemis overlay-only DS4 touchpad with matching HUD edit modes and opacity. */
public final class SoulyDs4TouchpadView extends LinearLayout {
    private static final String PREFS = "soultemis_ds4_touchpad_panel";
    private final ControllerHandler handler;
    private final FingerSurface surface;
    private final TextView click;
    private VirtualController controller;
    private boolean surfaceActive;
    private float startRawX, startRawY;
    private int startLeft, startTop, startWidth, startHeight;
    private boolean editingGesture;
    private boolean clickHeld;
    private float touchStartX, touchStartY;
    private boolean touchMoved;
    private final Handler delayed = new Handler(Looper.getMainLooper());
    private final Runnable longClick = () -> {
        if (surfaceActive && !touchMoved && prefs().getBoolean("soultemis_ds4_long_click", false)) {
            setClick(true);
        }
    };
    private TextView diagnostic;
    private String transportStatus = "idle";

    private SharedPreferences prefs() {
        return PreferenceManager.getDefaultSharedPreferences(getContext());
    }
    private boolean enabled(String key, boolean defaultValue) {
        return prefs().getBoolean(key, defaultValue);
    }
    private void setClick(boolean pressed) {
        if (clickHeld == pressed) return;
        clickHeld = pressed;
        handler.setSoulyDs4TouchpadClick(pressed);
        setClickVisual(pressed);
        updateDiagnostic();
    }
    private void updateDiagnostic() {
        if (diagnostic == null) return;
        boolean show = enabled("soultemis_ds4_diagnostics", false);
        diagnostic.setVisibility(show ? VISIBLE : GONE);
        if (show) diagnostic.setText("DS4 slot: " + handler.getSoulyDs4ControllerIndex()
                + " | touch: " + surface.count + " | click: " + (clickHeld ? "DOWN" : "up")
                + " | event: " + transportStatus);
    }

    private int dp(int n) {
        return Math.round(n * getResources().getDisplayMetrics().density);
    }

    private GradientDrawable bg(int color, int border) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(10));
        d.setStroke(dp(1), border);
        return d;
    }

    private final class FingerSurface extends View {
        private static final int MAX_FINGERS = 2;
        private final float[] xs = new float[MAX_FINGERS];
        private final float[] ys = new float[MAX_FINGERS];
        private int count = 0;
        private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint hint = new Paint(Paint.ANTI_ALIAS_FLAG);

        FingerSurface(Context context) {
            super(context);
            setBackground(bg(0xD9363636, 0xFF777777));
            fill.setStyle(Paint.Style.FILL);
            fill.setColor(0xE5D8D8D8);
            ring.setStyle(Paint.Style.STROKE);
            ring.setStrokeWidth(dp(2));
            ring.setColor(Color.WHITE);
            hint.setColor(0xFFBDBDBD);
            hint.setTextAlign(Paint.Align.CENTER);
            hint.setTextSize(dp(12));
        }

        void updateContacts(MotionEvent event) {
            int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP) {
                clearContacts();
                return;
            }
            int lifted = action == MotionEvent.ACTION_POINTER_UP ? event.getActionIndex() : -1;
            count = 0;
            for (int i = 0; i < event.getPointerCount() && count < MAX_FINGERS; i++) {
                if (i == lifted) continue;
                xs[count] = event.getX(i);
                ys[count] = event.getY(i);
                count++;
            }
            invalidate();
        }

        void clearContacts() {
            count = 0;
            invalidate();
        }

        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            if (count == 0) {
                canvas.drawText("DS4 TOUCHPAD", getWidth() / 2f,
                        getHeight() / 2f + dp(4), hint);
                return;
            }
            if (!enabled("soultemis_ds4_show_fingers", true)) return;
            for (int i = 0; i < count; i++) {
                canvas.drawCircle(xs[i], ys[i], dp(10), fill);
                canvas.drawCircle(xs[i], ys[i], dp(12), ring);
            }
        }
    }

    public SoulyDs4TouchpadView(Context context, ControllerHandler handler) {
        super(context);
        this.handler = handler;
        setOrientation(VERTICAL);
        setPadding(dp(5), dp(5), dp(5), dp(5));
        setBackground(bg(0xD9262626, 0xFF606060));
        setClipChildren(false);

        surface = new FingerSurface(context);
        addView(surface, new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, 0, 1f));
        surface.setOnTouchListener((v, event) -> {
            if (handleEditTouch(event)) return true;
            int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN) {
                surfaceActive = true;
                touchMoved = false;
                touchStartX = event.getX();
                touchStartY = event.getY();
                if (enabled("soultemis_ds4_long_click", false))
                    delayed.postDelayed(longClick, 450);
            }
            if (action == MotionEvent.ACTION_MOVE && !touchMoved) {
                float dx = event.getX() - touchStartX;
                float dy = event.getY() - touchStartY;
                if (dx * dx + dy * dy > dp(12) * dp(12)) {
                    touchMoved = true;
                    delayed.removeCallbacks(longClick);
                }
            }
            if (action == MotionEvent.ACTION_POINTER_DOWN) {
                touchMoved = true;
                delayed.removeCallbacks(longClick);
            }
            boolean sent = handler.sendSoulyDs4Touch(event, v.getWidth(), v.getHeight());
            transportStatus = sent ? "sent" : "unsupported";
            surface.updateContacts(event);
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                delayed.removeCallbacks(longClick);
                if (action == MotionEvent.ACTION_UP && !touchMoved && !clickHeld &&
                        enabled("soultemis_ds4_tap_click", false)) {
                    setClick(true);
                    delayed.postDelayed(() -> setClick(false), 80);
                } else if (clickHeld) {
                    setClick(false);
                }
                surfaceActive = false;
            }
            updateDiagnostic();
            return true;
        });

        click = new TextView(context);
        click.setTextColor(Color.WHITE);
        click.setTextSize(11f);
        click.setGravity(Gravity.CENTER);
        setClickVisual(false);
        LinearLayout.LayoutParams clickParams = new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, dp(38));
        clickParams.topMargin = dp(5);
        addView(click, clickParams);
        click.setOnTouchListener((v, event) -> {
            if (handleEditTouch(event)) return true;
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    setClick(true);
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    setClick(false);
                    break;
            }
            return true;
        });
        diagnostic = new TextView(context);
        diagnostic.setTextSize(10f);
        diagnostic.setTextColor(0xFFE0E0E0);
        diagnostic.setBackgroundColor(0x88000000);
        diagnostic.setGravity(Gravity.CENTER);
        addView(diagnostic, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, dp(30)));
        updateDiagnostic();
        setVisibility(GONE);
    }

    public void setEditorController(VirtualController controller) {
        this.controller = controller;
        post(this::restorePlacement);
        applyHudOpacity();
    }

    private void applyHudOpacity() {
        int opacity = PreferenceConfiguration.readPreferences(getContext()).oscOpacity;
        int level = 100;
        try { level = Integer.parseInt(prefs().getString("soultemis_ds4_opacity", "100")); }
        catch (NumberFormatException ignored) { }
        setAlpha(Math.max(0.05f, Math.min(1f, opacity / 100f * level / 100f)));
        updateDiagnostic();
    }

    private boolean handleEditTouch(MotionEvent event) {
        if (controller == null) return false;
        VirtualController.ControllerMode mode = controller.getControllerMode();
        if (mode == VirtualController.ControllerMode.Active && !editingGesture) return false;

        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN) {
            editingGesture = true;
            surface.clearContacts();
            if (surfaceActive) handler.cancelSoulyDs4Touch();
            surfaceActive = false;
            delayed.removeCallbacks(longClick);
            setClick(false);
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) getLayoutParams();
            startRawX = event.getRawX();
            startRawY = event.getRawY();
            startLeft = lp.leftMargin;
            startTop = lp.topMargin;
            startWidth = lp.width;
            startHeight = lp.height;
            return true;
        }
        if (!editingGesture) return true;
        if (action == MotionEvent.ACTION_MOVE) {
            FrameLayout parent = (FrameLayout) getParent();
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) getLayoutParams();
            int dx = Math.round(event.getRawX() - startRawX);
            int dy = Math.round(event.getRawY() - startRawY);
            if (mode == VirtualController.ControllerMode.MoveButtons) {
                lp.leftMargin = clamp(startLeft + dx, 0, Math.max(0, parent.getWidth() - lp.width));
                lp.topMargin = clamp(startTop + dy, 0, Math.max(0, parent.getHeight() - lp.height));
                setLayoutParams(lp);
            } else if (mode == VirtualController.ControllerMode.ResizeButtons) {
                lp.width = clamp(startWidth + dx, dp(150), Math.max(dp(150), parent.getWidth() - lp.leftMargin));
                lp.height = clamp(startHeight + dy, dp(90), Math.max(dp(90), parent.getHeight() - lp.topMargin));
                setLayoutParams(lp);
            }
            return true;
        }
        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            editingGesture = false;
            savePlacement();
            return true;
        }
        return true;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private void savePlacement() {
        FrameLayout parent = (FrameLayout) getParent();
        if (parent == null || parent.getWidth() == 0 || parent.getHeight() == 0) return;
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) getLayoutParams();
        getContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putFloat("x", (float) lp.leftMargin / parent.getWidth())
                .putFloat("y", (float) lp.topMargin / parent.getHeight())
                .putFloat("w", (float) lp.width / parent.getWidth())
                .putFloat("h", (float) lp.height / parent.getHeight())
                .putBoolean("saved", true).apply();
    }

    private void restorePlacement() {
        FrameLayout parent = (FrameLayout) getParent();
        if (parent == null || parent.getWidth() == 0 || parent.getHeight() == 0) return;
        SharedPreferences p = getContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (!p.getBoolean("saved", false)) return;
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) getLayoutParams();
        lp.gravity = Gravity.TOP | Gravity.LEFT;
        lp.width = clamp(Math.round(p.getFloat("w", 0.3f) * parent.getWidth()),
                dp(150), parent.getWidth());
        lp.height = clamp(Math.round(p.getFloat("h", 0.2f) * parent.getHeight()),
                dp(90), parent.getHeight());
        lp.leftMargin = clamp(Math.round(p.getFloat("x", 0.2f) * parent.getWidth()),
                0, Math.max(0, parent.getWidth() - lp.width));
        lp.topMargin = clamp(Math.round(p.getFloat("y", 0.1f) * parent.getHeight()),
                0, Math.max(0, parent.getHeight() - lp.height));
        setLayoutParams(lp);
    }

    private void setClickVisual(boolean pressed) {
        click.setText(pressed ? "DS4 TOUCHPAD CLICK - PRESSED" : "DS4 TOUCHPAD CLICK");
        click.setBackground(bg(pressed ? 0xFF757575 : 0xFF424242,
                pressed ? 0xFFE0E0E0 : 0xFF888888));
        click.setScaleX(pressed ? 0.985f : 1f);
        click.setScaleY(pressed ? 0.96f : 1f);
    }

    public FrameLayout.LayoutParams createLayoutParams() {
        FrameLayout.LayoutParams p = new FrameLayout.LayoutParams(dp(240), dp(151));
        p.gravity = Gravity.TOP | Gravity.LEFT;
        p.leftMargin = Math.max(0, (getResources().getDisplayMetrics().widthPixels - dp(240)) / 2);
        p.topMargin = dp(45);
        return p;
    }

    public void setActive(boolean active) {
        if (!active && surfaceActive) handler.cancelSoulyDs4Touch();
        if (!active) {
            delayed.removeCallbacks(longClick);
            setClick(false);
        }
        surfaceActive = false;
        editingGesture = false;
        if (!active) {
            surface.clearContacts();
            setClickVisual(false);
        }
        if (active) applyHudOpacity();
        click.setVisibility(enabled("soultemis_ds4_separate_click", true) ? VISIBLE : GONE);
        updateDiagnostic();
        setVisibility(active ? VISIBLE : GONE);
    }
}