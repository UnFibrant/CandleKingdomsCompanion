package un.fibrant.candlekingdomscompanion;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

public final class CandleKingdomsCompanionFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                CandleKingdomsCompanion.registerCommands(dispatcher));
    }
}
