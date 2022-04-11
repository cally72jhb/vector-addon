package cally72jhb.addon.utils.window.theme;

import java.awt.*;

public class LightTheme implements Theme {
   @Override public Color getDefaultBackgroundColor() {
      return new Color(225, 225, 225);
   }
   @Override public Color getDefaultForegroundColor() {
      return new Color(25, 25, 25);
   }

   @Override public Color getLightForegroundColor() {
      return new Color(175, 175, 175);
   }
    @Override public Color getLightBackgroundColor() {
        return new Color(145, 145, 145);
    }

   @Override public Color getDefaultButtonHoverColor() {
      return new Color(100, 100, 100);
   }
   @Override public Color getDefaultButtonPressedColor() {
      return new Color(100, 100, 100);
   }
}
