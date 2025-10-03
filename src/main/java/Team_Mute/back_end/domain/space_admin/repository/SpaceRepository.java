package Team_Mute.back_end.domain.space_admin.repository;

import Team_Mute.back_end.domain.space_admin.dto.response.SpaceDatailResponseDto;
import Team_Mute.back_end.domain.space_admin.dto.response.SpaceListResponseDto;
import Team_Mute.back_end.domain.space_admin.entity.Space;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Space Repository
 * - 테이블: tb_spaces
 * - 역할: 공간 등록/수정 중복 체크, 목록/상세 조회(Projection 기반) 제공
 * - 목록/상세는 Native Query + 인터페이스 Projection으로 매핑
 * - 상세 조회는 PostgreSQL json_build_object/json_agg 등을 사용해
 * 연관 데이터(지역/카테고리/위치/태그/운영시간/휴무일)를 한 번에 조립
 */
@Repository
public interface SpaceRepository extends JpaRepository<Space, Integer> {
	/**
	 * 공간명 중복 체크 (공간 등록 시 사용)
	 *
	 * @param spaceName 공간 이름
	 * @return true = 중복 존재
	 */
	boolean existsBySpaceName(String spaceName);

	/**
	 * 공간명 중복 여부 (공간 수정 시 사용)
	 * - 동일 ID는 제외하고, 같은 이름이 존재하는지 검사
	 *
	 * @param spaceName 공간 이름
	 * @param spaceId   제외할 PK (자기 자신)
	 * @return true = 중복 존재
	 */
	boolean existsBySpaceNameAndSpaceIdNot(String spaceName, Integer spaceId);

	/**
	 * 공간 리스트 페이징 조회 (관리자 전용 전체 목록)
	 * - Projection: SpaceListResponseDto
	 * - 정렬: region_id 오름차순, reg_date 내림차순
	 *
	 * @param pageable 페이징 정보
	 * @return Page<SpaceListResponseDto>
	 */
	@Query(value = """
		   SELECT
		     s.space_id           AS spaceId,
		     s.space_name         AS spaceName,
		     r.region_name        AS regionName,
		     s.region_id          AS regionId,
		     /* 담당자명 */
			  COALESCE(
			  (
				  SELECT admin_name
				  FROM tb_admins
				  WHERE admin_id = s.user_id
			  ), '알 수 없음'
		        ) AS adminName,
		     s.space_image_url    AS spaceImageUrl,
		     s.space_is_available AS spaceIsAvailable

		   FROM tb_spaces s
		   JOIN tb_admin_region     r ON r.region_id   = s.region_id
		   ORDER BY s.region_id ASC, s.reg_date DESC
		""",
		countQuery = "SELECT COUNT(*) FROM tb_spaces",
		nativeQuery = true)
	Page<SpaceListResponseDto> findAllWithNames(Pageable pageable);

	/**
	 * 공간 리스트 페이징 조회 (1차 승인자의 담당 지역만)
	 * - Projection: SpaceListResponseDto
	 * - 필터: region_id = :adminRegionId
	 * - 정렬: region_id 오름차순, reg_date 내림차순
	 *
	 * @param pageable      페이징 정보
	 * @param adminRegionId 담당 지역 ID
	 * @return Page<SpaceListResponseDto>
	 */
	@Query(value = """
		   SELECT
		     s.space_id           AS spaceId,
		     s.space_name         AS spaceName,
		     r.region_name        AS regionName,
		     s.region_id          AS regionId,
		     COALESCE(
		     (
		        SELECT admin_name
		        FROM tb_admins
		        WHERE admin_id = s.user_id
		     ), '알 수 없음'
		        ) AS adminName,
		     s.space_image_url    AS spaceImageUrl,
		     s.space_is_available AS spaceIsAvailable
		   FROM tb_spaces s
		   JOIN tb_admin_region r ON r.region_id = s.region_id
		   WHERE s.region_id = :adminRegionId
		   ORDER BY s.region_id ASC, s.reg_date DESC
		""",
		countQuery = "SELECT COUNT(*) FROM tb_spaces WHERE region_id = :adminRegionId",
		nativeQuery = true)
	Page<SpaceListResponseDto> findAllByAdminRegion(Pageable pageable, @Param("adminRegionId") Integer adminRegionId);


	/**
	 * 지역별 공간 리스트 조회 (비페이징)
	 * - Projection: SpaceListResponseDto
	 * - 필터: region_id = :regionId
	 * - 정렬: reg_date 내림차순
	 * <p>
	 * NOTE
	 * - List 반환이므로 countQuery는 사용되지 않음
	 *
	 * @param regionId 지역 ID
	 * @return List 형태의 공간 목록
	 */
	@Query(value = """
		   SELECT
		     s.space_id           AS spaceId,
		     s.space_name         AS spaceName,
		     r.region_id          AS regionId,
		     r.region_name        AS regionName,
		     /* 담당자명 */
			  COALESCE(
			  (
				  SELECT admin_name
				  FROM tb_admins
				  WHERE admin_id = s.user_id
			  ), '알 수 없음'
		        ) AS adminName,
		     s.space_image_url    AS spaceImageUrl,
		     s.space_is_available AS spaceIsAvailable

		   FROM tb_spaces s
		   JOIN tb_admin_region     r ON r.region_id   = s.region_id
		   WHERE s.region_id = :regionId
		   ORDER BY s.reg_date DESC
		""",
		countQuery = "SELECT COUNT(*) FROM tb_spaces",
		nativeQuery = true)
	List<SpaceListResponseDto> findAllWithRegion(@Param("regionId") Integer regionId);

