package kaleidok.kaleidoscope.controls;

import javafx.beans.property.Property;
import javafx.beans.property.ReadOnlyProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.value.ObservableStringValue;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import kaleidok.javafx.beans.property.PropertyUtils;
import kaleidok.kaleidoscope.Kaleidoscope;
import kaleidok.kaleidoscope.layer.ImageLayer;
import kaleidok.util.function.InstanceSupplier;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;


public class KaleidoscopeConfigurationEditor
  extends TreeTableView<ReadOnlyProperty<?>>
{
  protected final Kaleidoscope parent;


  public KaleidoscopeConfigurationEditor( Kaleidoscope parent )
  {
    this.parent = Objects.requireNonNull(parent);
  }


  public void init()
  {
    initColumns();
    initItems();
  }


  private void initColumns()
  {
    List<TreeTableColumn<ReadOnlyProperty<?>, ?>> columns = getColumns();

    TreeTableColumn<ReadOnlyProperty<?>, String> propertyNameColumn =
      new TreeTableColumn<>("Property");
    propertyNameColumn.setCellValueFactory((cdf) -> {
        TreeItem<ReadOnlyProperty<?>> item = cdf.getValue();
        ReadOnlyProperty<?> p = item.getValue();
        //noinspection OverlyStrongTypeCast
        return
          (!item.getChildren().isEmpty() &&
            p instanceof ObservableStringValue &&
            "name".equals(p.getName()))
          ?
            (ObservableStringValue) p :
            new ReadOnlyStringWrapper(
              p.getBean(), "property name", p.getName());
      });
    columns.add(propertyNameColumn);

    TreeTableColumn<ReadOnlyProperty<?>, Object> propertyValueColumn =
      new TreeTableColumn<>("Value");
    propertyValueColumn.setCellValueFactory((cdf) -> {
        TreeItem<ReadOnlyProperty<?>> item = cdf.getValue();
        //noinspection unchecked
        return item.getChildren().isEmpty() ?
          (ObservableValue<Object>) item.getValue() :
          null;
      });
    columns.add(propertyValueColumn);
  }


  private void initItems()
  {
    TreeItem<ReadOnlyProperty<?>> root = new TreeItem<>(
      new ReadOnlyStringWrapper(null, "name", "Kaleidoscope"));
    root.setExpanded(true);
    Set<Property<?>> propertySet = new HashSet<>();

    for (final ImageLayer layer: parent.getLayers())
    {
      TreeItem<ReadOnlyProperty<?>> layerRoot = new TreeItem<>(layer.name);
      layerRoot.setExpanded(true);

      propertySet.clear();
      propertySet = PropertyUtils.getProperties(layer, propertySet);
      propertySet.remove(layer.name);
      //noinspection ResultOfMethodCallIgnored
      propertySet.stream()
        .map((Function<Property<?>, TreeItem<ReadOnlyProperty<?>>>) TreeItem::new)
        .collect(Collectors.toCollection(
          new InstanceSupplier<>(layerRoot.getChildren())));

      root.getChildren().add(layerRoot);
    }

    setRoot(root);
  }
}
