package io.github.cjustinn.specialisedworkforce2.services;

import net.coreprotect.CoreProtectAPI;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.event.block.BlockPlaceEvent;

import java.util.ArrayList;
import java.util.List;

public class CoreProtectService {
    private static CoreProtectAPI api = null;

    public static void SetAPI(CoreProtectAPI api) {
        CoreProtectService.api = api;
    }

    public static boolean LogBlockInteraction(final String mode, final String player, final Location location, final Material type, final BlockData data) {
        boolean success = false;

        if (api != null && mode.equalsIgnoreCase("place")) {
            success = api.logPlacement(player, location, type, data);
        } else if (api != null && mode.equalsIgnoreCase("remove")) {
            success = api.logRemoval(player, location, type, data);
        }

        return success;
    }

    public static boolean IsBlockNatural(Block block) {
        if (block.getMetadata("non-natural").size() > 0) {
            return false;
        }
        List<List<String[]>> queries = new ArrayList<List<String[]>>() {{
            add(api.blockLookup(block, 157800000));
            add(api.queueLookup(block));
        }};

        for (List<String[]> result : queries) {
            if (result != null && result.size() >= 1) {
                CoreProtectAPI.ParseResult parsedResult = api.parseResult(result.get(0));
                if (parsedResult.getActionId() == 1) {
                    return false;
                }
            }
        }

        return true;
    }
}
