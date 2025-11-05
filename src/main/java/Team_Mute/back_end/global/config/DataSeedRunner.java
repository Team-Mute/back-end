package Team_Mute.back_end.global.config;

import Team_Mute.back_end.domain.member.entity.AdminRegion;
import Team_Mute.back_end.domain.member.entity.UserRole;
import Team_Mute.back_end.domain.member.service.AdminService;
import Team_Mute.back_end.domain.reservation.entity.ReservationStatus;
import Team_Mute.back_end.domain.space_admin.entity.SpaceCategory;
import Team_Mute.back_end.domain.space_admin.entity.SpaceLocation;
import Team_Mute.back_end.global.constants.ReservationStatusEnum;
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
	private final AdminService userService;

	/**
	 * 애플리케이션 시작 시 실행되는 메인 시드(Seed) 함수
	 * 카테고리, 지역, 위치 데이터를 데이터베이스에 생성하거나 갱신
	 *
	 * @param args 커맨드 라인 인자 (사용되지 않음)
	 */
	@Override
	@Transactional
	public void run(String... args) {

		// 1) 카테고리: 초기 공간 카테고리 데이터를 보장 (없으면 생성)
		ensureCategory("미팅룸");
		ensureCategory("행사장");

		// 2) 지역: 초기 관리자 지역 데이터를 보장 (없으면 생성)
		ensureRegion("서울");
		ensureRegion("인천");
		ensureRegion("대구");
		ensureRegion("대전");

		// 3) 위치: 초기 공간 위치 데이터 (주소) 목록 정의
		List<LocSeed> seeds = List.of(
			new LocSeed("서울", "신한 스퀘어브릿지 서울", "서울특별시 명동10길 52", "충무로2가 65-4", "04536", "명동역 도보 2분", true),
			new LocSeed("인천", "신한 스퀘어브릿지 인천", "인천광역시 연수구 컨벤시아대로 204", "송도동 93", "22004", "인천대입구역 도보 5분", true),
			new LocSeed("대구", "신한 스퀘어브릿지 대구", "대구광역시 동구 동대구로 465 대구스케일업허브", "신천동 106", "41260", "동대구역 도보 10분", true),
			new LocSeed("대전", "신한 스퀘어브릿지 대전 S1", "대전광역시 유성구 대학로155번길 4", "궁동 426-12", "34138", "월평역 도보 20분", true)
		);
		seeds.forEach(this::upsertLocationByRegionName);

		// 4) UserRole
		upsertUserRole(0, "Master"); // 마스터 관리자 (관리자 계정 생성용 계정)
		upsertUserRole(1, "Approver"); // 2차 승인자
		upsertUserRole(2, "Manager"); // 1차 승인자
		upsertUserRole(3, "Customer"); // 사용자

		// 5) Master 관리자 계정 생성
		userService.createInitialAdmin();

		// 6) ReservationStatus
		upsertReservationStatuses();
	}

	/**
	 * 주어진 카테고리 이름의 SpaceCategory 엔티티가 DB에 존재하는지 확인하고,
	 * 존재하지 않으면 새로 생성하여 영속화. (멱등성 보장)
	 *
	 * @param name 확인할 카테고리 이름
	 */
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

	/**
	 * 주어진 지역 이름의 AdminRegion 엔티티가 DB에 존재하는지 확인하고,
	 * 존재하지 않으면 새로 생성하여 영속화 (멱등성 보장)
	 *
	 * @param name 확인할 지역 이름
	 */
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

	/**
	 * 주어진 지역 이름으로 AdminRegion 엔티티를 조회
	 *
	 * @param regionName 조회할 지역 이름
	 * @return AdminRegion 엔티티 (존재하지 않으면 null 반환)
	 */
	private AdminRegion getRegionByName(String regionName) {
		var regions = em.createQuery(
				"select r from AdminRegion r where r.regionName = :n", AdminRegion.class)
			.setParameter("n", regionName)
			.setMaxResults(1)
			.getResultList();
		return regions.isEmpty() ? null : regions.get(0);
	}

	/**
	 * LocSeed 객체를 사용하여 SpaceLocation 엔티티를 생성하거나 업데이트(Upsert).
	 * LocSeed의 지역 이름(regionName)을 기반으로 AdminRegion을 찾아 매핑
	 *
	 * @param s 위치 정보가 담긴 LocSeed 객체
	 * @throws IllegalStateException 지역 매핑에 실패한 경우
	 */
	private void upsertLocationByRegionName(LocSeed s) {
		// 1. 지역 엔티티 조회/보장
		AdminRegion region = getRegionByName(s.regionName);
		if (region == null) {
			ensureRegion(s.regionName);
			region = getRegionByName(s.regionName);
			if (region == null)
				throw new IllegalStateException("지역 매핑 실패: " + s.regionName);
		}

		// 2. 위치 엔티티 조회 (locationName 기준)
		var found = em.createQuery(
				"select l from SpaceLocation l where l.locationName = :n", SpaceLocation.class)
			.setParameter("n", s.locationName)
			.setMaxResults(1)
			.getResultList();

		// 3. 엔티티 생성 또는 갱신
		if (found.isEmpty()) {
			var e = new SpaceLocation();
			e.setAdminRegion(region); // 엔티티 객체로 직접 매핑
			e.setLocationName(s.locationName);
			e.setAddressRoad(s.addressRoad);
			e.setAddressJibun(s.addressJibun);
			e.setPostalCode(s.postalCode);
			e.setAccessInfo(s.accessInfo);
			e.setIsActive(s.active);
			e.setAccessInfo(s.accessInfo);
			e.setAddressJibun(s.addressJibun);
			em.persist(e);
		} else {
			var e = found.get(0);
			e.setAdminRegion(region);
			e.setAddressRoad(s.addressRoad);
			e.setAddressJibun(s.addressJibun);
			e.setPostalCode(s.postalCode);
			e.setAccessInfo(s.accessInfo);
			e.setIsActive(s.active);
			e.setAccessInfo(s.accessInfo);
			e.setAddressJibun(s.addressJibun);
			em.merge(e);
		}
	}

	/**
	 * 초기 데이터 시드를 위한 내부 클래스.
	 * 하나의 SpaceLocation 엔티티와 그에 연결된 AdminRegion의 정보를 담는 DTO 역할.
	 */
	private static class LocSeed {
		final String regionName;
		final String locationName;
		final String addressRoad;
		final String addressJibun;
		final String postalCode;
		final String accessInfo;
		final Boolean active;

		/**
		 * LocSeed 생성자.
		 */
		LocSeed(String regionName, String locationName, String addressRoad, String addressJibun, String postalCode, String accessInfo, Boolean active) {
			this.regionName = regionName;
			this.locationName = locationName;
			this.addressRoad = addressRoad;
			this.addressJibun = addressJibun;
			this.postalCode = postalCode;
			this.accessInfo = accessInfo;
			this.active = active;
		}
	}

	/**
	 * 주어진 역할 ID(roleId)를 기준으로 UserRole 엔티티를 생성하거나 갱신(Upsert)
	 * - roleId를 PK로 조회하여 없으면 삽입(Insert), 있으면 roleName을 갱신(Update)
	 *
	 * @param roleId   확인할 역할 ID (PK)
	 * @param roleName 갱신할 역할 이름
	 */
	private void upsertUserRole(Integer roleId, String roleName) {
		var found = em.createQuery(
				"select r from UserRole r where r.roleId = :roleId", UserRole.class)
			.setParameter("roleId", roleId)
			.setMaxResults(1)
			.getResultList();

		if (found.isEmpty()) {
			// 신규 삽입
			UserRole u = new UserRole();
			u.setRoleId(roleId);
			u.setRoleName(roleName);
			em.persist(u);
		} else {
			// roleName만 update (roleId는 PK)
			UserRole u = found.get(0);
			u.setRoleName(roleName);
			em.merge(u);
		}
	}

	/**
	 * ReservationStatusEnum에 정의된 모든 예약 상태 목록을 DB에 Upsert
	 * - ID 기준(1~6)으로 존재하지 않으면 삽입하고, 존재하면 Description(이름)을 갱신
	 */
	private void upsertReservationStatuses() {
		for (ReservationStatusEnum item : ReservationStatusEnum.values()) {
			var found = em.find(ReservationStatus.class, item.getId());
			if (found == null) {
				ReservationStatus rs = new ReservationStatus();
				rs.setReservationStatusId(item.getId());
				rs.setReservationStatusName(item.getDescription());
				em.persist(rs);
			} else {
				found.setReservationStatusName(item.getDescription());
				// persist 또는 merge 호출 없이 변경 적용만 함
			}
		}
	}
}
