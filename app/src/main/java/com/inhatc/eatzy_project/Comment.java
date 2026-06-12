package com.inhatc.eatzy_project;

/**
 * 댓글 하나를 담는 POJO (posts/{postKey}/comments/{key} 에 저장).
 */
public class Comment {

    public String author;    // 작성자
    public String text;      // 댓글 내용
    public long createdAt;   // 작성 시각

    // Firebase 변환용 빈 생성자 (필수)
    public Comment() { }

    public Comment(String author, String text) {
        this.author = author;
        this.text = text;
        this.createdAt = System.currentTimeMillis();
    }
}
