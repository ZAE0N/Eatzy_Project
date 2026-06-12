package com.inhatc.eatzy_project;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

/**
 * Firebase Realtime Database 연결을 한 곳에서 관리하는 헬퍼 클래스.
 *
 * 강의(13주차)에서는 Activity마다 아래 두 줄을 반복했지만,
 *   myFirebase    = FirebaseDatabase.getInstance();
 *   myDB_Reference = myFirebase.getReference();
 * 여기서 한 번만 만들어 두고, 어디서든 FirebaseHelper.get()으로 꺼내 쓴다.
 *
 * 사용 예) 회원 정보 저장
 *   FirebaseHelper.get().users().child(uid).setValue(user);
 */
public class FirebaseHelper {

    // ── DB 노드(테이블 역할) 이름. 오타 방지를 위해 상수로 모아둔다. ──
    public static final String NODE_USERS = "users";             // 사용자(취향/알레르기/기피메뉴)
    public static final String NODE_MENUS = "menus";             // 메뉴(룰렛 후보)
    public static final String NODE_RESTAURANTS = "restaurants"; // 식당(이름/좌표/메뉴카테고리)
    public static final String NODE_POSTS = "posts";             // 커뮤니티 게시글

    // 싱글톤: 앱 전체에서 인스턴스 하나만 공유한다.
    private static FirebaseHelper instance;

    private final FirebaseDatabase database;   // Firebase 본체
    private final DatabaseReference rootRef;    // DB 최상위 경로 참조

    private FirebaseHelper() {
        // google-services.json 의 firebase_url 을 보고 자동으로 연결된다.
        database = FirebaseDatabase.getInstance();
        rootRef = database.getReference();
    }

    /** 어디서든 FirebaseHelper.get() 으로 동일 인스턴스를 얻는다. */
    public static synchronized FirebaseHelper get() {
        if (instance == null) {
            instance = new FirebaseHelper();
        }
        return instance;
    }

    // ── 자주 쓰는 노드 참조를 메서드로 제공 (rootRef.child("users") 등을 매번 안 쳐도 됨) ──
    public DatabaseReference root() { return rootRef; }
    public DatabaseReference users() { return rootRef.child(NODE_USERS); }
    public DatabaseReference menus() { return rootRef.child(NODE_MENUS); }
    public DatabaseReference restaurants() { return rootRef.child(NODE_RESTAURANTS); }
    public DatabaseReference posts() { return rootRef.child(NODE_POSTS); }

    /**
     * 연결이 실제로 되는지 확인하는 테스트용 메서드.
     * connectionTest 노드에 현재 시각을 한 번 써본다.
     * 성공하면 콘솔 Realtime Database 화면에 connectionTest 값이 보인다.
     */
    public void writeConnectionTest() {
        rootRef.child("connectionTest").setValue(System.currentTimeMillis());
    }

    /**
     * 강의의 mGet_FirebaseDatabase() 패턴(한 번만 읽기)을 그대로 사용한 예시.
     * 전달한 노드를 한 번 읽어 onResult 콜백으로 돌려준다.
     *
     * @param ref      읽을 위치 (예: FirebaseHelper.get().menus())
     * @param callback 읽기 완료/실패 시 호출
     */
    public void readOnce(@NonNull DatabaseReference ref, @NonNull ReadCallback callback) {
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                callback.onResult(snapshot);   // 정상적으로 데이터를 읽음
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onError(error);       // 통신 실패 등 에러
            }
        });
    }

    /** readOnce 의 결과를 받기 위한 콜백 인터페이스. */
    public interface ReadCallback {
        void onResult(DataSnapshot snapshot);
        void onError(DatabaseError error);
    }
}
