package me.ricky.banana.utils;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.ArrayList;
import java.util.List;

public class DynamicUtil {
    private static final List<BlockPos> posList = new ArrayList<>();
    private static final BlockPos.Mutable testPos = new BlockPos.Mutable();
    private static Box testBox = new Box(testPos);

    public static List<BlockPos> topPos(PlayerEntity player) {
        posList.clear();
        testBox = player.getBoundingBox().contract(0.01);

        for (double[] point : fourWay(testBox)) {
            testPos.set(point[0], testBox.maxY + 0.5, point[1]);
            if (!posList.contains(testPos)) posList.add(testPos.toImmutable());
        }

        return posList;
    }

    public static List<BlockPos> facePos(PlayerEntity player) {
        posList.clear();
        testBox = player.getBoundingBox().contract(0.01);
        
        for (double[] point : eightWay(testBox)) {
            testPos.set(point[0], testBox.maxY - 0.5, point[1]);
            if (!posList.contains(testPos)) posList.add(testPos.toImmutable());
        }

        return posList;
    }

    public static List<BlockPos> feetPos(PlayerEntity player) {
        posList.clear();
        testBox = player.getBoundingBox().contract(0.01);

        for (double[] point : eightWay(testBox)) {
            testPos.set(point[0], testBox.minY + 0.5, point[1]);
            if (!posList.contains(testPos)) posList.add(testPos.toImmutable());
        }

        return posList;
    }

    public static List<BlockPos> underPos(PlayerEntity player) {
        posList.clear();
        testBox = player.getBoundingBox().contract(0.01);

        for (double[] point : fourWay(testBox)) {
            testPos.set(point[0], testBox.minY - 0.5, point[1]);
            if (!posList.contains(testPos)) posList.add(testPos.toImmutable());
        }

        return posList;
    }

    private static double[][] eightWay(Box box) {
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

    private static double[][] fourWay(Box box) {
        return new double[][]{
            {box.minX, box.minZ},
            {box.maxX, box.minZ},
            {box.maxX, box.maxZ},
            {box.minX, box.maxZ}
        };
    }
}
