package cally72jhb.addon.system.modules.player;

import cally72jhb.addon.system.categories.Categories;
import cally72jhb.addon.system.events.InteractEvent;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;

public class MultiTask extends Module {
    public MultiTask() {
        super(Categories.Misc, "multi-task", "Allows you to mine a block while consuming a item at the same time.");
    }

    @EventHandler
    private void onInteract(InteractEvent event) {
        event.usingItem = false;
    }
}
