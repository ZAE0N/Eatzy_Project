package com.inhatc.eatzy_project;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 개발/시연용 샘플 데이터를 Firebase 에 한 번만 심어주는 헬퍼.
 *
 * 좌표는 폴백 기준점인 인하공업전문대학(37.4516, 126.7015) 주변으로 잡았다.
 * 대부분 반경 500m 안에 있고, 끝의 2개(★)는 500m 밖이라 반경 필터 동작 확인용이다.
 */
public class SampleData {

    /** 시연용 식당 목록. 카테고리는 룰렛 6칸(한식/중식/일식/양식/분식/치킨)과 1:1로 맞춘다. */
    private static final List<Restaurant> RESTAURANTS = Arrays.asList(
            // ── 반경 500m 이내 ──
            new Restaurant("맛있는 국밥집", 37.4520, 126.7018, "한식", "인천 미추홀구 인하로 100", 4.5),
            new Restaurant("엄마손 백반",   37.4513, 126.7012, "한식", "인천 미추홀구 인하로 88",  4.2),
            new Restaurant("북경반점",     37.4512, 126.7022, "중식", "인천 미추홀구 인하로 72",  4.3),
            new Restaurant("스시 오마카세", 37.4519, 126.7008, "일식", "인천 미추홀구 인하로 110", 4.7),
            new Restaurant("파스타 공방",   37.4524, 126.7015, "양식", "인천 미추홀구 인하로 120", 4.1),
            new Restaurant("화덕 피자",     37.4510, 126.7019, "양식", "인천 미추홀구 인하로 60",  4.6),
            new Restaurant("신전 떡볶이",   37.4515, 126.7009, "분식", "인천 미추홀구 인하로 95",  4.0),
            new Restaurant("BHC 치킨",     37.4517, 126.7025, "치킨", "인천 미추홀구 인하로 130", 4.4),
            // ── 반경 500m 밖 (필터 동작 확인용 ★) ──
            new Restaurant("멀리있는 한정식", 37.4560, 126.7060, "한식", "인천 미추홀구 멀리길 1", 4.8),
            new Restaurant("외곽 중화요리",   37.4555, 126.7065, "중식", "인천 미추홀구 멀리길 2", 4.1)
    );

    /** 룰렛 후보 메뉴. category 는 룰렛 6칸과 맞추고, allergens 로 알레르기 필터를 테스트한다. */
    private static final List<Menu> MENUS = Arrays.asList(
            new Menu("김치찌개", "한식", Arrays.asList("돼지고기")),
            new Menu("비빔밥",   "한식", Arrays.asList("계란")),
            new Menu("된장찌개", "한식", new ArrayList<>()),
            new Menu("짜장면",   "중식", Arrays.asList("밀")),
            new Menu("짬뽕",     "중식", Arrays.asList("갑각류", "밀")),
            new Menu("탕수육",   "중식", Arrays.asList("밀", "돼지고기")),
            new Menu("초밥",     "일식", Arrays.asList("생선", "갑각류")),
            new Menu("라멘",     "일식", Arrays.asList("밀", "계란")),
            new Menu("돈카츠",   "일식", Arrays.asList("밀", "돼지고기")),
            new Menu("파스타",   "양식", Arrays.asList("밀")),
            new Menu("피자",     "양식", Arrays.asList("밀", "우유")),
            new Menu("스테이크", "양식", new ArrayList<>()),
            new Menu("떡볶이",   "분식", Arrays.asList("밀")),
            new Menu("김밥",     "분식", Arrays.asList("계란")),
            new Menu("순대",     "분식", new ArrayList<>()),
            new Menu("후라이드", "치킨", Arrays.asList("밀")),
            new Menu("양념치킨", "치킨", Arrays.asList("밀"))
    );

    /** 커뮤니티 샘플 게시글. */
    private static final List<Post> POSTS = Arrays.asList(
            new Post("강남역 혼밥 가능한 한식집 추천해요!",
                    "오늘 Eatzy 룰렛 돌렸더니 국밥 나왔는데 진짜 맛있었어요! 가격도 착하고 양도 많아요. 혼밥하기도 분위기 좋아서 강추합니다 ㅎㅎ",
                    "김점심"),
            new Post("룰렛이 맨날 일식 나오는 사람 있어요?",
                    "니즈 설정에서 일식 체크 해제해도 자꾸 나오는 것 같은데.. 저만 그런가요 😅",
                    "박메뉴"),
            new Post("분식집인데 파스타도 팔아요 ㄷㄷ",
                    "오늘 룰렛 결과로 간 집인데 메뉴판 보고 깜짝 놀랐습니다 진짜 퓨전 그 자체...",
                    "이밥먹자"),
            new Post("점심 예산 8천원으로 먹을 수 있는 메뉴들",
                    "요즘 물가 오르면서 8천원 이하로 배부르게 먹기 힘들더라고요. 그래도 찾아보니 꽤 있어요!",
                    "최뭐먹지")
    );

    /**
     * restaurants 노드가 비어 있을 때만 샘플 데이터를 심는다(중복 방지).
     * 완료(또는 이미 데이터 있음/실패) 시 onDone 을 호출한다.
     */
    public static void seedRestaurantsIfEmpty(@NonNull Runnable onDone) {
        seedIfEmpty(FirebaseHelper.get().restaurants(), RESTAURANTS, onDone);
    }

    /** posts 노드가 비어 있을 때만 샘플 게시글을 심는다. */
    public static void seedPostsIfEmpty(@NonNull Runnable onDone) {
        seedIfEmpty(FirebaseHelper.get().posts(), POSTS, onDone);
    }

    /** menus 노드가 비어 있을 때만 샘플 메뉴를 심는다. */
    public static void seedMenusIfEmpty(@NonNull Runnable onDone) {
        seedIfEmpty(FirebaseHelper.get().menus(), MENUS, onDone);
    }

    /** 노드가 비어 있으면 items 를 push 로 1회 시딩하고, 끝나면 onDone 호출. */
    private static void seedIfEmpty(DatabaseReference ref, List<?> items, @NonNull Runnable onDone) {
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && snapshot.getChildrenCount() > 0) {
                    onDone.run();   // 이미 데이터가 있으면 아무것도 안 함
                    return;
                }
                for (Object item : items) {
                    ref.push().setValue(item);
                }
                onDone.run();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                onDone.run();   // 실패해도 화면은 계속 진행
            }
        });
    }

    private SampleData() { }
}
