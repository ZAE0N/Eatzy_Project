package com.inhatc.eatzy_project;

import android.app.Activity;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.google.android.recaptcha.Recaptcha;
import com.google.android.recaptcha.RecaptchaAction;
import com.google.android.recaptcha.RecaptchaTasksClient;

/**
 * reCAPTCHA Enterprise 봇 방지를 한 곳에서 처리하는 헬퍼.
 *
 * 흐름(클라이언트 측):
 *   1) strings.xml 의 recaptcha_site_key 로 RecaptchaTasksClient 를 가져온다.
 *   2) 보호할 동작에 맞는 RecaptchaAction 으로 토큰을 발급(executeTask)한다.
 *   3) 토큰이 정상 발급되면 "사람"으로 보고 통과시킨다.
 *
 * 주의: 이 앱은 백엔드가 없어 토큰을 서버에서 재검증(assessment)하지는 못한다.
 * 실제 운영에선 발급한 토큰을 백엔드(예: Cloud Functions)가 reCAPTCHA Enterprise API
 * 로 보내 점수를 확인해야 완전한 봇 차단이 된다. 여기서는 토큰 발급까지로 데모한다.
 *
 * 사이트 키가 placeholder 거나 비어 있으면(개발 단계) 검증을 건너뛰고 통과시킨다.
 */
public class RecaptchaHelper {

    public interface Callback {
        /** pass=true 면 사람으로 보고 진행, false 면 봇 의심으로 차단. */
        void onResult(boolean pass);
    }

    // 프로세스 내 클라이언트 캐시(키별 1회 fetch)
    private static RecaptchaTasksClient cachedClient;

    private RecaptchaHelper() { }

    /** 사이트 키가 아직 설정되지 않았는지(개발 단계인지) 판단. */
    private static boolean isPlaceholderKey(String key) {
        return TextUtils.isEmpty(key) || key.startsWith("YOUR_");
    }

    /**
     * 봇 검증을 수행하고 결과를 콜백으로 돌려준다.
     *
     * @param activity   호출 화면
     * @param action     보호할 동작 (RecaptchaAction.LOGIN()/SIGNUP()/custom("post_write") 등)
     * @param callback   통과/차단 결과
     */
    public static void verify(@NonNull Activity activity,
                              @NonNull RecaptchaAction action,
                              @NonNull Callback callback) {
        String siteKey = activity.getString(R.string.recaptcha_site_key);

        // 키 미설정(개발 단계) → 그냥 통과시켜 앱 흐름이 막히지 않게 한다.
        if (isPlaceholderKey(siteKey)) {
            callback.onResult(true);
            return;
        }

        if (cachedClient != null) {
            execute(activity, action, callback);
            return;
        }

        Recaptcha.fetchTaskClient(activity.getApplication(), siteKey)
                .addOnSuccessListener(activity, client -> {
                    cachedClient = client;
                    execute(activity, action, callback);
                })
                .addOnFailureListener(activity, e -> callback.onResult(false));
    }

    private static void execute(@NonNull Activity activity,
                                @NonNull RecaptchaAction action,
                                @NonNull Callback callback) {
        cachedClient.executeTask(action)
                .addOnSuccessListener(activity, token ->
                        // 토큰이 정상 발급되면 사람으로 간주하고 통과
                        callback.onResult(!TextUtils.isEmpty(token)))
                .addOnFailureListener(activity, e -> callback.onResult(false));
    }
}
