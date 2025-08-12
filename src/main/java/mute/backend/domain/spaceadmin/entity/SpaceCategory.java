package mute.backend.domain.spaceadmin.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tb_space_categories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SpaceCategory {

  @Id
  @Column(name = "category_id")
  private Integer id;

  @Column(name = "category_name", nullable = false, length = 50)
  private String categoryName;

  @Column(name = "reg_date")
  private LocalDateTime regDate;

  @Column(name = "upd_date")
  private LocalDateTime updDate;
}
