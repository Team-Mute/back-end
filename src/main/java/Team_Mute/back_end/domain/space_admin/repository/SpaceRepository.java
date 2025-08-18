package Team_Mute.back_end.domain.space_admin.repository;


import Team_Mute.back_end.domain.space_admin.dto.SpaceListResponse;
import Team_Mute.back_end.domain.space_admin.entity.Space;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpaceRepository extends JpaRepository<Space, Integer> {
	// 공간 등록할 경우 공간명 중복 체크
	boolean existsBySpaceName(String spaceName);

	// 공간 수정할 경우 공간명 중복 체크
	boolean existsBySpaceNameAndSpaceIdNot(String spaceName, Integer spaceId);

	// 목록 + 조인 (regionName, categoryName 포함)
	@Query(value = """
		   SELECT
		     s.space_id           AS spaceId,
		     s.space_name         AS spaceName,
		     r.region_name        AS regionName,
		     c.category_name      AS categoryName,
		     s.user_id            AS userId,
		     s.space_capacity     AS spaceCapacity,
		     l.address_road       AS addressRoad,
		     s.space_description  AS spaceDescription,
		     s.space_image_url    AS spaceImageUrl,
		     s.space_is_available AS spaceIsAvailable,
		     s.reservation_way    AS reservationWay,
		     s.space_rules        AS spaceRules,
		     s.save_status::text  AS saveStatus,
		     /* 상세 이미지 */
			 COALESCE((
			   SELECT array_agg(si.image_url ORDER BY si.image_priority ASC, si.image_id ASC)
			   FROM tb_space_images si
			   WHERE si.space_id = s.space_id
				 AND (s.space_image_url IS NULL OR si.image_url <> s.space_image_url)
			 ), ARRAY[]::text[]) AS detailImageUrls,

		     /* 태그 배열 */
		     COALESCE((
				SELECT ARRAY(
				  SELECT DISTINCT t.tag_name
				  FROM tb_space_tag_map m
				  JOIN tb_space_tags t ON t.tag_id = m.tag_id
				  WHERE m.space_id = s.space_id
				  ORDER BY t.tag_name
				)
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
		   ORDER BY s.reg_date DESC
		""",
		countQuery = "SELECT COUNT(*) FROM tb_spaces",
		nativeQuery = true)
	List<SpaceListResponse> findAllWithNames();

	// 단건 상세 + 조인
	@Query(value = """
		SELECT
		  s.space_id           AS spaceId,
		  s.space_name         AS spaceName,
		  r.region_name        AS regionName,
		  c.category_name      AS categoryName,
		  s.user_id            AS userId,
		  s.space_capacity     AS spaceCapacity,
		  l.address_road       AS addressRoad,
		  s.space_description  AS spaceDescription,
		  s.space_image_url    AS spaceImageUrl,
		  s.space_is_available AS spaceIsAvailable,
		  s.reservation_way    AS  reservationWay,
		  s.space_rules        AS spaceRules,
		  s.save_status        AS saveStatus,
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
		WHERE s.space_id = :spaceId
		""", nativeQuery = true)
	Optional<SpaceListResponse> findDetailWithNames(@Param("spaceId") Integer spaceId);
}
