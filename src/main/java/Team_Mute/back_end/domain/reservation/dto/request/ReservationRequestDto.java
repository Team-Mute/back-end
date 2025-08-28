package Team_Mute.back_end.domain.reservation.dto.request;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter // @ModelAttribute를 위해 Setter 추가
public class ReservationRequestDto {

	@NotNull(message = "공간 ID는 필수입니다.")
	private Integer spaceId;

	@NotNull(message = "예약 인원은 필수입니다.")
	@Min(value = 1, message = "예약 인원은 1명 이상이어야 합니다.")
	private Integer reservationHeadcount;

	@NotNull(message = "예약 시작 시간은 필수입니다.")
	@Future(message = "현재 시간 이후여야 합니다.")
	private LocalDateTime reservationFrom;

	@NotNull(message = "예약 종료 시간은 필수입니다.")
	private LocalDateTime reservationTo;

	@NotBlank(message = "예약 목적은 필수입니다.")
	private String reservationPurpose;

	private List<MultipartFile> reservationAttachments;

	private List<String> existingAttachments;

	public List<String> getExistingAttachments() {
		return existingAttachments;
	}

	public void setExistingAttachments(List<String> existingAttachments) {
		this.existingAttachments = existingAttachments;
	}
}
