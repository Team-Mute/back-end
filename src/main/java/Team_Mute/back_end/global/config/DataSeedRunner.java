package Team_Mute.back_end.global.config;

import Team_Mute.back_end.domain.member.entity.AdminRegion;
import Team_Mute.back_end.domain.space_admin.entity.SpaceCategory;
import Team_Mute.back_end.domain.space_admin.entity.SpaceLocation;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;


/**
 * 서버 기동 시 JPA 기반으로 초기 데이터 시드 (SQL 없이 JPQL/엔티티로 처리)
 * - 멱등성: 존재 여부 확인 후 생성/갱신
 * - 지역명(region_name)으로 region_id 자동 매핑
 */
@Configuration
@RequiredArgsConstructor
public class DataSeedRunner implements CommandLineRunner {
	private final EntityManager em;

	@Override
	@Transactional
	public void run(String... args) {

		// 1) 카테고리
		ensureCategory("미팅룸");
		ensureCategory("행사장");
		ensureCategory("다목적");

		// 2) 지역
		ensureRegion("서울");
		ensureRegion("인천");
		ensureRegion("대구");
		ensureRegion("대전");

		// 3) 위치: region_name → region_id 매핑 + 업서트
		List<LocSeed> seeds = List.of(
			new LocSeed("서울", "신한 스퀘어브릿지 서울", "서울특별시 명동10길 52 (충무로2가 65-4)", "04536", true),
			new LocSeed("인천", "신한 스퀘어브릿지 인천", "인천 연수구 컨벤시아대로 204 (송도동 93)", "22004", true),
			new LocSeed("대구", "신한 스퀘어브릿지 대구", "대구 동구 동대구로 465 대구스케일업허브 (신천동 106)", "41260", true),
			new LocSeed("대전", "신한 스퀘어브릿지 대전 S1", "대전 유성구 대학로155번길 4 (궁동 426-12)", "34138", true)
		);
		seeds.forEach(this::upsertLocationByRegionName);
	}

	// ---------- helpers ----------

	private void ensureCategory(String name) {
		boolean exists = !em.createQuery(
				"select c.categoryId from SpaceCategory c where c.categoryName = :n", Integer.class)
			.setParameter("n", name)
			.setMaxResults(1)
			.getResultList().isEmpty();

		if (!exists) {
			var c = new SpaceCategory();
			c.setCategoryName(name);
			em.persist(c);
		}
	}

	private void ensureRegion(String name) {
		boolean exists = !em.createQuery(
				"select r.regionId from AdminRegion r where r.regionName = :n", Integer.class)
			.setParameter("n", name)
			.setMaxResults(1)
			.getResultList().isEmpty();

		if (!exists) {
			var r = new AdminRegion();
			r.setRegionName(name);
			em.persist(r);
		}
	}

	private AdminRegion getRegionByName(String regionName) {
		var regions = em.createQuery(
				"select r from AdminRegion r where r.regionName = :n", AdminRegion.class)
			.setParameter("n", regionName)
			.setMaxResults(1)
			.getResultList();
		return regions.isEmpty() ? null : regions.get(0);
	}

	private void upsertLocationByRegionName(LocSeed s) {
		AdminRegion region = getRegionByName(s.regionName);
		if (region == null) {
			ensureRegion(s.regionName);
			region = getRegionByName(s.regionName);
			if (region == null) throw new IllegalStateException("지역 매핑 실패: " + s.regionName);
		}

		var found = em.createQuery(
				"select l from SpaceLocation l where l.locationName = :n", SpaceLocation.class)
			.setParameter("n", s.locationName)
			.setMaxResults(1)
			.getResultList();

		if (found.isEmpty()) {
			var e = new SpaceLocation();
			e.setAdminRegion(region); // 엔티티 객체로 직접 매핑
			e.setLocationName(s.locationName);
			e.setAddressRoad(s.addressRoad);
			e.setPostalCode(s.postalCode);
			e.setIsActive(s.active);
			em.persist(e);
		} else {
			var e = found.get(0);
			e.setAdminRegion(region);
			e.setAddressRoad(s.addressRoad);
			e.setPostalCode(s.postalCode);
			e.setIsActive(s.active);
			em.merge(e);
		}
	}

	private Integer getRegionIdByName(String regionName) {
		var ids = em.createQuery(
				"select r.regionId from AdminRegion r where r.regionName = :n", Integer.class)
			.setParameter("n", regionName)
			.setMaxResults(1)
			.getResultList();
		return ids.isEmpty() ? null : ids.get(0);
	}

	private static class LocSeed {
		final String regionName;
		final String locationName;
		final String addressRoad;
		final String postalCode;
		final Boolean active;

		LocSeed(String regionName, String locationName, String addressRoad, String postalCode, Boolean active) {
			this.regionName = regionName;
			this.locationName = locationName;
			this.addressRoad = addressRoad;
			this.postalCode = postalCode;
			this.active = active;
		}
	}
}
