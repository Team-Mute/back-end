package Team_Mute.back_end.domain.reservation_admin.util;

import Team_Mute.back_end.domain.member.entity.User;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 사용자나 회사 정보를 기반으로 해당 예약이 '신한금융희만재단' 관련 예약인지 판별하는 유틸리티 클래스
 * 판별 기준은 {@code SHINHAN_COMPANY_NAME}에 정의된 회사명
 */
public final class ShinhanGroupUtils {

	private ShinhanGroupUtils() {
	}

	// 비교 기준 회사명
	public static final String SHINHAN_COMPANY_NAME = "신한금융희망재단";

	/**
	 * 제공된 회사명이 정의된 {@code SHINHAN_COMPANY_NAME}과 일치하는지 판별
	 *
	 * @param companyName 비교할 회사명
	 * @return [신한금융희망재단] 회사명과 일치하면 true
	 */
	public static boolean isShinhanCompanyName(String companyName) {
		return companyName != null && companyName.trim().equals(SHINHAN_COMPANY_NAME);
	}

	/**
	 * User 엔티티에 연결된 회사 정보({@code userCompany})를 바탕으로 [신한금융희망재단]인지 판별합니다.
	 * (User 엔티티의 {@code userCompany}가 로딩되어 있어야 함)
	 *
	 * @param user User 엔티티
	 * @return [신한금융희망재단] 소속이면 true
	 */
	public static boolean isShinhan(User user) {
		if (user == null || user.getUserCompany() == null) return false;
		return isShinhanCompanyName(user.getUserCompany().getCompanyName());
	}

	/**
	 * 회사 ID와 사전 조회된 회사명 맵을 사용하여 [신한금융희망재단]인지 판별
	 * (주로 다수의 예약을 한 번에 처리하는 배치/리스트 조회 패턴에 사용)
	 *
	 * @param companyId       확인할 회사 ID
	 * @param companyNameById 회사 ID를 회사명(String)으로 매핑한 Map
	 * @return [신한금융희망재단] 소속이면 true
	 */
	public static boolean isShinhanByCompanyId(Integer companyId, Map<Integer, String> companyNameById) {
		if (companyId == null || companyNameById == null) return false;
		return isShinhanCompanyName(companyNameById.get(companyId));
	}

	/**
	 * 다수의 사용자 목록에 대해 {@code userId}를 키로, [신한금융희망재단] 여부({@code Boolean})를 값으로 하는 Map을 생성
	 * (주로 리스트 조회를 위한 사전 데이터 가공에 사용)
	 *
	 * @param users           사용자 엔티티 목록
	 * @param companyNameById 회사 ID를 회사명(String)으로 매핑한 Map
	 * @return {@code Long(userId) -> Boolean(isShinhan)} 형태의 Map
	 */
	public static Map<Long, Boolean> buildIsShinhanByUserId(List<User> users,
															Map<Integer, String> companyNameById) {
		if (users == null || users.isEmpty()) return Collections.emptyMap();
		Map<Long, Boolean> result = new HashMap<>(users.size());
		for (User u : users) {
			if (u == null) continue;
			Integer cid = (u.getUserCompany() != null) ? u.getUserCompany().getCompanyId() : null;
			result.put(u.getUserId(), isShinhanByCompanyId(cid, companyNameById));
		}
		return result;
	}
}
