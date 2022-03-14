package com.hiyoon.querydsl;

import com.hiyoon.querydsl.dto.MemberDto;
import com.hiyoon.querydsl.dto.MemberQueryProjectionDto;
import com.hiyoon.querydsl.dto.QMemberQueryProjectionDto;
import com.hiyoon.querydsl.dto.UserDto;
import com.hiyoon.querydsl.entity.Member;
import com.hiyoon.querydsl.entity.QMember;
import com.hiyoon.querydsl.entity.Team;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import java.util.List;

import static com.hiyoon.querydsl.entity.QMember.member;
import static com.hiyoon.querydsl.entity.QTeam.team;
import static com.querydsl.jpa.JPAExpressions.select;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {

    @Autowired
    EntityManager em;
    JPAQueryFactory queryFactory;

    @BeforeEach
    public void before() {
        queryFactory = new JPAQueryFactory(em);

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
    }

    @Test
    public void startJPQL() {
        // member1을 찾아라
        Member findMember = em.createQuery("select m from Member m where m.username = :username", Member.class)
                .setParameter("username", "member1")
                .getSingleResult();
        Assertions.assertEquals(findMember.getUsername(), "member1");
    }

    @Test
    public void startQuerydsl() {
        Member findMember = queryFactory.select(member)
                .from(member)
                .where(member.username.eq("member1"))
                .fetchOne();
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void search() {
        Member findMember = queryFactory.selectFrom(QMember.member)
                .where(member.username.eq("member1").and(member.age.eq(10)))
                .fetchOne();
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void searchAndParam() {
        Member findMember = queryFactory.selectFrom(member)
                .where(member.username.eq("member1"),
                        member.age.eq(10))
                .fetchOne();
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void featch() {
        queryFactory.selectFrom(member)
                .where(member.username.eq("member1"),
                        member.age.eq(10))
                .fetchResults();
    }

    @Test
    public void paging() {
        QueryResults<Member> queryResults = queryFactory.selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1) // 0부터시작
                .limit(2)
                .fetchResults();

        assertThat(queryResults.getTotal()).isEqualTo(4);
        assertThat(queryResults.getLimit()).isEqualTo(2);
    }

    @Test
    public void aggregation() {
        List<Tuple> results = queryFactory.select(
                        member.count(),
                        member.age.avg(),
                        member.age.sum(),
                        member.age.max(),
                        member.age.min())
                .from(member)
                .fetch();

        Tuple tuple = results.get(0);
        assertThat(tuple.get(member.count())).isEqualTo(4);
        assertThat(tuple.get(member.age.max())).isEqualTo(40);
    }

    @Test
    public void group() {
        List<Tuple> groupResults = queryFactory.select(team.name, member.age.avg())
                .from(member)
                .join(member.team, team)
                .groupBy(team.name)
                .fetch();

        Tuple teamA = groupResults.get(0);
        Tuple teamB = groupResults.get(1);
        assertThat(teamA.get(team.name)).isEqualTo("teamA");
        assertThat(teamA.get(member.age.avg())).isEqualTo(15);
        assertThat(teamB.get(team.name)).isEqualTo("teamB");
        assertThat(teamB.get(member.age.avg())).isEqualTo(35);

    }

    /**
     * 팀 A에 소속된 모든 회원
     */
    @Test
    public void join() {
        List<Member> result = queryFactory.selectFrom(member)
                .join(member.team, team)
                .where(team.name.eq("teamA"))
                .fetch();

        assertThat(result).extracting("username")
                .containsExactly("member1", "member2");
    }

    /**
     * 세타조인
     * 회읜의 이름이 팀 이름과 같은 회원 조회 -> 연관관계가 없는 컬럼을 조인
     * 모든 데이터를 불러와서 모든 값을 조인한다.(cross join)
     */
    @Test
    public void theta_join() {
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));

        List<Member> result = queryFactory
                .select(member)
                .from(member, team)
                .where(member.username.eq(team.name))
                .fetch();

        assertThat(result).extracting("username")
                .containsExactly("teamA", "teamB");
    }

    /**
     * 회원과 팀을 조인하면서, 팀 이름이 teamA인 팀만 조인, 회원은 모두 조회
     * JPQL: select m, t from Member m left join m.team t on t.name = 'teamA'
     */
    @Test
    public void join_on_filtering() {
        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(member.team, team).on(team.name.eq("teamA"))
                .fetch();

        for (Tuple tuple : result) {
            System.out.println(tuple);
        }
    }

    /**
     * 연관관계 없는 엔티티 외부 조인
     * 회읜의 이름이 팀 이름과 같은 대상 외부 조인
     */
    @Test
    public void join_on_no_relation() {
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));

        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(team).on(member.username.eq(team.name))
                .fetch();

        for (Tuple tuple : result) {
            System.out.println(tuple);
        }
    }

    @Autowired
    EntityManagerFactory emf;

    @Test
    public void fetchJoinNo() {
        // 패치조인 테스트할 때는 영속성 컨텍스트 다 정리하고 테스트한다.
        em.flush();
        em.clear();

        Member findMember = queryFactory.selectFrom(QMember.member)
                .where(QMember.member.username.eq("member1"))
                .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        assertThat(loaded).as("패치 조인 미적용").isFalse();

    }

    /**
     * 한방에 조회한다.
     */
    @Test
    public void fetchJoinUse() {
        // 패치조인 테스트할 때는 영속성 컨텍스트 다 정리하고 테스트한다.
        em.flush();
        em.clear();

        Member findMember = queryFactory
                .selectFrom(member)
                .join(member.team, team).fetchJoin()
                .where(member.username.eq("member1"))
                .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        assertThat(loaded).as("패치 조인 적용").isTrue();
    }

    /**
     * 나이가 가장 많은 회원 조회
     */
    @Test
    public void subQuery() {
        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(
                        select(member.age.max())
                                .from(member)
                ))
                .fetch();

        assertThat(result).extracting("age").containsExactly(40);
    }

    /**
     * 나이가 평균 이상인 회원
     */
    @Test
    public void subQueryGoe() {
        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.goe(
                        select(member.age.avg())
                                .from(member)
                ))
                .fetch();

        assertThat(result).extracting("age").containsExactly(30, 40);
    }

    /**
     * 나이가 10보다 큰 회원
     */
    @Test
    public void subQueryIn() {
        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.in(
                        select(member.age)
                                .from(member)
                                .where(member.age.gt(10))

                ))
                .fetch();

        assertThat(result).extracting("age").containsExactly(20, 30, 40);
    }

    @Test
    public void selectSubQuery() {
        List<Tuple> result = queryFactory
                .select(member.username,
                        select(member.age.avg())
                                .from(member))
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple -> " + tuple);
        }
    }

    @Test
    public void basicCase() {
        List<String> result = queryFactory
                .select(member.age
                        .when(10).then("열살")
                        .when(20).then("스무살")
                        .otherwise("기타"))
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    public void complexCase() {
        List<String> result = queryFactory
                .select(new CaseBuilder()
                        .when(member.age.between(0, 20)).then("0~20살")
                        .when(member.age.between(21, 30)).then("21~30살")
                        .otherwise("기타"))
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    /**
     * Tuple은 Repository내에서만 사용하는게 좋긴 함.. querydslcore 라이브러리이기 때문에
     */
    @Test
    public void constant() {
        List<Tuple> result = queryFactory
                .select(member.username, Expressions.constant("A"))
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            System.out.println(tuple);
        }
    }

    @Test
    public void concat() {
        List<String> result = queryFactory
                .select(member.username.concat("_").concat(member.age.stringValue())) // stringValue 자주 사용함
                .from(member)
                .where(member.username.eq("member1"))
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    public void simpleProjection() {
        List<String> result = queryFactory
                .select(member.username)
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    public void tupleProjection() {
        List<Tuple> result = queryFactory
                .select(member.username, member.age)
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            String username = tuple.get(member.username);
            Integer age = tuple.get(member.age);

            System.out.println("username = " + username);
            System.out.println("age = " + age);
        }
    }

    @Test
    public void findDtoByJPQL() {
        List<MemberDto> result = em.createQuery("select new com.hiyoon.querydsl.dto.MemberDto(m.username, m.age) from Member m",
                        MemberDto.class)
                .getResultList();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    /**
     * setter 사용해서 동작
     */
    @Test
    public void findDtoBySetter() {
        List<MemberDto> result = queryFactory
                .select(Projections.bean(MemberDto.class, member.username, member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    /**
     * 필드에 직접 주입
     */
    @Test
    public void findDtoByFields() {
        List<MemberDto> result = queryFactory
                .select(Projections.fields(MemberDto.class, member.username, member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    public void findDtoByConstructor() {
        List<MemberDto> result = queryFactory
                .select(Projections.constructor(MemberDto.class, member.username, member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    public void findUserDtoByFields() {
        List<UserDto> result = queryFactory
                .select(Projections.fields(UserDto.class,
                        member.username.as("name"),
                        member.age)) // 이름이 안맞는 경우
                .from(member)
                .fetch();

        for (UserDto userDto : result) {
            System.out.println("userDto = " + userDto);
        }
    }

    @Test
    public void findUserDtoSubquery() {
        List<UserDto> result = queryFactory
                .select(Projections.fields(UserDto.class,
                        member.username.as("name"),
                        ExpressionUtils.as(JPAExpressions
                                .select(member.age.max())
                                .from(member), "age"))) // 이름이 안맞는 경우
                .from(member)
                .fetch();

        for (UserDto userDto : result) {
            System.out.println("userDto = " + userDto);
        }
    }

    @Test
    public void findUserDtoByConstructor() {
        List<UserDto> result = queryFactory
                .select(Projections.constructor(UserDto.class, member.username, member.age))
                .from(member)
                .fetch();

        for (UserDto userDto : result) {
            System.out.println("memberDto = " + userDto);
        }
    }

    /**
     * DTO에 @QueryProjection을 사용하여 처리함
     * 편리하지만 DTO가 라이브러리의 영향하에 놓인다. -> DTO라서 여러 Layer에 걸쳐서 사용하기 때문에 의존적인 설계이다.
     */
    @Test
    public void findDtoByQueryProjection() {
        List<MemberQueryProjectionDto> result = queryFactory
                .select(new QMemberQueryProjectionDto(member.username, member.age))
                .from(member)
                .fetch();

        for (MemberQueryProjectionDto memberQueryProjectionDto : result) {
            System.out.println("memberQueryProjectionDto = " + memberQueryProjectionDto);
        }
    }

    @Test
    public void distinct() {
        List<String> result = queryFactory
                .select(member.username).distinct()
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    /**
     * 다이나믹쿼리
     * BooleanBuilder 사용
     */
    @Test
    public void dynamicQuery_BooleanBuilder() {
        String usernameParam = "member1";
        Integer ageParam = null;

        List<Member> result = searchMember1(usernameParam, ageParam);
        for (Member member1 : result) {
            System.out.println("member1 = " + member1);
        }
    }

    private List<Member> searchMember1(String usernameCond, Integer ageCond) {
        BooleanBuilder builder = new BooleanBuilder();
        if (usernameCond != null) {
            builder.and(member.username.eq(usernameCond));
        }
        if (ageCond != null) {
            builder.and(member.age.eq(ageCond));
        }

        return queryFactory
                .selectFrom(member)
                .where(builder)
                .fetch();
    }

    /**
     * 다이나믹쿼리
     * where 다중 파라미터 사용
     */
    @Test
    public void dynamicQuery_WhereParam() {
        String usernameParam = "member1";
        Integer ageParam = null;

        List<Member> result = searchMember2(usernameParam, ageParam);
        for (Member member1 : result) {
            System.out.println("member1 = " + member1);
        }
    }

    private List<Member> searchMember2(String usernameCond, Integer ageCond) {
        return queryFactory
                .selectFrom(member)
                .where(usernameEq(usernameCond), ageEq(ageCond))
                .fetch();
    }

    private BooleanExpression usernameEq(String usernameCond) {
        return usernameCond == null ? null : member.username.eq(usernameCond);
    }

    private BooleanExpression ageEq(Integer ageCond) {
        return ageCond == null ? null : member.age.eq(ageCond);
    }

    private BooleanExpression allEq(String usernameCond, Integer ageCond) {
        return usernameEq(usernameCond).and(ageEq(ageCond));
    }

    /**
     * 쿼리 한번으로 대량 데이터 수정
     */
    @Test
    public void bulkUpdate() {
        // 실행전
        //member1 = 10 -> DB: member1
        //member2 = 20 -> DB: member2
        //member3 = 30 -> DB: member3
        //member4 = 40 -> DB: member4

        long count = queryFactory
                .update(member)
                .set(member.username, "비회원")
                .where(member.age.lt(28))
                .execute();

        // 실행후
        //member1 = 10 -> DB: 비회원
        //member2 = 20 -> DB: 비회원
        //member3 = 30 -> DB: member3
        //member4 = 40 -> DB: member4

        // 주의 사항 bulk연산은 persistence context를 무시하고 db에 직접 쿼리를 날린다.
        // 따라서 다시 재조회 한 경우 persistence context가 우선순위이기 때문에 값이 다른 경우 db값을 버린다.

        em.flush(); // persistence context와 db 맞추기 위해 사용
        em.clear(); // persistence context와 db 맞추기 위해 사용

        List<Member> result = queryFactory
                .selectFrom(member)
                .fetch();

        for (Member member1 : result) {
            System.out.println("member1 = " + member1);
        }
    }

    @Test
    public void bulkAdd() {
        long count = queryFactory
                .update(member)
                .set(member.age, member.age.add(1))
//                .set(member.age, member.age.multiply(2))
                .execute();
    }

    @Test
    public void buikDelete() {
        queryFactory
                .delete(member)
                .where(member.age.gt(18))
                .execute();
    }

    @Test
    public void sqlFunction() {
        List<String> result = queryFactory
                .select(Expressions.stringTemplate("function('replace', {0}, {1}, {2})",
                        member.username, "member", "M"))
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    public void sqlFunction2() {
        List<String> result = queryFactory
                .select(member.username)
                .from(member)
//                .where(member.username.eq(Expressions.stringTemplate("function('lower', {0})", member.username)))
                .where(member.username.eq(member.username.lower()))
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }


    }
}
