package Team_Mute.back_end.domain.space_admin.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SpaceCloneOptions {
	/**
	 * 이미지 S3 실제 복사 여부 (true: 원본 파일을 새 키로 복사, false: URL만 재사용)
	 */
	private Boolean copyImages = true;

	/**
	 * 태그 매핑 복제 여부
	 */
	private Boolean copyTags = true;

	/**
	 * 운영시간/휴무일 복제 여부
	 */
	private Boolean copySchedules = true;

	/**
	 * 복제본 이름 접미사
	 */
	private String nameSuffix = " (복제)";

	/**
	 * 복제본 저장 상태를 공개로 바꿀지 여부 (프로젝트 정책에 맞춰 사용)
	 */
	private Boolean publish = false;
}
