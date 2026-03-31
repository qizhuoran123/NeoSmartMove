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
    // 1:stepping on the wall
    // 2:grabbing
    // 3:when grab hit the block top or jump while grabbing: climb up
    // 4:vaulting from ground
    // 5:vaulting in the air
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
