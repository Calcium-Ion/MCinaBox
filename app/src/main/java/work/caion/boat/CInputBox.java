package work.caion.boat;

import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;

import com.aof.mcinabox.R;
import com.aof.mcinabox.gamecontroller.controller.Controller;
import com.aof.mcinabox.gamecontroller.definitions.id.key.KeyEvent;
import com.aof.mcinabox.gamecontroller.definitions.map.KeyMap;
import com.aof.mcinabox.gamecontroller.event.BaseKeyEvent;
import com.aof.mcinabox.gamecontroller.input.screen.InputBox;

import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

public class CInputBox extends Dialog implements View.OnClickListener {

    private final Context mContext;
    private final Controller mController;

    private final EditText editBox;
    private final Button buttonNone;
    private final Button buttonEnter;
    private final Button buttonTEnter;
    private final Button buttonCancel;
    //private final boolean multi_line;

    private static final String TAG = "InputDialog";

    private void sendKey(String keyName) {
        mController.sendKey(new BaseKeyEvent(TAG, keyName, true, KeyEvent.KEYBOARD_BUTTON, null));
        mController.sendKey(new BaseKeyEvent(TAG, keyName, false, KeyEvent.KEYBOARD_BUTTON, null));
    }

    private void sleep() {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public CInputBox(@NonNull Context context, Controller controller) {
        super(context);
        setContentView(R.layout.dialog_input);
        setCanceledOnTouchOutside(true);
        Objects.requireNonNull(getWindow()).getAttributes().dimAmount = 0f;
        this.mContext = context;
        this.mController = controller;

        this.editBox = findViewById(R.id.dialog_input_edit_box);
        this.buttonNone = findViewById(R.id.dialog_input_button_none);
        this.buttonEnter = findViewById(R.id.dialog_input_button_enter);
        this.buttonTEnter = findViewById(R.id.dialog_input_button_t_enter);
        this.buttonCancel = findViewById(R.id.dialog_input_button_cancel);
        //this.multi_line = mContext.getSharedPreferences(InputBox.InputBoxConfigDialog.spFileName, InputBox.InputBoxConfigDialog.spMode).getBoolean(InputBox.InputBoxConfigDialog.sp_multi_line_name, true);

        editBox.setOnKeyListener(new View.OnKeyListener() {

            @Override
            public boolean onKey(View v, int keyCode, android.view.KeyEvent event) {
                //当输入框为空的时候，拦截Backspace的按键事件，然后向控制器发送退格事件
                if (event.getAction() == android.view.KeyEvent.ACTION_DOWN && keyCode == android.view.KeyEvent.KEYCODE_BACK && editBox.getText().toString().equals("")) {
                    sendKey(KeyMap.KEYMAP_KEY_BACKSPACE);
                    return true;
                }
                return false;
            }
        });

        for (View v : new View[]{buttonNone, buttonCancel, buttonTEnter, buttonEnter}) {
            v.setOnClickListener(this);
        }

    }

    @Override
    public void onClick(View v) {

        if (v == buttonCancel) {
            this.cancel();
        }

        if (v == buttonNone) {
            if (editBox.getText() != null && !editBox.getText().toString().equals("")) {
                mController.typeWords(editBox.getText().toString());
                dismiss();
            }
        }

        if (v == buttonEnter) {
            if (editBox.getText() != null) {
                if (!editBox.getText().toString().equals(""))
                    mController.typeWords(editBox.getText().toString());
                sendKey(KeyMap.KEYMAP_KEY_ENTER);
                dismiss();
            }
        }

        if (v == buttonTEnter) {
            if (editBox.getText() != null) {
                sendKey(KeyMap.KEYMAP_KEY_T);
                sleep();
                if (!editBox.getText().toString().equals(""))
                    mController.typeWords(editBox.getText().toString());
                sendKey(KeyMap.KEYMAP_KEY_ENTER);
                dismiss();
            }
        }
    }

    private void showKeyboard() {

    }

    @Override
    public void show() {
        super.show();
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                InputMethodManager inputManager = (InputMethodManager) editBox.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                inputManager.showSoftInput(editBox, 0);
            }
        }, 100);
    }
}
