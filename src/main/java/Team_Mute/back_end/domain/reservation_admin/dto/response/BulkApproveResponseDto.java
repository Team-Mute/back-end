package Team_Mute.back_end.domain.reservation_admin.dto.response;

import java.util.ArrayList;
import java.util.List;

public class BulkApproveResponseDto {
	private int total;
	private int successCount;
	private int failureCount;
	private List<BulkApproveItemResultDto> results = new ArrayList<>();

	public int getTotal() {
		return total;
	}

	public void setTotal(int total) {
		this.total = total;
	}

	public int getSuccessCount() {
		return successCount;
	}

	public void setSuccessCount(int successCount) {
		this.successCount = successCount;
	}

	public int getFailureCount() {
		return failureCount;
	}

	public void setFailureCount(int failureCount) {
		this.failureCount = failureCount;
	}

	public List<BulkApproveItemResultDto> getResults() {
		return results;
	}

	public void setResults(List<BulkApproveItemResultDto> results) {
		this.results = results;
	}

	public void add(BulkApproveItemResultDto item) {
		this.results.add(item);
	}
}
