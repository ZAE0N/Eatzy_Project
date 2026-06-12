package com.inhatc.eatzy_project;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * 현재 로그인한 사용자를 앱 전역에서 알 수 있게 SharedPreferences 에 저장/조회한다.
 *
 * Firebase RTDB 의 users/{key} 와 1:1로 연결되는 "키"를 들고 있어서,
 * 니즈설정 저장(누구 것?)과 룰렛 필터(누구 기피메뉴?)가 같은 사용자를 가리키게 한다.
 *
 * 이메일은 RTDB 키에 못 쓰는 문자가 있어 keyFromEmail() 로 치환한 값을 저장한다.
 * 게스트는 고정 키 "guest" 를 사용한다.
 */
public class SessionManager {

    private static final String PREFS = "eatzy_session";
    private static final String KEY_USER = "user_key";
    public static final String GUEST_KEY = "guest";

    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /** 로그인/게스트 진입 시 현재 사용자 키를 저장. */
    public void setCurrentUserKey(String userKey) {
        prefs.edit().putString(KEY_USER, userKey).apply();
    }

    /** 현재 사용자 키. 저장된 게 없으면 게스트로 폴백. */
    public String getCurrentUserKey() {
        return prefs.getString(KEY_USER, GUEST_KEY);
    }

    public void clear() {
        prefs.edit().remove(KEY_USER).apply();
    }

    /**
     * 이메일을 RTDB 키로 쓸 수 있게 변환한다.
     * RTDB 키에는 . # $ [ ] / 를 쓸 수 없으므로 '_'로 치환.
     * 예) kim@a.com → kim@a_com
     */
    public static String keyFromEmail(String email) {
        if (email == null) return GUEST_KEY;
        String trimmed = email.trim();
        if (trimmed.isEmpty()) return GUEST_KEY;
        return trimmed.replaceAll("[.#$\\[\\]/]", "_");
    }
}
