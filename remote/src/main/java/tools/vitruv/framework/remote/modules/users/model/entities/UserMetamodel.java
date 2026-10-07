package tools.vitruv.framework.remote.modules.users.model.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tools.vitruv.framework.remote.common.entities.BaseEntity;

/**
 * One metamodel a user knows, such as {@code amalthea} or {@code ascet}.
 */
@Entity
@Table(
    name = "user-metamodels",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "metamodel_name"})
)
@NoArgsConstructor
@Getter
@Setter
public class UserMetamodel extends BaseEntity {

  @ManyToOne(optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private AppUser user;

  private String metamodelName;
}
