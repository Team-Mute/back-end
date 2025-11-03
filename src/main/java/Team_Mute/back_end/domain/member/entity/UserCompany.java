package Team_Mute.back_end.domain.member.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "tb_user_company")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserCompany {

	@Id
	@Column(name = "company_id")
	private Integer companyId;

	@Column(name = "company_name", length = 50, nullable = false)
	private String companyName;

	@CreationTimestamp
	@Column(name = "reg_date", nullable = false)
	private LocalDateTime regDate;

	@UpdateTimestamp
	@Column(name = "upd_date")
	private LocalDateTime updDate;

	@OneToMany(mappedBy = "userCompany", fetch = FetchType.LAZY)
	private List<User> users;

	@OneToMany(mappedBy = "userCompany", fetch = FetchType.LAZY)
	private List<Admin> admin;
}
