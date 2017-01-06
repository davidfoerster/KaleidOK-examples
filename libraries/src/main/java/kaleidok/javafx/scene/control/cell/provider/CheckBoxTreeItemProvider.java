package kaleidok.javafx.scene.control.cell.provider;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.event.ActionEvent;
import javafx.scene.control.Cell;
import javafx.scene.control.CheckBox;
import kaleidok.javafx.scene.control.cell.DynamicEditableTreeItem;
import kaleidok.javafx.scene.control.cell.EditorNodeInfo;

import java.util.Objects;

import static kaleidok.javafx.scene.control.cell.DynamicEditableTreeItem.TreeItemProvider.findParentCell;


public class CheckBoxTreeItemProvider
  extends FilteredTreeItemProvider<Boolean, CheckBox>
{
  @Override
  public boolean isApplicable( DynamicEditableTreeItem<?, ?> item )
  {
    return item.getValue() instanceof ReadOnlyBooleanProperty;
  }


  @Override
  protected EditorNodeInfo<CheckBox, Boolean> callTypeChecked(
    DynamicEditableTreeItem<Boolean, CheckBox> item )
  {
    CheckBox checkBox = new CheckBox();
    checkBox.setAllowIndeterminate(false);
    checkBox.setOnAction(CheckBoxTreeItemProvider::handleCheckBoxActionEvent);
    return EditorNodeInfo.of(checkBox, true,
      (cell, value) -> cell.getEditorNode().setSelected(value));
  }


  private static void handleCheckBoxActionEvent( ActionEvent ev )
  {
    CheckBox checkBox = (CheckBox) ev.getSource();
    //noinspection unchecked
    Cell<Boolean> cell = (Cell<Boolean>)
      Objects.requireNonNull(findParentCell(checkBox));
    cell.commitEdit(checkBox.isSelected());
    ev.consume();
  }
}
