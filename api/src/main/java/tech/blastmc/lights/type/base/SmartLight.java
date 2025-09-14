package tech.blastmc.lights.type.base;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.serialization.SerializableAs;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.ItemDisplay.ItemDisplayTransform;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import tech.blastmc.lights.type.model.Fixture;
import tech.blastmc.lights.type.model.Hued;
import tech.blastmc.lights.type.model.Mover;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@SerializableAs("SmartLight")
public class SmartLight extends Fixture implements Mover, Hued {

    int color;
    int pitch;
    int yaw;

    ItemDisplay base;
    ItemDisplay yoke;
    ItemDisplay body;
    ItemDisplay beam;

    private World world;
    private Location anchor;
    private Quaternionf faceQ;

    private final Vector3f baseToYoke = new Vector3f(0f, 0f, 0.05f);
    private final Vector3f yokeToBody = new Vector3f(0f, 0f, 0f);
    private final Vector3f bodyForward = new Vector3f(0f, 0f, 1.39f);

    private float beamLength = 4.0f;
    private Vector3f beamScale = new Vector3f(0.08f, 0.08f, beamLength);

    @Override
    public void handleIntensityChange(int intensity) {

    }

    @Override
    public void handleColorChange(int color) {

    }

    @Override
    public void handlePositionChange(int yaw, int pitch) {
        setPosition(yaw, pitch);
        applyTransforms();
    }

    @Override
    public void spawn(Location location, BlockFace face) {
        this.world = location.getWorld();

        var center = location.toCenterLocation();
        var n = face.getDirection();
        this.anchor = center.add(n.getX(), n.getY(), n.getZ());

        this.faceQ = faceBasis(face);

        this.base = spawnItemDisplay(new ItemStack(Material.PAPER), "minigames/blockparty/light/base");
        this.yoke = spawnItemDisplay(new ItemStack(Material.PAPER), "minigames/blockparty/light/yoke");
        this.body = spawnItemDisplay(new ItemStack(Material.PAPER), "minigames/blockparty/light/fixture");

        var beamItem = new ItemStack(Material.LEATHER_HORSE_ARMOR);
        tintLeather(beamItem, Color.WHITE);
        this.beam = spawnItemDisplay(beamItem,  "minigames/blockparty/light/glow_100");
        this.beam.setItemDisplayTransform(ItemDisplayTransform.GROUND);

        for (var d : new ItemDisplay[]{base, yoke, body, beam}) {
            d.setInterpolationDelay(0);
            d.setInterpolationDuration(1);
            d.setShadowStrength(0f);
            d.setBrightness(new Display.Brightness(15, 15));
            d.setViewRange(64f);
            d.setGlowColorOverride(null);
        }

        applyTransforms();
    }

    @Override
    public void setPosition(int yaw, int pitch) {
        this.yaw = yaw;
        this.pitch = Math.max(-135, Math.min(135, pitch));
    }

    private void applyTransforms() {
        if (world == null || anchor == null || faceQ == null) return;

        var basePos = anchor.clone();
        var baseRot = new Quaternionf(faceQ);
        setPose(base, basePos, baseRot, new Vector3f(1, 1, 1));

        var yokeRot = new Quaternionf(baseRot).rotateAxis((float) Math.toRadians(yaw), 0f, 0f, 1f);
        var yokePos = basePos.clone().add(rotated(baseRot, baseToYoke));
        setPose(yoke, yokePos, yokeRot, new Vector3f(1, 1, 1));

        var bodyRot = new Quaternionf(yokeRot).rotateAxis((float) Math.toRadians(pitch), 0f, 1f, 0f);
        var bodyPos = yokePos.clone().add(rotated(yokeRot, yokeToBody));
        setPose(body, bodyPos, bodyRot, new Vector3f(1, 1, 1));

        var beamRot = new Quaternionf(bodyRot);
        var beamPos = bodyPos.clone().add(rotated(bodyRot, bodyForward));
        setPose(beam, beamPos, beamRot, new Vector3f(1, 1, 1));
    }

