package kaleidok.swing;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JToggleButton;
import java.awt.Graphics;


public class JRoundToggleButton extends JToggleButton
{
  public JRoundToggleButton() { }

  public JRoundToggleButton( Icon icon )
  {
    super(icon);
  }

  public JRoundToggleButton( Icon icon, boolean selected )
  {
    super(icon, selected);
  }

  public JRoundToggleButton( String text )
  {
    super(text);
  }

  public JRoundToggleButton( String text, boolean selected )
  {
    super(text, selected);
  }

  public JRoundToggleButton( Action a )
  {
    super(a);
  }

  public JRoundToggleButton( String text, Icon icon )
  {
    super(text, icon);
  }

  public JRoundToggleButton( String text, Icon icon, boolean selected )
  {
    super(text, icon, selected);
  }


  public boolean paintUI = true;

  @Override
  protected void paintComponent( Graphics g )
  {
    if (paintUI) {
      super.paintComponent(g);
    } else {
      final Icon icon = getCurrentIcon();
      icon.paintIcon(this, g, getIconX(icon), getIconY(icon));
    }
  }


  protected int getIconX( Icon icon )
  {
    return getX() + (getWidth() - icon.getIconWidth()) / 2;
  }

  protected int getIconY( Icon icon )
  {
    return getY() + (getHeight() - icon.getIconHeight()) / 2;
  }


  @Override
  public boolean contains( int x, int y )
  {
    /* Normalize the coordinates compared to the ellipse having a center at
     * (0, 0) and a radius of 0.5.
     */
    final Icon icon = getCurrentIcon();
    final float
      normX = (float)(x - getIconX(icon)) / icon.getIconWidth() - 0.5f,
      normY = (float)(y - getIconY(icon)) / icon.getIconHeight() - 0.5f;
    return normX * normX + normY * normY < 0.25f;
  }


  public Icon getCurrentIcon()
  {
    Icon icon =
      isRolloverEnabled() ?
        (isSelected() ? getRolloverSelectedIcon() : getRolloverIcon()) :
      !isEnabled() ?
        (isSelected() ? getDisabledSelectedIcon() : getDisabledIcon()) :
      null;
    return (icon != null) ? icon : (isSelected() ? getSelectedIcon() : getIcon());
  }
}
