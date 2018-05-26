package kaleidok.javafx.scene.control.cell.provider;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogEvent;
import javafx.scene.control.DialogPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Window;
import javafx.util.StringConverter;
import kaleidok.javafx.beans.property.AspectedListProperty;
import kaleidok.javafx.beans.property.aspect.StringConverterAspectTag;
import kaleidok.javafx.scene.control.cell.DynamicEditableTreeItem;
import kaleidok.javafx.scene.control.cell.EditableTreeTableCell;
import kaleidok.javafx.scene.control.cell.EditorNodeInfo;
import kaleidok.javafx.util.converter.CollectionStringConverter;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static kaleidok.javafx.scene.control.cell.DynamicEditableTreeItem.TreeItemProvider.findParentCell;


public class MultiLineTextTreeItemProvider<E>
  extends FormattedTextTreeItemProviderBase<ObservableList<E>, HBox>
{
  @SuppressWarnings("HardcodedLineSeparator")
  public static final Collector<CharSequence, ?, String> lineBreakJoiner =
    Collectors.joining("\n");

  private static final Insets buttonOpenDetailPadding = new Insets(1);

  private Dialog<ButtonType> dialog;

  private TextArea textArea;

  private volatile AspectedListProperty<E> currentProperty = null;


  @Override
  public boolean isApplicable( DynamicEditableTreeItem<?, ?> item )
  {
    //noinspection OverlyStrongTypeCast
    return item.getValue() instanceof AspectedListProperty &&
      ((AspectedListProperty<?>) item.getValue()).getAspect(
        StringConverterAspectTag.getInstance()) instanceof CollectionStringConverter;
  }


  public synchronized TextArea getTextArea()
  {
    if (textArea == null)
    {
      textArea = new TextArea();
      textArea.setEditable(true);
      textArea.addEventHandler(KeyEvent.KEY_RELEASED,
        MultiLineTextTreeItemProvider::handleTextAreaKeyReleased);
    }
    return textArea;
  }


  public synchronized Dialog<ButtonType> getDialog()
  {
    if (dialog == null)
    {
      dialog = new Dialog<>();
      dialog.initModality(Modality.WINDOW_MODAL);
      dialog.setResizable(true);
      dialog.setOnShown(
        MultiLineTextTreeItemProvider::handleDialogShownSizeWorkaround);

      DialogPane dialogPane = dialog.getDialogPane();
      dialogPane.setContent(getTextArea());
      dialogPane.getButtonTypes()
        .setAll(ButtonType.OK, ButtonType.CANCEL);
      dialogPane.lookupButton(ButtonType.OK)
        .addEventFilter(ActionEvent.ACTION, this::filterDialogButtonOk);
    }
    return dialog;
  }


  public AspectedListProperty<E> getCurrentProperty()
  {
    return currentProperty;
  }


  @Override
  protected EditorNodeInfo<HBox, ObservableList<E>> callTypeChecked(
    DynamicEditableTreeItem<ObservableList<E>, HBox> item )
  {
    TextField textField = makeTextField(item);

    Button buttonOpenDetail = new Button("…");
    buttonOpenDetail.setPadding(buttonOpenDetailPadding);
    buttonOpenDetail.prefWidthProperty()
      .bind(buttonOpenDetail.minWidthProperty());
    buttonOpenDetail.setOnAction(this::handleButtonOpenDetailAction);

    HBox wrapper = new HBox(textField, buttonOpenDetail);
    buttonOpenDetail.prefHeightProperty().bind(wrapper.heightProperty());
    wrapper.parentProperty().addListener(
      FormattedTextTreeItemProviderBase::handleEditorParentChange);

    return EditorNodeInfo.of(wrapper,
      MultiLineTextTreeItemProvider::handleValueChange,
      ((AspectedListProperty<E>) item.getValue())
        .getAspect(StringConverterAspectTag.getInstance()));
  }


  private static <E> void handleValueChange(
    EditableTreeTableCell<? super ObservableList<E>, HBox> cell,
    ObservableList<E> value )
  {
    StringConverter<? super ObservableList<E>> converter =
      getStringConverter(cell);
    if (converter != null)
    {
      TextField textField =
        (TextField) cell.getEditorNode().getChildren().get(0);
      textField.setText(converter.toString(value));
    }
  }


  private static DialogPane findParentDialogPane( Node node )
  {
    for (; node != null; node = node.getParent())
    {
      if (node instanceof DialogPane)
        return (DialogPane) node;
    }
    return null;
  }


  private static void handleTextAreaKeyReleased( KeyEvent ev )
  {
    if (ev.isControlDown() && ev.getCode() == KeyCode.ENTER)
    {
      @SuppressWarnings("ConstantConditions")
      Button buttonOk = (Button)
        findParentDialogPane((Node) ev.getSource()).lookupButton(ButtonType.OK);
      if (buttonOk.isArmed())
      {
        buttonOk.fire();
        ev.consume();
      }
    }
  }


  private void handleButtonOpenDetailAction( ActionEvent ev )
  {
    Button button = (Button) ev.getSource();
    @SuppressWarnings({ "unchecked", "ConstantConditions" })
    AspectedListProperty<E> prop = (AspectedListProperty<E>)
      findParentCell(button).getTreeTableRow().getTreeItem().getValue();
    ObservableList<E> value = prop.get();
    CollectionStringConverter<E, ?> converter =
      (CollectionStringConverter<E, ?>)
        prop.getAspect(StringConverterAspectTag.getInstance());

    Dialog<?> dialog = getDialog();
    dialog.setTitle(prop.getName());
    Window ownerWindow = button.getScene().getWindow();
    if (dialog.getOwner() == null) {
      dialog.initOwner(ownerWindow);
    } else {
      assert dialog.getOwner() == ownerWindow;
    }

    TextArea textArea = getTextArea();
    textArea.setText(value.stream()
      .map(converter.elementToString)
      .collect(lineBreakJoiner));

    currentProperty = prop;
    if (dialog.showAndWait().orElse(null) == ButtonType.OK)
    {
      Stream<E> newValueStream =
        textArea.getParagraphs().stream()
          .filter(StringUtils::isNotEmpty)
          .map(converter.elementFromString.compose(Object::toString));
      List<E> newValue = newValueStream.collect(Collectors.toList());
      if (!newValue.equals(value))
        value.setAll(newValue);
    }
    currentProperty = null;

    removeTooltip(textArea);
    ev.consume();
  }


  private void filterDialogButtonOk( ActionEvent ev )
  {
    CollectionStringConverter<E, ?> converter = (CollectionStringConverter<E, ?>)
      getCurrentProperty().getAspect(StringConverterAspectTag.getInstance());
    Function<? super List<CharSequence>, ? extends Exception> paragraphVerifier =
      converter.paragraphVerifier;
    if (paragraphVerifier != null)
    {
      TextArea textArea = getTextArea();
      Exception ex = paragraphVerifier.apply(textArea.getParagraphs());
      if (ex != null)
      {
        showTooltip(textArea, getExceptionMessage(ex));
        ev.consume();
      }
    }
  }


  private static void handleDialogShownSizeWorkaround( DialogEvent ev )
  {
    final DialogPane dialogPane =
      ((Dialog<?>) ev.getSource()).getDialogPane();
    if (dialogPane.getScene() == null)
    {
      // Work around a bug that would result in a miscalculated scene size
      Platform.runLater(() ->
        dialogPane.getScene().getWindow().sizeToScene());
    }
  }
}
