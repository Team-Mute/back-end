package Team_Mute.back_end.domain.reservation.dto.response;

import java.util.List;

import org.springframework.data.domain.Page;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 페이징된 예약 목록 응답 DTO
 * 사용자의 예약 목록을 페이징 정보와 함께 반환
 *
 * 사용 목적:
 * - 마이페이지에서 예약 내역 조회
 * - 대량 예약 데이터의 효율적 조회
 * - 페이지네이션 UI 구현 지원
 *
 * API 엔드포인트:
 * - GET /api/reservations?page=1&size=10&filterOption=approved
 *
 * @author Team Mute
 * @since 1.0
 */
@Data
@AllArgsConstructor
public class PagedReservationResponse {
	/**
	 * 예약 목록 (현재 페이지)
	 * - 현재 페이지에 포함된 예약 목록
	 * - ReservationListDto 리스트
	 * - pageSize 개수만큼 포함 (마지막 페이지는 적을 수 있음)
	 * - 빈 리스트 가능 (예약이 없는 경우)
	 */
	private List<ReservationListDto> content;

	/**
	 * 전체 예약 개수
	 * - 필터링 후 전체 예약 개수
	 * - 모든 페이지의 예약을 합친 개수
	 * - 페이지네이션 계산에 사용
	 */
	private long totalElements;

	/**
	 * 전체 페이지 수
	 * - totalElements를 pageSize로 나눈 값 (올림)
	 * - 프론트엔드 페이지네이션 버튼 개수
	 */
	private int totalPages;

	/**
	 * 현재 페이지 번호
	 * - 1부터 시작 (사용자 친화적)
	 * - 내부적으로는 0-based (Spring Data JPA)
	 * - 프론트엔드에서 현재 페이지 강조 표시
	 */
	private int currentPage;

	/**
	 * 페이지 크기
	 * - 한 페이지에 표시할 예약 개수
	 * - 요청 시 size 파라미터와 동일
	 * - 기본값: 5 (설정에 따라 다름)
	 */
	private int pageSize;

	/**
	 * Spring Data JPA Page 객체로부터 PagedReservationResponse 생성
	 * - 정적 팩토리 메서드
	 * - Page<ReservationListDto>를 PagedReservationResponse로 변환
	 * - 페이지 번호를 1-based로 변환 (+1)
	 *
	 * @param page Spring Data JPA Page 객체
	 * @return PagedReservationResponse 인스턴스
	 */
	public static PagedReservationResponse fromPage(Page<ReservationListDto> page) {
		return new PagedReservationResponse(
			page.getContent(),            // 현재 페이지 내용
			page.getTotalElements(),      // 전체 요소 개수
			page.getTotalPages(),         // 전체 페이지 수
			page.getNumber() + 1,         // 현재 페이지 (0-based → 1-based)
			page.getSize()                // 페이지 크기
		);
	}
}
