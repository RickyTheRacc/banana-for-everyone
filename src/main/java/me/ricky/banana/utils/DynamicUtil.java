package me.ricky.banana.utils;

import me.ricky.banana.mixininterface.IBlink;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.movement.Blink;
import meteordevelopment.meteorclient.utils.PostInit;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.*;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class DynamicUtil {
    private static final Set<BlockPos> posSet = new HashSet<>();
    private static final BlockPos.Mutable testPos = new BlockPos.Mutable();
    private static Box testBox = new Box(testPos);
    private static IBlink blink;

    @PostInit
    public static void init() {
        blink = (IBlink) Modules.get().get(Blink.class);
    }

    public static Set<BlockPos> topPos(PlayerEntity player) {
        posSet.clear();

        testBox = player == mc.player ? blink.getHitbox() : player.getBoundingBox();
        testBox = testBox.contract(0.001);

        for (double[] point : fourWay(testBox)) {
            testPos.set(point[0], testBox.maxY + 0.5, point[1]);
            posSet.add(testPos.toImmutable());
        }

        return posSet;
    }

    public static Set<BlockPos> facePos(PlayerEntity player) {
        posSet.clear();

        testBox = player == mc.player ? blink.getHitbox() : player.getBoundingBox();
        testBox = testBox.contract(0.001);
        
        for (double[] point : eightWay(testBox)) {
            testPos.set(point[0], testBox.maxY - 0.5, point[1]);
            posSet.add(testPos.toImmutable());
        }

        return posSet;
    }

    public static Set<BlockPos> feetPos(PlayerEntity player) {
        posSet.clear();

        testBox = player == mc.player ? blink.getHitbox() : player.getBoundingBox();
        testBox = testBox.contract(0.001);

        for (double[] point : eightWay(testBox)) {
            testPos.set(point[0], testBox.minY + 0.5, point[1]);
            posSet.add(testPos.toImmutable());
        }

        return posSet;
    }

    public static Set<BlockPos> underPos(PlayerEntity player) {
        posSet.clear();

        testBox = player == mc.player ? blink.getHitbox() : player.getBoundingBox();
        testBox = testBox.contract(0.001);

        for (double[] point : fourWay(testBox)) {
            testPos.set(point[0], testBox.minY - 0.5, point[1]);
            posSet.add(testPos.toImmutable());
        }

        return posSet;
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
