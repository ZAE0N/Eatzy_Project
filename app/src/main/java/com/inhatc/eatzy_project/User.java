package com.inhatc.eatzy_project;

import java.util.ArrayList;
import java.util.List;

/**
 * 사용자 한 명을 담는 POJO (users/{uid} 에 저장).
 * 강의의 CustomerInfo.java 와 동일한 패턴: public 필드 + 빈 생성자 + 편의 생성자.
 */
public class User {

    public String email;                      // 이메일(로그인 ID)
    public List<String> allergies;            // 알레르기/기피 키워드 (예: "땅콩", "갑각류") — 룰렛에서 제외
    public List<String> avoidMenus;           // 기피 메뉴 (특정 메뉴명 제외용, 예약)
    public List<String> preferredCategories;  // 선호 음식 카테고리 (니즈설정 음식종류 칩) — 룰렛 후보
    public List<String> preferredIngredients; // 선호 주재료 (밥/면 등)
    public int maxPrice;                       // 가격 상한 (원)

    // Firebase가 JSON → 객체로 변환할 때 사용하는 빈 생성자 (필수!)
    public User() {
        this.allergies = new ArrayList<>();
        this.avoidMenus = new ArrayList<>();
        this.preferredCategories = new ArrayList<>();
        this.preferredIngredients = new ArrayList<>();
    }

    public User(String email, List<String> allergies, List<String> avoidMenus) {
        this();
        this.email = email;
        this.allergies = allergies;
        this.avoidMenus = avoidMenus;
    }
}
