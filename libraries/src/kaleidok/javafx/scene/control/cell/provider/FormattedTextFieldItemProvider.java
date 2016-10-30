package kaleidok.javafx.scene.control.cell.provider;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyProperty;
import javafx.event.ActionEvent;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.Cell;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.Tooltip;
import javafx.util.StringConverter;
import kaleidok.javafx.beans.property.aspect.StringConverterAspectTag;
import kaleidok.javafx.scene.control.cell.DynamicEditableTreeItem;
import kaleidok.javafx.scene.control.cell.EditableTreeItem.EditorNodeInfo;
import kaleidok.javafx.scene.control.cell.EditableTreeTableCell;

import java.util.Objects;


public class FormattedTextFieldItemProvider<T>
  extends AspectedTreeItemProvider<T, TextField, StringConverter<T>, StringConverterAspectTag<T>>
{
  public FormattedTextFieldItemProvider()
  {
    super(StringConverterAspectTag.getInstance());
  }


  @Override
  public boolean isApplicable( DynamicEditableTreeItem<T, TextField> item )
  {
    return super.isApplicable(item) && item.getValue().getValue() != null;
  }


  private static final String STRING_CONVERTER_KEY =
    FormattedTextFieldItemProvider.class.getName() + '.' + StringConverter.class.getSimpleName();


  @Override
  protected EditorNodeInfo<TextField, T> callTypeChecked(
    DynamicEditableTreeItem<T, TextField> item )
  {
    TextField textField = new TextField();
    StringConverter<T> converter = getAspect(item);
    textField.getProperties().put(STRING_CONVERTER_KEY, converter);
    textField.setOnAction(FormattedTextFieldItemProvider::handleActionEvent);

    textField.parentProperty().addListener((obs, oldValue, newValue) -> {
        if (oldValue != null && newValue == null)
          removeTooltip((Node) ((ReadOnlyProperty<?>) obs).getBean());
      });

    return EditorNodeInfo.of(textField,
      FormattedTextFieldItemProvider::handleValueChange, converter);
  }


  @SuppressWarnings("unchecked")
  private static <T> StringConverter<T> getStringConverter(
    TextInputControl textField )
  {
    return (StringConverter<T>) textField.getProperties().get(STRING_CONVERTER_KEY);
  }


  private static <T> void handleValueChange(
    EditableTreeTableCell<T, TextField> cell, T value )
  {
    TextField textField = cell.getEditorNode();
    textField.setText(
      (value != null) ?
        getStringConverter(textField).toString(value) :
        "");
  }


  private static <T> void handleActionEvent( ActionEvent ev )
  {
    final TextInputControl textField = (TextInputControl) ev.getSource();
    StringConverter<T> converter = getStringConverter(textField);
    T newValue;
    try
    {
      newValue = converter.fromString(textField.getText());
    }
    catch (final RuntimeException ex)
    {
      Platform.runLater(() -> {
        Throwable cause = ex;
        String msg;
        do
        {
          msg = cause.getLocalizedMessage();
          if (msg != null && !msg.isEmpty())
            break;
          cause = cause.getCause();
        }
        while (cause != null);

        if (msg == null || msg.isEmpty())
          msg = "Invalid value (" + ex.getClass().getSimpleName() + ')';
        showTooltip(textField, msg);
      });

      newValue = null;
    }

    if (newValue != null)
    {
      removeTooltip(textField);

      @SuppressWarnings("unchecked")
      Cell<T> cell = (Cell<T>) textField.getParent();
      T oldValue = cell.isEmpty() ? null : cell.getItem();

      if (!Objects.equals(oldValue, newValue))
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


  private static void showTooltip( Node anchor, String message )
  {
    Tooltip tooltip = (Tooltip) anchor.getProperties()
      .computeIfAbsent(TOOLTIP_KEY, (k) -> new Tooltip());
    tooltip.setText(message);
    if (!tooltip.isShowing())
    {
      Bounds b = anchor.localToScreen(anchor.getBoundsInLocal());
      tooltip.show(anchor, b.getMinX(), b.getMaxY() + 5);
    }
  }
}
