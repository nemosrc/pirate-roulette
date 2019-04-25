package com.nemosw.spigot.pirateroulette;

import com.google.common.collect.ImmutableList;
import com.nemosw.mox.math.Vector;
import com.nemosw.spigot.customentity.CustomEntityPacket;
import com.nemosw.spigot.tap.Particle;
import com.nemosw.spigot.tap.Tap;
import com.nemosw.spigot.tap.entity.TapArmorStand;
import com.nemosw.spigot.tap.firework.FireworkEffect;
import com.nemosw.spigot.tap.item.TapItemStack;
import com.nemosw.spigot.tap.math.BoundingBox;
import com.nemosw.spigot.tap.packet.Packet;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class RouletteBlock
{
    private final Vector pos;

    private final BoundingBox box;

    private final TapItemStack item;

    private final TapArmorStand stand;

    private final List<Slot> slots;

    private final Slot weakpoint;

    public RouletteBlock(double x, double y, double z, TapItemStack item)
    {
        this.pos = new Vector(x, y, z);
        double width = 1.5D;
        double height = width * 2.0D;
        this.box = Tap.MATH.newBoundingBox(x - width, y, z - width, x + width, y + height, z + width);
        this.item = item;
        this.stand = Tap.ENTITY.createEntity(ArmorStand.class);
        this.stand.setInvisible(true);
        this.stand.setPositionAndRotation(pos.x, pos.y, pos.z, 0F, 0F);

        double offset = 1.0D;
        double gap = 1D;
        width = 1.7;

        List<Slot> slots = new ArrayList<>();

        generateSlot(x + width, y + offset / 2.0D, z - offset, 0.0F, gap, gap, 9, 180, 0F, slots);
        generateSlot(x - width, y + offset / 2.0D, z - offset, 0.0F, gap, gap, 9, 0, 0F, slots);
        generateSlot(x - offset, y + offset / 2.0D, z + width, gap, gap, 0.0F, 9, 270, 0F, slots);
        generateSlot(x - offset, y + offset / 2.0D, z - width, gap, gap, 0.0F, 9, 90, 0F, slots);
        generateSlot(x - offset, y + height + 0.2, z - offset, gap, 0.0D, gap, 9, 0, 90F, slots);
        generateSlot(x - offset, y - 0.2, z - offset, gap, 0.0D, gap, 9, 0, -90F, slots);

        this.slots = ImmutableList.copyOf(slots);

        Random random = new Random();
        this.weakpoint = slots.get(random.nextInt(slots.size()));
    }

    private static void generateSlot(double x, double y, double z, double moveX, double moveY, double moveZ, int count, float yaw, float pitch, List<Slot> slots)
    {
        for (int i = 0; i < count; i++)
        {
            Slot slot;

            if (moveY == 0.0D)
            {
                slot = new Slot(x + moveX * (i / 3), y, z + moveZ * (i % 3), yaw, pitch);
            }
            else
            {
                slot = new Slot(x + moveX * (i % 3), y + moveY * (i / 3), z + moveZ * (i % 3), yaw, pitch);
            }

            slots.add(slot);
        }
    }

    public boolean stab(double x, double y, double z, BlockFace face, TapItemStack item)
    {
        Slot found = null;
        double ds = 0.0D;

        for (Slot slot : slots)
        {
            double cds = slot.pos.distance(x, y, z);

            if (ds == 0.0D || cds < ds)
            {
                found = slot;
                ds = cds;
            }
        }

        if (found != null && found.item == null)
        {
            if (found == weakpoint)
            {
                FireworkEffect effect = FireworkEffect.builder().type(FireworkEffect.Type.LARGE_BALL).color(0xFFFFFF).trail(true).flicker(true).build();
                Packet.EFFECT.firework(effect, pos.x, pos.y + 1.5D, pos.z).sendAll();
                destroy();
            }
            else
            {
                float yaw = 0.0F;
                float pitch = 0.0F;

                if (face == BlockFace.UP)
                {
                    pitch = 90.0F;
                }
                else if (face == BlockFace.DOWN)
                {
                    pitch = -90.0F;
                }
                else if (face == BlockFace.EAST)
                {
                    yaw = 0F;
                }
                else if (face == BlockFace.SOUTH)
                {
                    yaw = -90F;
                }
                else if (face == BlockFace.NORTH)
                {
                    yaw = 270F;
                }

                found.setItem(item, yaw, pitch);
            }
        }

        return false;
    }

    public void destroy()
    {
        Packet.ENTITY.destroy(stand.getId()).sendAll();
        for (Slot slot : slots)
        {
            slot.destroy();
        }

        PirateRoulettePlugin.instance.rouletteBlock = null;
    }

    public void spawnTo(Iterable<? extends Player> p)
    {
        Packet.ENTITY.spawnMob(stand.getBukkitEntity()).sendTo(p);
        Packet.ENTITY.metadata(stand.getBukkitEntity()).sendTo(p);
        Packet.ENTITY.equipment(stand.getId(), EquipmentSlot.HEAD, item).sendTo(p);
        CustomEntityPacket.register(stand.getId()).sendTo(p);
        CustomEntityPacket.scale(stand.getId(), 3.0F, 3.0F, 3.0F, 0).sendTo(p);

        for (Slot slot : slots)
        {
            slot.spawnTo(p);
        }
    }

    public void update()
    {
        for (Slot slot : slots)
        {
            if (slot.item == null)
            {
                Vector pos = slot.pos;
                Packet.EFFECT.particle(Particle.VILLAGER_HAPPY, (float) pos.x, (float) pos.y, (float) pos.z, 0F, 0F, 0F, 0F, 1).sendAll();
            }
        }
    }

    public BoundingBox getBox()
    {
        return box;
    }

    private static class Slot
    {
        private final Vector pos;

        private final TapArmorStand stand;

        private TapItemStack item;

        public Slot(double x, double y, double z, float yaw, float pitch)
        {
            this.pos = new Vector(x, y, z);
            this.stand = Tap.ENTITY.createEntity(ArmorStand.class);
            this.stand.setPositionAndRotation(x, y - 0.5, z, yaw, 0F);
            this.stand.setHeadPose(0, 0F, pitch + 45);
            this.stand.setInvisible(true);
        }

        public void spawnTo(Iterable<? extends Player> p)
        {
            Packet.ENTITY.spawnMob(stand.getBukkitEntity()).sendTo(p);
            Packet.ENTITY.metadata(stand.getBukkitEntity()).sendTo(p);

            if (item != null)
                Packet.ENTITY.equipment(stand.getId(), EquipmentSlot.HEAD, item).sendTo(p);
        }

        public void setItem(TapItemStack item, float yaw, float pitch)
        {
            this.item = item;
            stand.setPositionAndRotation(pos.x, pos.y - 0.5D, pos.z, yaw, 0F);
            stand.setHeadPose(0F, 0F, pitch + 45);

            Packet.ENTITY.equipment(stand.getId(), EquipmentSlot.HEAD, item).sendAll();
        }

        public void destroy()
        {
            Packet.ENTITY.destroy(stand.getId()).sendAll();
        }
    }
}
