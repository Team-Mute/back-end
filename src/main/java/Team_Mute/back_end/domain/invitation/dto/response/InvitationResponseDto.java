package Team_Mute.back_end.domain.invitation.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Builder;
import lombok.Getter;

/**
 * 초대장 상세 정보를 담는 응답 DTO
 * - 완료된 예약에 대한 초대장 조회 시 반환되는 데이터 구조
 * - 예약자 정보, 공간 정보, 예약 일정 등 초대장 표시에 필요한 모든 정보를 포함
 */
@Getter
@Builder
public class InvitationResponseDto {

	/**
	 * 예약자 이름
	 * - 초대장에 표시될 예약자의 성명
	 */
	private final String userName;

	/**
	 * 예약된 공간 이름
	 * - 예약이 완료된 공간의 명칭 (예: "회의실 A")
	 */
	private final String spaceName;

	/**
	 * 공간의 도로명 주소
	 * - 예약된 공간의 위치 정보
	 */
	private final String addressRoad;

	/**
	 * 예약 시작 일시
	 * - 예약이 시작되는 날짜와 시간 (LocalDateTime 형식)
	 */
	private final LocalDateTime reservationFrom;

	/**
	 * 예약 종료 일시
	 * - 예약이 종료되는 날짜와 시간 (LocalDateTime 형식)
	 */
	private final LocalDateTime reservationTo;

	/**
	 * 예약 목적
	 * - 예약자가 작성한 공간 사용 목적
	 */
	private final String reservationPurpose;

	/**
	 * 예약 첨부 파일 URL 목록
	 * - 예약 신청 시 업로드된 첨부 파일들의 URL 리스트
	 * - 빈 리스트일 수 있음 (첨부 파일이 없는 경우)
	 */
	private final List<String> reservationAttachment;
}
