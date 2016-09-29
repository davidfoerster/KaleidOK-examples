package kaleidok.kaleidoscope.controls;

import javafx.beans.property.Property;
import javafx.beans.property.ReadOnlyProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.value.ObservableStringValue;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.scene.control.cell.TextFieldTreeTableCell;
import kaleidok.javafx.beans.property.PropertyUtils;
import kaleidok.javafx.util.converter.StringConvertible;
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
    setEditable(true);
    initColumns();
    initItems();
  }


  private void initColumns()
  {
    List<TreeTableColumn<ReadOnlyProperty<?>, ?>> columns = getColumns();

    TreeTableColumn<ReadOnlyProperty<?>, String> propertyNameColumn =
      new TreeTableColumn<>("Property");
    propertyNameColumn.setEditable(false);
    propertyNameColumn.setCellValueFactory((cdf) -> {
        TreeItem<ReadOnlyProperty<?>> item = cdf.getValue();
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

    TreeTableColumn<ReadOnlyProperty<?>, Object> propertyValueColumn =
      new TreeTableColumn<>("Value");
    propertyValueColumn.setEditable(true);
    propertyValueColumn.setCellValueFactory((cdf) -> {
        TreeItem<ReadOnlyProperty<?>> item = cdf.getValue();
        //noinspection unchecked
        return item.isLeaf() ?
          (ObservableValue<Object>) item.getValue() :
          null;
      });
    propertyValueColumn.setCellFactory(
      (col) -> new TextFieldTreeTableCell<ReadOnlyProperty<?>, Object>()
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
          //logMethodCall("updateItem");
          //System.out.format("empty: %s, item: %s%n", empty, item);
          super.updateItem(item, empty);

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
              beanClassName = bean.getClass().getName();
          }

          System.out.format(
            "%s#%s() called on \"%s.%s\" - value: %s%n",
            this.getClass().getName(), methodName, beanClassName, propertyName,
            propertyValue);
        }
      });
    columns.add(propertyValueColumn);
  }


  /*
  private static final MultiTreeTableCellFactory<ReadOnlyProperty<?>, Object, Node> cellFactories;

  static
  {
    List<? extends CellNodeFactory<? extends ReadOnlyProperty<?>, ?, ? extends Node>> cfList =
      Arrays.asImmutableList(
        (CellNodeFactory<? extends ReadOnlyProperty<?>, ?, ? extends Node>)
          CellSpinnerFactory.BoundedValueCellSpinnerFactory.INSTANCE);

    //noinspection unchecked
    cellFactories = new MultiTreeTableCellFactory<>(
      (List<? extends CellNodeFactory<ReadOnlyProperty<?>, Object, Node>>) cfList);
  }
  */


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
      //noinspection ResultOfMethodCallIgnored
      propertySet.stream()
        .map((Function<Property<?>, TreeItem<ReadOnlyProperty<?>>>) TreeItem::new)
        .collect(Collectors.toCollection(
          new InstanceSupplier<>(layerRoot.getChildren())));

      root.getChildren().add(layerRoot);
    }

    setShowRoot(false);
    setRoot(root);
  }
}
