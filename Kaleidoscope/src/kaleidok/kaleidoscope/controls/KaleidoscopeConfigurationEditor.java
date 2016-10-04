package kaleidok.kaleidoscope.controls;

import javafx.beans.property.Property;
import javafx.beans.property.ReadOnlyProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.value.ObservableStringValue;
import javafx.scene.Node;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import kaleidok.javafx.beans.property.PropertyUtils;
import kaleidok.javafx.scene.control.cell.DynamicEditableTreeItem;
import kaleidok.javafx.scene.control.cell.DynamicEditableTreeItem.TreeItemProvider;
import kaleidok.javafx.scene.control.cell.EditableTreeTableCell;
import kaleidok.javafx.scene.control.cell.factory.SpinnerItemProvider;
import kaleidok.javafx.scene.control.cell.factory.MultiTreeItemProvider;
import kaleidok.kaleidoscope.Kaleidoscope;
import kaleidok.kaleidoscope.layer.ImageLayer;
import kaleidok.util.Arrays;
import kaleidok.util.function.InstanceSupplier;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
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
    propertyValueColumn.setCellValueFactory((cdf) -> {
        TreeItem<ReadOnlyProperty<Object>> item = cdf.getValue();
        return item.isLeaf() ? item.getValue() : null;
      });
    propertyValueColumn.setCellFactory(
      (col) -> new DebugDynamicEditableTreeCell());
    columns.add(propertyValueColumn);
  }


  // TODO: remove debugging code
  private static class DebugDynamicEditableTreeCell
    extends EditableTreeTableCell<Object, Node>
  {
    @Override
    public void startEdit()
    {
      logMethodCall("startEdit");
      super.startEdit();
    }


    @Override
    public void cancelEdit()
    {
      logMethodCall("cancelEdit");
      super.cancelEdit();
    }


    @Override
    public void updateItem( Object item, boolean empty )
    {
      logMethodCall("updateItem");
      System.out.format("empty: %s, item (%s): %s%n",
        empty, (item != null) ? item.getClass().getName() : null, item);

      super.updateItem(item, empty);

      /*
      if (getConverter() == null)
      {
        ReadOnlyProperty<?> property = getTreeTableRow().getItem();
        if (property instanceof StringConvertible)
        {
          //noinspection unchecked
          setConverter(
            ((StringConvertible<Object>) property).getStringConverter());
        }
      }
      */
    }


    private void logMethodCall( String methodName )
    {
      ReadOnlyProperty<?> property = getTreeTableRow().getItem();
      String beanClassName = "<null>", propertyName = "<null>";
      Object propertyValue = "N/A";
      if (property != null)
      {
        propertyName = property.getName();
        propertyValue = property.getValue();
        Object bean = property.getBean();
        if (bean != null)
          beanClassName = bean.getClass().getSimpleName();
      }

      System.out.format(
        "%s#%s() called on \"%s.%s\" - value: %s%n",
        this.getClass().getSimpleName(), methodName,
        beanClassName, propertyName,
        propertyValue);
    }
  }


  private void initItems()
  {
    TreeItem<ReadOnlyProperty<?>> root = new TreeItem<>(
      new ReadOnlyStringWrapper(null, "name", "Kaleidoscope")
        .getReadOnlyProperty());
    root.setExpanded(true);
    Set<Property<?>> propertySet = new HashSet<>();

    for (final ImageLayer layer: parent.getLayers())
    {
      TreeItem<ReadOnlyProperty<?>> layerRoot = new TreeItem<>(layer.name);
      layerRoot.setExpanded(true);

      propertySet.clear();
      propertySet = PropertyUtils.getProperties(layer, propertySet);
      propertySet.remove(layer.name);
      //noinspection ResultOfMethodCallIgnored,unchecked
      propertySet.stream()
        .map((p) -> (TreeItem<ReadOnlyProperty<?>>) (TreeItem<?>)
          new DynamicEditableTreeItem<>((ReadOnlyProperty<Object>) p, cellFactories))
        .sequential().collect(Collectors.toCollection(
          new InstanceSupplier<>(layerRoot.getChildren())));

      root.getChildren().add(layerRoot);
    }

    setShowRoot(false);
    //noinspection unchecked
    setRoot((TreeItem<ReadOnlyProperty<Object>>) (TreeItem<?>) root);
  }


  private static final MultiTreeItemProvider<Object, Node> cellFactories;

  static
  {
    List<TreeItemProvider<?, ?>> treeItemFactories =
      Arrays.asImmutableList(
        new SpinnerItemProvider.BoundedValueSpinnerItemProvider<>());

    //noinspection unchecked
    cellFactories = new MultiTreeItemProvider<>(
      (List<TreeItemProvider<Object, Node>>) (List<?>) treeItemFactories);
  }
}
