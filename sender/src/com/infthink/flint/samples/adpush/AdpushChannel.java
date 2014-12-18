/*
 * Copyright (C) 2013-2014, Infthink (Beijing) Technology Co., Ltd. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.infthink.flint.samples.adpush;

import org.json.JSONException;
import org.json.JSONObject;

import tv.matchstick.flint.Flint;
import tv.matchstick.flint.FlintDevice;
import tv.matchstick.flint.FlintManager;
import tv.matchstick.flint.ResultCallback;
import tv.matchstick.flint.Status;

import android.util.Log;

public class AdpushChannel implements Flint.MessageReceivedCallback {
    private static final String TAG = AdpushChannel.class.getSimpleName();

    private static final String GAME_NAMESPACE = "urn:flint:org.openflint.fling.media_ad";

    private AdChangeListener mAdChangeListener;

    public AdpushChannel() {
    }

    public String getNamespace() {
        return GAME_NAMESPACE;
    }
    
    public void setAdChangeListener(AdChangeListener listener) {
        mAdChangeListener = listener;
    }

    /**
     * The sender can use that to send String messages to the receiver over that
     * channel
     * 
     * @param apiClient
     * @param message
     */
    private final void sendMessage(FlintManager apiClient, String message) {
        Log.d(TAG, "Sending message: (ns=" + GAME_NAMESPACE + ") " + message);
        Flint.FlintApi.sendMessage(apiClient, GAME_NAMESPACE, message)
                .setResultCallback(new SendMessageResultCallback(message));
    }

    @Override
    public void onMessageReceived(FlintDevice flingDevice, String namespace,
            String message) {
        Log.d(TAG, "onTextMessageReceived: " + message);
        JSONObject payload;
        try {
            payload = new JSONObject(message);
            Log.d(TAG, "payload: " + payload);
            AdData data = new AdData();
            data.time = payload.optLong("time");
            data.video_url = payload.optString("video_url");
            data.video_title = payload.optString("video_title");
            data.type = payload.optString("type");
            JSONObject ad_data = payload.optJSONObject("ad_data");
            data.image_url = ad_data.optString("image_url");
            data.click_link = ad_data.optString("click_link");
            if (mAdChangeListener != null) {
                mAdChangeListener.onAdChange(data);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private final class SendMessageResultCallback implements
            ResultCallback<Status> {
        String mMessage;

        SendMessageResultCallback(String message) {
            mMessage = message;
        }

        @Override
        public void onResult(Status result) {
            if (!result.isSuccess()) {
                Log.d(TAG,
                        "Failed to send message. statusCode: "
                                + result.getStatusCode() + " message: "
                                + mMessage);
            }
        }
    }

    class AdData {
        long time;
        String video_url;
        String video_title;
        String type;
        String image_url;
        String click_link;
    }
    
    interface AdChangeListener {
        void onAdChange(AdData data);
    }
}
