package kaleidok.kaleidoscope.controls;

import javafx.beans.property.Property;
import javafx.beans.property.ReadOnlyProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.value.ObservableStringValue;
import javafx.scene.Node;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import kaleidok.javafx.beans.property.LevelOfDetail.DefaultLevelOfDetailComparator;
import kaleidok.javafx.beans.property.PropertyUtils;
import kaleidok.javafx.scene.control.cell.DynamicEditableTreeItem;
import kaleidok.javafx.scene.control.cell.DynamicEditableTreeItem.TreeItemProvider;
import kaleidok.javafx.scene.control.cell.EditableTreeTableCell;
import kaleidok.javafx.scene.control.cell.provider.SpinnerItemProvider;
import kaleidok.javafx.scene.control.cell.provider.MultiTreeItemProvider;
import kaleidok.kaleidoscope.Kaleidoscope;
import kaleidok.kaleidoscope.layer.ImageLayer;
import kaleidok.util.Arrays;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;


public class KaleidoscopeConfigurationEditor
  extends TreeTableView<ReadOnlyProperty<Object>>
{
  protected final Kaleidoscope parent;


  public KaleidoscopeConfigurationEditor( Kaleidoscope parent )
  {
    this.parent = Objects.requireNonNull(parent);
  }


  public void init()
  {
    setEditable(true);
    initColumns();
    initItems();
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
        return
          (!item.isLeaf() &&
            p instanceof ObservableStringValue &&
            "name".equals(p.getName()))
          ?
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


  private void initItems()
  {
    TreeItem<ReadOnlyProperty<?>> root = new TreeItem<>(
      new ReadOnlyStringWrapper(null, "name", "Kaleidoscope")
        .getReadOnlyProperty());

    root.getChildren().addAll(
      parent.getLayers().stream()
        .map(KaleidoscopeConfigurationEditor::makeLayerItem)
        .collect(Collectors.toList()));

    root.setExpanded(true);
    setShowRoot(false);
    //noinspection unchecked
    setRoot((TreeItem<ReadOnlyProperty<Object>>) (TreeItem<?>) root);
  }


  private static TreeItem<ReadOnlyProperty<?>> makeLayerItem(
    final ImageLayer layer )
  {
    TreeItem<ReadOnlyProperty<?>> layerRoot = new TreeItem<>(
      new ReadOnlyStringWrapper(layer, "name", layer.getName())
        .getReadOnlyProperty());

    //noinspection unchecked,OverlyStrongTypeCast,RedundantCast
    layerRoot.getChildren().addAll(
      (List<? extends TreeItem<ReadOnlyProperty<?>>>) (List<? extends TreeItem<?>>)
        PropertyUtils.getProperties(layer)
          .filter((p) -> !p.getName().isEmpty())
          .sorted(
            new DefaultLevelOfDetailComparator<Property<?>>(0)
              .thenComparing(Comparator.comparing(Property::getName)))
          .map(MyTreeItemProvider.INSTANCE)
          .collect(Collectors.toList()));

    layerRoot.setExpanded(true);
    return layerRoot;
  }


  private static final class MyTreeItemProvider
    extends MultiTreeItemProvider<Object, Node>
    implements Function<Property<?>, TreeItem<ReadOnlyProperty<Object>>>
  {
    public static final MyTreeItemProvider INSTANCE = new MyTreeItemProvider();


    private MyTreeItemProvider()
    {
      //noinspection unchecked,RedundantCast
      super(
        (List<? extends TreeItemProvider<Object, Node>>) (List<? extends TreeItemProvider<?,?>>)
        Arrays.asImmutableList(
          new SpinnerItemProvider.BoundedValueSpinnerItemProvider<>()));
    }


    @Override
    public TreeItem<ReadOnlyProperty<Object>> apply( Property<?> property )
    {
      //noinspection unchecked
      return new DynamicEditableTreeItem<>(
        (ReadOnlyProperty<Object>) property, this);
    }
  }
}
