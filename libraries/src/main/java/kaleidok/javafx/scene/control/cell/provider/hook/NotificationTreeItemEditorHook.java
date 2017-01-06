package kaleidok.javafx.scene.control.cell.provider.hook;

import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableBooleanValue;
import javafx.beans.value.ObservableObjectValue;
import javafx.scene.Node;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import kaleidok.javafx.beans.property.aspect.PropertyAspectTag;
import kaleidok.javafx.scene.control.cell.DynamicEditableTreeItem;
import kaleidok.javafx.scene.control.cell.EditorNodeInfo;
import kaleidok.javafx.scene.control.cell.SimpleEditorNodeInfo;
import kaleidok.javafx.scene.control.cell.provider.MultiTreeItemProvider;

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.function.Supplier;

import static kaleidok.util.AssertionUtils.fastAssert;


public class NotificationTreeItemEditorHook<T, N extends Node>
  implements MultiTreeItemProvider.TreeItemEditorHook<T, N>
{
  public final PropertyAspectTag<? extends ObservableBooleanValue, ? super T> notificationTag;

  public final StringProperty tooltipText;

  private final Supplier<? extends Image> iconSupplier;

  private Image icon = null;

  private Tooltip tooltip = null;


  private NotificationTreeItemEditorHook(
    PropertyAspectTag<? extends ObservableBooleanValue, ? super T> notificationTag,
    String tooltipText, Supplier<? extends Image> iconSupplier, Image icon )
  {
    fastAssert(iconSupplier != null || icon != null);

    this.notificationTag =
      Objects.requireNonNull(notificationTag, "notification tag");
    this.tooltipText =
      new SimpleStringProperty(this, "notification text", tooltipText);
    this.iconSupplier = iconSupplier;
    this.icon = icon;
  }


  public NotificationTreeItemEditorHook(
    PropertyAspectTag<? extends ObservableBooleanValue, ? super T> notificationTag,
    String tooltipText, Supplier<? extends Image> iconSupplier )
  {
    this(notificationTag, tooltipText,
      Objects.requireNonNull(iconSupplier, "icon supplier"), null);
  }


  public NotificationTreeItemEditorHook(
    PropertyAspectTag<? extends ObservableBooleanValue, ? super T> notificationTag,
    String tooltipText, Image icon )
  {
    this(notificationTag, tooltipText, null,
      Objects.requireNonNull(icon, "icon"));
  }


  public NotificationTreeItemEditorHook(
    PropertyAspectTag<? extends ObservableBooleanValue, ? super T> notificationTag,
    String tooltipText, String iconUrl )
  {
    this(notificationTag, tooltipText, getIconSupplier(iconUrl), null);

  }


  private static Supplier<? extends Image> getIconSupplier( final String url )
  {
    if (Objects.requireNonNull(url, "icon URL").isEmpty())
      throw new IllegalArgumentException("Empty icon URL");

    return () -> new Image(url, true);
  }


  @Override
  @Nonnull
  public EditorNodeInfo<N, T> modify(
    @Nonnull DynamicEditableTreeItem<?, ?> treeItem,
    @Nonnull EditorNodeInfo<N, T> nodeInfo )
  {
    if (nodeInfo.getClass() == SimpleEditorNodeInfo.class)
    {
      ObservableBooleanValue needsRestart =
        notificationTag.ofAny(treeItem.getValue());
      if (needsRestart != null)
        nodeInfo = new NotificationNodeInfo(nodeInfo, needsRestart);
    }
    return nodeInfo;
  }


  @Nonnull
  public synchronized Image getIcon()
  {
    if (icon == null)
      icon = Objects.requireNonNull(iconSupplier.get(), "icon");
    return icon;
  }


  @Nonnull
  public synchronized Tooltip getTooltip()
  {
    if (tooltip == null)
    {
       tooltip = new Tooltip();
       tooltip.textProperty().bind(tooltipText);
    }
    return tooltip;
  }


  protected class NotificationNodeInfo extends SimpleEditorNodeInfo<N, T>
  {
    private final ObservableBooleanValue needsRestart;

    private final ObservableObjectValue<? extends HBox> notificationNodeBinding;

    private HBox notificationBox = null;

    private ImageView notificationIconView = null;


    protected NotificationNodeInfo( EditorNodeInfo<N, T> nodeInfo,
      ObservableBooleanValue needsRestart )
    {
      super(nodeInfo.editorNode, nodeInfo.alwaysEditing, nodeInfo.valueChange,
        nodeInfo.converter);
      this.needsRestart = needsRestart;

      notificationNodeBinding =
        Bindings.createObjectBinding(
          () -> this.needsRestart.get() ? getNotificationBox() : null,
          needsRestart);
    }


    @Override
    public ObservableObjectValue<? extends HBox> notificationNodeProperty()
    {
      return notificationNodeBinding;
    }


    private HBox getNotificationBox()
    {
      if (notificationBox == null)
        notificationBox = new HBox(getNotificationIconView());
      return notificationBox;
    }


    private ImageView getNotificationIconView()
    {
      if (notificationIconView == null)
      {
        notificationIconView = new ImageView(getIcon());
        Tooltip.install(notificationIconView, getTooltip());
      }
      return notificationIconView;
    }
  }
}
