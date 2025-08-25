package Team_Mute.back_end.domain.previsit.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import Team_Mute.back_end.domain.previsit.dto.request.PrevisitCreateRequest;
import Team_Mute.back_end.domain.previsit.dto.request.PrevisitUpdateRequest;
import Team_Mute.back_end.domain.previsit.dto.response.PrevisitResponse;
import Team_Mute.back_end.domain.previsit.service.PrevisitService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/previsit")
@RequiredArgsConstructor
public class PrevisitController {

	private final PrevisitService previsitService;

	@PostMapping
	public ResponseEntity<PrevisitResponse> createPrevisit(@Valid @RequestBody PrevisitCreateRequest request) {
		PrevisitResponse response = previsitService.createPrevisit(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@GetMapping
	public ResponseEntity<PrevisitResponse> getPrevisit(@RequestParam("reservation_id") Long reservationId) {
		PrevisitResponse response = previsitService.getPrevisitByReservationId(reservationId);
		return ResponseEntity.ok(response);
	}

	@PutMapping("/{previsitId}")
	public ResponseEntity<PrevisitResponse> updatePrevisit(@PathVariable Long previsitId,
		@Valid @RequestBody PrevisitUpdateRequest request) {
		PrevisitResponse response = previsitService.updatePrevisit(previsitId, request);
		return ResponseEntity.ok(response);
	}

	@DeleteMapping("/{previsitId}")
	public ResponseEntity<Map<String, String>> deletePrevisit(@PathVariable Long previsitId) {
		previsitService.deletePrevisit(previsitId);
		return ResponseEntity.ok(Map.of("message", "사전답사 예약(ID: " + previsitId + ")이(가) 성공적으로 삭제되었습니다."));
	}
}
