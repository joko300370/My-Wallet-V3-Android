package piuk.blockchain.android.ui.customviews;

import android.annotation.SuppressLint;
import android.content.Context;

import androidx.appcompat.app.AlertDialog;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import com.google.android.material.textfield.TextInputLayout;
import piuk.blockchain.android.R;

/**
 * This class intercepts "reveal" events when the wrapped EditText's input type is "textPassword".
 * It warns users once that revealing their password allows clipboard access, and passes a touch
 * event if a user allows it.
 */
public class AnimatedPasswordInputLayout extends TextInputLayout {

    private ImageButton mToggle;
    // Shared across all instances
    private static boolean mPasswordWarningSeen = false;

    public AnimatedPasswordInputLayout(Context context) {
        this(context, null);
    }

    public AnimatedPasswordInputLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AnimatedPasswordInputLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        setEndIconMode(END_ICON_PASSWORD_TOGGLE);
        initListener();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void initListener() {
        if (getEndIconMode() == END_ICON_PASSWORD_TOGGLE) {
            mToggle = findViewById(R.id.text_input_end_icon);
            mToggle.setOnTouchListener((v, event) -> {
                if (event != null && event.getAction() == MotionEvent.ACTION_UP) {
                    if (!mPasswordWarningSeen
                            && getEditText() != null
                            && getEditText().getTransformationMethod() != null) {

                        showCopyWarningDialog(mToggle);
                        return true;
                    } else {
                        return false;
                    }
                }
                v.performClick();
                return false;
            });
        }
    }

    private void showCopyWarningDialog(View toggle) {
        mPasswordWarningSeen = true;

        new AlertDialog.Builder(getContext(), R.style.AlertDialogStyle)
                .setTitle(R.string.app_name)
                .setCancelable(false)
                .setMessage(R.string.password_reveal_warning)
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> mPasswordWarningSeen = false)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> toggle.performClick())
                .create()
                .show();
    }
}
