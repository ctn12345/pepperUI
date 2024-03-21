/*
 *  Copyright (C) 2018 Softbank Robotics Europe
 *  See COPYING for the license
 */
package com.softbankrobotics.chatbotsample;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.util.TypedValue;

import com.aldebaran.qi.sdk.QiContext;
import com.aldebaran.qi.sdk.RobotLifecycleCallbacks;
import com.aldebaran.qi.sdk.builder.ChatBuilder;
import com.aldebaran.qi.sdk.builder.QiChatbotBuilder;
import com.aldebaran.qi.sdk.builder.SayBuilder;
import com.aldebaran.qi.sdk.builder.TopicBuilder;
import com.aldebaran.qi.sdk.object.conversation.Chat;
import com.aldebaran.qi.sdk.object.conversation.Chatbot;
import com.aldebaran.qi.sdk.object.conversation.Phrase;
import com.aldebaran.qi.sdk.object.conversation.QiChatExecutor;
import com.aldebaran.qi.sdk.object.conversation.QiChatbot;
import com.aldebaran.qi.sdk.object.conversation.Say;
import com.aldebaran.qi.sdk.object.conversation.Topic;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Class that gathers main robot-related operations of our application.
 */
public class Robot implements RobotLifecycleCallbacks {

    private static final String TAG = "Robot";
    private QiContext qiContext;
    private String text;
    private Context context;


    public Robot(Context context, String text) {
        this.context = context;
        this.text = text;
    }

    @Override
    public void onRobotFocusGained(final QiContext theContext) {
        Log.d(TAG, "onRobotFocusGained");
        this.qiContext = theContext;

        // Now that the focus is owned by this app, the chat can be run
        runChat();
    }

    @Override
    public void onRobotFocusLost() {
        Log.d(TAG, "onRobotFocusLost");
        this.qiContext = null;

        // 发送说话完毕的信号
        Intent intent = new Intent(context, MyBackgroundService.class);
        intent.setAction("ACTION_LOST_FOCUS");
        intent.putExtra("signal", true);
        context.startService(intent);
        Log.d("Robot","I have finished");

    }

    @Override
    public void onRobotFocusRefused(final String reason) {
        Log.e(TAG, "Robot is not available: " + reason);
    }

    private void runChat() {
        Say say = SayBuilder.with(qiContext)
                .withText(text)
                .build();
        say.run();
        Intent intent = new Intent(context, MyBackgroundService.class);
        intent.setAction("ACTION_LOST_FOCUS");
        intent.putExtra("signal", true);
        context.startService(intent);
        Log.d(TAG, "runChat()");

    }




}
