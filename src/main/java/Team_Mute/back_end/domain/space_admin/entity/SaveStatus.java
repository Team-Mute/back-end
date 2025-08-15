package Team_Mute.back_end.domain.space_admin.entity;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * SaveStatus.java
 * <p>
 * 공간(Space) 엔티티의 저장 상태를 나타내는 Enum 클래스
 * <p>
 * DRAFT     : 공간 등록을 임시 저장한 상태
 * PUBLISHED : 공간 등록이 완료되어 공개된 상태
 * <p>
 * 사용처:
 * - Space 엔티티의 saveStatus 컬럼 매핑
 * - 공간 등록/수정 시 상태 관리 로직
 * <p>
 * 주의사항:
 * - PostgreSQL ENUM 타입("save_status")과 매핑됩니다.
 * - 새로운 상태를 추가할 경우, DB에도 ALTER TYPE 구문을 통해 값을 추가해야 합니다.
 * <p>
 * 작성 목적:
 * - 공간 데이터의 저장 상태를 코드로 명확히 표현하고, DB와 일관성을 유지하기 위함.
 */

public enum SaveStatus {
	DRAFT, PUBLISHED;

	@JsonCreator
	public static SaveStatus from(String v) {
		if (v == null) return null;
		try {
			return SaveStatus.valueOf(v.toUpperCase());
		} catch (Exception e) {
			// InvalidFormatException은 checked 예외라서 RuntimeException으로 감싸줌
			throw new IllegalArgumentException("saveStatus 허용값: DRAFT, PUBLISHED. 입력값=" + v);
		}
	}
}
