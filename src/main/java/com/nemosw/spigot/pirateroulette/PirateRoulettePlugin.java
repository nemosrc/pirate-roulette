package com.nemosw.spigot.pirateroulette;

import com.nemosw.mox.math.Vector;
import com.nemosw.spigot.tap.Tap;
import com.nemosw.spigot.tap.entity.TapPlayer;
import com.nemosw.spigot.tap.math.RayTraceResult;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collections;
import java.util.EnumSet;

public final class PirateRoulettePlugin extends JavaPlugin implements Listener
{

    public static PirateRoulettePlugin instance;
    RouletteBlock rouletteBlock;

    @Override
    public void onEnable()
    {
        getServer().getPluginManager().registerEvents(this, this);

        getServer().getScheduler().runTaskTimer(this, () -> {
            if (rouletteBlock != null)
                rouletteBlock.update();
        }, 0L, 1L);

        instance = this;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        if (rouletteBlock != null)
            rouletteBlock.destroy();
        else
        {
            TapPlayer player = ((Tap.ENTITY.wrapEntity((Player) sender)));
            rouletteBlock = new RouletteBlock(0, 10, 0, player.getHeldItemMainHand());
            rouletteBlock.spawnTo(Bukkit.getOnlinePlayers());
        }

        return true;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event)
    {
        if (rouletteBlock == null)
            return;

        getServer().getScheduler().runTaskLater(this, () -> rouletteBlock.spawnTo(Collections.singleton(event.getPlayer())), 10L);
    }

    private static final EnumSet<Material> SWORDS = EnumSet.of(Material.DIAMOND_SWORD, Material.GOLD_SWORD, Material.WOOD_SWORD, Material.STONE_SWORD, Material.IRON_SWORD);

    @EventHandler
    public void onPlayerAnimation(PlayerAnimationEvent event)
    {
        if (rouletteBlock == null)
            return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item != null && SWORDS.contains(item.getType()))
        {
            Location loc = event.getPlayer().getEyeLocation();
            org.bukkit.util.Vector v = loc.getDirection();
            Vector from = new Vector(loc.getX(), loc.getY(), loc.getZ());
            Vector to = new Vector(v.getX(), v.getY(), v.getZ()).multiply(8).add(from);
            RayTraceResult result = Tap.MATH.newRayTraceCalculator(from, to).calculate(rouletteBlock.getBox());

            if (result != null)
            {
                TapPlayer tp = Tap.ENTITY.wrapEntity(event.getPlayer());
                rouletteBlock.stab(result.getX(), result.getY(), result.getZ(), result.getFace(), tp.getHeldItemMainHand());
            }
        }
    }
}
