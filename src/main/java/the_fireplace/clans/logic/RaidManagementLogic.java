package the_fireplace.clans.logic;

import com.google.common.collect.Lists;
import net.minecraft.block.BlockPistonBase;
import net.minecraft.block.BlockSlime;
import net.minecraft.block.material.EnumPushReaction;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityFallingBlock;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityPiston;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import the_fireplace.clans.Clans;
import the_fireplace.clans.cache.ClanCache;
import the_fireplace.clans.cache.RaidingParties;
import the_fireplace.clans.cache.WorldTrackingCache;
import the_fireplace.clans.data.ChunkRestoreData;
import the_fireplace.clans.data.ClaimDataManager;
import the_fireplace.clans.data.RaidRestoreDatabase;
import the_fireplace.clans.model.Clan;
import the_fireplace.clans.util.ChunkUtils;

import java.util.UUID;

public class RaidManagementLogic {
    public static boolean shouldCancelBlockDrops(World world, BlockPos pos) {
        if(!world.isRemote && !Clans.getConfig().disableRaidRollback()) {
            Chunk c = world.getChunk(pos);
            UUID chunkOwner = ChunkUtils.getChunkOwner(c);
            if (chunkOwner != null) {
                if(ChunkUtils.isBorderland(c))
                    return false;
                Clan chunkClan = ClanCache.getClanById(chunkOwner);
                if (chunkClan != null && RaidingParties.hasActiveRaid(chunkClan)) {
                    return true;
                } else {
                    //Remove the uuid as the chunk owner since the uuid is not associated with a clan.
                    ChunkUtils.clearChunkOwner(c);
                }
            }
        }
        return false;
    }

    public static boolean onPlayerDeath(EntityPlayerMP player) {
        if(!player.getEntityWorld().isRemote) {
            for(Clan clan: ClanCache.getPlayerClans(player.getUniqueID())) {
                if (clan != null && RaidingParties.hasActiveRaid(clan))
                    RaidingParties.getActiveRaid(clan).removeDefender(player);
                if (RaidingParties.getRaidingPlayers().contains(player.getUniqueID()) && RaidingParties.getRaid(player).isActive())
                    RaidingParties.getRaid(player).removeAttacker(player);
            }
        }
        return false;
    }

    public static void checkAndRestoreChunk(Chunk chunk) {
        if(!chunk.getWorld().isRemote) {
            Clan chunkOwner = ClanCache.getClanById(ChunkUtils.getChunkOwner(chunk));
            if (chunkOwner == null || !RaidingParties.hasActiveRaid(chunkOwner)) {
                ChunkRestoreData data = RaidRestoreDatabase.popChunkRestoreData(chunk.getWorld().provider.getDimension(), chunk);
                if (data != null)
                    data.restore(chunk);
            }
        }
    }

    public static void onNeighborBlockNotified(World world, IBlockState state, BlockPos pos) {
        if(!world.isRemote && !Clans.getConfig().disableRaidRollback()) {
            if (state.getBlock() instanceof BlockPistonBase) {
                if (state.getProperties().containsKey(BlockPistonBase.FACING) && state.getProperties().containsKey(BlockPistonBase.EXTENDED)) {
                    Comparable facing = state.getProperties().get(BlockPistonBase.FACING);
                    Comparable extended = state.getProperties().get(BlockPistonBase.EXTENDED);
                    BlockPos oldPos = pos;
                    BlockPos newPos = pos;
                    if(!WorldTrackingCache.pistonPhases.containsKey(pos))
                        WorldTrackingCache.pistonPhases.put(pos, !(Boolean) extended);

                    if (facing instanceof EnumFacing && extended instanceof Boolean && WorldTrackingCache.pistonPhases.get(pos) == extended) {
                        if ((Boolean) extended) {
                            int pushRange = 0;
                            for(int i=1;i<14;i++)
                                if(world.getBlockState(pos.offset((EnumFacing) facing, i)).getBlock() == Blocks.AIR || world.getBlockState(pos.offset((EnumFacing) facing, i)).getPushReaction().equals(EnumPushReaction.DESTROY)) {
                                    pushRange = i;
                                    break;
                                }
                            for(int i=pushRange-1;i>1;i--) {
                                newPos = pos.offset((EnumFacing) facing, i);
                                oldPos = pos.offset((EnumFacing) facing, i-1);
                                Chunk oldChunk = world.getChunk(oldPos);
                                Chunk newChunk = world.getChunk(newPos);
                                shiftBlocks(world, oldPos, newPos, oldChunk, newChunk);
                                TileEntity piston = world.getTileEntity(newPos);
                                if(piston instanceof TileEntityPiston && ((TileEntityPiston)piston).getPistonState().getBlock() instanceof BlockSlime) {
                                    switch((EnumFacing) facing) {
                                        case UP:
                                        case DOWN:
                                            for(EnumFacing shiftDir: Lists.newArrayList(EnumFacing.NORTH, EnumFacing.SOUTH, EnumFacing.EAST, EnumFacing.WEST))
                                                doSlimePush(world, (EnumFacing) facing, newPos, shiftDir);
                                            break;
                                        case EAST:
                                        case WEST:
                                            for(EnumFacing shiftDir: Lists.newArrayList(EnumFacing.NORTH, EnumFacing.SOUTH, EnumFacing.UP, EnumFacing.DOWN))
                                                doSlimePush(world, (EnumFacing) facing, newPos, shiftDir);
                                            break;
                                        case NORTH:
                                        case SOUTH:
                                            for(EnumFacing shiftDir: Lists.newArrayList(EnumFacing.UP, EnumFacing.DOWN, EnumFacing.EAST, EnumFacing.WEST))
                                                doSlimePush(world, (EnumFacing) facing, newPos, shiftDir);
                                            break;
                                    }
                                }
                            }
                        } else if(state.getBlock().equals(Blocks.STICKY_PISTON)) {
                            oldPos = oldPos.offset((EnumFacing) facing, 2);
                            newPos = newPos.offset((EnumFacing) facing);
                            Chunk oldChunk = world.getChunk(oldPos);
                            Chunk newChunk = world.getChunk(newPos);
                            shiftBlocks(world, oldPos, newPos, oldChunk, newChunk);
                            if(world.getBlockState(newPos) instanceof BlockSlime) {
                                switch((EnumFacing) facing) {
                                    case UP:
                                    case DOWN:
                                        for(EnumFacing shiftDir: Lists.newArrayList(EnumFacing.NORTH, EnumFacing.SOUTH, EnumFacing.EAST, EnumFacing.WEST))
                                            doSlimePull(world, (EnumFacing) facing, newPos, shiftDir);
                                        break;
                                    case EAST:
                                    case WEST:
                                        for(EnumFacing shiftDir: Lists.newArrayList(EnumFacing.NORTH, EnumFacing.SOUTH, EnumFacing.UP, EnumFacing.DOWN))
                                            doSlimePull(world, (EnumFacing) facing, newPos, shiftDir);
                                        break;
                                    case NORTH:
                                    case SOUTH:
                                        for(EnumFacing shiftDir: Lists.newArrayList(EnumFacing.UP, EnumFacing.DOWN, EnumFacing.EAST, EnumFacing.WEST))
                                            doSlimePull(world, (EnumFacing) facing, newPos, shiftDir);
                                        break;
                                }
                            }
                        }
                        WorldTrackingCache.pistonPhases.put(pos, !WorldTrackingCache.pistonPhases.get(pos));
                    }
                }
            }
        }
    }

