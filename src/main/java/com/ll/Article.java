package com.ll;

import java.time.LocalDateTime;

public class Article {
    private Long id;
    private LocalDateTime createdDate;
    private LocalDateTime modifiedDate;
    private String title;
    private String body;
    private Boolean isBlind;

    public Article() {
    }

    public Article( Long id,
            LocalDateTime createdDate, LocalDateTime modifiedDate,
                   String title, String body,Boolean isBlind) {
        this.id = id;
        this.createdDate = createdDate;
        this.modifiedDate = modifiedDate;
        this.title = title;
        this.body = body;
        this.isBlind =isBlind;
    }

}
