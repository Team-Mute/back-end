package Team_Mute.back_end.domain.reservation.dto.response;

import java.util.List;

import org.springframework.data.domain.Page;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PagedReservationResponse {
	private List<ReservationListDto> content;
	private long totalElements;
	private int totalPages;
	private int currentPage;
	private int pageSize;

	public static PagedReservationResponse fromPage(Page<ReservationListDto> page) {
		return new PagedReservationResponse(
			page.getContent(),
			page.getTotalElements(),
			page.getTotalPages(),
			page.getNumber() + 1, // Page 객체는 0부터 시작하므로 +1
			page.getSize()
		);
	}
}
