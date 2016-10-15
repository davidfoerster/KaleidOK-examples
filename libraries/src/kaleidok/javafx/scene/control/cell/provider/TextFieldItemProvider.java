package kaleidok.javafx.scene.control.cell.provider;

import javafx.beans.property.StringProperty;
import javafx.event.ActionEvent;
import javafx.scene.control.Cell;
import javafx.scene.control.TextField;
import kaleidok.javafx.scene.control.cell.DynamicEditableTreeItem;
import kaleidok.javafx.scene.control.cell.EditableTreeItem.EditorNodeInfo;


public class TextFieldItemProvider extends FilteredTreeItemProvider<String, TextField>
{
  @Override
  public boolean isApplicable( DynamicEditableTreeItem<String, TextField> item )
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
    return EditorNodeInfo.of(textField, textField.textProperty(), null);
  }


  private static void actionEventHandler( ActionEvent ev )
  {
    TextField textField = (TextField) ev.getSource();
    @SuppressWarnings("unchecked")
    Cell<String> cell = (Cell<String>) textField.getParent();
    String oldValue = cell.isEmpty() ? null : cell.getItem();
    CharSequence newValue = textField.getCharacters();
    if (oldValue == null || !oldValue.contentEquals(newValue))
      cell.commitEdit(newValue.toString());

    ev.consume();
  }
}
