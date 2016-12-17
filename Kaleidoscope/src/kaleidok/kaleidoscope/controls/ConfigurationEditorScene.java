package kaleidok.kaleidoscope.controls;

import javafx.scene.Scene;
import javafx.stage.Stage;


public class ConfigurationEditorScene extends Scene
{
  public final ConfigurationEditorTreeTable configurationEditor;


  public ConfigurationEditorScene()
  {
    super(new ConfigurationEditorTreeTable());
    configurationEditor = (ConfigurationEditorTreeTable) getRoot();
  }


  public void start( Stage stage )
  {
    stage.setWidth(configurationEditor.getPrefWidth() + 20);
    stage.setScene(this);
  }
}
