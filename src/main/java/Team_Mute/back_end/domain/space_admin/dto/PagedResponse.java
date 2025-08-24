package Team_Mute.back_end.domain.space_admin.dto;

import lombok.Data;

import java.util.List;
import org.springframework.data.domain.Page;

@Data
public class PagedResponse<T> {

	private List<T> content;
	private long totalElements;
	private int totalPages;
	private int currentPage;
	private int pageSize;

	public PagedResponse(Page<T> page) {
		this.content = page.getContent();
		this.totalElements = page.getTotalElements();
		this.totalPages = page.getTotalPages();
		this.currentPage = page.getNumber();
		this.pageSize = page.getSize();
	}
}
