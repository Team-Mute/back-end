package Team_Mute.back_end.domain.member.jwt;

public record TokenClaims(
	String sub,       // userId
	String cid,       // companyId
	String sid,       // sessionId
	Integer ver,      // password/role version
	Integer roles
) {
	public static TokenClaims of(String sub, String cid, String sid, Integer ver, Integer roles) {
		return new TokenClaims(sub, cid, sid, ver, roles);
	}
}
