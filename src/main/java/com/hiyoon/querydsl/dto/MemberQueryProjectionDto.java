package com.hiyoon.querydsl.dto;

import com.querydsl.core.annotations.QueryProjection;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class MemberQueryProjectionDto {
    private String username;
    private int age;

    @QueryProjection
    public MemberQueryProjectionDto(String username, int age) {
        this.username = username;
        this.age = age;
    }
}
