package kaleidok.javafx.scene.control.cell.provider;

import javafx.scene.control.TextField;
import kaleidok.javafx.scene.control.cell.DynamicEditableTreeItem;
import kaleidok.javafx.scene.control.cell.EditorNodeInfo;


public class FormattedTextTreeItemProvider<T>
  extends FormattedTextTreeItemProviderBase<T, TextField>
{
  @Override
  protected EditorNodeInfo<TextField, T> callTypeChecked(
    DynamicEditableTreeItem<T, TextField> item )
  {
    return EditorNodeInfo.of(makeTextField(item),
      FormattedTextTreeItemProvider::handleValueChange, getAspect(item));
  }
}
