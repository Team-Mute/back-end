package Team_Mute.back_end.domain.reservation_admin.dto.response;

import Team_Mute.back_end.domain.reservation.entity.Reservation;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class ReservationListResponseDto {
    public Long reservationId;
    public String reservationStatusName;
    public String spaceName;
    public String userName;
    public Integer reservationHeadcount;
    public LocalDateTime reservationFrom;
    public LocalDateTime reservationTo;
    public LocalDateTime regDate;
    public boolean isShinhan;
    public boolean isEmergency;
    public boolean isApprovable;
    public boolean isRejectable;
    public List<PrevisitItemResponseDto> previsits;

    public static ReservationListResponseDto from(
            Reservation r,
            String statusName,
            String spaceName,
            String userName,
            boolean isShinhan,
            boolean isEmergency,
            boolean isApprovable,
            boolean isRejectable,
            List<PrevisitItemResponseDto> previsitDtos
    ) {
        ReservationListResponseDto res = new ReservationListResponseDto();
        res.reservationId = r.getReservationId();
        res.reservationStatusName = statusName;
        res.spaceName = spaceName;
        res.userName = userName;
        res.reservationHeadcount = r.getReservationHeadcount();
        res.reservationFrom = r.getReservationFrom();
        res.reservationTo = r.getReservationTo();
        res.regDate = r.getRegDate();
        res.previsits = previsitDtos;
        res.isShinhan = isShinhan;
        res.isEmergency = isEmergency;
        res.isApprovable = isApprovable;
        res.isRejectable = isRejectable;
        return res;
    }

    public Boolean getIsEmergency() {
        return isShinhan;
    }

    public Boolean getIsShinhan() {
        return isEmergency;
    }
}
