package cn.qi.pocdap.data.state;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

public class SmartMoveState {
    public boolean isGrabbing = false;
    //How many ticks remain for current animation
    public int AnimeTicks = 0;
    //In which direction animation should play
    public Vec3 moveDirection = Vec3.ZERO;
    public BlockPos targetPos = new BlockPos(0,0,0);
    public double VecX = 0;
    public double VecY = 0;
    public double VecZ = 0;
    // 0:jumping/sprinting to wall
    // 1:标准翻越前半，和3用一个动作
    // 2:抓取
    // 3:when grab hit the block top or jump while grabbing: climb up
    // 4:vaulting from ground
    // 5:vaulting in the air
    // 6:踩一块踮脚然后抓取
    // 7:冲向墙壁准备抓取
    // 8,9,10,11:猩猩越，左腿懒人翻，右腿懒人翻，反身翻越阶段1
    // 12,13,14,15阶段2
    // 16,17,18,19,20阶段3加标准翻越后半
    // (optional below)
    // vaulting between the space of 1 block height
    // vaulting through 1*1 space
    // sliding
    public int AnimeNr;
    public int shimmyTicks = 0;
    public boolean shimmyLeft = true;
    public int toleranceTick = 5;
    public Direction wallFace = null;
    public Vec3 shimmyVector = Vec3.ZERO;
}
