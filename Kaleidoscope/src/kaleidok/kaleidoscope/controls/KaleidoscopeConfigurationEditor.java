package kaleidok.kaleidoscope.controls;

import javafx.beans.property.ReadOnlyProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.value.ObservableStringValue;
import javafx.scene.Node;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import kaleidok.javafx.beans.property.adapter.preference.PreferenceBean;
import kaleidok.javafx.beans.property.adapter.preference.ReadOnlyPropertyPreferencesAdapter;
import kaleidok.javafx.beans.property.aspect.LevelOfDetailTag.DefaultLevelOfDetailComparator;
import kaleidok.javafx.scene.control.cell.DynamicEditableTreeItem;
import kaleidok.javafx.scene.control.cell.DynamicEditableTreeItem.TreeItemProvider;
import kaleidok.javafx.scene.control.cell.EditableTreeTableCell;
import kaleidok.javafx.scene.control.cell.provider.FormattedTextFieldItemProvider;
import kaleidok.javafx.scene.control.cell.provider.SpinnerItemProvider;
import kaleidok.javafx.scene.control.cell.provider.MultiTreeItemProvider;
import kaleidok.javafx.scene.control.cell.provider.TextFieldItemProvider;
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


  public void init()
  {
    setEditable(true);
    initColumns();
    initRoot();
  }


  private void initColumns()
  {
    List<TreeTableColumn<ReadOnlyProperty<Object>, ?>> columns = getColumns();

    TreeTableColumn<ReadOnlyProperty<Object>, String> propertyNameColumn =
      new TreeTableColumn<>("Property");
    propertyNameColumn.setEditable(false);
    propertyNameColumn.setCellValueFactory((cdf) -> {
        TreeItem<ReadOnlyProperty<Object>> item = cdf.getValue();
        ReadOnlyProperty<?> p = item.getValue();
        //noinspection OverlyStrongTypeCast
        return (!item.isLeaf() && p instanceof ObservableStringValue) ?
          (ObservableStringValue) p :
          new ReadOnlyStringWrapper(
            p.getBean(), "property name", p.getName()).getReadOnlyProperty();
      });
    columns.add(propertyNameColumn);

    TreeTableColumn<ReadOnlyProperty<Object>, Object> propertyValueColumn =
      new TreeTableColumn<>("Value");
    propertyValueColumn.setEditable(true);
    propertyValueColumn.setPrefWidth(125);
    propertyValueColumn.setCellValueFactory((cdf) -> {
        TreeItem<ReadOnlyProperty<Object>> item = cdf.getValue();
        return item.isLeaf() ? item.getValue() : null;
      });
    propertyValueColumn.setCellFactory(
      (col) -> new EditableTreeTableCell<>());
    columns.add(propertyValueColumn);
  }


  private void initRoot()
  {
    TreeItem<ReadOnlyProperty<Object>> root = new TreeItem<>(
      makeSectionRootProperty("root name", null, "KaleidOK"));
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


  @SuppressWarnings("unchecked")
  private static ReadOnlyProperty<Object> makeSectionRootProperty(
    String propertyName, Object bean, String beanName )
  {
    ReadOnlyStringWrapper property =
      new ReadOnlyStringWrapper(
        bean, propertyName, Objects.requireNonNull(beanName));
    return (ReadOnlyProperty<Object>) (ReadOnlyProperty<?>)
      property.getReadOnlyProperty();
  }


  private static ReadOnlyProperty<Object> makeSectionRootProperty( Object bean,
    String beanName )
  {
    String propertyName = (bean != null) ? "bean name" : "section name";
    return makeSectionRootProperty(propertyName, bean, beanName);
  }


  private static ReadOnlyProperty<Object> makeSectionRootProperty( Object bean )
  {
    String beanName =
      (bean == null) ?
        "<null>" :
      (bean instanceof PreferenceBean) ?
        ((PreferenceBean) bean).getName() :
        kaleidok.util.Objects.objectToString(bean);
    return makeSectionRootProperty(bean, beanName);
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
    Comparator<TreeItem<ReadOnlyProperty<Object>>> treeItemComparator =
      Comparator.comparing(TreeItem::getValue,
        new DefaultLevelOfDetailComparator<ReadOnlyProperty<Object>>(0)
          .thenComparing(Comparator.comparing(ReadOnlyProperty::getName)));

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
            beanItem = new TreeItem<>(makeSectionRootProperty(bean));
            beanItem.setExpanded(true);
            parentItem.add(beanItem);
          }
        }
      }

      if (beanItem != null)
      {
        List<TreeItem<ReadOnlyProperty<Object>>> beanItemChildren =
          beanItem.getChildren();
        beanItemChildren.addAll(e.getValue());
        beanItemChildren.sort(treeItemComparator);

        it.remove();
      }
    }

    TreeItem<ReadOnlyProperty<Object>> anchor =
      (defaultAnchor != null) ?
        this.beansToItemsMap.get(defaultAnchor) :
        getRoot();
    if (anchor == null)
    {
      anchor = new TreeItem<>(makeSectionRootProperty(defaultAnchor));
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
        new TreeItem<>(makeSectionRootProperty(bean));
      beanItem.setExpanded(true);
      List<TreeItem<ReadOnlyProperty<Object>>> beanItemChildren =
        beanItem.getChildren();
      beanItemChildren.addAll(e.getValue());
      beanItemChildren.sort(treeItemComparator);

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
      sectionItem = new TreeItem<>(makeSectionRootProperty(bean, name));
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
}
