package com.inhatc.eatzy_project;

/**
 * 커뮤니티 게시글 하나를 담는 POJO (posts/{key} 에 저장).
 */
public class Post {

    public String title;     // 제목
    public String content;   // 내용
    public String author;    // 작성자
    public long createdAt;   // 작성 시각 (System.currentTimeMillis())

    // Firebase 변환용 빈 생성자 (필수)
    public Post() { }

    public Post(String title, String content, String author) {
        this.title = title;
        this.content = content;
        this.author = author;
        this.createdAt = System.currentTimeMillis();
    }
}
