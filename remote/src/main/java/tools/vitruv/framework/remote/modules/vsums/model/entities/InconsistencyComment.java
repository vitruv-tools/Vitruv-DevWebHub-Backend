package tools.vitruv.framework.remote.modules.vsums.model.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tools.vitruv.framework.remote.common.entities.BaseEntity;

/** Persisted comment on an open inconsistency. */
@Entity
@Table(name = "inconsistency-comments")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class InconsistencyComment extends BaseEntity {

  private UUID inconsistencyId;

  private String author;

  @Column(columnDefinition = "TEXT")
  private String body;

  private Instant createdAt;
}
