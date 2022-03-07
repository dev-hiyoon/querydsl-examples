package com.hiyoon.querydsl;

import com.hiyoon.querydsl.entity.Member;
import com.hiyoon.querydsl.entity.Team;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class MemberTest {

    @Autowired
    EntityManager em;

    @Test
    public void testEntity() {
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



        //초기화
        em.flush();
        em.clear();

        // 확인
        List<Member> resultList = em.createQuery("select m from Member m", Member.class)
                .getResultList();

        // 로그
        for (Member member : resultList) {
            System.out.println("########## member = " + member);
            System.out.println("########## -> member.team" + member.getTeam());
        }

    }

}