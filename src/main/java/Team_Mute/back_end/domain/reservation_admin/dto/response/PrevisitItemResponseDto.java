package Team_Mute.back_end.domain.reservation_admin.dto.response;


import Team_Mute.back_end.domain.previsit.entity.PrevisitReservation;

import java.time.LocalDateTime;

public class PrevisitItemResponseDto {
	public Long previsitId;
	public LocalDateTime previsitFrom;
	public LocalDateTime previsitTo;

	public static PrevisitItemResponseDto from(PrevisitReservation p, String statusName) {
		PrevisitItemResponseDto res = new PrevisitItemResponseDto();
		res.previsitId = p.getPrevisitId();
		res.previsitFrom = p.getPrevisitFrom();
		res.previsitTo = p.getPrevisitTo();
		return res;
	}
}
