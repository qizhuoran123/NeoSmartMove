package cn.qi.pocdap.registers;

import cn.qi.pocdap.NeoSmartMoving;
import cn.qi.pocdap.data.state.SmartMoveState;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public class AttachmentRegister {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, NeoSmartMoving.MODID);
    public static final Supplier<AttachmentType<SmartMoveState>> SMART_MOVE_STATE =
            ATTACHMENT_TYPES.register("smart_move_state",()-> AttachmentType.<SmartMoveState>builder(SmartMoveState::new).build());
}
