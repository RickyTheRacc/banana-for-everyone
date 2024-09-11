package me.ricky.bananaplus.utils;

import me.ricky.bananaplus.systems.BananaUtils;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.HashSet;
import java.util.Set;

public class DynamicUtil extends BananaUtils {
    private static final Set<BlockPos> posSet = new HashSet<>();
    private static Box testBox = new Box(BlockPos.ORIGIN);
    private static final BlockPos.Mutable testPos = new BlockPos.Mutable();

    public static Set<BlockPos> topPos(PlayerEntity player) {
        posSet.clear();
        testBox = box(player).contract(0.001);

        for (double[] point : fourWay(testBox)) {
            testPos.set(point[0], testBox.maxY + 0.5, point[1]);
            posSet.add(testPos.toImmutable());
        }

        return posSet;
    }

    public static Set<BlockPos> facePos(PlayerEntity player) {
        posSet.clear();
        testBox = box(player).contract(0.001);

        for (double[] point : eightWay(testBox)) {
            testPos.set(point[0], testBox.maxY - 0.5, point[1]);
            posSet.add(testPos.toImmutable());
        }

        return posSet;
    }

    public static Set<BlockPos> feetPos(PlayerEntity player) {
        posSet.clear();
        testBox = box(player).contract(0.001);

        for (double[] point : eightWay(testBox)) {
            testPos.set(point[0], testBox.minY + 0.5, point[1]);
            posSet.add(testPos.toImmutable());
        }

        return posSet;
    }

    public static Set<BlockPos> underPos(PlayerEntity player) {
        posSet.clear();
        testBox = box(player).contract(0.001);

        for (double[] point : fourWay(testBox)) {
            testPos.set(point[0], testBox.minY - 0.5, point[1]);
            posSet.add(testPos.toImmutable());
        }

        return posSet;
    }

    public static double[][] eightWay(Box box) {
        return new double[][]{
            {box.minX, box.minZ - 1},
            {box.maxX, box.minZ - 1},
            {box.maxX + 1, box.minZ},
            {box.maxX + 1, box.maxZ},
            {box.maxX, box.maxZ + 1},
            {box.minX, box.maxZ + 1},
            {box.minX - 1, box.maxZ},
            {box.minX - 1, box.minZ}
        };
    }

    public static double[][] fourWay(Box box) {
        return new double[][]{
            {box.minX, box.minZ},
            {box.maxX, box.minZ},
            {box.maxX, box.maxZ},
            {box.minX, box.maxZ}
        };
    }
}