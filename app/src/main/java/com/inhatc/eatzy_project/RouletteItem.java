package com.inhatc.eatzy_project;

/**
 * 사용자가 맛집 추천 화면에서 룰렛에 직접 추가한 맛집 한 개
 * (rouletteItems/{userKey}/{key} 에 저장).
 * 룰렛판에는 name 을 표시하고, 당첨 시 category 로 맛집을 다시 찾는다.
 */
public class RouletteItem {

    public String name;       // 맛집명(룰렛판 표시용)
    public String category;   // 메뉴 카테고리(당첨 후 맛집 필터용)

    // Firebase 변환용 빈 생성자 (필수)
    public RouletteItem() { }

    public RouletteItem(String name, String category) {
        this.name = name;
        this.category = category;
    }
}
