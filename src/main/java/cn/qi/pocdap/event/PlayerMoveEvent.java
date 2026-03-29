package cn.qi.pocdap.event;

import cn.qi.pocdap.data.state.SmartMoveState;
import cn.qi.pocdap.registers.AttachmentRegister;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

public class PlayerMoveEvent {
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Pre event)
    {
        Player player = event.getEntity();
        SmartMoveState state = player.getData(AttachmentRegister.SMART_MOVE_STATE);
        if (state.AnimeTicks > 0){
            player.setDeltaMovement(state.moveDirection);
            player.fallDistance = 0;
            state.AnimeTicks --;
        }
        if (state.isGrabbing){
            player.setDeltaMovement(0,0,0);
            player.fallDistance = 0;
            if(player.isCrouching())
                state.isGrabbing = false;
        }
    }
}
