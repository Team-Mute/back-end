package Team_Mute.back_end.domain.member.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tb_users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "user_id")
	private Long userId;

	@Column(name = "user_email", length = 100, nullable = false, unique = true)
	private String userEmail;

	@Column(name = "user_name", length = 50, nullable = false)
	private String userName;

	@Column(name = "user_pwd", length = 255, nullable = false)
	private String userPwd;

	@CreationTimestamp
	@Column(name = "reg_date")
	private LocalDateTime regDate;

	@UpdateTimestamp
	@Column(name = "upd_date")
	private LocalDateTime updDate;

	@Column(name = "agree_email")
	private Boolean agreeEmail;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "role_id")
	private UserRole userRole;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "company_id")
	private UserCompany userCompany;

	@Column(name = "token_ver", nullable = false)
	private Integer tokenVer = 1;
}