    private ItemDisplay spawnItemDisplay(ItemStack item, String model) {
        var meta = item.getItemMeta();
        meta.setItemModel(NamespacedKey.minecraft(model));
        item.setItemMeta(meta);

        return world.spawn(anchor, ItemDisplay.class, d -> {
            d.setItemStack(item);
            d.setTransformation(identityXform());
            d.setBillboard(Display.Billboard.FIXED);
        });
    }

    private static Transformation identityXform() {
        return new Transformation(new Vector3f(), new Quaternionf(), new Vector3f(1, 1, 1), new Quaternionf());
    }

    private static final Quaternionf MODEL_FIX =
            new Quaternionf().rotationX((float) Math.toRadians(90));

    private void setPose(ItemDisplay d, Location pos, Quaternionf rot, Vector3f scale) {
        d.teleport(pos);
        d.setTransformation(new Transformation(
                new Vector3f(0, 0, 0),
                new Quaternionf(rot).mul(MODEL_FIX),
                new Vector3f(scale),
                new Quaternionf()
        ));
    }

    private static Vector rotated(Quaternionf q, Vector3f v) {
        var out = new Vector3f(v);
        q.transform(out);
        return new Vector(out.x, out.y, out.z);
    }

    private static Quaternionf faceBasis(BlockFace face) {
        Vector3f fwd; Vector3f up;

        switch (face) {
            case UP    -> { fwd = new Vector3f(0,  1,  0); up = new Vector3f(0, 0, -1); }
            case DOWN  -> { fwd = new Vector3f(0, -1,  0); up = new Vector3f(0, 0,  1); }
            case NORTH -> { fwd = new Vector3f(0,  0, -1); up = new Vector3f(0, 1,  0); }
            case SOUTH -> { fwd = new Vector3f(0,  0,  1); up = new Vector3f(0, 1,  0); }
            case WEST  -> { fwd = new Vector3f(-1, 0,  0); up = new Vector3f(0, 1,  0); }
            case EAST  -> { fwd = new Vector3f(1,  0,  0); up = new Vector3f(0, 1,  0); }
            default    -> { fwd = new Vector3f(0,  0,  1); up = new Vector3f(0, 1,  0); }
        }

        return new Quaternionf().identity().lookAlong(new Vector3f(fwd).negate(), up).conjugate().normalize();
    }

    private static void tintLeather(ItemStack item, org.bukkit.Color c) {
        if (!(item.getItemMeta() instanceof LeatherArmorMeta lam)) return;
        lam.setColor(c);
        item.setItemMeta(lam);
    }

    public SmartLight(Map<String, Object> map) {
        anchor = (Location) map.get("anchor");
        world = anchor.getWorld();
        if (world == null) return;
        yaw = (int) map.get("yaw");
        pitch = (int) map.get("pitch");
        color = (int) map.get("color");
        setIntensity((int) map.get("intensity"));
        double x = (double) map.get("x");
        double y = (double) map.get("y");
        double z = (double) map.get("z");
        double w = (double) map.get("w");
        faceQ = new Quaternionf(x, y, z, w);

        base = (ItemDisplay) world.getEntity(UUID.fromString((String) map.get("base")));
        yoke = (ItemDisplay) world.getEntity(UUID.fromString((String) map.get("yoke")));
        body = (ItemDisplay) world.getEntity(UUID.fromString((String) map.get("body")));
        beam = (ItemDisplay) world.getEntity(UUID.fromString((String) map.get("beam")));
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        return new HashMap<>() {{
            put("yaw", yaw);
            put("pitch", pitch);
            put("color", color);
            put("intensity", getIntensity());
            put("anchor", anchor);
            put("x", faceQ.x);
            put("y", faceQ.y);
            put("z", faceQ.z);
            put("w", faceQ.w);
            put("base", base.getUniqueId().toString());
            put("yoke", yoke.getUniqueId().toString());
            put("body", body.getUniqueId().toString());
            put("beam", beam.getUniqueId().toString());
        }};
    }
}
