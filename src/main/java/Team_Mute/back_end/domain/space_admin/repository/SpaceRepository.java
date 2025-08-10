package Team_Mute.back_end.domain.space_admin.repository;


import Team_Mute.back_end.domain.space_admin.dto.SpaceListResponse;
import Team_Mute.back_end.domain.space_admin.entity.Space;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SpaceRepository extends JpaRepository<Space, Integer> {

	// 목록 + 조인 (regionName, categoryName 포함)
	@Query(value = """
    SELECT
      s.space_id           AS spaceId,
      s.space_name         AS spaceName,
      r.region_name        AS regionName,
      c.category_name      AS categoryName,
      s.user_id            AS userId,
      s.space_capacity     AS spaceCapacity,
      s.space_location     AS spaceLocation,
      s.space_description  AS spaceDescription,
      s.space_image_url    AS spaceImageUrl,
      s.space_is_available AS spaceIsAvailable,
      COALESCE(array_agg(DISTINCT t.tag_name ORDER BY t.tag_name), '{}') AS tagNames,
      s.reg_date           AS regDate,
      s.upd_date           AS updDate
    FROM tb_spaces s
    JOIN tb_admin_region     r ON r.region_id   = s.region_id
    JOIN tb_space_categories c ON c.category_id = s.category_id
	LEFT JOIN tb_space_tag_map m ON m.space_id   = s.space_id
	LEFT JOIN tb_space_tags  t ON t.tag_id      = m.tag_id
	GROUP BY
        s.space_id, r.region_name, c.category_name, s.user_id,
		s.space_name, s.space_capacity, s.space_location, s.space_description,
		s.space_image_url, s.space_is_available, s.reg_date, s.upd_date
    ORDER BY s.reg_date DESC
    """,
		countQuery = "SELECT COUNT(*) FROM tb_spaces",
		nativeQuery = true)
	List<SpaceListResponse> findAllWithNames();
}
