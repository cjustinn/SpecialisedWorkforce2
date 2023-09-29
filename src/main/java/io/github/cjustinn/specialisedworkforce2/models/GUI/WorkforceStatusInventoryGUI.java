package io.github.cjustinn.specialisedworkforce2.models.GUI;

import io.github.cjustinn.specialisedworkforce2.interfaces.WorkforceInventoryGUI;
import org.bukkit.inventory.Inventory;

public class WorkforceStatusInventoryGUI extends WorkforceInventoryGUI {
    public WorkforceStatusInventoryGUI(Inventory inv, int page, final int max) {
        super(inv, page, max);
    }

    @Override
    public void nextPage() {}
    @Override
    public void prevPage() {}
}
