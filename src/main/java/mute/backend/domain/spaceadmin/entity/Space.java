package mute.backend.domain.spaceadmin.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "tb_spaces")
public class Space {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "space_id")
  private Integer spaceId;

  @Column(name = "region_id")
  private Integer regionId;

  @Column(name = "category_id")
  private Integer categoryId;

  @Column(name = "user_id")
  private Integer userId;

  @Column(name = "space_name", unique = true, nullable = false, length = 100)
  private String spaceName;

  @Column(name = "space_capacity", nullable = false)
  private Integer spaceCapacity;

  @Column(name = "space_location", nullable = false, length = 256)
  private String spaceLocation;

  @Column(name = "space_description", nullable = false, columnDefinition = "TEXT")
  private String spaceDescription;

  @Column(name = "space_image_url", nullable = false, columnDefinition = "TEXT")
  private String spaceImageUrl;

  @Column(name = "space_is_available", nullable = false)
  private Boolean spaceIsAvailable;

  @Column(name = "reg_date", nullable = false)
  private LocalDateTime regDate;

  @Column(name = "upd_date")
  private LocalDateTime updDate;

  @OneToMany(mappedBy = "space", cascade = CascadeType.REMOVE, orphanRemoval = true)
  @Builder.Default
  private List<SpaceImage> images = new ArrayList<>();

  @OneToMany(mappedBy = "space", cascade = CascadeType.REMOVE, orphanRemoval = true)
  @Builder.Default
  private List<SpaceTagMap> tagMaps = new ArrayList<>();
}
