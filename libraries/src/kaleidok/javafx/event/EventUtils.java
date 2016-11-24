package kaleidok.javafx.event;

import javafx.beans.value.WritableValue;
import javafx.event.Event;
import javafx.event.EventHandler;


public final class EventUtils
{
  private EventUtils() { }


  public static <E extends Event> EventHandler<E> chain(
    final EventHandler<E> first, final EventHandler<E> second )
  {
    return
      (first == null) ? second :
      (second == null) ? first :
        (EventHandler<E>) ev -> {
          first.handle(ev);
          if (!ev.isConsumed())
            second.handle(ev);
        };
  }


  public static <E extends Event> void chain(
    WritableValue<EventHandler<E>> actionProperty, EventHandler<E> second )
  {
    actionProperty.setValue(chain(actionProperty.getValue(), second));
  }


  public static <E extends Event> void chain( EventHandler<E> first,
    WritableValue<EventHandler<E>> actionProperty )
  {
    actionProperty.setValue(chain(first, actionProperty.getValue()));
  }
}
