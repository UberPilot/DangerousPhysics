package me.evillootly.physics.storage;

import org.bukkit.World;
import org.bukkit.configuration.serialization.ConfigurationSerializable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PhysicsConfig implements ConfigurationSerializable, Cloneable
{
    private boolean blockPhysicsEnabled;
    private boolean temperatureEnabled;
    private boolean fireSmokeEnabled;
    private boolean steamEnabled;
    private boolean enhancedFireEnabled;
    private boolean enhancedExplosionsEnabled;
    private double explosionPotency;
    private boolean ashEnabled;
    private boolean netherrackEnabled;
    private boolean doRealisticDrops;
    private List<String> worlds;

    public boolean isBlockPhysicsEnabled()
    {
        return blockPhysicsEnabled;
    }

    public boolean isTemperatureEnabled()
    {
        return temperatureEnabled;
    }

    public boolean isFireSmokeEnabled()
    {
        return fireSmokeEnabled;
    }

    public boolean isSteamEnabled()
    {
        return steamEnabled;
    }

    public boolean isEnhancedFireEnabled()
    {
        return enhancedFireEnabled;
    }

    public boolean isEnhancedExplosionsEnabled()
    {
        return enhancedExplosionsEnabled;
    }

    public double getExplosionPotency()
    {
        return explosionPotency;
    }

    public boolean isAshEnabled()
    {
        return ashEnabled;
    }

    public boolean isNetherrackEnabled()
    {
        return netherrackEnabled;
    }

    public boolean isDoRealisticDrops()
    {
        return doRealisticDrops;
    }

    public List<String> getWorlds()
    {
        return worlds;
    }

    private PhysicsConfig(boolean blockPhysicsEnabled, boolean temperatureEnabled, boolean fireSmokeEnabled, boolean steamEnabled,
                          boolean enhancedFireEnabled, boolean enhancedExplosionsEnabled, double explosionPotency, boolean ashEnabled,
                          boolean netherrackEnabled, boolean doRealisticDrops, List<String> worlds)
    {
        this.blockPhysicsEnabled = blockPhysicsEnabled;
        this.temperatureEnabled = temperatureEnabled;
        this.fireSmokeEnabled = fireSmokeEnabled;
        this.steamEnabled = steamEnabled;
        this.enhancedFireEnabled = enhancedFireEnabled;
        this.enhancedExplosionsEnabled = enhancedExplosionsEnabled;
        this.explosionPotency = explosionPotency;
        this.ashEnabled = ashEnabled;
        this.netherrackEnabled = netherrackEnabled;
        this.doRealisticDrops = doRealisticDrops;
        this.worlds = worlds;
    }

    @Override
    public Map<String, Object> serialize()
    {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("block-physics", this.blockPhysicsEnabled);
        map.put("temperature", this.temperatureEnabled);
        map.put("fire-smoke", this.fireSmokeEnabled);
        map.put("steam", this.steamEnabled);
        map.put("enhanced-fire", this.enhancedFireEnabled);
        map.put("enhanced-explosions", this.enhancedExplosionsEnabled);
        map.put("explosion-potency", this.explosionPotency);
        map.put("ash", this.ashEnabled);
        map.put("netherrack", this.netherrackEnabled);
        map.put("realistic-drops", this.doRealisticDrops);
        map.put("worlds", this.worlds);
        return map;
    }

    public static PhysicsConfig deserialize(Map<String, Object> map)
    {
        PhysicsConfigBuilder builder = new PhysicsConfigBuilder();
        // Handle block physics.
        if(map.containsKey("block-physics")
                && map.get("block-physics") != null
                && (map.get("block-physics") instanceof Boolean))
        {
            builder.blockPhysicsEnabled((boolean) map.get("block-physics"));
        }
        else builder.blockPhysicsEnabled(false);

        // Handle temperature.
        if(map.containsKey("temperature")
                && map.get("temperature") != null
                && (map.get("temperature") instanceof Boolean))
        {
            builder.temperatureEnabled((boolean) map.get("temperature"));
        }
        else builder.temperatureEnabled(false);

        // Handle fire smoke.
        if(map.containsKey("fire-smoke")
                && map.get("fire-smoke") != null
                && (map.get("fire-smoke") instanceof Boolean))
        {
            builder.fireSmokeEnabled((boolean) map.get("fire-smoke"));
        }
        else builder.fireSmokeEnabled(false);

        // Handle steam.
        if(map.containsKey("steam")
                && map.get("steam") != null
                && (map.get("steam") instanceof Boolean))
        {
            builder.steamEnabled((boolean) map.get("steam"));
        }
        else builder.steamEnabled(false);

        // Handle enhanced fire.
        if(map.containsKey("enhanced-fire")
                && map.get("enhanced-fire") != null
                && (map.get("enhanced-fire") instanceof Boolean))
        {
            builder.enhancedFireEnabled((boolean) map.get("enhanced-fire"));
        }
        else builder.enhancedFireEnabled(false);

        // Handle enhanced explosions.
        if(map.containsKey("enhanced-explosions")
                && map.get("enhanced-explosions") != null
                && (map.get("enhanced-explosions") instanceof Boolean))
        {
            builder.enhancedExplosionsEnabled((boolean) map.get("enhanced-explosions"));
        }
        else builder.enhancedExplosionsEnabled(false);

        // Handle explosion potency.
        if(map.containsKey("explosion-potency")
                && map.get("explosion-potency") != null
                && (map.get("explosion-potency") instanceof Double))
        {
            builder.explosionPotency((double) map.get("explosion-potency"));
        }
        else builder.explosionPotency(1.0d);

        // Handle ash.
        if(map.containsKey("ash")
                && map.get("ash") != null
                && (map.get("ash") instanceof Boolean))
        {
            builder.ashEnabled((boolean) map.get("ash"));
        }
        else builder.ashEnabled(false);

        // Handle netherrack.
        if(map.containsKey("netherrack")
                && map.get("netherrack") != null
                && (map.get("netherrack") instanceof Boolean))
        {
            builder.netherrackEnabled((boolean) map.get("netherrack"));
        }
        else builder.netherrackEnabled(false);

        // Handle realistic drops.
        if(map.containsKey("realistic-drops")
                && map.get("realistic-drops") != null
                && (map.get("realistic-drops") instanceof Boolean))
        {
            builder.doRealisticDrops((boolean) map.get("realistic-drops"));
        }
        else builder.doRealisticDrops(false);

        // Handle worlds.
        if(map.containsKey("worlds")
                && map.get("worlds") != null
                && (map.get("worlds") instanceof List))
        {
            builder.worlds((List<String>) map.get("worlds"));
        }
        else builder.worlds(Collections.singletonList("world"));
        return builder.build();
    }

    public static final class PhysicsConfigBuilder
    {
        private boolean blockPhysicsEnabled;
        private boolean temperatureEnabled;
        private boolean fireSmokeEnabled;
        private boolean steamEnabled;
        private boolean enhancedFireEnabled;
        private boolean enhancedExplosionsEnabled;
        private double explosionPotency;
        private boolean ashEnabled;
        private boolean netherrackEnabled;
        private boolean doRealisticDrops;
        private List<String> worlds;

        private PhysicsConfigBuilder() {}

        public static PhysicsConfigBuilder aPhysicsConfig() { return new PhysicsConfigBuilder(); }

        public PhysicsConfigBuilder blockPhysicsEnabled(boolean blockPhysicsEnabled)
        {
            this.blockPhysicsEnabled = blockPhysicsEnabled;
            return this;
        }

        public PhysicsConfigBuilder temperatureEnabled(boolean temperatureEnabled)
        {
            this.temperatureEnabled = temperatureEnabled;
            return this;
        }

        public PhysicsConfigBuilder fireSmokeEnabled(boolean fireSmokeEnabled)
        {
            this.fireSmokeEnabled = fireSmokeEnabled;
            return this;
        }

        public PhysicsConfigBuilder steamEnabled(boolean steamEnabled)
        {
            this.steamEnabled = steamEnabled;
            return this;
        }

        public PhysicsConfigBuilder enhancedFireEnabled(boolean enhancedFireEnabled)
        {
            this.enhancedFireEnabled = enhancedFireEnabled;
            return this;
        }

        public PhysicsConfigBuilder enhancedExplosionsEnabled(boolean enhancedExplosionsEnabled)
        {
            this.enhancedExplosionsEnabled = enhancedExplosionsEnabled;
            return this;
        }

        public PhysicsConfigBuilder explosionPotency(double explosionPotency)
        {
            this.explosionPotency = explosionPotency;
            return this;
        }

        public PhysicsConfigBuilder ashEnabled(boolean ashEnabled)
        {
            this.ashEnabled = ashEnabled;
            return this;
        }

        public PhysicsConfigBuilder netherrackEnabled(boolean netherrackEnabled)
        {
            this.netherrackEnabled = netherrackEnabled;
            return this;
        }

        public PhysicsConfigBuilder doRealisticDrops(boolean doRealisticDrops)
        {
            this.doRealisticDrops = doRealisticDrops;
            return this;
        }

        public PhysicsConfigBuilder worlds(List<String> worlds)
        {
            this.worlds = worlds;
            return this;
        }

        public PhysicsConfig build() { return new PhysicsConfig(blockPhysicsEnabled, temperatureEnabled, fireSmokeEnabled, steamEnabled, enhancedFireEnabled, enhancedExplosionsEnabled, explosionPotency, ashEnabled, netherrackEnabled, doRealisticDrops, worlds); }
    }
}
