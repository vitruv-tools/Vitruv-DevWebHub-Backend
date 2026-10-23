package tools.vitruv.framework.remote.modules.vsums.model.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tools.vitruv.framework.remote.common.entities.BaseEntity;
import tools.vitruv.framework.remote.modules.users.model.services.MentionKind;

import java.util.UUID;

/**
 * One {@code @tag} found in a comment when it was saved: a user or a metamodel.
 * Metamodel tags are expanded to users when read, so later changes to who knows
 * a metamodel are reflected.
 */
@Entity
@Table(name = "inconsistency-comment-mentions")
@NoArgsConstructor
@Getter
@Setter
public class InconsistencyCommentMention extends BaseEntity {

  private UUID commentId;

  @Enumerated(EnumType.STRING)
  private MentionKind kind;

  /** Canonical username for {@link MentionKind#USER}, metamodel name for {@link MentionKind#METAMODEL}. */
  private String targetName;

  /**
   * First-appearance order in the comment body, starting at 0.
   * GET uses this so {@code @alice} then {@code @model} stays in that order.
   * Default 0 lets Hibernate add the column when older mention rows already exist.
   */
  @Column(nullable = false, columnDefinition = "integer not null default 0")
  private int position;
}
