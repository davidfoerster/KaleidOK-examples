package kaleidok.javafx.scene.control.cell.provider;

import javafx.beans.property.StringProperty;
import javafx.event.ActionEvent;
import javafx.scene.control.Cell;
import javafx.scene.control.TextField;
import kaleidok.javafx.scene.control.cell.DynamicEditableTreeItem;
import kaleidok.javafx.scene.control.cell.EditorNodeInfo;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;

import static kaleidok.javafx.scene.control.cell.DynamicEditableTreeItem.TreeItemProvider.findParentCell;


public class TextFieldItemProvider extends FilteredTreeItemProvider<String, TextField>
{
  @Override
  public boolean isApplicable( DynamicEditableTreeItem<?, ?> item )
  {
    return item.getValue() instanceof StringProperty;
  }


  @Override
  protected EditorNodeInfo<TextField, String> callTypeChecked(
    final DynamicEditableTreeItem<String, TextField> item )
  {
    final TextField textField = new TextField();
    textField.setEditable(true);
    textField.setOnAction(TextFieldItemProvider::actionEventHandler);
    return EditorNodeInfo.of(textField,
      (cell, value) ->
        cell.getEditorNode().setText(StringUtils.defaultString(value)),
      null);
  }


  private static void actionEventHandler( ActionEvent ev )
  {
    TextField textField = (TextField) ev.getSource();
    @SuppressWarnings("unchecked")
    Cell<String> cell = (Cell<String>)
      Objects.requireNonNull(findParentCell(textField));
    String oldValue = cell.isEmpty() ? null : cell.getItem();
    CharSequence newValue = textField.getCharacters();
    if (oldValue != null && !oldValue.contentEquals(newValue))
      newValue = oldValue;
    cell.commitEdit(newValue.toString());

    ev.consume();
  }
}
