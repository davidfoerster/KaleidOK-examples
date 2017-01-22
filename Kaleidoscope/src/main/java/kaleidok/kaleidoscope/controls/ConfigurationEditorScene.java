package kaleidok.kaleidoscope.controls;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import kaleidok.javafx.beans.property.AspectedProperty;
import kaleidok.javafx.beans.property.adapter.preference.PropertyPreferencesAdapter;
import kaleidok.javafx.beans.property.adapter.preference.ReadOnlyPropertyPreferencesAdapter;
import kaleidok.javafx.beans.property.aspect.PropertyPreferencesAdapterTag;
import kaleidok.kaleidoscope.KaleidoscopeApp;

import java.io.*;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.prefs.BackingStoreException;
import java.util.prefs.InvalidPreferencesFormatException;
import java.util.prefs.Preferences;


public class ConfigurationEditorScene extends Scene
{
  public final ConfigurationEditorTreeTable configurationEditor =
    new ConfigurationEditorTreeTable();

  private final MenuBar menuBar;

  {
    MenuItem loadItem = new MenuItem("Import…");
    loadItem.setOnAction((ev) -> {
      openFileChooser(
        "open", "Import Preferences", this::importPreferences);
      ev.consume();
    });

    MenuItem saveItem = new MenuItem("Export…");
    saveItem.setOnAction((ev) -> {
      openFileChooser(
        "save", "Export Preferences", this::exportPreferences);
      ev.consume();
    });

    MenuItem closeItem = new MenuItem("Close");
    closeItem.setOnAction((ev) -> {
      getWindow().hide();
      ev.consume();
    });

    Menu configurationMenu =
      new Menu("Preferences", null,
        loadItem, saveItem, new SeparatorMenuItem(), closeItem);
    menuBar = new MenuBar(configurationMenu);
    menuBar.setUseSystemMenuBar(true);
  }


  private FileChooser fileChooser;

  private Alert alertDialog;


  public ConfigurationEditorScene()
  {
    super(new VBox());
    //noinspection OverlyStrongTypeCast
    ((VBox) getRoot()).getChildren().addAll(menuBar, configurationEditor);
  }


  public void start( Stage stage )
  {
    stage.setWidth(configurationEditor.getPrefWidth() + 20);
    stage.setScene(this);
  }


  private synchronized FileChooser getFileChooser()
  {
    if (fileChooser == null)
    {
      fileChooser = new FileChooser();
      fileChooser.setInitialFileName("kaleidok.preferences.xml");
      fileChooser.getExtensionFilters().addAll(
        new ExtensionFilter("Preferences Files", "*.preferences.xml"),
        new ExtensionFilter("XML Files", "*.xml"),
        new ExtensionFilter("All Files", "*"));
    }
    return fileChooser;
  }


  private synchronized File openFileChooser( String mode,
    String title, Predicate<File> successPredicate )
  {
    BiFunction<FileChooser, Window, File> op =
      "open".equals(mode) ? FileChooser::showOpenDialog :
      "save".equals(mode) ? FileChooser::showSaveDialog :
        null;
    if (op == null)
      throw new IllegalArgumentException("Unknown mode: " + mode);

    FileChooser fc = getFileChooser();
    fc.setTitle(title);
    File f = op.apply(fc, getWindow());
    if (f != null && successPredicate.test(f))
    {
      fc.setInitialDirectory(f.getParentFile());
      fc.setInitialFileName(f.getName());
    }
    return f;
  }


  private boolean importPreferences( File f )
  {
    try (InputStream is =
      new BufferedInputStream(new FileInputStream(f)))
    {
      Preferences.importPreferences(is);
    }
    catch (IOException | InvalidPreferencesFormatException ex)
    {
      ex.printStackTrace();
      final String message = String.format(
        "Couldn’t import preferences from \"%s\": %s",
        f, ex.getLocalizedMessage());
      Platform.runLater(() -> showAlertDialog(message));
      return false;
    }

    //System.out.format("Loaded preferences from %s", f);
    configurationEditor.streamItems()
      .map(TreeItem::getValue)
      .filter((prop) -> prop instanceof AspectedProperty)
      .map((prop) -> (AspectedProperty<?>) prop)
      .filter((prop) -> !prop.isBound())
      .map(PropertyPreferencesAdapterTag.getWritableInstance()::ofAny)
      .filter(Objects::nonNull)
      .forEach(PropertyPreferencesAdapter::load);
    return true;
  }


  private boolean exportPreferences( File f )
  {
    try (OutputStream os =
      new BufferedOutputStream(new FileOutputStream(f)))
    {
      configurationEditor.streamItems()
        .map(TreeItem::getValue)
        .map(PropertyPreferencesAdapterTag.getInstance()::ofAny)
        .filter(Objects::nonNull)
        .forEach(ReadOnlyPropertyPreferencesAdapter::save);

      Preferences.userNodeForPackage(KaleidoscopeApp.class).parent()
        .exportSubtree(os);
    }
    catch (IOException | BackingStoreException ex)
    {
      ex.printStackTrace();
      final String message = String.format(
        "Couldn’t export preferences to \"%s\": %s",
        f, ex.getLocalizedMessage());
      Platform.runLater(() -> showAlertDialog(message));
      return false;
    }
    //System.out.format("Saved preferences to %s", f);
    return true;
  }


  private synchronized Alert getAlertDialog()
  {
    if (alertDialog == null)
    {
      alertDialog = new Alert(Alert.AlertType.ERROR, null, ButtonType.OK);
      alertDialog.initModality(Modality.WINDOW_MODAL);
      alertDialog.initOwner(getWindow());
    }
    return alertDialog;
  }


  private synchronized ButtonType showAlertDialog( String message )
  {
    Alert alertDialog = getAlertDialog();
    alertDialog.setContentText(message);
    return alertDialog.showAndWait().orElse(null);
  }
}
