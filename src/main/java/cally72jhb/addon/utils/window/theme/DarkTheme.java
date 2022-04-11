package cally72jhb.addon.utils.window.theme;

import java.awt.*;

public class DarkTheme implements Theme {
   @Override public Color getDefaultBackgroundColor() {
      return new Color(64, 64, 64);
   }
   @Override public Color getDefaultForegroundColor() {
      return new Color(255, 255, 255);
   }

   @Override public Color getLightForegroundColor() {
      return new Color(175, 175, 175);
   }
   @Override public Color getLightBackgroundColor() {
        return new Color(200, 200, 200);
    }

   @Override public Color getDefaultButtonHoverColor() {
      return new Color(101, 101, 101);
   }
   @Override public Color getDefaultButtonPressedColor() {
      return new Color(101,101,101);
   }
}
