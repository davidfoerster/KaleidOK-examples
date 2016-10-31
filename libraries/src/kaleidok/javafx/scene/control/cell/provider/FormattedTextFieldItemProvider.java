package kaleidok.javafx.scene.control.cell.provider;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyProperty;
import javafx.event.ActionEvent;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeTableCell;
import javafx.util.StringConverter;
import kaleidok.javafx.beans.property.AspectedReadOnlyProperty;
import kaleidok.javafx.beans.property.aspect.StringConverterAspectTag;
import kaleidok.javafx.scene.control.cell.DynamicEditableTreeItem;
import kaleidok.javafx.scene.control.cell.EditableTreeItem.EditorNodeInfo;
import kaleidok.javafx.scene.control.cell.EditableTreeTableCell;
import kaleidok.util.function.Functions;

import java.util.Objects;


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
    TreeTableCell<? extends AspectedReadOnlyProperty<T>, ?> cell )
  {
    return StringConverterAspectTag.<T>getInstance().of(
      cell.getTreeTableRow().getTreeItem().getValue());
  }


  private static <T> void handleValueChange(
    EditableTreeTableCell<T, TextField> cell, T value )
  {
    TextField textField = cell.getEditorNode();
    //noinspection unchecked,RedundantCast
    textField.setText(getStringConverter(
        (TreeTableCell<? extends AspectedReadOnlyProperty<T>, ?>)
          (TreeTableCell<?,?>) cell)
      .toString(value));
  }


  private static <T> void handleActionEvent( ActionEvent ev )
  {
    final TextInputControl textField = (TextInputControl) ev.getSource();
    @SuppressWarnings("unchecked")
    TreeTableCell<? extends AspectedReadOnlyProperty<T>, T> cell =
      (TreeTableCell<? extends AspectedReadOnlyProperty<T>, T>)
        textField.getParent();
    StringConverter<T> converter = getStringConverter(cell);
    T newValue;
    try
    {
      newValue = converter.fromString(textField.getText());
    }
    catch (final RuntimeException ex)
    {
      final String msg = getExceptionMessage(ex);
      Platform.runLater(() -> showTooltip(textField, msg));
      newValue = null;
    }

    if (newValue != null)
    {
      removeTooltip(textField);
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
      .computeIfAbsent(TOOLTIP_KEY, Functions.ignoreArg(Tooltip::new));
    tooltip.setText(message);
    if (!tooltip.isShowing())
    {
      Bounds b = anchor.localToScreen(anchor.getBoundsInLocal());
      tooltip.show(anchor, b.getMinX(), b.getMaxY() + 5);
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
