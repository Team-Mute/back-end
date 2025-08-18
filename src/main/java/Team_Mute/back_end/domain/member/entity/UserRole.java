package Team_Mute.back_end.domain.member.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tb_user_roles")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRole {

	@Id
	@Column(name = "role_id")
	private Integer roleId;

	@Column(name = "role_name", length = 50, nullable = false)
	private String roleName;

	@CreationTimestamp
	@Column(name = "reg_date")
	private LocalDateTime regDate;

	@UpdateTimestamp
	@Column(name = "upd_date")
	private LocalDateTime updDate;

	public UserRole(String roleName) {
		this.roleName = roleName;
	}
}
