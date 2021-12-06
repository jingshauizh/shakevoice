package com.tian.audio.wave.dialog;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Paint;
import android.os.Bundle;
import android.text.TextPaint;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.tian.audio.wave.R;


public class DemoAppUpgradeDialog extends Dialog {

    private ImageView imgClose;
    private TextView tvReleaseNotes;
    private TextView tvUpgradeNotice;
    private Button sureBtn;
    private OnDialogClickListener onDialogClickListener;
    private Boolean isForceUpgrade = false;

    public DemoAppUpgradeDialog(Context context) {
        super(context, R.style.Theme_Ios_Dialog);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.app_dialog);
        this.setCancelable(false);//设置点击弹出框外部，无法取消对话框
        initDialog();

    }

    private void initDialog(){
        imgClose = findViewById(R.id.img_app_upgrade_close);
        tvReleaseNotes = findViewById(R.id.tv_app_upgrade_detailed_content);
        tvUpgradeNotice = findViewById(R.id.tv_app_upgrade_description);
        TextView tvReleaseNoteTitle = findViewById(R.id.tv_app_upgrade_detailed_title);
        setTextviewFakeBold(tvUpgradeNotice);
        setTextviewFakeBold(tvReleaseNoteTitle);
        tvReleaseNotes.setMovementMethod(ScrollingMovementMethod.getInstance());
        sureBtn = findViewById(R.id.btn_app_upgrade);
        imgClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (null != onDialogClickListener) {
                    onDialogClickListener.onCancelClickListener();
                }
            }
        });

        sureBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (null != onDialogClickListener) {
                    onDialogClickListener.onSureClickListener();
                }
            }
        });
        String text = "";
        tvUpgradeNotice.setText(text);
    }

    public void setMsg(String msg) {
        String temp = msg.replace("\\n", "\n");
        tvReleaseNotes.setText(temp);
    }


    public void setOnDialogClickListener(OnDialogClickListener onDialogClickListener) {
        this.onDialogClickListener = onDialogClickListener;
    }

    public interface OnDialogClickListener {
        void onSureClickListener();     //当点击了确认按钮之后执行

        void onCancelClickListener();   //当点击了取消按钮之后执行（取消操作通常都是弹窗消失）
    }


    public Boolean getForceUpgrade() {
        return isForceUpgrade;
    }

    public void setForceUpgrade(Boolean forceUpgrade) {
        isForceUpgrade = forceUpgrade;
    }


    protected void setTextviewFakeBold(TextView tv){
        if(null != tv){
            TextPaint tp = tv.getPaint();
            tp.setStrokeWidth(0.8f);
            tp.setStyle(Paint.Style.FILL_AND_STROKE);
        }


    }
}