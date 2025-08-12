package mute.backend.domain.spaceadmin.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tb_space_tag_map")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SpaceTagMap {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "map_id")
  private Integer id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(
      name = "space_id",
      nullable = false,
      foreignKey = @ForeignKey(name = "fk_space_tag_map_space"))
  @org.hibernate.annotations.OnDelete(
      action = org.hibernate.annotations.OnDeleteAction.CASCADE) // (선택)
  private Space space;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "tag_id")
  private SpaceTag tag;

  @Column(name = "reg_date")
  private LocalDateTime regDate;
}
