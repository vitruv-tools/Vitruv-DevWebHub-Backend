package tools.vitruv.methodologisttemplate.viewtype.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import tools.vitruv.change.atomic.EChange;
import tools.vitruv.change.atomic.hid.HierarchicalId;
import tools.vitruv.change.composite.description.VitruviusChange;
import tools.vitruv.change.composite.description.VitruviusChangeFactory;
import tools.vitruv.framework.views.impl.ModifiableView;
import tools.vitruv.methodologisttemplate.viewtype.ChangeTransformingViewType;

/** View type that applies registered filters to view changes before committing them. */
public class ChangeTransformingViewTypeImpl extends ChangeTransformingViewType {
  private List<Function<List<EChange<HierarchicalId>>, List<EChange<HierarchicalId>>>> filters;

  /** Creates a view type with the given name and no filters. */
  public ChangeTransformingViewTypeImpl(String name) {
    super(name);
    this.filters = new ArrayList<>();
  }

  @Override
  public void commitViewChanges(ModifiableView view, VitruviusChange<HierarchicalId> viewChange) {
    var modifiedChange = viewChange.copy().getEChanges();
    for (Function<List<EChange<HierarchicalId>>, List<EChange<HierarchicalId>>> filter : filters) {
      modifiedChange = filter.apply(modifiedChange);
    }
    super.commitViewChanges(
        view, VitruviusChangeFactory.getInstance().createTransactionalChange(modifiedChange));
  }

  /**
   * Registers a filter that transforms the changes of a view before they are committed.
   *
   * @param filter the filter to register
   * @return {@code true} if the filter was added
   */
  public boolean registerFilter(
      Function<List<EChange<HierarchicalId>>, List<EChange<HierarchicalId>>> filter) {
    return this.filters.add(filter);
  }

  @Override
  public boolean unregisterFilter(
      Function<List<EChange<HierarchicalId>>, List<EChange<HierarchicalId>>> filter) {
    return this.filters.remove(filter);
  }
}
