package com.hiyoon.querydsl.repository;

import com.hiyoon.querydsl.dto.MemberSearchCondition;
import com.hiyoon.querydsl.dto.MemberTeamDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface MemberRepositoryCustom {

    List<MemberTeamDto> search(MemberSearchCondition condition);
    Page<MemberTeamDto> searchPageSimple(MemberSearchCondition condition, Pageable pageable);
    Page<MemberTeamDto> searchPageConplex(MemberSearchCondition condition, Pageable pageable);
}
