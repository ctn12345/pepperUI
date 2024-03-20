/*
 *  Copyright (C) 2018 Softbank Robotics Europe
 *  See COPYING for the license
 */
package com.softbankrobotics.chatbotsample;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.aldebaran.qi.sdk.QiSDK;
import com.aldebaran.qi.sdk.design.activity.RobotActivity;
import com.aldebaran.qi.sdk.object.conversation.Phrase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Random;
import java.util.Set;

import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.RecognizerListener;
import com.iflytek.cloud.RecognizerResult;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechRecognizer;
import com.iflytek.cloud.SpeechUtility;
import com.iflytek.cloud.ui.RecognizerDialog;
import com.iflytek.cloud.ui.RecognizerDialogListener;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Main activity of the application.
 */
public class MainActivity extends RobotActivity implements UiNotifier {

    private Robot robot;

    private TextView qiChatBotIcon;
    private TextView dialogFlowIcon;
    private TextView pepperTxt;
    private TextView suggestion1;
    private TextView suggestion2;
    private TextView suggestion3;
    private TextView suggestion4;
    private boolean isDialogFlow = false;
    private List<Phrase> suggestions = new ArrayList<>();
    final Handler handler = new Handler();

    private SpeechRecognizer mIat;
    // 语音听写UI
    private RecognizerDialog mIatDialog;
    // 用HashMap存储听写结果
    private HashMap<String, String> mIatResults = new LinkedHashMap<>();
    private EditText mResultText;
    private Button languageText, dialogButton;
    // 语言类型【默认中文】
    private String language = "zh_cn";
    // 格式类型【默认json】
    private String resultType = "json";
    private boolean cyclic = false;//音频流识别是否循环调用
    //拼接字符串
    private StringBuffer buffer = new StringBuffer();
    //Handler码
    private int handlerCode = 0x123;
    // 函数调用返回值
    private int resultCode = 0;
    // 切换中英文
    private boolean languageType;
    // 弹框是否显示
    private int dialogType;

    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;

    private String [] permissions = {Manifest.permission.RECORD_AUDIO};

