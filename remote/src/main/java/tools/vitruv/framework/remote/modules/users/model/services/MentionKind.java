package tools.vitruv.framework.remote.modules.users.model.services;

/**
 * What an {@code @tag} in a comment points at.
 */
public enum MentionKind {
  /** A single user, written as {@code @username}. */
  USER,
  /** Everyone who knows a metamodel, written as {@code @metamodel}. */
  METAMODEL
}
