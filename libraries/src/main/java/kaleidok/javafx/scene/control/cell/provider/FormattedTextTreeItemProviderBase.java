package kaleidok.javafx.scene.control.cell.provider;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyProperty;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeItem;
import javafx.util.StringConverter;
import kaleidok.javafx.beans.property.AspectedReadOnlyProperty;
import kaleidok.javafx.beans.property.aspect.StringConverterAspectTag;
import kaleidok.javafx.scene.control.cell.DynamicEditableTreeItem;
import kaleidok.javafx.scene.control.cell.EditableTreeTableCell;

import javax.annotation.Nullable;
import java.util.Objects;

import static kaleidok.javafx.scene.control.cell.DynamicEditableTreeItem.TreeItemProvider.findParentCell;


public abstract class FormattedTextTreeItemProviderBase<T, N extends Node>
  extends AspectedTreeItemProvider<T, N, StringConverter<T>, StringConverterAspectTag<T>>
{
  private static final String TOOLTIP_KEY =
    "kaleidok.javafx.scene.control.cell.provider.FormattedTextTreeItemProvider.tooltip";


  protected FormattedTextTreeItemProviderBase()
  {
    super(StringConverterAspectTag.getInstance());
  }


  protected TextField makeTextField(
    @SuppressWarnings("unused") DynamicEditableTreeItem<T, N> item )
  {
    TextField textField = new TextField();
    textField.setEditable(true);
    textField.setOnAction(
      FormattedTextTreeItemProviderBase::handleActionEvent);
    textField.parentProperty().addListener(
      FormattedTextTreeItemProviderBase::handleEditorParentChange);
    return textField;
  }


  @Nullable
  protected static <T> StringConverter<T> getStringConverter(
    EditableTreeTableCell<T, ?> cell )
  {
    TreeItem<ReadOnlyProperty<T>> item = cell.getTreeTableRow().getTreeItem();
    return (item != null) ?
        StringConverterAspectTag.<T>getInstance().of(
          (AspectedReadOnlyProperty<T>) item.getValue()) :
        null;
  }


  protected static <T> void handleValueChange(
    EditableTreeTableCell<T, TextField> cell, T value )
  {
    StringConverter<T> converter = getStringConverter(cell);
    if (converter != null)
      cell.getEditorNode().setText(converter.toString(value));
  }


  protected static <T> void handleActionEvent( ActionEvent ev )
  {
    final TextInputControl textField = (TextInputControl) ev.getSource();
    @SuppressWarnings("unchecked")
    EditableTreeTableCell<T, ?> cell =
      (EditableTreeTableCell<T, ?>)
        Objects.requireNonNull(findParentCell(textField));
    StringConverter<T> converter =
      Objects.requireNonNull(getStringConverter(cell));
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


  protected static void handleEditorParentChange( ObservableValue<? extends Parent> obs,
    Parent oldValue, Parent newValue )
  {
    if (oldValue != null && newValue == null)
      removeTooltip((Node) ((ReadOnlyProperty<?>) obs).getBean());
  }


  protected static void removeTooltip( Node anchor )
  {
    Tooltip tooltip = (Tooltip) anchor.getProperties().remove(TOOLTIP_KEY);
    if (tooltip != null)
      Platform.runLater(tooltip::hide);
  }


  protected static void showTooltip( final Node anchor, String message )
  {
    final Tooltip tooltip = (Tooltip) anchor.getProperties()
      .computeIfAbsent(TOOLTIP_KEY, (k) -> new Tooltip());
    tooltip.setText(message);
    if (!tooltip.isShowing())
    {
      Platform.runLater(() ->
      {
        Bounds b = anchor.localToScreen(anchor.getBoundsInLocal());
        // TODO: Choose a smarter tooltip location based on the available screen space
        tooltip.show(anchor, b.getMinX(), b.getMaxY() + 5);
      });
    }
  }


  protected static String getExceptionMessage( Exception ex )
  {
    for (Throwable cause = ex; cause != null; cause = cause.getCause())
    {
      String msg = cause.getLocalizedMessage();
      if (msg != null && !msg.isEmpty())
        return msg;
    }

    String name = ex.getClass().getSimpleName();
    if (name.isEmpty())
      name = ex.getClass().getName();
    return "Invalid value (" + name + ')';
  }
}
