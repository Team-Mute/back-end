package Team_Mute.back_end.domain.space_user.repository;

import Team_Mute.back_end.domain.space_admin.entity.Space;
import Team_Mute.back_end.domain.space_user.dto.SpaceUserDtailResponseDto;
import Team_Mute.back_end.domain.space_user.dto.SpaceUserResponseDto;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 사용자 공간 검색 및 상세 조회를 위한 데이터 접근 레포지토리
 * JpaRepository를 상속받으며, 복잡한 검색 및 상세 조회는 Native SQL을 사용
 */
public interface SpaceUserRepository extends JpaRepository<Space, Integer> {

	/**
	 * 공간 검색
	 * - 사용자 필터링 조건(지역, 인원, 태그)에 맞는 공간 목록을 조회
	 * - 태그는 AND 조건으로 필터링되며, 미팅룸(category_id = 1)과 행사장(category_id = 2)을 포함
	 *
	 * @param regionId 지역 ID (선택 사항, NULL 허용)
	 * @param people   최소 수용 인원 (선택 사항, NULL 허용)
	 * @param tagNames 선택된 태그 이름 배열 (선택 사항, 빈 배열 허용)
	 * @param tagCount {@code tagNames} 배열의 길이 (태그 AND 필터링 로직에 사용됨)
	 * @return 필터링 조건에 맞는 공간 목록 {@code SpaceUserResponseDto}
	 */
	@Query(value = """
		/* CHANGE: 태그 목록 미리 집계 (응답에 tagNames 배열 포함) */
		WITH tag_agg AS (
		  SELECT stm.space_id,
		         array_agg(st.tag_name ORDER BY st.tag_name) AS tag_names
		  FROM tb_space_tag_map stm
		  JOIN tb_space_tags    st ON st.tag_id = stm.tag_id
		  GROUP BY stm.space_id
		),

		/* CHANGE: 태그 AND 필터
		   - :tagCount = 0 이면 모든 공간 허용
		   - :tagCount > 0 이면, 선택한 tagNames 전부를 포함하는 space_id만 통과
		   - ANY(CAST(:tagNames AS text[])) 로 타입을 text[]로 고정 (Postgres 타입 추론 에러 방지)
		*/
		tag_and_filter AS (
		  SELECT s.space_id
		  FROM tb_spaces s
		  WHERE :tagCount = 0

		  UNION

		  SELECT s2.space_id
		  FROM tb_spaces s2
		  WHERE :tagCount > 0 AND (
		    SELECT COUNT(DISTINCT st2.tag_name)
		    FROM tb_space_tag_map stm2
		    JOIN tb_space_tags    st2 ON st2.tag_id = stm2.tag_id
		    WHERE stm2.space_id = s2.space_id
		      AND st2.tag_name = ANY(CAST(:tagNames AS text[]))  /* CHANGE: 타입 캐스팅 명시 */
		  ) = :tagCount
		)

		/* CHANGE: 실제 데이터 셀렉트 (Projection alias는 DTO 게터와 동일) */
		SELECT
		  s.space_id           AS spaceId,
		  s.space_name         AS spaceName,
		  s.space_description  AS spaceDescription,
		  s.space_capacity     AS spaceCapacity,
		  s.category_id        AS categoryId,
		  c.category_name      AS categoryName,
		  /* 태그 배열 */
		 COALESCE((
		   SELECT array_agg(DISTINCT tt.tag_name ORDER BY tt.tag_name)
		   FROM tb_space_tag_map mm
		   JOIN tb_space_tags tt ON tt.tag_id = mm.tag_id
		   WHERE mm.space_id = s.space_id
		 ), ARRAY[]::text[]) AS tagNames,
		  COALESCE(
		      json_build_object(
			      'locationName', l.location_name,
			      'addressRoad', l.address_road || ' (' || l.address_jibun || ')',
			      'addressInfo', l.access_info
			  ), '{}'::json
		  ) AS location,
		  s.space_image_url    AS spaceImageUrl
		FROM tb_spaces s
		JOIN tb_admin_region     r ON r.region_id   = s.region_id
		JOIN tb_space_categories c ON c.category_id = s.category_id
		LEFT JOIN tag_agg t       ON t.space_id     = s.space_id
		JOIN tag_and_filter tf    ON tf.space_id    = s.space_id
		JOIN tb_locations        l ON l.location_id = s.location_id
		WHERE
		  s.space_is_available = true
		  AND (:regionId   IS NULL OR s.region_id   = :regionId)      /* CHANGE: 옵셔널 */
		  AND (:people   IS NULL OR s.space_capacity >= :people)  /* CHANGE: 옵셔널(이상) */
		ORDER BY s.reg_date DESC                                   /* 필요시 다른 기본 정렬 가능 */
		""",
		nativeQuery = true)
	List<SpaceUserResponseDto> searchSpacesForUser(
		@Param("regionId") Integer regionId,    // CHANGE: null 허용
		@Param("people") Integer people,      // CHANGE: null 허용(이상)
		@Param("tagNames") String[] tagNames,   // CHANGE: 배열 파라미터 (이름 기반)
		@Param("tagCount") Integer tagCount     // CHANGE: 배열 길이 전달
	);

	/**
	 * 특정 공간 상세 조회
	 * - 특정 공간 ID에 해당하는 상세 정보를 조회
	 * - 담당자/위치/상세 이미지/태그(편의시설)/운영 시간/휴무일 정보 등을 JSON/배열 형태로 포함하여 반환
	 *
	 * @param spaceId 조회할 공간의 ID
	 * @return 공간 상세 정보를 담은 {@code Optional<SpaceUserDtailResponseDto>}
	 */
	@Query(value = """
		SELECT
		  s.space_id           AS spaceId,
		  s.space_name         AS spaceName,
		  r.region_name        AS regionName,
		  c.category_name      AS categoryName,
		  s.space_capacity     AS spaceCapacity,
		  s.space_description  AS spaceDescription,
		  s.space_image_url    AS spaceImageUrl,
		  s.reservation_way    AS reservationWay,
		  s.space_rules        AS spaceRules,
		  s.space_is_available AS spaceIsAvailable,
		  /* 담당자 정보 추가 */
		   COALESCE(
			   json_build_object(
				  'managerName', a.admin_name,
				  'managerPhone', a.admin_phone
			  ), '{}'::json
		   ) AS manager,
		  /* 주소 */
		  COALESCE(
		      json_build_object(
			      'locationName', l.location_name,
			      'addressRoad', l.address_road || ' (' || l.address_jibun || ')',
			      'addressInfo', l.access_info
			  ), '{}'::json
		  ) AS location,

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
		WHERE s.space_id = :spaceId
		""", nativeQuery = true)
	Optional<SpaceUserDtailResponseDto> findSpaceDetail(@Param("spaceId") Integer spaceId);
}
