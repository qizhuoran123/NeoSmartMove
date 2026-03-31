package cn.qi.pocdap.event;

import cn.qi.pocdap.data.state.SmartMoveState;
import cn.qi.pocdap.registers.AttachmentRegister;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

public class PlayerMoveEvent {

    // 🌟 工具方法 1：判断方块是否可抓取
    private static boolean isGrabableEdge(Level level, BlockPos pos) {
        boolean isSolid = !level.getBlockState(pos).getCollisionShape(level, pos).isEmpty();
        boolean isTopClear = level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty();
        return isSolid && isTopClear;
    }

    // 🌟 工具方法 2：极简吸附触发器
    private static void snapToCorner(BlockPos cornerBlock, Direction newFace, SmartMoveState state, Player player) {
        double playerRadius = 0.35;
        double targetX = cornerBlock.getX() + 0.5 + newFace.getStepX() * (0.5 + playerRadius);
        double targetZ = cornerBlock.getZ() + 0.5 + newFace.getStepZ() * (0.5 + playerRadius);
        double targetY = cornerBlock.getY() - 0.8;

        state.AnimeTicks = 3;
        state.moveDirection = new Vec3(targetX, targetY, targetZ).subtract(player.position()).scale(1.0 / 3);
        state.AnimeNr = 2; // 重新触发 3 帧吸附动画
        state.isGrabbing = false;
        state.wallFace = newFace;
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Pre event) {
        Player player = event.getEntity();
        if (!player.hasData(AttachmentRegister.SMART_MOVE_STATE)) return;

        SmartMoveState state = player.getData(AttachmentRegister.SMART_MOVE_STATE);
        Level level = player.level();

        // ==========================================
        // 【阶段 1：动画播放器】
        // ==========================================
        if (state.AnimeTicks > 0) {
            player.fallDistance = 0;
            player.setNoGravity(true);
            player.hurtMarked = true;

            if (state.AnimeNr == 3) { // 3:攀爬
                if (state.AnimeTicks > 3) {
                    player.setDeltaMovement(new Vec3(0, 0.55, 0));
                } else {
                    player.setDeltaMovement(new Vec3(state.VecX, -0.1, state.VecZ));
                }
            }
            else if (state.AnimeNr == 7) {
                // 【动作 7：平地冲刺直上 L型轨迹】总共4帧
                if (state.AnimeTicks > 2) {
                    // 前2帧：纯 XZ 轴平移，迅速冲向墙根
                    player.setDeltaMovement(new Vec3(state.moveDirection.x * 2, 0, state.moveDirection.z * 2));
                } else {
                    // 🌟 终极绝杀：拐点瞬移补正！
                    // 在停止水平移动的瞬间，强行把玩家吸附到绝对完美的墙根坐标！
                    // 彻底抹杀摩擦力导致的 0.8 格掉队！
                    if (state.AnimeTicks == 2) {
                        player.setPos(state.VecX, state.VecY, state.VecZ);
                    }

                    // 后2帧：纯 Y 轴拔高，贴墙拉起
                    player.setDeltaMovement(new Vec3(0, state.moveDirection.y * 4, 0));
                }
            }
            else if (state.AnimeNr == 6) {
                // 【动作 6：踩一格高方块借力跳】总共6帧
                if (state.AnimeTicks > 3) {
                    // 前3帧：斜上跳到那个1格高的方块上
                    player.setDeltaMovement(new Vec3(state.moveDirection.x, Math.max(0.4, state.moveDirection.y), state.moveDirection.z));
                } else {
                    if (state.AnimeTicks == 3) { player.setPos(state.VecX, state.VecY, state.VecZ); }
                    // 后3帧：从方块上起跳抓墙
                    player.setDeltaMovement(new Vec3(state.moveDirection.x*1.2, state.moveDirection.y * 1.2, state.moveDirection.z*1.2));
                }
            }
            // 🌟 动态碰撞箱缩放：如果是猩猩越(8)或懒人翻(9, 10)
            else if (state.AnimeNr == 8 || state.AnimeNr == 9 || state.AnimeNr == 10) {
                // 强制变为游泳姿态，碰撞箱瞬间变成 0.6 x 0.6！
                player.setPose(Pose.SWIMMING);
                player.setDeltaMovement(state.moveDirection);
            }
            else if (state.AnimeNr == 1) {
                // 标准翻越，如果空间大，不需要压碰撞箱
                player.setDeltaMovement(state.moveDirection);
            }
            else {
                // 默认的斜向直接吸附 (AnimeNr == 2)
                player.setDeltaMovement(state.moveDirection);
            }

            state.AnimeTicks--;

            // 🌟 极其重要：状态机闭环！不管是普通抓(2)、借力抓(6)还是冲刺抓(7)，动画结束都必须锁死！
            if (state.AnimeTicks == 0 && (state.AnimeNr == 2 || state.AnimeNr == 6 || state.AnimeNr == 7)) {
                state.isGrabbing = true;
                state.AnimeNr = -1;
                // 强制把玩家速度归零，彻底抹杀物理引擎的向上惯性！
                player.setDeltaMovement(Vec3.ZERO);
                player.moveTo(state.VecX, state.VecY, state.VecZ);
                player.displayClientMessage(Component.literal("Teleported"),false);
            }
        }
        // ==========================================
        // 【阶段 2：死锁与网格扫描系统】
        // ==========================================
        else if (state.isGrabbing) {
            if (state.shimmyTicks > 0) {
                player.setDeltaMovement(state.shimmyVector);
                state.shimmyTicks--;
            } else {
                player.setDeltaMovement(Vec3.ZERO);
            }

            player.fallDistance = 0;
            player.setNoGravity(true);
            player.hurtMarked = true;

            // 🌟 1. 绝对抗漂移锚点：强行将判定点往身后拉 0.3 格！
            Direction facing = state.wallFace.getOpposite(); // 玩家面朝墙的方向
            // 🌟 终极容错修复：加 1.3 直接瞄准方块正中心！
            // 无论你往上飘了 0.4 格，还是往下掉了 0.5 格，floor 算出来的永远是正确的方块层！
            int wallY = (int) Math.floor(player.getY() + 1.3);


            int safeX = (int) Math.floor(player.getX() - facing.getStepX() * 0.3);
            int safeZ = (int) Math.floor(player.getZ() - facing.getStepZ() * 0.3);

            BlockPos playerUpperPos = new BlockPos(safeX, wallY, safeZ);
            BlockPos currentWallPos = playerUpperPos.relative(facing);

            // ==========================================
            // 🌟 核心扫描逻辑
            // ==========================================
            if (isGrabableEdge(level, currentWallPos)) {
                // 1. 墙还在 (完美平移中)
                state.toleranceTick = 8; // 只要摸到墙，立刻充满容错！

                // 2. 检测内角碰撞 (如果正在平移)
                Direction moveDir = null;
                if (state.shimmyTicks > 0) {
                    if (state.shimmyVector.x > 0.05) moveDir = Direction.EAST;
                    else if (state.shimmyVector.x < -0.05) moveDir = Direction.WEST;
                    else if (state.shimmyVector.z > 0.05) moveDir = Direction.SOUTH;
                    else if (state.shimmyVector.z < -0.05) moveDir = Direction.NORTH;
                }

                if (moveDir != null) {
                    BlockPos blockAhead = playerUpperPos.relative(moveDir);
                    if (isGrabableEdge(level, blockAhead)) {
                        Direction newFace = moveDir.getOpposite();
                        snapToCorner(blockAhead, newFace, state, player); // 吸附到内角
                    }
                }
            } else {
                // 3. 墙没了！(滑出边缘，或墙被挖了)
                boolean foundNewEdge = false;

                // 计算玩家当前视角的“左”和“右”
                Direction leftDir = facing.getCounterClockWise();
                Direction rightDir = facing.getClockWise();

                // 🌟 精准检测两个“对角线”外角块
                BlockPos leftDiagonal = playerUpperPos.relative(facing).relative(leftDir);
                BlockPos rightDiagonal = playerUpperPos.relative(facing).relative(rightDir);

                if (isGrabableEdge(level, leftDiagonal)) {
                    // 滑向了左边，把玩家吸附到左对角线方块的右侧面上
                    snapToCorner(leftDiagonal, rightDir, state, player);
                    foundNewEdge = true;
                } else if (isGrabableEdge(level, rightDiagonal)) {
                    // 滑向了右边，把玩家吸附到右对角线方块的左侧面上
                    snapToCorner(rightDiagonal, leftDir, state, player);
                    foundNewEdge = true;
                }

                // 🌟 如果对角线也没有 (被外力推开，而非平移) -> 回退到十字相邻格扫描
                if (!foundNewEdge) {
                    for (Direction dir : Direction.Plane.HORIZONTAL) {
                        if (dir == facing) continue; // 面前已经测过了
                        BlockPos neighbor = playerUpperPos.relative(dir);
                        if (isGrabableEdge(level, neighbor)) {
                            snapToCorner(neighbor, dir.getOpposite(), state, player);
                            foundNewEdge = true;
                            break;
                        }
                    }
                }

                // 4. 周围 8 格彻底空了 -> 消耗容错，准备掉落
                if (!foundNewEdge) {
                    if (state.toleranceTick > 0) {
                        state.toleranceTick--;
                    } else {
                        state.isGrabbing = false;
                        player.setNoGravity(false);
                        player.displayClientMessage(Component.literal("corner"),false);
                        state.toleranceTick = 8;
                    }
                }
            }

            // 按 Shift 掉落
            if (player.isCrouching()) {
                state.isGrabbing = false;
                player.setNoGravity(false);
            }
        }
        else if (player.isNoGravity()) {
            player.setNoGravity(false);
        }
    }
}