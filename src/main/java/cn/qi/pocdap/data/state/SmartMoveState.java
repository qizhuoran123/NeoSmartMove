package cn.qi.pocdap.data.state;

import net.minecraft.world.phys.Vec3;

public class SmartMoveState {
    public boolean isGrabbing = false;
    //How many ticks remain for current animation
    public int AnimeTicks = 0;
    //In which direction animation should play
    public Vec3 moveDirection = Vec3.ZERO;
}
