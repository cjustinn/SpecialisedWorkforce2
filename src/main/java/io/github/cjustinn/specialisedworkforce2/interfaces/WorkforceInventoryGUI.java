package io.github.cjustinn.specialisedworkforce2.interfaces;

import org.bukkit.inventory.Inventory;

public abstract class WorkforceInventoryGUI {
    protected Inventory inventory;
    public int page;
    public final int maxPages;

    protected WorkforceInventoryGUI() {
        this.inventory = null;
        this.page = 0;
        this.maxPages = 0;
    }

    protected WorkforceInventoryGUI(Inventory inv, int page, int max) {
        this.inventory = inv;
        this.page = page;
        this.maxPages = max;
    }

    abstract public void nextPage();
    abstract public void prevPage();
}
