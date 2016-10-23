package kaleidok.kaleidoscope.controls;

import javafx.beans.property.ReadOnlyProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.value.ObservableStringValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeSortMode;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import kaleidok.javafx.beans.property.SimpleReadOnlyStringProperty;
import kaleidok.javafx.beans.property.adapter.preference.PreferenceBean;
import kaleidok.javafx.beans.property.adapter.preference.ReadOnlyPropertyPreferencesAdapter;
import kaleidok.javafx.beans.property.aspect.LevelOfDetailTag.DefaultLevelOfDetailComparator;
import kaleidok.javafx.scene.control.cell.DynamicEditableTreeItem;
import kaleidok.javafx.scene.control.cell.DynamicEditableTreeItem.TreeItemProvider;
import kaleidok.javafx.scene.control.cell.EditableTreeTableCell;
import kaleidok.javafx.scene.control.cell.provider.*;
import kaleidok.util.Arrays;

import java.util.Collection;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class KaleidoscopeConfigurationEditor
  extends TreeTableView<ReadOnlyProperty<Object>>
{
  private final Map<Object, TreeItem<ReadOnlyProperty<Object>>> beansToItemsMap =
    new IdentityHashMap<>();


  {
    setSortPolicy((ttv) ->
      ((KaleidoscopeConfigurationEditor) ttv).sortPolicyImpl());

    setEditable(true);
    initColumns();
    initRoot();
  }


  private void initColumns()
  {
    List<TreeTableColumn<ReadOnlyProperty<Object>, ?>> columns = getColumns();

    TreeTableColumn<ReadOnlyProperty<Object>, String> nameCol =
      new TreeTableColumn<>("Property");
    nameCol.setEditable(false);
    nameCol.setSortable(true);
    nameCol.setMaxWidth(200);
    nameCol.setCellValueFactory((cdf) -> {
        TreeItem<ReadOnlyProperty<Object>> item = cdf.getValue();
        ReadOnlyProperty<?> p = item.getValue();
        //noinspection OverlyStrongTypeCast
        return (!item.isLeaf() && p instanceof ObservableStringValue) ?
          (ObservableStringValue) p :
          makeSectionRootProperty("property name", p.getBean(), p.getName());
      });
    columns.add(nameCol);

    TreeTableColumn<ReadOnlyProperty<Object>, Object> valueCol =
      new TreeTableColumn<>("Value");
    valueCol.setEditable(true);
    valueCol.setSortable(false);
    valueCol.setPrefWidth(125);
    valueCol.setCellValueFactory((cdf) -> {
        TreeItem<ReadOnlyProperty<Object>> item = cdf.getValue();
        return item.isLeaf() ? item.getValue() : null;
      });
    valueCol.setCellFactory(
      (col) -> new EditableTreeTableCell<>());
    columns.add(valueCol);

    setSortMode(TreeSortMode.ALL_DESCENDANTS);
    setPrefWidth(nameCol.getMaxWidth() + valueCol.getPrefWidth());
  }


  private void initRoot()
  {
    TreeItem<ReadOnlyProperty<Object>> root = new TreeItem<>(
      makeSectionRootProperty2("root name", null, "KaleidOK"));
    root.setExpanded(true);
    setShowRoot(false);

    beansToItemsMap.put(root.getValue().getBean(), root);
    setRoot(root);
  }


  private static final class MyTreeItemProvider
    extends MultiTreeItemProvider<Object, Node>
    implements Function<ReadOnlyProperty<?>, TreeItem<ReadOnlyProperty<Object>>>
  {
    public static final MyTreeItemProvider INSTANCE = new MyTreeItemProvider();


    private MyTreeItemProvider()
    {
      //noinspection unchecked,RedundantCast
      super(
        (List<? extends TreeItemProvider<Object, Node>>) (List<? extends TreeItemProvider<?,?>>)
        Arrays.asImmutableList(
          new CheckBoxTreeItemProvider(),
          new SpinnerItemProvider.BoundedValueSpinnerItemProvider<>(),
          new FormattedTextFieldItemProvider<>(),
          new TextFieldItemProvider()));
    }


    @Override
    public TreeItem<ReadOnlyProperty<Object>> apply( ReadOnlyProperty<?> property )
    {
      //noinspection unchecked
      return new DynamicEditableTreeItem<>(
        (ReadOnlyProperty<Object>) property, this);
    }
  }


  private static ReadOnlyStringProperty makeSectionRootProperty(
    String propertyName, Object bean, String beanName )
  {
    return new SimpleReadOnlyStringProperty(
      bean, propertyName, Objects.requireNonNull(beanName));
  }


  @SuppressWarnings("unchecked")
  private static ReadOnlyProperty<Object> makeSectionRootProperty2(
    String propertyName, Object bean, String beanName )
  {
    return (ReadOnlyProperty<Object>) (ReadOnlyProperty<?>)
      makeSectionRootProperty(propertyName, bean, beanName);
  }


  private static ReadOnlyProperty<Object> makeSectionRootProperty2( Object bean,
    String beanName )
  {
    String propertyName = (bean != null) ? "bean name" : "section name";
    return makeSectionRootProperty2(propertyName, bean, beanName);
  }


  private static ReadOnlyProperty<Object> makeSectionRootProperty2( Object bean )
  {
    String beanName =
      (bean == null) ?
        "<null>" :
      (bean instanceof PreferenceBean) ?
        ((PreferenceBean) bean).getName() :
        kaleidok.util.Objects.objectToString(bean);
    return makeSectionRootProperty2(bean, beanName);
  }


  public void addBean( PreferenceBean bean )
  {
    addProperties(bean.getPreferenceAdapters().map((pa) -> pa.property), bean);
  }


  public void addBeans( Stream<? extends PreferenceBean> beans, Object defaultAnchor )
  {
    addPreferencesAdapters(
      beans.flatMap(PreferenceBean::getPreferenceAdapters), defaultAnchor);
  }


  public void addPreferencesAdapters(
    Stream<? extends ReadOnlyPropertyPreferencesAdapter<?,?>> childProperties,
    Object defaultAnchor )
  {
    addProperties(childProperties.map(pa -> pa.property), defaultAnchor);
  }


  public void addProperties(
    Stream<? extends ReadOnlyProperty<?>> childProperties,
    Object defaultAnchor )
  {
    Map<Object, ? extends Collection<TreeItem<ReadOnlyProperty<Object>>>> beansToItemsMap =
      childProperties.collect(Collectors.groupingBy(
        ReadOnlyProperty::getBean, IdentityHashMap::new,
        Collectors.mapping(MyTreeItemProvider.INSTANCE, Collectors.toList())));

    // Find item for each bean and attach property items to it.
    // If no existing bean item can be found create a new one if there's already a known parent.
    for (
      Iterator<? extends Map.Entry<Object, ? extends Collection<TreeItem<ReadOnlyProperty<Object>>>>> it =
        beansToItemsMap.entrySet().iterator();
      it.hasNext(); )
    {
      Map.Entry<Object, ? extends Collection<TreeItem<ReadOnlyProperty<Object>>>> e = it.next();
      Object bean = e.getKey();
      TreeItem<ReadOnlyProperty<Object>> beanItem = this.beansToItemsMap.get(bean);

      if (beanItem == null && bean instanceof PreferenceBean)
      {
        Object parent = ((PreferenceBean) bean).getParent();
        if (parent != null)
        {
          // Find parent among currently processed beans
          Collection<TreeItem<ReadOnlyProperty<Object>>> parentItem =
            beansToItemsMap.get(parent);

          if (parentItem == null)
          {
            // Find parent among previously known beans
            TreeItem<ReadOnlyProperty<Object>> parentItem2 =
              this.beansToItemsMap.get(parent);
            if (parentItem2 != null)
              parentItem = parentItem2.getChildren();
          }

          if (parentItem != null)
          {
            beanItem = new TreeItem<>(makeSectionRootProperty2(bean));
            beanItem.setExpanded(true);
            parentItem.add(beanItem);
          }
        }
      }

      if (beanItem != null)
      {
        beanItem.getChildren().addAll(e.getValue());
        it.remove();
      }
    }

    TreeItem<ReadOnlyProperty<Object>> anchor =
      (defaultAnchor != null) ?
        this.beansToItemsMap.get(defaultAnchor) :
        getRoot();
    if (anchor == null)
    {
      anchor = new TreeItem<>(makeSectionRootProperty2(defaultAnchor));
      anchor.setExpanded(true);
      TreeItem<ReadOnlyProperty<Object>> superAnchor =
        (defaultAnchor instanceof PreferenceBean) ?
          this.beansToItemsMap.getOrDefault(
            ((PreferenceBean) defaultAnchor).getParent(), getRoot()) :
          getRoot();
      superAnchor.getChildren().add(anchor);
    }
    List<TreeItem<ReadOnlyProperty<Object>>> anchorChildren =
      anchor.getChildren();

    // Find item for each bean and attach property items to it.
    // These are the remaining beans that have no parent among the currently processed or the previously known beans.
    // We'll put them underneath the root item.
    for (Map.Entry<Object, ? extends Collection<TreeItem<ReadOnlyProperty<Object>>>> e:
      beansToItemsMap.entrySet())
    {
      Object bean = e.getKey();
      TreeItem<ReadOnlyProperty<Object>> beanItem =
        new TreeItem<>(makeSectionRootProperty2(bean));
      beanItem.setExpanded(true);
      beanItem.getChildren().addAll(e.getValue());

      this.beansToItemsMap.put(bean, beanItem);
      anchorChildren.add(beanItem);
    }
  }


  public TreeItem<ReadOnlyProperty<Object>> addSection( Object bean,
    String name, Object anchorBean )
  {
    TreeItem<ReadOnlyProperty<Object>> sectionItem =
      beansToItemsMap.get(Objects.requireNonNull(bean));
    if (sectionItem == null)
    {
      sectionItem = new TreeItem<>(makeSectionRootProperty2(bean, name));
      sectionItem.setExpanded(true);
      TreeItem<ReadOnlyProperty<Object>> anchor =
        (anchorBean != null) ?
          beansToItemsMap.getOrDefault(anchorBean, getRoot()) :
          getRoot();
      anchor.getChildren().add(sectionItem);
      beansToItemsMap.put(bean, sectionItem);
    }
    return sectionItem;
  }


  private static final Comparator<? super TreeItem<? extends ReadOnlyProperty<?>>> DEFAULT_COMPARATOR;

  static
  {
    //noinspection OverlyStrongTypeCast
    final Comparator<ReadOnlyProperty<?>>
      lodComparator = new DefaultLevelOfDetailComparator<>(0),

      innerComparator = lodComparator
        .thenComparing(Comparator.nullsFirst(Comparator.comparing(
          (ReadOnlyProperty<?> p) ->
            (p.getBean() != null) ? p.getBean().getClass().getName() : null)))
        .thenComparing(Comparator.nullsLast(Comparator.comparing((p) ->
          (p instanceof ObservableStringValue) ?
            ((ObservableStringValue) p).get() :
            null))),

      leafComparator = lodComparator.thenComparing(ReadOnlyProperty::getName);

    DEFAULT_COMPARATOR = (o1, o2) -> {
        int r = Boolean.compare(o1.isLeaf(), o2.isLeaf());
        return (r != 0) ? -r :
          (o1.isLeaf() ? leafComparator : innerComparator)
            .compare(o1.getValue(), o2.getValue());
      };
  }


  private boolean sortPolicyImpl()
  {
    TreeItem<ReadOnlyProperty<Object>> root = getRoot();
    if (root == null)
      return false;

    TreeSortMode sortMode = getSortMode();
    if (sortMode == null)
      return false;

    List<TreeTableColumn<ReadOnlyProperty<Object>, ?>> sortOrder =
      getSortOrder();
    if (sortOrder.size() != 1 || sortOrder.get(0) != getColumns().get(0))
      return false;

    TreeTableColumn.SortType sortType = sortOrder.get(0).getSortType();
    if (sortType == null)
      return false;

    Comparator<? super TreeItem<? extends ReadOnlyProperty<?>>> comparator =
      DEFAULT_COMPARATOR;
    if (sortType == TreeTableColumn.SortType.DESCENDING)
      comparator = comparator.reversed();

    switch (sortMode)
    {
    case ALL_DESCENDANTS:
      sort(root, comparator);
      break;

    case ONLY_FIRST_LEVEL:
      FXCollections.sort(root.getChildren(), comparator);
      break;
    }

    return true;
  }


  private static void sort( TreeItem<? extends ReadOnlyProperty<?>> item,
    Comparator<? super TreeItem<? extends ReadOnlyProperty<?>>> comparator )
  {
    ObservableList<? extends TreeItem<? extends ReadOnlyProperty<?>>> children =
      item.getChildren();
    FXCollections.sort(children, comparator);

    for (TreeItem<? extends ReadOnlyProperty<?>> subItem: children)
      sort(subItem, comparator);
  }
}
