package Team_Mute.back_end.domain.reservation_admin.dto.response;

import java.util.ArrayList;
import java.util.List;

/**
 * [예약 일괄 승인] 최종 응답 DTO
 * * 요청된 전체 건수, 성공/실패 건수 및 개별 예약 건의 처리 결과를 요약하여 반환
 */
public class BulkApproveResponseDto {
	/**
	 * 요청된 전체 예약 건수
	 */
	private int total;

	/**
	 * 성공적으로 처리된 예약 건수
	 */
	private int successCount;

	/**
	 * 처리 실패한 예약 건수
	 */
	private int failureCount;

	/**
	 * 개별 예약 건에 대한 처리 결과 리스트 (BulkApproveItemResultDto의 리스트)
	 */
	private List<BulkApproveItemResultDto> results = new ArrayList<>();

	/**
	 * 요청된 전체 예약 건수를 반환
	 */
	public int getTotal() {
		return total;
	}

	/**
	 * 요청된 전체 예약 건수를 설정
	 */
	public void setTotal(int total) {
		this.total = total;
	}

	/**
	 * 성공적으로 처리된 예약 건수를 반환
	 */
	public int getSuccessCount() {
		return successCount;
	}

	/**
	 * 성공적으로 처리된 예약 건수를 설정
	 */
	public void setSuccessCount(int successCount) {
		this.successCount = successCount;
	}

	/**
	 * 처리 실패한 예약 건수를 반환
	 */
	public int getFailureCount() {
		return failureCount;
	}

	/**
	 * 처리 실패한 예약 건수를 설정
	 */
	public void setFailureCount(int failureCount) {
		this.failureCount = failureCount;
	}

	/**
	 * 개별 예약 건에 대한 처리 결과 리스트를 반환
	 */
	public List<BulkApproveItemResultDto> getResults() {
		return results;
	}

	/**
	 * 개별 예약 건에 대한 처리 결과 리스트를 설정
	 */
	public void setResults(List<BulkApproveItemResultDto> results) {
		this.results = results;
	}

	/**
	 * 결과 리스트에 개별 항목의 처리 결과를 추가
	 *
	 * @param item 개별 처리 결과 DTO
	 */
	public void add(BulkApproveItemResultDto item) {
		this.results.add(item);
	}
}
