package com.inhatc.eatzy_project;

/**
 * 커뮤니티 게시글 하나를 담는 POJO (posts/{key} 에 저장).
 */
public class Post {

    public String title;     // 제목
    public String content;   // 내용
    public String author;    // 작성자
    public long createdAt;   // 작성 시각 (System.currentTimeMillis())

    public String imageUri;  // 첨부 이미지 URI(없으면 null)
    public String placeName; // 등록 위치 이름/주소(없으면 null)
    public double lat;        // 위치 위도(0이면 없음)
    public double lng;        // 위치 경도(0이면 없음)
    public int likes;         // 좋아요 수(베스트 탭 판정용)

    // Firebase 변환용 빈 생성자 (필수)
    public Post() { }

    public Post(String title, String content, String author) {
        this.title = title;
        this.content = content;
        this.author = author;
        this.createdAt = System.currentTimeMillis();
    }

    /** 위치가 등록됐는지(맵 버튼 노출 조건). */
    public boolean hasLocation() {
        return lat != 0 || lng != 0;
    }
}
