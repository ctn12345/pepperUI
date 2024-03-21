package com.softbankrobotics.chatbotsample;


import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;

import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.RecognizerListener;
import com.iflytek.cloud.RecognizerResult;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechRecognizer;
import com.iflytek.cloud.SpeechUtility;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.LinkedHashMap;

public class MyBackgroundService extends Service {
    public static final String ACTION_STRING_RESPONSE = "com.example.action.STRING_RESPONSE";
    public static final String EXTRA_STRING_RESPONSE = "com.example.extra.STRING_RESPONSE";
    private AudioManager audioManager;
    private SpeechRecognizer mIat;
    private Thread backgroundThread;
    private HashMap<String, String> mIatResults = new LinkedHashMap<>();
    // 语言类型【默认中文】
    private String language = "zh_cn";
    // 格式类型【默认json】
    private String resultType = "json";
    // 说话结束
    Boolean signal = true;


    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // 处理接收到的intent
            String action = intent.getAction();
            if ("ACTION_LOST_FOCUS".equals(action)) {
                // 接收布尔值数据
                signal = true;
                Log.d("mReceiver","signallllll");
            }
        }
    };

    // 目的是为了避免重复startlistener的情况 说话完成为true


    // 初始化监听器(android)
    private InitListener mInitListener = new InitListener() {

        @Override
        public void onInit(int code) {
            Log.d("robot", "SpeechRecognizer init() code = " + code);
            if (code != ErrorCode.SUCCESS) {
                // showTip("初始化失败，错误码：" + code + ",请点击网址https://www.xfyun.cn/document/error-code查询解决方案");
            }
        }
    };

    // 设置监听器的各个参数
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
        mIat.setParameter(SpeechConstant.VAD_EOS, "10000");
        // 设置标点符号,设置为"0"返回结果无标点,设置为"1"返回结果有标点
        mIat.setParameter(SpeechConstant.ASR_PTT, "1");
        // 设置音频保存路径，保存音频格式支持pcm、wav，设置路径为sd卡请注意WRITE_EXTERNAL_STORAGE权限
        mIat.setParameter(SpeechConstant.AUDIO_FORMAT, "wav");
        mIat.setParameter(SpeechConstant.ASR_AUDIO_PATH,
                getExternalFilesDir("msc").getAbsolutePath() + "/iat.wav");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        //        IntentFilter filter = new IntentFilter("com.example.action.SEND_SIGNAL");
        //        registerReceiver(mReceiver, filter);
        // 设置科大讯飞listener的各个参数
        SpeechUtility.createUtility(this, SpeechConstant.APPID +"=0584d9cf");
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mIat = SpeechRecognizer.createRecognizer(getApplicationContext(), mInitListener);
        setParam();
        IntentFilter filter = new IntentFilter("ACTION_LOST_FOCUS");
        registerReceiver(mReceiver, filter);
    }

    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {

        backgroundThread = new Thread(new Runnable() {
            @Override
            public void run() {
                // 在此处执行耗时操作
                startListening();
            }
        });
        backgroundThread.start();

        return START_STICKY;
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
        String message = resultBuffer.toString();

        // 初始化广播器
        Intent broadcastIntent = new Intent();
        // 发送广播
        broadcastIntent.setAction(ACTION_STRING_RESPONSE);
        broadcastIntent.putExtra(EXTRA_STRING_RESPONSE, message);
        sendBroadcast(broadcastIntent);
    }

    private void startListening() {
        int sampleRate = 8000; // 采样率
        int bufferSize = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        AudioRecord audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize);
        short[] buffer = new short[bufferSize];
        audioRecord.startRecording();

        // 生成符合条件的音频
        while (true) {
            audioRecord.read(buffer, 0, bufferSize);
            double rms = calculateRms(buffer);
            if (rms > 1000) {
                // 开始使用科大讯飞SDK进行识别
                Log.d("speechtest", String.valueOf(rms));
                mIat.startListening(mRecognizerListener);
                signal = false;
                Log.d("speechtestHIGH","test111999");
                break;
            }
            else{
                Log.d("speechtest","novoice 111");
            }
            // 监听是否说话完成，说话完成signal为true
        }
    }

    private double calculateRms(short[] buffer) {
        double sum = 0;
        for (short sample : buffer) {
            sum += sample * sample;
        }
        double rms = Math.sqrt(sum / buffer.length);
        return rms;
    }

    private RecognizerListener mRecognizerListener = new RecognizerListener() {
        @Override
        public void onVolumeChanged(int i, byte[] bytes) {

        }

        @Override
        public void onBeginOfSpeech() {

        }

        @Override
        public void onEndOfSpeech() {

        }

        @Override
        public void onResult(RecognizerResult recognizerResult, boolean b) {
            printResult(recognizerResult);
            Log.d("enter","onResultttt");
            return;
        }

        @Override
        public void onError(SpeechError speechError) {

        }

        @Override
        public void onEvent(int i, int i1, int i2, Bundle bundle) {

        }
        // 实现识别监听器的方法
    };

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
