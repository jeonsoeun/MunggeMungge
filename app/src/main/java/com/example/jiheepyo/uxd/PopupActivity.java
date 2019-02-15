package com.example.jiheepyo.uxd;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;

public class PopupActivity extends Activity implements View.OnClickListener {
    CheckBox ch_booth;
    CheckBox ch_cafe;
    CheckBox ch_pc;
    CheckBox ch_hotple;
   // CheckBox ch_booth;
   ImageButton btnExit;
    Button btnOk;
    Button btnCancle;
    boolean checked[] = {true,true,true,true};
    boolean changeChecked[]= {true,true,true,true};
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.popup_activity);
        WindowManager.LayoutParams windowManager = getWindow().getAttributes();
        windowManager.dimAmount = 0.75f;
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        init();
    }

    void init(){
        ch_booth = (CheckBox)findViewById(R.id.ch_booth);
        ch_pc = (CheckBox)findViewById(R.id.ch_pc);
        ch_cafe = (CheckBox)findViewById(R.id.ch_cafe);
        ch_hotple =(CheckBox)findViewById(R.id.ch_none);
        btnExit = (ImageButton)findViewById(R.id.exitBtn);
        btnCancle = (Button)findViewById(R.id.cancelBtn);
        btnOk = (Button)findViewById(R.id.okBtn);
        //체크박스값 가져옴.
        Intent intent = getIntent();
        boolean checkedList[] = intent.getBooleanArrayExtra("checkedList");
        for(int i = 0; i<checked.length; i++){
            checked[i] = checkedList[i];
            changeChecked[i] = checkedList[i];
        }
        //체크박스에 반영
        settingCheckbox();
    }
    public void settingCheckbox(){
        if(checked[0])
            ch_booth.setChecked(true);
        if(checked[2])
            ch_cafe.setChecked(true);
        if(checked[1])
            ch_pc.setChecked(true);
        if(checked[3])
            ch_hotple.setChecked(true);
    }
    public void btnClick(View v){
        Intent it = new Intent();
        switch (v.getId()){
            case R.id.exitBtn:
            case R.id.cancelBtn:
                it.putExtra("result",checked);
                setResult(RESULT_CANCELED,it);
                finish();
                break;
            case R.id.okBtn:
                it.putExtra("result",changeChecked);
                setResult(RESULT_OK,it);
                finish();
                break;
        }
    }
    public void  clickCheckbox(View v){
        switch (v.getId()){
            case R.id.ch_booth:
                if(ch_booth.isChecked()) {
                    changeChecked[0] = true;
                } else{
                    changeChecked[0] = false;
                }
                break;
            case R.id.ch_cafe:
                if(ch_cafe.isChecked()) {
                    changeChecked[2] = true;
                } else{
                    changeChecked[2] = false;
                }
                break;
            case R.id.ch_pc:
                if(ch_pc.isChecked()) {
                    changeChecked[1] = true;
                } else{
                    changeChecked[1] = false;
                }
                break;
            case R.id.ch_none:
                if(ch_hotple.isChecked()) {
                    changeChecked[3] = true;
                }
                else{
                    changeChecked[3] = false;
                }
            default:
                break;
        }
    }

    @Override
    public void onClick(View v) {

    }

}
