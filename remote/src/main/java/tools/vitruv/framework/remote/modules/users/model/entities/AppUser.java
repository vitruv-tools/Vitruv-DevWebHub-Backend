package tools.vitruv.framework.remote.modules.users.model.entities;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tools.vitruv.framework.remote.common.entities.BaseEntity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * A person who signs in to the hub. Created on first sign-in.
 * Metamodel knowledge lives in {@link UserMetamodel}, one row per metamodel.
 */
@Entity
@Table(name = "users")
@NoArgsConstructor
@Getter
@Setter
public class AppUser extends BaseEntity {

  @Column(nullable = false, unique = true)
  private String username;

  private String displayName;

  private String email;

  private Instant createdAt;

  private Instant lastLoginAt;

  /** SHA-256 hex of the secret returned once when this profile is claimed. */
  @Column(length = 64)
  private String profileTokenHash;

  @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<UserMetamodel> metamodels = new ArrayList<>();
}