    public static boolean shouldCancelFallingBlockCreation(EntityFallingBlock entity) {
        Clan owningClan = ClaimDataManager.getChunkClan(entity.chunkCoordX, entity.chunkCoordZ, entity.dimension);
        return owningClan != null && RaidingParties.hasActiveRaid(owningClan) && !ClaimDataManager.getChunkPositionData(entity.chunkCoordX, entity.chunkCoordZ, entity.dimension).isBorderland() && !Clans.getConfig().disableRaidRollback();//TODO monitor where it goes rather than just preventing it from falling
    }

    private static void doSlimePush(World world, EnumFacing facing, BlockPos newPos, EnumFacing shiftDir) {
        BlockPos testPos = newPos.offset(shiftDir);
        BlockPos oldPos;
        if(world.getBlockState(testPos).getPushReaction().equals(EnumPushReaction.NORMAL) || world.getBlockState(testPos).getPushReaction().equals(EnumPushReaction.IGNORE))
            return;
        int pushRange2 = 0;
        for(int j=1;j<14;j++)
            if(world.getBlockState(testPos.offset(facing, j)).getBlock() == Blocks.AIR || world.getBlockState(testPos.offset(facing, j)).getPushReaction().equals(EnumPushReaction.DESTROY)) {
                pushRange2 = j;
                break;
            }
        for(int j=pushRange2;j>0;j--) {
            newPos = testPos.offset(facing, j - 1);
            oldPos = testPos.offset(facing, j - 2);
            Chunk oldChunk2 = world.getChunk(oldPos);
            Chunk newChunk2 = world.getChunk(newPos);
            shiftBlocks(world, oldPos, newPos, oldChunk2, newChunk2);
        }
    }

    private static void doSlimePull(World world, EnumFacing facing, BlockPos newPos, EnumFacing shiftDir) {
        BlockPos testPos = newPos.offset(shiftDir);
        BlockPos oldPos;
        if(world.getBlockState(testPos).getPushReaction().equals(EnumPushReaction.BLOCK) || world.getBlockState(testPos).getPushReaction().equals(EnumPushReaction.IGNORE) || world.getBlockState(testPos).getPushReaction().equals(EnumPushReaction.PUSH_ONLY))
            return;
        newPos = testPos.offset(facing);
        oldPos = testPos.offset(facing, 2);
        Chunk oldChunk2 = world.getChunk(oldPos);
        Chunk newChunk2 = world.getChunk(newPos);
        shiftBlocks(world, oldPos, newPos, oldChunk2, newChunk2);
    }

    private static void shiftBlocks(World world, BlockPos oldPos, BlockPos newPos, Chunk oldChunk, Chunk newChunk) {
        String oldBlock = RaidRestoreDatabase.popRestoreBlock(world.provider.getDimension(), oldChunk, oldPos);
        if (oldBlock != null)
            RaidRestoreDatabase.addRestoreBlock(world.provider.getDimension(), newChunk, newPos, oldBlock);
        if(RaidRestoreDatabase.delRemoveBlock(world.provider.getDimension(), oldChunk, oldPos))
            RaidRestoreDatabase.addRemoveBlock(world.provider.getDimension(), newChunk, newPos);
    }
}
