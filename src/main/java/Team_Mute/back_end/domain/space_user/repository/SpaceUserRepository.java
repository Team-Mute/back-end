package Team_Mute.back_end.domain.space_user.repository;

import Team_Mute.back_end.domain.space_admin.entity.Space;
import Team_Mute.back_end.domain.space_user.dto.SpaceUserResponseDto;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpaceUserRepository extends JpaRepository<Space, Integer> {

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
		  c.category_name      AS categoryName,
		  COALESCE(t.tag_names, ARRAY[]::text[]) AS tagNames,   /* CHANGE: NULL → 빈 배열 */
		  COALESCE(
		      json_build_object(
			      'locationName', l.location_name,
			      'addressRoad', l.address_road
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
		  AND (:categoryId IS NULL OR s.category_id = :categoryId)/* CHANGE: 옵셔널 */
		  AND (:people   IS NULL OR s.space_capacity >= :people)  /* CHANGE: 옵셔널(이상) */
		ORDER BY s.reg_date DESC                                   /* 필요시 다른 기본 정렬 가능 */
		""",
		nativeQuery = true)
	List<SpaceUserResponseDto> searchSpacesForUser(
		@Param("regionId") Integer regionId,    // CHANGE: null 허용
		@Param("categoryId") Integer categoryId,  // CHANGE: null 허용
		@Param("people") Integer people,      // CHANGE: null 허용(이상)
		@Param("tagNames") String[] tagNames,   // CHANGE: 배열 파라미터 (이름 기반)
		@Param("tagCount") Integer tagCount     // CHANGE: 배열 길이 전달
	);
}