	/**
	 * 공간 단건 상세 조회 (+ 연관 데이터 조인/조립)
	 * - Projection: SpaceDatailResponseDto
	 * - PostgreSQL json_build_object/json_agg 사용
	 * - operations/closedDays/region/category/location 필드는 문자열로
	 * 반환 후 @JsonRawValue로 그대로 JSON에 포함됨
	 *
	 * @param spaceId 공간 ID
	 * @return 상세 Projection
	 */
	@Query(value = """
		SELECT
		  s.space_id           AS spaceId,
		  s.space_name         AS spaceName,
		  /* 지역 */
		  COALESCE(
		      json_build_object(
			      'regionId',   r.region_id,
			      'regionName', r.region_name
			  ), '{}'::json
		  ) AS region,

		  /* 카테고리 */
		  COALESCE(
		         json_build_object(
			      'categoryId',   c.category_id,
			      'categoryName', c.category_name
			  ), '{}'::json
		  ) AS category,

		  /* 주소 */
		  COALESCE(
		      json_build_object(
			      'locationId',  l.location_id,
			      'addressRoad', l.address_road
			  ), '{}'::json
		  ) AS location,
		  /* 담당자 정보 (admin 객체) */
		        COALESCE(
		            json_build_object(
		               'adminId', a.admin_id,
		               'adminNameWithRole', (
		                   a.admin_name || '(' ||\s
		                   CASE ur.role_id
		                       WHEN 1 THEN '2차 승인자'
		                       WHEN 2 THEN '1차 승인자'
		                       ELSE '미지정'
		                   END || ')'
		               )
		           ),\s
		           -- 담당자 정보가 NULL일 경우 기본값 설정 (기존 '알 수 없음' 반영)
		           json_build_object(
		               'adminId', NULL::BIGINT,
		               'adminNameWithRole', '알 수 없음'
		           )
		        ) AS admin,
		  s.space_capacity     AS spaceCapacity,
		  s.space_description  AS spaceDescription,
		  s.space_image_url    AS spaceImageUrl,
		  s.space_is_available AS spaceIsAvailable,
		  s.reservation_way    AS reservationWay,
		  s.space_rules        AS spaceRules,
		  /* 상세 이미지 */
			 COALESCE((
			   SELECT array_agg(si.image_url ORDER BY si.image_priority ASC, si.image_id ASC)
			   FROM tb_space_images si
			   WHERE si.space_id = s.space_id
				 AND (s.space_image_url IS NULL OR si.image_url <> s.space_image_url)
			 ), ARRAY[]::text[]) AS detailImageUrls,

		 /* 태그 배열 */
			 COALESCE((
			   SELECT array_agg(DISTINCT tt.tag_name ORDER BY tt.tag_name)
			   FROM tb_space_tag_map mm
			   JOIN tb_space_tags tt ON tt.tag_id = mm.tag_id
			   WHERE mm.space_id = s.space_id
			 ), ARRAY[]::text[]) AS tagNames,

		  s.reg_date           AS regDate,
		  s.upd_date           AS updDate,

		 /* 운영시간 JSON */
			COALESCE((
			  SELECT json_agg(
					   json_build_object(
						 'day',    o.day,
						 'from',   to_char(o.operation_from,'HH24:MI'),
						 'to',     to_char(o.operation_to,  'HH24:MI'),
						 'isOpen', o.is_open
					   )
					   ORDER BY o.day
					 )
			  FROM tb_space_operation o
			  WHERE o.space_id = s.space_id
			), '[]'::json)::text AS operations,

		 /* 휴무일 JSON */
			COALESCE((
			  SELECT json_agg(
					   json_build_object(
						 'from', to_char(cd.closed_from,'YYYY-MM-DD"T"HH24:MI:SS'),
						 'to',   to_char(cd.closed_to,  'YYYY-MM-DD"T"HH24:MI:SS')
					   )
					   ORDER BY cd.closed_from
					 )
			  FROM tb_space_closedday cd
			  WHERE cd.space_id = s.space_id
			), '[]'::json)::text AS closedDays

		FROM tb_spaces s
		JOIN tb_admin_region     r ON r.region_id   = s.region_id
		JOIN tb_space_categories c ON c.category_id = s.category_id
		JOIN tb_locations        l ON l.location_id = s.location_id
		LEFT JOIN tb_admins       a ON a.admin_id    = s.user_id
		LEFT JOIN tb_user_roles   ur ON ur.role_id    = a.role_id
		WHERE s.space_id = :spaceId
		""", nativeQuery = true)
	Optional<SpaceDatailResponseDto> findDetailWithNames(@Param("spaceId") Integer spaceId);

	/**
	 * ID로 단건 조회 (비관적 잠금)
	 * - 업데이트 직전 안전하게 단일 행 잠금을 걸고 싶을 때 사용
	 * - 트랜잭션 범위 내에서만 유효
	 *
	 * @param id PK
	 * @return Optional<Space>
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Override
	Optional<Space> findById(Integer id);

}
