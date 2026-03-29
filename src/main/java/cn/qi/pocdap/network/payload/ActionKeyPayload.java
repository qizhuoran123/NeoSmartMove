package cn.qi.pocdap.network.payload;

import cn.qi.pocdap.NeoSmartMoving;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ActionKeyPayload(int actionType) implements CustomPacketPayload {
    //denote the resource location, id for this payload
    public static final Type<ActionKeyPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(NeoSmartMoving.MODID,"action_key"));
    // define decoder,<x,y>: decode x into y
    public static final StreamCodec<ByteBuf, ActionKeyPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT,ActionKeyPayload::actionType, // turn byte into action type
            ActionKeyPayload::new
    );
    @Override
    public Type<? extends CustomPacketPayload> type(){
        return  TYPE;
    }
}
