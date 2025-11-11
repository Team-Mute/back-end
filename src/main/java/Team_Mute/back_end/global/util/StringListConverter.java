package Team_Mute.back_end.global.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * List<String>과 String 간 JPA 타입 변환기
 *
 * 목적:
 * - 데이터베이스에는 쉼표로 구분된 문자열로 저장
 * - 자바 엔티티에서는 List<String>으로 사용
 *
 * 사용처:
 * - Reservation.reservationAttachment (첨부파일 URL 리스트)
 * - @Convert(converter = StringListConverter.class) 어노테이션과 함께 사용
 */
@Converter
public class StringListConverter implements AttributeConverter<List<String>, String> {

	private static final String SPLIT_CHAR = ",";

	/**
	 * 엔티티 → 데이터베이스 변환
	 * List<String>를 쉼표로 구분된 문자열로 변환
	 *
	 * @param attribute List<String> (엔티티의 필드 값)
	 * @return 쉼표로 구분된 문자열 (DB 저장 값)
	 */
	@Override
	public String convertToDatabaseColumn(List<String> attribute) {
		if (attribute == null || attribute.isEmpty()) {
			return null;
		}
		return String.join(SPLIT_CHAR, attribute);
	}

	/**
	 * 데이터베이스 → 엔티티 변환
	 * 쉼표로 구분된 문자열을 List<String>으로 변환
	 *
	 * @param dbData 쉼표로 구분된 문자열 (DB 저장 값)
	 * @return List<String> (엔티티의 필드 값)
	 */
	@Override
	public List<String> convertToEntityAttribute(String dbData) {
		if (dbData == null || dbData.trim().isEmpty()) {
			return Collections.emptyList();
		}
		return Arrays.asList(dbData.split(SPLIT_CHAR));
	}
}
