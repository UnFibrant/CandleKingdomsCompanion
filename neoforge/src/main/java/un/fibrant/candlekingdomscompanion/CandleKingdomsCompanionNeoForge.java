package un.fibrant.candlekingdomscompanion;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@Mod("candlekingdoms")
public final class CandleKingdomsCompanionNeoForge {
    public CandleKingdomsCompanionNeoForge(IEventBus eventBus) {
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent event) ->
                CandleKingdomsCompanion.registerCommands(event.getDispatcher()));
    }
}