    public void setParam() {
        // 清空参数
        mIat.setParameter(SpeechConstant.PARAMS, null);
        // 设置听写引擎类型
        mIat.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD);
        // 设置返回结果格式【目前支持json,xml以及plain 三种格式，其中plain为纯听写文本内容】
        mIat.setParameter(SpeechConstant.RESULT_TYPE, resultType);
        //目前Android SDK支持zh_cn：中文、en_us：英文、ja_jp：日语、ko_kr：韩语、ru-ru：俄语、fr_fr：法语、es_es：西班牙语、
        // 注：小语种若未授权无法使用会报错11200，可到控制台-语音听写（流式版）-方言/语种处添加试用或购买。
        mIat.setParameter(SpeechConstant.LANGUAGE, language);
        // 设置语言区域、当前仅在LANGUAGE为简体中文时，支持方言选择，其他语言区域时，可把此参数值设为mandarin。
        // 默认值：mandarin，其他方言参数可在控制台方言一栏查看。
        mIat.setParameter(SpeechConstant.ACCENT, "mandarin");
        //获取当前语言（同理set对应get方法）
        Log.e("robot", "last language:" + mIat.getParameter(SpeechConstant.LANGUAGE));
        //此处用于设置dialog中不显示错误码信息
        //mIat.setParameter("view_tips_plain","false");
        //开始录入音频后，音频后面部分最长静音时长，取值范围[0,10000ms]，默认值5000ms
        mIat.setParameter(SpeechConstant.VAD_BOS, "3000");
        // 设置语音后端点:后端点静音检测时间，即用户停止说话多长时间内即认为不再输入， 自动停止录音取值范围[0,10000ms]，默认值1800ms。
        mIat.setParameter(SpeechConstant.VAD_EOS, "500");
        // 设置标点符号,设置为"0"返回结果无标点,设置为"1"返回结果有标点
        mIat.setParameter(SpeechConstant.ASR_PTT, "1");
        // 设置音频保存路径，保存音频格式支持pcm、wav，设置路径为sd卡请注意WRITE_EXTERNAL_STORAGE权限
        mIat.setParameter(SpeechConstant.AUDIO_FORMAT, "wav");
        mIat.setParameter(SpeechConstant.ASR_AUDIO_PATH, Environment.getExternalStorageDirectory() + "/msc/helloword.wav");
    }

    private void printResult(RecognizerResult results) {
        String text = JsonParser.parseIatResult(results.getResultString());
        String sn = null;
        // 读取json结果中的sn字段
        try {
            JSONObject resultJson = new JSONObject(results.getResultString());
            sn = resultJson.optString("sn");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        mIatResults.put(sn, text);

        StringBuffer resultBuffer = new StringBuffer();
        for (String key : mIatResults.keySet()) {
            resultBuffer.append(mIatResults.get(key));
        }
       //  mResultText.setText(resultBuffer.toString());

        Log.d("kedaxunfei",resultBuffer.toString());
        robot = new Robot(resultBuffer.toString());
        QiSDK.register(this,robot);
       // mResultText.setSelection(mResultText.length());
        // QiSDK.register(this, this);
    }

    private RecognizerDialogListener mRecognizerDialogListener = new RecognizerDialogListener() {
        public void onResult(RecognizerResult results, boolean isLast) {

            Log.d("robot", results.getResultString());
            if (isLast) {
                Log.d("robot", "onResult 结束");
            }
            if (resultType.equals("json")) {
                printResult(results);
                return;
            }
            if (resultType.equals("plain")) {
                buffer.append(results.getResultString());
                mResultText.setText(buffer.toString());
                mResultText.setSelection(mResultText.length());
            }

        }

        /**
         * 识别回调错误.
         */
        public void onError(SpeechError error) {
          //  showMsg(error.getPlainDescription(true));
        }

    };

    private RecognizerListener mRecognizerListener = new RecognizerListener() {

        @Override
        public void onVolumeChanged(int i, byte[] bytes) {

        }

        @Override
        public void onBeginOfSpeech() {
            // 此回调表示：sdk内部录音机已经准备好了，用户可以开始语音输入
            // showToast("开始说话");
            Log.d("is ", "talking");
        }

        @Override
        public void onEndOfSpeech() {

        }

        @Override
        public void onResult(RecognizerResult recognizerResult, boolean b) {
            printResult(recognizerResult);
            return;
//            Log.e("i", "onResult: " + results.getResultString());
//            if (resultType.equals(resultType)) {
//                printResult(results);
//            } else if (resultType.equals("plain")) {
//                buffer.append(results.getResultString());
//                mResultText.setText(buffer.toString());
//                mResultText.setSelection(mResultText.length());
//            }
//            if (isLast & cyclic) {
//                // TODO 最后的结果
//                Message message = Message.obtain();
//                message.what = handlerCode;
//                handler.sendMessageDelayed(message, 100);
//            }
        }

        @Override
        public void onError(SpeechError error) {
            // Tips：
            // 错误码：10118(您没有说话)，可能是录音机权限被禁，需要提示用户打开应用的录音权限。
            //showToast(error.getPlainDescription(true));
//            if (null != dialog) {
//                dialog.dismiss();
//            }
        }

        @Override
        public void onEvent(int i, int i1, int i2, Bundle bundle) {

        }
    };


        @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SpeechUtility.createUtility(this, SpeechConstant.APPID +"=0584d9cf");
        setContentView(R.layout.activity_main);

        dialogFlowIcon = findViewById(R.id.dialogFlow);
        qiChatBotIcon = findViewById(R.id.qiChatBot);
        pepperTxt = findViewById(R.id.pepperTxt);
        suggestion1 = findViewById(R.id.suggestion1);
        suggestion2 = findViewById(R.id.suggestion2);
        suggestion3 = findViewById(R.id.suggestion3);
        suggestion4 = findViewById(R.id.suggestion4);
        fillSuggestion();
        ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION);
        mIat = SpeechRecognizer.createRecognizer(MainActivity.this, mInitListener);

        mIatDialog = new RecognizerDialog(MainActivity.this, mInitListener);
        findViewById(R.id.btn_exit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finishAffinity();
            }
        });
        findViewById(R.id.resetSuggestions).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fillSuggestion();
            }
        });
        findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mIat == null){
                    Log.d("this","uiuiuiooo");
                }
                setParam(); // 设置参数
               // mIatDialog.setListener(mRecognizerDialogListener);//设置监听
                mIat.startListening(mRecognizerListener);
                mIatDialog.setListener(mRecognizerDialogListener);
                mIatDialog.show();


            }
        });

        // In this sample, instead of implementing robotlifecycle callbacks in the main activity,
        // we delegate them to a robot dedicated class.
        // robot = new Robot(this);
       // QiSDK.register(this, robot);

    }

    private void fillSuggestion() {
        if (suggestions.isEmpty()) {
            return;
        }
        int[] fourRandomInt = get4RandomInt(suggestions.size());
        suggestion1.setText(String.format(getString(R.string.suggestion_format), suggestions.get(fourRandomInt[0]).getText()));
        suggestion2.setText(String.format(getString(R.string.suggestion_format), suggestions.get(fourRandomInt[1]).getText()));
        suggestion3.setText(String.format(getString(R.string.suggestion_format), suggestions.get(fourRandomInt[2]).getText()));
        suggestion4.setText(String.format(getString(R.string.suggestion_format), suggestions.get(fourRandomInt[3]).getText()));

    }

    private InitListener mInitListener = new InitListener() {

        @Override
        public void onInit(int code) {
            Log.d("robot", "SpeechRecognizer init() code = " + code);
            if (code != ErrorCode.SUCCESS) {
               // showTip("初始化失败，错误码：" + code + ",请点击网址https://www.xfyun.cn/document/error-code查询解决方案");
            }
        }
    };

    @Override
    protected void onDestroy() {
        // QiSDK.unregister(this, robot);
        super.onDestroy();
    }

    public void colorDialogFlow() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                qiChatBotIcon.setBackground(ContextCompat.getDrawable(MainActivity.this, R.drawable.top_left_rounded_background));
                dialogFlowIcon.setBackground(ContextCompat.getDrawable(MainActivity.this, R.drawable.green_top_right_rounded_background));
                pepperTxt.setBackground(ContextCompat.getDrawable(MainActivity.this, R.drawable.peper_talk_green_background));
            }
        });
    }

    public void colorQiChatBot() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                qiChatBotIcon.setBackground(ContextCompat.getDrawable(MainActivity.this, R.drawable.green_top_left_rounded_background));
                dialogFlowIcon.setBackground(ContextCompat.getDrawable(MainActivity.this, R.drawable.top_right_rounded_background));
                pepperTxt.setBackground(ContextCompat.getDrawable(MainActivity.this, R.drawable.peper_talk_green_background));
            }
        });
    }

    @Override
    public void setText(final String text) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (!TextUtils.isEmpty(text)) {
                    pepperTxt.setText(text);
                    if (isDialogFlow) {
                        colorDialogFlow();
                        isDialogFlow = false;
                    } else {
                        colorQiChatBot();
                    }
                }
                resetLayout(text);
            }
        });

    }

    @Override
    public void isDialogFlow(boolean dialogFlow) {
        isDialogFlow = dialogFlow;
    }

    private void resetLayout(final String text) {
        if (TextUtils.isEmpty(text)) {
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    pepperTxt.setText("");
                    dialogFlowIcon.setBackground(ContextCompat.getDrawable(MainActivity.this, R.drawable.top_right_rounded_background));
                    qiChatBotIcon.setBackground(ContextCompat.getDrawable(MainActivity.this, R.drawable.top_left_rounded_background));
                    pepperTxt.setBackground(ContextCompat.getDrawable(MainActivity.this, R.drawable.peper_talk_background));
                    fillSuggestion();
                }
            }, 500);
        }
    }

    @Override
    public void updateQiChatSuggestions(List<Phrase> suggestions) {
        this.suggestions = suggestions;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                fillSuggestion();
            }
        });
    }

    public int[] get4RandomInt(int maxRange) {
        int[] result = new int[4];
        Set<Integer> used = new HashSet<>();
        Random gen = new Random();
        for (int i = 0; i < result.length; i++) {
            int newRandom;
            do {
                newRandom = gen.nextInt(maxRange);
            } while (used.contains(newRandom));
            result[i] = newRandom;
            used.add(newRandom);
        }
        return result;
    }


}
