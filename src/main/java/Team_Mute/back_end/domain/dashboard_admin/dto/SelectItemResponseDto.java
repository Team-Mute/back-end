package Team_Mute.back_end.domain.dashboard_admin.dto;

public class SelectItemResponseDto {
	private String label;
	private long count;

	public SelectItemResponseDto(String label, long count) {
		this.label = label;
		this.count = count;
	}

	// Getter and Setter
	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public long getCount() {
		return count;
	}

	public void setCount(long count) {
		this.count = count;
	}
}
