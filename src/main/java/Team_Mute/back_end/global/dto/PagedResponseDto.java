package Team_Mute.back_end.global.dto;

import lombok.Data;

import java.util.List;
import org.springframework.data.domain.Page;

/**
 * 공통 페이징 응답 DTO
 * - Spring Data JPA의 Page<T>를 클라이언트 친화적 구조로 변환
 * - 어떤 도메인에서도 재사용 가능
 */
@Data
public class PagedResponseDto<T> {

	private List<T> content; // 현재 페이지 데이터
	private long totalElements; // 전체 데이터 개수
	private int totalPages; // 전체 페이지 수
	private int currentPage; // 현재 페이지 번호 (1부터 시작)
	private int pageSize; // 페이지 크기

	// Page<T> → PagedResponseDto<T> 변환 생성자
	public PagedResponseDto(Page<T> page) {
		this.content = page.getContent();
		this.totalElements = page.getTotalElements();
		this.totalPages = page.getTotalPages();
		this.currentPage = page.getNumber() + 1;
		this.pageSize = page.getSize();
	}
}
