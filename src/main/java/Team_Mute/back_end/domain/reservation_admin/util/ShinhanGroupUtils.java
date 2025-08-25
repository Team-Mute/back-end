package Team_Mute.back_end.domain.reservation_admin.util;

import Team_Mute.back_end.domain.member.entity.User;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ShinhanGroupUtils {

	private ShinhanGroupUtils() {
	}

	// 비교 기준 회사명 (팀 정책에 맞게 변경 가능)
	public static final String SHINHAN_COMPANY_NAME = "신한금융희망재단";

	// 회사명으로 직접 판별
	public static boolean isShinhanCompanyName(String companyName) {
		return companyName != null && companyName.trim().equals(SHINHAN_COMPANY_NAME);
	}

	// User 엔티티로 판별 (userCompany가 로딩되어 있어야 함)
	public static boolean isShinhan(User user) {
		if (user == null || user.getUserCompany() == null) return false;
		return isShinhanCompanyName(user.getUserCompany().getCompanyName());
	}

	// companyId + 사전 조회한 companyNameById 맵으로 판별 (배치 조회 패턴)
	public static boolean isShinhanByCompanyId(Integer companyId, Map<Integer, String> companyNameById) {
		if (companyId == null || companyNameById == null) return false;
		return isShinhanCompanyName(companyNameById.get(companyId));
	}

	// 다건 사용자에 대해 userId -> isShinhan 맵을 생성 (배치 조회 패턴)
	public static Map<Integer, Boolean> buildIsShinhanByUserId(List<User> users,
															   Map<Integer, String> companyNameById) {
		if (users == null || users.isEmpty()) return Collections.emptyMap();
		Map<Integer, Boolean> result = new HashMap<>(users.size());
		for (User u : users) {
			if (u == null) continue;
			Integer cid = ((u.getUserCompany() != null) ? u.getUserCompany().getCompanyId() : null);
			result.put(Math.toIntExact(u.getUserId()), isShinhanByCompanyId(cid, companyNameById));
		}
		return result;
	}
}
