package buildcraft.api.blocks;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import buildcraft.api.core.BCDebugging;
import buildcraft.api.core.BCLog;

public enum CustomRotationHelper {
    INSTANCE;

    /* If you want to test your class-based rotation registration then add the system property
     * "-Dbuildcraft.api.rotation.debug.class=true" to your launch. */
    private static final boolean DEBUG_CLASS = BCDebugging.shouldDebugLog("api.rotation.class");
    private static final boolean DEBUG_SINGLE = BCDebugging.shouldDebugLog("api.rotation.single");

    private final Map<Block, List<ICustomRotationHandler>> handlers = new IdentityHashMap<>();

    public void registerHandlerForAll(Class<? extends Block> blockClass, ICustomRotationHandler handler) {
        for (Block block : Block.REGISTRY) {
            Class<? extends Block> foundClass = block.getClass();
            if (blockClass.isAssignableFrom(foundClass)) {
                if (DEBUG_CLASS) {
                    BCLog.logger.info("[api.rotation.class] Found an assignable block " + block.getRegistryName() + " (" + foundClass + ") for " + blockClass);
                }
                registerHandlerInternal(block, handler);
            }
        }
    }

    public void registerHandler(Block block, ICustomRotationHandler handler) {
        if (registerHandlerInternal(block, handler)) {
            if (DEBUG_SINGLE) {
                BCLog.logger.info("[api.rotation.single] Setting a rotation handler for block " + block.getRegistryName());
            }
        } else if (DEBUG_SINGLE) {
            BCLog.logger.info("[api.rotation.single] Adding another rotation handler for block " + block.getRegistryName());
        }
    }

    private boolean registerHandlerInternal(Block block, ICustomRotationHandler handler) {
        if (!handlers.containsKey(block)) {
            List<ICustomRotationHandler> forBlock = new ArrayList<>();
            forBlock.add(handler);
            handlers.put(block, forBlock);
            return true;
        } else {
            handlers.get(block).add(handler);
            return false;
        }
    }

    public EnumActionResult attemptRotateBlock(World world, BlockPos pos, IBlockState state, EnumFacing sideWrenched) {
        Block block = state.getBlock();
        if (block instanceof ICustomRotationHandler) {
            return ((ICustomRotationHandler) block).attemptRotation(world, pos, state, sideWrenched);
        }
        if (!handlers.containsKey(block)) return EnumActionResult.PASS;
        for (ICustomRotationHandler handler : handlers.get(block)) {
            EnumActionResult result = handler.attemptRotation(world, pos, state, sideWrenched);
            if (result != EnumActionResult.PASS) {
                return result;
            }
        }
        return EnumActionResult.PASS;
    }
}