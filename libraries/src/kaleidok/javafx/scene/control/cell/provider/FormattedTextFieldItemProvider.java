package kaleidok.javafx.scene.control.cell.provider;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyProperty;
import javafx.event.ActionEvent;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.Tooltip;
import javafx.util.StringConverter;
import kaleidok.javafx.beans.property.AspectedReadOnlyProperty;
import kaleidok.javafx.beans.property.aspect.StringConverterAspectTag;
import kaleidok.javafx.scene.control.cell.DynamicEditableTreeItem;
import kaleidok.javafx.scene.control.cell.EditorNodeInfo;
import kaleidok.javafx.scene.control.cell.EditableTreeTableCell;

import java.util.Objects;

import static kaleidok.javafx.scene.control.cell.DynamicEditableTreeItem.TreeItemProvider.findParentCell;


public class FormattedTextFieldItemProvider<T>
  extends AspectedTreeItemProvider<T, TextField, StringConverter<T>, StringConverterAspectTag<T>>
{
  public FormattedTextFieldItemProvider()
  {
    super(StringConverterAspectTag.getInstance());
  }


  @Override
  public boolean isApplicable( DynamicEditableTreeItem<?, ?> item )
  {
    return super.isApplicable(item) && item.getValue().getValue() != null;
  }


  @Override
  protected EditorNodeInfo<TextField, T> callTypeChecked(
    DynamicEditableTreeItem<T, TextField> item )
  {
    TextField textField = new TextField();
    textField.setOnAction(FormattedTextFieldItemProvider::handleActionEvent);
    textField.parentProperty().addListener((obs, oldValue, newValue) -> {
        if (oldValue != null && newValue == null)
          removeTooltip((Node) ((ReadOnlyProperty<?>) obs).getBean());
      });

    return EditorNodeInfo.of(textField,
      FormattedTextFieldItemProvider::handleValueChange, getAspect(item));
  }


  private static <T> StringConverter<T> getStringConverter(
    EditableTreeTableCell<T, ?> cell )
  {
    return StringConverterAspectTag.<T>getInstance().of(
      (AspectedReadOnlyProperty<T>) cell.getTreeTableRow().getTreeItem().getValue());
  }


  private static <T> void handleValueChange(
    EditableTreeTableCell<T, TextField> cell, T value )
  {
    cell.getEditorNode().setText(getStringConverter(cell).toString(value));
  }


  private static <T> void handleActionEvent( ActionEvent ev )
  {
    final TextInputControl textField = (TextInputControl) ev.getSource();
    @SuppressWarnings("unchecked")
    EditableTreeTableCell<T, ?> cell =
      (EditableTreeTableCell<T, ?>)
        Objects.requireNonNull(findParentCell(textField));
    StringConverter<T> converter = getStringConverter(cell);
    T newValue;
    try
    {
      newValue = converter.fromString(textField.getText());
    }
    catch (final RuntimeException ex)
    {
      showTooltip(textField, getExceptionMessage(ex));
      newValue = null;
    }

    if (newValue != null)
    {
      removeTooltip(textField);
      T oldValue = cell.isEmpty() ? null : cell.getItem();
      if (Objects.equals(oldValue, newValue))
        newValue = oldValue;
      cell.commitEdit(newValue);
    }

    ev.consume();
  }


  private static final String TOOLTIP_KEY =
    FormattedTextFieldItemProvider.class.getName() + '.' + Tooltip.class.getSimpleName();


  private static void removeTooltip( Node anchor )
  {
    Tooltip tooltip = (Tooltip) anchor.getProperties().remove(TOOLTIP_KEY);
    if (tooltip != null)
      Platform.runLater(tooltip::hide);
  }


  private static void showTooltip( final Node anchor, String message )
  {
    final Tooltip tooltip = (Tooltip) anchor.getProperties()
      .computeIfAbsent(TOOLTIP_KEY, (k) -> new Tooltip());
    tooltip.setText(message);
    if (!tooltip.isShowing())
    {
      Platform.runLater(() ->
      {
        Bounds b = anchor.localToScreen(anchor.getBoundsInLocal());
        tooltip.show(anchor, b.getMinX(), b.getMaxY() + 5);
      });
    }
  }


  private static String getExceptionMessage( Exception ex )
  {
    Throwable cause = ex;
    do
    {
      String msg = cause.getLocalizedMessage();
      if (msg != null && !msg.isEmpty())
        return msg;
      cause = cause.getCause();
    }
    while (cause != null);

    String name = ex.getClass().getSimpleName();
    if (name.isEmpty())
      name = ex.getClass().getName();
    return "Invalid value (" + name + ')';
  }
}
