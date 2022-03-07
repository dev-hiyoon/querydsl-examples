package com.hiyoon.querydsl;

import com.hiyoon.querydsl.entity.Hello;
import com.hiyoon.querydsl.entity.QHello;
import com.mysema.commons.lang.Assert;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;

@SpringBootTest
@Transactional
class QuerydslApplicationTests {

    @Autowired
    private EntityManager em;

    @Test
    void contextLoads() {
        Hello hello = new Hello();
        em.persist(hello);

        JPAQueryFactory query = new JPAQueryFactory(em);
        QHello qHello = new QHello("h");
        Hello result = query
                .selectFrom(qHello)
                .fetchOne();

        Assertions.assertEquals(hello, result);
        Assertions.assertEquals(hello.getId(), result.getId());
    }

}
