package Team_Mute.back_end.domain.reservation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 예약 상태 엔티티 (코드 테이블)
 * 예약의 현재 상태를 나타내는 마스터 데이터
 *
 * 테이블 구조:
 * - 테이블명: tb_reservation_status
 * - 기본 키: reservationStatusId (수동 할당)
 * - 고정 데이터: INSERT로 초기 데이터 생성 후 변경 없음
 *
 * 상태 종류:
 * - 1: 1차 승인 대기 (PENDING_APPROVAL) - 초기 상태
 * - 2: 2차 승인 대기 (FIRST_APPROVAL) - 1차 승인자 승인
 * - 3: 최종 승인 (FINAL_APPROVAL) - 2차 승인자 승인, 예약 완료
 * - 4: 반려 (REJECTED) - 승인 거부
 * - 5: 이용 완료 (COMPLETED) - 예약 사용 완료
 * - 6: 예약 취소 (CANCELLED) - 사용자가 취소
 *
 * 연관 관계:
 * - Reservation (OneToMany): 이 상태를 갖는 예약들
 *
 * 코드 테이블 특징:
 * - @GeneratedValue 없음 (수동 할당)
 * - 애플리케이션 실행 중 수정/삭제 없음
 * - 참조 무결성 보장
 *
 * @author Team Mute
 * @since 1.0
 */
@Entity
@Table(name = "tb_reservation_status")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationStatus {

	/**
	 * 예약 상태 ID (기본 키)
	 * - 수동 할당 (자동 증가 전략 사용 안 함)
	 * - 고정값 (1~6)
	 */
	@Id
	@Column(name = "reservation_status_id")
	private Integer reservationStatusId;

	/**
	 * 예약 상태 이름
	 * - 상태의 한글 명칭
	 */
	@Column(name = "reservation_status_name", nullable = false)
	private String reservationStatusName;
}
