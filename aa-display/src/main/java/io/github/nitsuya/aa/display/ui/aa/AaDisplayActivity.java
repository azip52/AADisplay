package io.github.nitsuya.aa.display.ui.aa;

import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.google.android.apps.auto.sdk.CarActivity;
import com.google.android.apps.auto.sdk.CarToast;
import io.github.nitsuya.aa.display.R;
import io.github.nitsuya.aa.display.databinding.ActivityAaDisplayBinding;

public class AaDisplayActivity extends CarActivity {
    private static final String TAG = "AADisplay_AaActivity";

    private ActivityAaDisplayBinding mBinding;

    protected Window getWindow() {
        return this.c();
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        Log.i(TAG, "onCreate");
        setIgnoreConfigChanges(0xFFFF);
        this.setTheme(R.style.Theme_AADisplay);
        this.mBinding = ActivityAaDisplayBinding.inflate(getLayoutInflater());
        this.addGenericView(this.mBinding.getRoot());
        this.setContentView(this.mBinding.getRoot());
        this.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED, WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
        AaDisplayActivityKt.INSTANCE.showMain(getSupportFragmentManager());
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.i(TAG, "onStart");
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.i(TAG, "onResume");
    }

    @Override
    public void onPause() {
        Log.i(TAG, "onPause");
        super.onPause();
    }

    @Override
    public void onStop() {
        Log.i(TAG, "onStop");
        super.onStop();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent keyEvent) {
        switch (keyCode){
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
                AaDisplayActivityKt.INSTANCE.pressKey(KeyEvent.KEYCODE_DPAD_CENTER);
                return true;
            case KeyEvent.KEYCODE_DPAD_UP:
            case KeyEvent.KEYCODE_DPAD_DOWN:
            case KeyEvent.KEYCODE_DPAD_LEFT:
            case KeyEvent.KEYCODE_DPAD_RIGHT:
            case KeyEvent.KEYCODE_SYSTEM_NAVIGATION_UP:
            case KeyEvent.KEYCODE_SYSTEM_NAVIGATION_DOWN:
            case KeyEvent.KEYCODE_SYSTEM_NAVIGATION_LEFT:
            case KeyEvent.KEYCODE_SYSTEM_NAVIGATION_RIGHT:
                // Leave directional focus navigation to Android Auto so focus can cross from
                // AADisplay into the native navigation bar.
                return super.onKeyDown(keyCode, keyEvent);
            case KeyEvent.KEYCODE_BACK:
                AaDisplayActivityKt.INSTANCE.pressKey(keyCode);
                return true;
            default:
                AaDisplayActivityKt.INSTANCE.toast("键值:[" + keyCode + "]未适配");
                return super.onKeyDown(keyCode, keyEvent);
        }
    }

    private boolean onGenericMotionEvent(MotionEvent event) {
        if (event.getActionMasked() != MotionEvent.ACTION_SCROLL) {
            return false;
        }

        // Rotary encoders normally report AXIS_SCROLL. Keep AXIS_VSCROLL as a fallback for
        // head units that expose the controller as a mouse wheel.
        float scroll = event.getAxisValue(MotionEvent.AXIS_SCROLL);
        if (scroll == 0f) {
            scroll = event.getAxisValue(MotionEvent.AXIS_VSCROLL);
        }
        if (scroll == 0f) {
            return false;
        }

        int keyCode = scroll > 0
                ? KeyEvent.KEYCODE_DPAD_DOWN
                : KeyEvent.KEYCODE_DPAD_UP;
        int steps = Math.min(20, Math.max(1, Math.round(Math.abs(scroll))));
        for (int i = 0; i < steps; i++) {
            AaDisplayActivityKt.INSTANCE.pressKey(keyCode);
        }
        return true;
    }
    private void addGenericView(ViewGroup viewGroup){
        View view = new View(this){
            @Override
            public boolean onGenericMotionEvent(MotionEvent event) {
                return AaDisplayActivity.this.onGenericMotionEvent(event);
            };
        };
        view.setLayoutParams(new ViewGroup.LayoutParams(1, 1));
        view.setAlpha(0);
        viewGroup.addView(view, 0);
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy");
        super.onDestroy();
    }


}
