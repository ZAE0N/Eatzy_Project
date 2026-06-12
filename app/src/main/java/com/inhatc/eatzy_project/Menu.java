package com.inhatc.eatzy_project;

import java.util.ArrayList;
import java.util.List;

/**
 * 룰렛 후보 메뉴 하나를 담는 POJO (menus/{key} 에 저장).
 */
public class Menu {

    public String name;              // 메뉴명 (예: "김치찌개")
    public String category;          // 카테고리 (예: "한식") — 식당 매칭에 사용
    public List<String> allergens;   // 이 메뉴에 든 알레르기 유발 성분 (예: "갑각류")

    // Firebase 변환용 빈 생성자 (필수)
    public Menu() {
        this.allergens = new ArrayList<>();
    }

    public Menu(String name, String category, List<String> allergens) {
        this.name = name;
        this.category = category;
        this.allergens = allergens;
    }
}
