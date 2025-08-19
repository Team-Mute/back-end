package Team_Mute.back_end.domain.member.entity;

import java.time.LocalDateTime;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.domain.Persistable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
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
public class UserRole implements Persistable<Integer> {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
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

	@OneToMany(mappedBy = "userRole", fetch = FetchType.LAZY)
	private List<User> users;

	@Transient // DB에 저장되지 않도록 설정
	private boolean isNew = true; // 새로 생성된 객체는 기본적으로 isNew=true

	@Override
	public Integer getId() {
		return roleId;
	}

	@Override
	public boolean isNew() {
		return isNew;
	}

	@PrePersist
	@PostLoad
	void markNotNew() {
		this.isNew = false;
	}

	public UserRole(String roleName) {
		this.roleName = roleName;
	}
}
