package com.inhatc.eatzy_project;

/**
 * 식당 하나를 담는 POJO (restaurants/{key} 에 저장).
 * 룰렛 당첨 메뉴 카테고리와 매칭하고, 좌표로 반경 500m 검색에 사용한다.
 */
public class Restaurant {

    public String name;          // 식당명
    public double lat;           // 위도
    public double lng;           // 경도
    public String menuCategory;  // 이 식당이 파는 메뉴 카테고리 (예: "한식")
    public String address;       // 주소 (리스트 표시용)
    public double rating;        // 별점 (리스트 표시용, 0~5)

    // Firebase 변환용 빈 생성자 (필수)
    public Restaurant() { }

    public Restaurant(String name, double lat, double lng, String menuCategory,
                      String address, double rating) {
        this.name = name;
        this.lat = lat;
        this.lng = lng;
        this.menuCategory = menuCategory;
        this.address = address;
        this.rating = rating;
    }
}
