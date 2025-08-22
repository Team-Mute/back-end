package Team_Mute.back_end.domain.space_admin.repository;

import Team_Mute.back_end.domain.space_admin.entity.Board;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BoardRepository extends JpaRepository<Board, Long> {

	// Pageable을 파라미터로 받고 Page<Board>를 리턴하는 메서드
	Page<Board> findByTitleContaining(String title, Pageable pageable);
}
