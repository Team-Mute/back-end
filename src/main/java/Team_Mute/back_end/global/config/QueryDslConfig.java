package Team_Mute.back_end.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.querydsl.jpa.impl.JPAQueryFactory;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * QueryDSL 설정 클래스
 * JPAQueryFactory를 Spring Bean으로 등록하여 프로젝트 전역에서 QueryDSL 사용 가능하도록 설정
 *
 * 사용처:
 * - ReservationRepositoryImpl에서 동적 쿼리 작성
 * - 복잡한 조회 로직 구현
 */
@Configuration
public class QueryDslConfig {

	/**
	 * JPA EntityManager 주입
	 * @PersistenceContext: 영속성 컨텍스트에서 EntityManager 자동 주입
	 */
	@PersistenceContext
	private EntityManager entityManager;

	/**
	 * JPAQueryFactory Bean 등록
	 * QueryDSL의 핵심 클래스로 타입 안전한 쿼리 빌더 제공
	 *
	 * @return JPAQueryFactory 인스턴스
	 */
	@Bean
	public JPAQueryFactory jpaQueryFactory() {
		return new JPAQueryFactory(entityManager);
	}
}
