package cn.qi.pocdap.registers;

import cn.qi.pocdap.NeoSmartMoving;
import cn.qi.pocdap.data.state.SmartMoveState;
import cn.qi.pocdap.network.payload.ActionKeyPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.logging.Level;

//@EventBusSubscriber(modid = NeoSmartMoving.MODID, bus = EventBusSubscriber.Bus.MOD)
public class NetworkRegister {
    @SubscribeEvent
    public static void register(final RegisterPayloadHandlersEvent event){
        final PayloadRegistrar registrar = event.registrar("1.0.0");

        registrar.playToServer(
                ActionKeyPayload.TYPE,
                ActionKeyPayload.STREAM_CODEC,
                NetworkRegister::handleActionKey
        );
    }
    public static void handleActionKey(final ActionKeyPayload payload, final IPayloadContext context){
        // make tasks queue up, no conflict
        context.enqueueWork(()->{
            if(context.player() instanceof ServerPlayer player) {
                HitResult hitResult = player.pick(3.0D, 1.0F, false);
                SmartMoveState state = player.getData(AttachmentRegister.SMART_MOVE_STATE);
                if (hitResult.getType() == HitResult.Type.BLOCK) {
                    BlockHitResult blockHit = (BlockHitResult) hitResult;
                    BlockPos targetPos = blockHit.getBlockPos();
                    Direction playerFacing = player.getDirection();
                    Direction face = blockHit.getDirection();
                    net.minecraft.world.level.Level level = player.level();
                    if (payload.actionType() == 0) { // action means grabbing
                        BlockPos playerBodyTop = targetPos.relative(face);
                        BlockPos playerBodyBottom = playerBodyTop.below();
                        if (face == Direction.DOWN) {
                            player.sendSystemMessage(Component.literal("Not allowed to grab bottom of block"));
                            return;
                        } else if (face == Direction.UP) {
                            if (level.getBlockState(targetPos.above()).getCollisionShape(level, targetPos.above()).isEmpty()
                                    && level.getBlockState(targetPos.above(2)).getCollisionShape(level, targetPos.above(2)).isEmpty()) {
                                player.sendSystemMessage(Component.literal("Grab Top, go to the top of block"));
                                double pushX = playerFacing.getStepX()*0.2;
                                double pushZ = playerFacing.getStepZ()*0.2;
                                state.AnimeTicks = 10;
                                state.moveDirection = new Vec3(pushX,0.8,pushZ);
                            }


                            else {
                                player.sendSystemMessage(Component.literal("You cant stand on such place"));
                            }
                        } else if (level.getBlockState(playerBodyTop).getCollisionShape(level, playerBodyTop).isEmpty() &&
                                level.getBlockState(playerBodyBottom).getCollisionShape(level, playerBodyBottom).isEmpty()) {
                            player.sendSystemMessage(Component.literal("抓取空间检测通过！准备挂墙！"));
                            state.isGrabbing = true;
                        } else {
                            player.sendSystemMessage(Component.literal("抓取失败：你身体贴墙的位置有方块挡着！"));
                        }
                        player.sendSystemMessage(Component.literal("Grab checked from " + player.getName().getString()));
                    } else if (payload.actionType() == 1) { // which means vaulting
                        // 计算墙后面的坐标 (沿被击中面的反方向)

                        BlockPos behindBase = targetPos.relative(playerFacing);

                        // 我们需要检查 behindBase.above() (Y+1) 和 behindBase.above(2) (Y+2) 是不是空的
                        BlockPos vaultTargetLower = behindBase.above();
                        BlockPos vaultTargetUpper = behindBase.above(2);

                        // 我们还需要确保你要翻越的墙的正上方 (Y+1) 也是空的，不然头会撞到天花板
                        BlockPos wallTop = targetPos.above();

                        // 综合判定
                        boolean isWallTopClear = level.getBlockState(wallTop).getCollisionShape(level, wallTop).isEmpty();
                        boolean isLandingClear = level.getBlockState(vaultTargetLower).getCollisionShape(level, vaultTargetLower).isEmpty() &&
                                level.getBlockState(vaultTargetUpper).getCollisionShape(level, vaultTargetUpper).isEmpty();
                        if (isWallTopClear && isLandingClear) {
                            player.sendSystemMessage(Component.literal("翻越路径畅通！准备执行翻越！"));
                            double pushX = playerFacing.getStepX()*0.4;
                            double pushZ = playerFacing.getStepZ()*0.4;
                            state.AnimeTicks = 10;
                            state.moveDirection = new Vec3(pushX,0.4,pushZ);
                        } else {
                            player.sendSystemMessage(Component.literal("翻越失败：墙上方或对面有障碍物！"));
                        }
                        player.sendSystemMessage(Component.literal("Vault checked from " + player.getName().getString()));
                    }
                }
            }
        });
    }
}
