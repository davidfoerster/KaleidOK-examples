package kaleidok.kaleidoscope.controls;

import javafx.beans.property.ReadOnlyProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.value.ObservableStringValue;
import javafx.scene.Node;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import kaleidok.javafx.beans.property.adapter.preference.PreferenceBean;
import kaleidok.javafx.beans.property.aspect.LevelOfDetailTag.DefaultLevelOfDetailComparator;
import kaleidok.javafx.scene.control.cell.DynamicEditableTreeItem;
import kaleidok.javafx.scene.control.cell.DynamicEditableTreeItem.TreeItemProvider;
import kaleidok.javafx.scene.control.cell.EditableTreeTableCell;
import kaleidok.javafx.scene.control.cell.provider.SpinnerItemProvider;
import kaleidok.javafx.scene.control.cell.provider.MultiTreeItemProvider;
import kaleidok.javafx.scene.control.cell.provider.TextFieldItemProvider;
import kaleidok.util.Arrays;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class KaleidoscopeConfigurationEditor
  extends TreeTableView<ReadOnlyProperty<Object>>
{
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
    TreeItem<ReadOnlyProperty<?>> root = new TreeItem<>(
      new ReadOnlyStringWrapper(null, "name", "KaleidOK")
        .getReadOnlyProperty());
    root.setExpanded(true);
    setShowRoot(false);
    //noinspection unchecked
    setRoot((TreeItem<ReadOnlyProperty<Object>>) (TreeItem<?>) root);
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


  private static ReadOnlyProperty<String> makeSectionRoot( Object bean,
    String name )
  {
    return new ReadOnlyStringWrapper(bean, "section name",
      Objects.requireNonNull(name)).getReadOnlyProperty();
  }


  public static TreeItem<ReadOnlyProperty<?>> makeSubtree2( Object rootBean,
    String rootName, Stream<? extends PreferenceBean> beans )
  {
    return makeSubtree2(makeSectionRoot(rootBean, rootName), beans);
  }


  public static TreeItem<ReadOnlyProperty<?>> makeSubtree2(
    ReadOnlyProperty<String> rootProperty,
    Stream<? extends PreferenceBean> beans )
  {
    TreeItem<ReadOnlyProperty<?>> subtreeRoot = new TreeItem<>(rootProperty);

    subtreeRoot.getChildren().addAll(
      beans.map(KaleidoscopeConfigurationEditor::makeSubtree)
        .collect(Collectors.toList()));

    subtreeRoot.setExpanded(true);
    return subtreeRoot;
  }


  public static TreeItem<ReadOnlyProperty<?>> makeSubtree(
    PreferenceBean bean )
  {
    return makeSubtree(bean, bean.getName(),
      bean.getPreferenceAdapters().map((pa) -> pa.property));
  }


  public static TreeItem<ReadOnlyProperty<?>> makeSubtree( Object rootBean,
    String rootName, Stream<? extends ReadOnlyProperty<?>> childProperties )
  {
    return makeSubtree(makeSectionRoot(rootBean, rootName), childProperties);
  }


  public static TreeItem<ReadOnlyProperty<?>> makeSubtree(
    ReadOnlyProperty<String> rootProperty,
    Stream<? extends ReadOnlyProperty<?>> childProperties )
  {
    TreeItem<ReadOnlyProperty<?>> subtreeRoot = new TreeItem<>(rootProperty);

    //noinspection unchecked,OverlyStrongTypeCast,RedundantCast
    subtreeRoot.getChildren().addAll(
      (List<? extends TreeItem<ReadOnlyProperty<?>>>) (List<? extends TreeItem<?>>)
        childProperties
          .filter((p) -> !p.getName().isEmpty())
          .sorted(
            new DefaultLevelOfDetailComparator<ReadOnlyProperty<?>>(0)
              .thenComparing(Comparator.comparing(ReadOnlyProperty::getName)))
          .map(MyTreeItemProvider.INSTANCE)
          .collect(Collectors.toList()));

    subtreeRoot.setExpanded(true);
    return subtreeRoot;
  }
}
