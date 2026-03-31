package cn.qi.pocdap.registers;

import cn.qi.pocdap.NeoSmartMoving;
import cn.qi.pocdap.data.state.SmartMoveState;
import cn.qi.pocdap.network.payload.ActionKeyPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredRegister;

import net.minecraft.world.level.Level;

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
            if (context.player() instanceof ServerPlayer player) {
                HitResult hitResult = player.pick(3.0D, 1.0F, false);
                if (hitResult.getType() == HitResult.Type.BLOCK) {
                    BlockHitResult blockHit = (BlockHitResult) hitResult;
                    BlockPos targetPos = blockHit.getBlockPos();
                    Direction face = blockHit.getDirection();
                    Level level = player.level();
                    SmartMoveState state = player.getData(AttachmentRegister.SMART_MOVE_STATE);

                    // ==========================================
                    // 【动作 0：智能抓取与地形分析】
                    // ==========================================
                    if (payload.actionType() == 0) {
                        hitResult = player.pick(3.0D, 1.0F, false);
                        if (hitResult.getType() == HitResult.Type.BLOCK) {
                            BlockHitResult blockHitResult = (BlockHitResult) hitResult;
                            targetPos = blockHitResult.getBlockPos();
                            face = blockHitResult.getDirection();

                            if (face.getAxis() != Direction.Axis.Y) {
                                BlockPos hangColumn = targetPos.relative(face);

                                int playerY = (int) Math.floor(player.getY()-1);
                                int targetY = targetPos.getY();
                                int obstacleY = playerY - 1; // 脚底下那一层

                                // 往下扫，找垫脚石
                                for (int y = targetY; y >= playerY; y--) {
                                    BlockPos checkPos = new BlockPos(hangColumn.getX(), y, hangColumn.getZ());
                                    if (!level.getBlockState(checkPos).getCollisionShape(level, checkPos).isEmpty()) {
                                        obstacleY = y;
                                        break;
                                    }
                                }

                                int heightDiff = obstacleY - playerY;

                                if (heightDiff >= 2) {
                                    // ❌ 挡路
                                    player.sendSystemMessage(Component.literal("抓取失败：前方有高墙挡路！"));
                                    return;
                                }
                                else if (heightDiff == 1) {
                                    // 🧗‍♂️ 借力踩踏跳 (动作 6)
                                    player.sendSystemMessage(Component.literal("发现一格高垫脚石，准备借力跃起！"));
                                    moveToTarget(targetPos.relative(face.getOpposite()).below(targetY-obstacleY),face,player,4,0,state,21);
                                    // 动作6: 6帧，目标Y是方块顶端减0.8
                                    moveToTarget(targetPos, face, player, 6, 0, state, 6);
                                }
                                else {
                                    // 🏃‍♂️ 冲刺直上 (动作 7)
                                    player.sendSystemMessage(Component.literal("前方平坦，冲刺抓取！"));
                                    // 动作7: 4帧，目标Y是方块顶端减0.8
                                    moveToTarget(targetPos.below(targetY-obstacleY),face,player,4,0,state,10);
                                    moveToTarget(targetPos, face, player, 6, 0, state, 7);
                                }

                                state.wallFace = face;
                            }
                        }
                    }
                    // ==========================================
                    // 【动作 1：智能翻越 (Vault) 与地形识别】
                    // ==========================================
                    else if (payload.actionType() == 1) {

                           Direction playerFacing = player.getDirection();
                           BlockPos wallPos = ((BlockHitResult) hitResult).getBlockPos(); // 假设 targetPos 存的是你正抓着的墙壁
                           BlockPos vaultSpace = wallPos.above(); // 玩家将要穿过的那个方块（墙顶上方）

                           // 确保前方翻越点上方是空的（不然头就撞墙了）
                           boolean isWallTopClear = level.getBlockState(vaultSpace).getCollisionShape(level, vaultSpace).isEmpty();
                           // 确保后方是空的（不然翻阅太远了）
                           boolean isWallBehindClear = level.getBlockState(((BlockHitResult) hitResult).getBlockPos().relative(playerFacing)).isEmpty();
                           if (isWallTopClear&&isWallBehindClear) {
                               // 🌟 1. 发射左右探测雷达
                               Direction leftDir = playerFacing.getCounterClockWise();
                               Direction rightDir = playerFacing.getClockWise();

                               BlockPos leftPos = vaultSpace.relative(leftDir);
                               BlockPos rightPos = vaultSpace.relative(rightDir);

                               boolean leftSolid = !level.getBlockState(leftPos).getCollisionShape(level, leftPos).isEmpty();
                               boolean rightSolid = !level.getBlockState(rightPos).getCollisionShape(level, rightPos).isEmpty();
                               player.sendSystemMessage(Component.literal("Left and right are"+leftPos.toString()+rightPos.toString()));
                               // 🌟 2. 动画分支矩阵
                               if(state.isGrabbing)
                               {
                                   state.AnimeNr = 11; //反身翻越
                                   player.sendSystemMessage(Component.literal("抓取状态，反身翻越！"));
                               }
                               else if (leftSolid && rightSolid) {
                                   state.AnimeNr = 8; // 猩猩越
                                   player.sendSystemMessage(Component.literal("狭窄地形：猩猩越！"));
                               }
                               else if (rightSolid && !leftSolid) {
                                   state.AnimeNr = 9; // 懒人翻左
                                   player.sendSystemMessage(Component.literal("右侧受阻：懒人翻左侧抬腿！"));
                               }
                               else if (leftSolid && !rightSolid) {
                                   state.AnimeNr = 10; // 懒人翻右
                                   player.sendSystemMessage(Component.literal("左侧受阻：懒人翻右侧抬腿！"));
                               }
                               else {
                                   state.AnimeNr = 1; // 标准翻越
                                   player.sendSystemMessage(Component.literal("地形开阔：标准翻越！"));
                               }

                               // 🌟 3. 执行物理推送
                               double pushX = playerFacing.getStepX() * 0.4;
                               double pushZ = playerFacing.getStepZ() * 0.4;
                               state.AnimeTicks = 9; // 翻越持续帧数
                               state.isGrabbing = false;
                           } else {
                               player.sendSystemMessage(Component.literal("翻越失败：正上方或后方空间不足！"));
                           }

                    }
                    // ==========================================
                    // 【动作 2：攀爬逻辑 (空格键)】
                    // ==========================================
                    else if (payload.actionType() == 2) {
                        if (state.isGrabbing) {
                            // 1. 解除死锁状态
                            state.isGrabbing = false;

                            // 2. 给予一个类似“翻越”的推力
                            // 获取玩家当前朝向
                            Direction playerFacing = player.getDirection();
                            // 把前方推力存进你的 VecX 和 VecZ 变量里
                            state.VecX = playerFacing.getStepX() * 0.25;
                            state.VecZ = playerFacing.getStepZ() * 0.25;

                            state.AnimeTicks = 8; // 总共攀爬 10 帧 (0.5秒)
                            state.AnimeNr = 3;     // 3 代表攀爬动作

                            player.sendSystemMessage(Component.literal("执行两段式攀爬！"));
                        }
                    }
                    // ==========================================
                    // 【动作 3/4：边缘平移 (A/D键)】
                    // ==========================================
                    else if (payload.actionType() == 3 || payload.actionType() == 4) {
                        if (state.isGrabbing && state.wallFace != null) {
                            boolean isLeft = (payload.actionType() == 3);
                            state.shimmyLeft = isLeft;

                            // 1. 获取玩家摄像机的真实偏航角 (Yaw)
                            float yaw = player.getYRot();
                            // A键就是视角往左90度，D键就是往右90度
                            float moveYaw = yaw + (isLeft ? -90 : 90);

                            // 2. 用三角函数把角度变成一个纯粹的 X/Z 向量
                            float radians = moveYaw * ((float)Math.PI / 180F);
                            double vecX = -Math.sin(radians);
                            double vecZ = Math.cos(radians);

                            // 3. 🌟 核心投影：把不属于墙壁轴线的力给抹除！
                            // 如果墙是朝南/朝北的，你只能在 X 轴上左右平移，抹除 Z 轴的移动
                            if (state.wallFace.getAxis() == Direction.Axis.Z) {
                                vecZ = 0;
                            }
                            // 如果墙是朝东/朝西的，你只能在 Z 轴上左右平移，抹除 X 轴的移动
                            else if (state.wallFace.getAxis() == Direction.Axis.X) {
                                vecX = 0;
                            }

                            // 4. 标准化向量，并乘以我们的平移速度
                            Vec3 moveVec = new Vec3(vecX, 0, vecZ).normalize().scale(0.15);

                            state.shimmyVector = moveVec;
                            state.shimmyTicks = 3;
                        }
                    }
                }
            }
        });
    }
    // 🌟 修复后的 moveToTarget：只控制 Y 轴偏移，强制对准方块中心！
    private static void moveToTarget(BlockPos targetPos, Direction face, Player player, int ticksToSnap, double diffY, SmartMoveState state, int nextAnimeNr) {
        // 🌟 将半径从 0.35 改为 0.38，防止瞬移后被原版物理引擎强行挤出墙壁！
        double playerRadius = 0;
        double targetX = targetPos.getX()  + 0.5 + face.getStepX() * (0 + playerRadius);
        double targetZ = targetPos.getZ()  + 0.5 + face.getStepZ() * (0 + playerRadius);
        double targetY = targetPos.getY() + diffY;

        state.VecX = targetX;
        state.VecY = targetY;
        state.VecZ = targetZ;

        Vec3 perfectPos = new Vec3(targetX, targetY, targetZ);
        Vec3 currentPos = player.position();

        state.moveDirection = perfectPos.subtract(currentPos).scale(1.0 / ticksToSnap);
        state.AnimeTicks = ticksToSnap;
        state.AnimeNr = nextAnimeNr;
        state.isGrabbing = false;
    }
}
