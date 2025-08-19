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
@Table(name = "tb_user_company")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserCompany implements Persistable<Integer> {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "company_id")
	private Integer companyId;

	@Column(name = "company_name", length = 50)
	private String companyName;

	@CreationTimestamp
	@Column(name = "reg_date")
	private LocalDateTime regDate;

	@UpdateTimestamp
	@Column(name = "upd_date")
	private LocalDateTime updDate;

	@OneToMany(mappedBy = "userCompany", fetch = FetchType.LAZY)
	private List<User> users;

	@Transient
	private boolean isNew = true;

	@Override
	public Integer getId() {
		return companyId;
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
}
