package Team_Mute.back_end.domain.reservation_admin.dto;

import java.time.LocalDateTime;

public class ApproveResponseDto {
    public Long reservationId;
    public String fromStatus;
    public String toStatus;
    public LocalDateTime approvedAt;
    public String message;

    public ApproveResponseDto(Long reservationId, String fromStatus, String toStatus, LocalDateTime approvedAt, String message) {
        this.reservationId = reservationId;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.approvedAt = approvedAt;
        this.message = message;
    }
}
