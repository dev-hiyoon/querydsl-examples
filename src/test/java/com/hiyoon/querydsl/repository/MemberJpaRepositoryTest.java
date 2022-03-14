package com.hiyoon.querydsl.repository;

import com.hiyoon.querydsl.dto.MemberSearchCondition;
import com.hiyoon.querydsl.dto.MemberTeamDto;
import com.hiyoon.querydsl.entity.Member;
import com.hiyoon.querydsl.entity.Team;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest
@Transactional
class MemberJpaRepositoryTest {

    @Autowired
    EntityManager em;

    @Autowired
    MemberJpaRepository memberJpaRepository;

    @Test
    public void basicTest() {
        Member member = new Member("member1", 10);
        memberJpaRepository.save(member);

        Member findMember = memberJpaRepository.findById(member.getId()).get();
        assertThat(findMember).isEqualTo(member);

        List<Member> result1 = memberJpaRepository.findAll();
        assertThat(result1).contains(member);

        List<Member> result2 = memberJpaRepository.findByUsername(member.getUsername());
        assertThat(result2).contains(member);
    }

    @Test
    public void querydslTest() {
        Member member = new Member("member1", 10);
        memberJpaRepository.save(member);

        Member findMember = memberJpaRepository.findById(member.getId()).get();
        assertThat(findMember).isEqualTo(member);

        List<Member> result1 = memberJpaRepository.findAll_Querydsl();
        assertThat(result1).contains(member);

        List<Member> result2 = memberJpaRepository.findByUsername_Querydsl(member.getUsername());
        assertThat(result2).contains(member);
    }

    @Test
    public void searchTest() {
        Team temaA = new Team("teamA");
        Team temaB = new Team("teamB");
        em.persist(temaA);
        em.persist(temaB);

        Member member1 = new Member("member1", 10, temaA);
        Member member2 = new Member("member2", 20, temaA);
        Member member3 = new Member("member3", 30, temaB);
        Member member4 = new Member("member4", 40, temaB);

        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);

        MemberSearchCondition condition = new MemberSearchCondition();
        condition.setAgeGoe(35);
        condition.setAgeLoe(40);

        List<MemberTeamDto> result = memberJpaRepository.searchByBuilder(condition);

        assertThat(result).extracting("username").containsExactly(member4.getUsername());
    }

}