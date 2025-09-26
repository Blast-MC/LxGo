package tech.blastmc.lights.handler;

import org.apache.commons.lang3.function.TriFunction;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import tech.blastmc.lights.LxBoard;
import tech.blastmc.lights.cue.Cue;
import tech.blastmc.lights.cue.CueTimes;
import tech.blastmc.lights.cue.Permutation;
import tech.blastmc.lights.cue.Permutation.Color;
import tech.blastmc.lights.cue.Permutation.Intensity;
import tech.blastmc.lights.cue.Permutation.Pitch;
import tech.blastmc.lights.cue.Permutation.StopEffect;
import tech.blastmc.lights.cue.Permutation.Yaw;
import tech.blastmc.lights.cue.SimulatedCue;
import tech.blastmc.lights.effect.Effect;
import tech.blastmc.lights.handler.interpolators.ColorEffectInterpolator;
import tech.blastmc.lights.handler.interpolators.ColorInterpolator;
import tech.blastmc.lights.handler.interpolators.DirectionEffectInterpolator;
import tech.blastmc.lights.handler.interpolators.DirectionInterpolator;
import tech.blastmc.lights.handler.interpolators.IntensityEffectInterpolator;
import tech.blastmc.lights.handler.interpolators.IntensityInterpolator;
import tech.blastmc.lights.handler.interpolators.Interpolator;
import tech.blastmc.lights.map.Channel;
import tech.blastmc.lights.type.model.Fixture;
import tech.blastmc.lights.type.model.Hued;
import tech.blastmc.lights.type.model.Mover;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;

public class CueHandler {

    private final Plugin plugin;
    private final LxBoard board;
    private final Cue cue;
    private final CueTimes times;
    private BukkitTask autoFollowTask;

    private final List<Interpolator> interpolators;
    private final SimulatedCue simulatedCue;

    public CueHandler(Plugin plugin, LxBoard board, Cue cue, CueTimes times, List<Interpolator> interpolators, SimulatedCue sim) {
        this.plugin = plugin;
        this.board = board;
        this.cue = cue;
        this.times = times;
        this.interpolators = interpolators;
        this.simulatedCue = sim;
    }

    public boolean isDone() {
        return interpolators.isEmpty() || interpolators.stream().noneMatch(interpolator -> !interpolator.isDone());
    }

    public void start() {
        // ---- PHASE 0: StopEffect (ID-specific falloff) ----
        if (cue.getChannelDiffs() != null) {
            for (var e : cue.getChannelDiffs().entrySet()) {
                int chId = e.getKey();
                var perms = e.getValue();
                for (Permutation p : perms) {
                    if (p instanceof StopEffect se) {
                        int stopId = se.getValue();
                        // find static targets in THIS cue for this channel (if provided)
                        var t = findTargetsForChannelInCue(cue, chId);

                        // tell matching running effect interpolators on this channel to fall off
                        for (var inter : interpolators) {
                            if (inter.getChannel() != chId || inter.isDone()) continue;

                            if (inter instanceof DirectionEffectInterpolator dei && dei.getEffect().getId() == stopId) {
                                int landYaw   = (t.yaw   != null) ? t.yaw   : currentYawOfChannel(chId, simulatedCue);
                                int landPitch = (t.pitch != null) ? t.pitch : currentPitchOfChannel(chId, simulatedCue);
                                dei.beginFalloffTo(landYaw, landPitch, times.getDirection(), simulatedCue);
                            } else if (inter instanceof ColorEffectInterpolator cei && cei.getEffect().getId() == stopId) {
                                int landRgb = (t.color != null) ? t.color : currentColorOfChannel(chId, simulatedCue);
                                cei.beginFalloffTo(landRgb, times.getColor(), simulatedCue);
                            } else if (inter instanceof IntensityEffectInterpolator iei && iei.getEffect().getId() == stopId) {
                                int landLevel = (t.intensity != null) ? t.intensity : currentIntensityOfChannel(chId, simulatedCue);
                                iei.beginFalloffTo(landLevel, times.getIntensity(), simulatedCue);
                            }
                        }
                    }
                }
            }
        }

        // Group-level StopEffect: apply to all group channels
        if (cue.getGroupDiffs() != null) {
            for (var e : cue.getGroupDiffs().entrySet()) {
                var group = board.getGroups().stream().filter(g -> g.getId() == e.getKey()).findFirst().orElse(null);
                if (group == null || group.getChannels() == null) continue;
                var channels = group.getChannels();

                for (Permutation p : e.getValue()) {
                    if (!(p instanceof StopEffect se)) continue;
                    int stopId = se.getValue();

                    // pre-compute group-level static targets (if present)
                    var gt = findTargetsForGroupInCue(cue, e.getKey());

                    for (int chId : channels) {
                        var t = gt.orElseGet(() -> findTargetsForChannelInCue(cue, chId)); // prefer group targets, fallback to per-channel

                        for (var inter : interpolators) {
                            if (inter.getChannel() != chId || inter.isDone()) continue;

                            if (inter instanceof DirectionEffectInterpolator dei && dei.getEffect().getId() == stopId) {
                                int landYaw   = (t.yaw   != null) ? t.yaw   : currentYawOfChannel(chId, simulatedCue);
                                int landPitch = (t.pitch != null) ? t.pitch : currentPitchOfChannel(chId, simulatedCue);
                                dei.beginFalloffTo(landYaw, landPitch, times.getDirection(), simulatedCue);
                            } else if (inter instanceof ColorEffectInterpolator cei && cei.getEffect().getId() == stopId) {
                                int landRgb = (t.color != null) ? t.color : currentColorOfChannel(chId, simulatedCue);
                                cei.beginFalloffTo(landRgb, times.getColor(), simulatedCue);
                            } else if (inter instanceof IntensityEffectInterpolator iei && iei.getEffect().getId() == stopId) {
                                int landLevel = (t.intensity != null) ? t.intensity : currentIntensityOfChannel(chId, simulatedCue);
                                iei.beginFalloffTo(landLevel, times.getIntensity(), simulatedCue);
                            }
                        }
                    }
                }
            }
        }

        class DirEff {
            Effect effect; int periodTicks; int offsetTicks; double expandSeconds;
        }
        class ColEff {
            Effect effect; int periodTicks; int offsetTicks; double expandSeconds;
        }
        class IntEff {
            Effect effect; int periodTicks; int offsetTicks; double expandSeconds;
        }
        Map<Integer, DirEff> dirEffects = new HashMap<>();
        Map<Integer, ColEff> colorEffects = new HashMap<>();
        Map<Integer, IntEff> intensityEffects = new HashMap<>();

        // Static targets collected per channel (only used if no effect of that type on that channel)
        Map<Integer, Integer> targetYawByCh = new HashMap<>();
        Map<Integer, Integer> targetPitchByCh = new HashMap<>();
        Map<Integer, Integer> targetColorByCh = new HashMap<>();
        Map<Integer, Integer> targetIntensityByCh = new HashMap<>();

        // Compute group offsets for an effect permutation
        // periodTicks is derived from the permutation/effect; adjust if you store it elsewhere
        TriFunction<Integer,Integer,Integer,Integer> orderedOffset = (periodTicks, i, n) -> {
            if (n <= 1) return 0;
            return (int) Math.round((i / (double) n) * periodTicks) % Math.max(1, periodTicks);
        };

        // ---- 1) GROUP DIFFS: effects here get offsets across the group's channels ----
        if (cue.getGroupDiffs() != null) {
            for (var entry : cue.getGroupDiffs().entrySet()) {
                int groupId = entry.getKey();
                var group = board.getGroups().stream().filter(g -> g.getId() == groupId).findFirst().orElse(null);
                if (group == null) continue;
                var channels = group.getChannels();
                if (channels == null || channels.isEmpty()) continue;

                for (Permutation perm : entry.getValue()) {
                    if (!(perm instanceof Permutation.Effect ep)) continue; // your Effect permutation subclass
                    var effect = board.getEffectRegistry().get(ep.getValue());
                    if (effect == null) continue;

                    // Determine period + offsets
                    int periodTicks = Math.max(1, (int) Math.round(effect.getDurationInSeconds() * 20.0));
                    for (int i = 0; i < channels.size(); i++) {
                        int chId = new ArrayList<>(channels).get(i);
                        int offsetTicks = switch (effect.getOffsetType()) {
                            case SYNCHRONIZED -> 0;
                            case OFFSET_ORDERED -> orderedOffset.apply(periodTicks, i, channels.size());
                            case OFFSET_RANDOM -> (int) (Math.random() * periodTicks);
                        };
                        if (ep.getOffset() != null)
                            offsetTicks = ep.getOffset();

                        switch (effect.getEffectType()) {
                            case DIRECTION -> {
                                // Direction times drive expand
                                DirEff de = new DirEff();
                                de.effect = effect;
                                de.periodTicks = periodTicks;
                                de.offsetTicks = offsetTicks;
                                de.expandSeconds = times.getDirection();
                                dirEffects.put(chId, de);
                            }
                            case COLOR -> {
                                ColEff ce = new ColEff();
                                ce.effect = effect;
                                ce.periodTicks = periodTicks;
                                ce.offsetTicks = offsetTicks;
                                ce.expandSeconds = times.getColor();
                                colorEffects.put(chId, ce);
                            }
                            case INTENSITY -> {
                                IntEff ie = new IntEff();
                                ie.effect = effect;
                                ie.periodTicks = periodTicks;
                                ie.offsetTicks = offsetTicks;
                                ie.expandSeconds = times.getIntensity();
                                intensityEffects.put(chId, ie);
                            }
                            default -> {}
                        }
                    }
                }

                Integer targetYaw = null, targetPitch = null, targetColor = null, targetIntensity = null;

                for (Permutation p : entry.getValue()) {
                    if (p instanceof Permutation.Yaw)   targetYaw = p.getValue();
                    if (p instanceof Permutation.Pitch) targetPitch = p.getValue();
                    if (p instanceof Permutation.Color) targetColor = p.getValue();
                    if (p instanceof Permutation.Intensity) targetIntensity = p.getValue();
                }

                for (int i = 0; i < channels.size(); i++) {
                    int chId = new ArrayList<>(channels).get(i);
                    if (targetYaw != null)   targetYawByCh.put(chId, targetYaw);
                    if (targetPitch != null) targetPitchByCh.put(chId, targetPitch);
                    if (targetColor != null) targetColorByCh.put(chId, targetColor);
                    if (targetIntensity != null) targetIntensityByCh.put(chId, targetIntensity);
                }
            }
        }

        // ---- 2) CHANNEL DIFFS: effects here start on that channel (no offsets) ----
        if (cue.getChannelDiffs() != null) {
            for (var entry : cue.getChannelDiffs().entrySet()) {
                int chId = entry.getKey();
                var perms = entry.getValue();

                // Collect static targets (we’ll only use them if no effect of that type exists on this channel)
                Integer targetYaw = null, targetPitch = null, targetColor = null, targetIntensity = null;

                // Scan for effects (priority)
                for (Permutation p : perms) {
                    if (p instanceof Permutation.Effect ep) {
                        board.debug("Found effect perm");
                        var effect = board.getEffectRegistry().get(ep.getValue());
                        if (effect == null) continue;
                        board.debug("Effect: " + ep.getValue());
                        int periodTicks = Math.max(1, (int) Math.round(effect.getDurationInSeconds() * 20.0));
                        int offsetTicks = 0; // channel-level: no group offset
                        if (ep.getOffset() != null)
                            offsetTicks = ep.getOffset();

                        switch (effect.getEffectType()) {
                            case DIRECTION -> {
                                DirEff de = new DirEff();
                                de.effect = effect;
                                de.periodTicks = periodTicks;
                                de.offsetTicks = offsetTicks;
                                de.expandSeconds = times.getDirection();
                                dirEffects.put(chId, de);
                            }
                            case COLOR -> {
                                ColEff ce = new ColEff();
                                ce.effect = effect;
                                ce.periodTicks = periodTicks;
                                ce.offsetTicks = offsetTicks;
                                ce.expandSeconds = times.getColor();
                                colorEffects.put(chId, ce);
                            }
                            case INTENSITY -> {
                                IntEff ie = new IntEff();
                                ie.effect = effect;
                                ie.periodTicks = periodTicks;
                                ie.offsetTicks = offsetTicks;
                                ie.expandSeconds = times.getIntensity();
                                intensityEffects.put(chId, ie);
                            }
                            default -> {}
                        }
                    }
                }

                // Gather static targets (kept only if no effect of that type exists for this channel)
                for (Permutation p : perms) {
                    if (p instanceof Permutation.Yaw)   targetYaw = p.getValue();
                    if (p instanceof Permutation.Pitch) targetPitch = p.getValue();
                    if (p instanceof Permutation.Color) targetColor = p.getValue();
                    if (p instanceof Permutation.Intensity) targetIntensity = p.getValue();
                }

                if (targetYaw != null)   targetYawByCh.put(chId, targetYaw);
                if (targetPitch != null) targetPitchByCh.put(chId, targetPitch);
                if (targetColor != null) targetColorByCh.put(chId, targetColor);
                if (targetIntensity != null) targetIntensityByCh.put(chId, targetIntensity);
            }
        }

        // ---- 3) Materialize effects / static interpolators per channel & fixture ----
        // Merge keys from both sources
        Set<Integer> allChannels = new HashSet<>();
        if (cue.getChannelDiffs() != null) allChannels.addAll(cue.getChannelDiffs().keySet());
        if (cue.getGroupDiffs() != null) {
            for (var entry : cue.getGroupDiffs().entrySet()) {
                var g = board.getGroups().stream().filter(gr -> gr.getId() == entry.getKey()).findFirst().orElse(null);
                if (g != null && g.getChannels() != null) allChannels.addAll(g.getChannels());
            }
        }

        var interpolators = new ArrayList<Interpolator>();

        for (int chId : allChannels) {
            Channel channel = board.getChannels().get(chId);
            if (channel == null) continue;

            boolean hasDirEffectCue = dirEffects.containsKey(chId);
            boolean hasColEffectCue = colorEffects.containsKey(chId);
            boolean hasIntEffectCue = intensityEffects.containsKey(chId);

            board.debug("ch: " + chId + ", hasDirEffectCue: " + hasDirEffectCue + ", hasColEffectCue: " + hasColEffectCue + ", hasIntEffectCue: " + hasIntEffectCue);

            boolean blockDirByOld = hasActiveDirectionEffect(chId);
            boolean blockColByOld = hasActiveColorEffect(chId);
            boolean blockIntByOld = hasActiveIntensityEffect(chId);

            board.debug("ch: " + chId + ", blockDirByOld: " + blockDirByOld + ", blockColByOld: " + blockColByOld + ", blockIntByOld: " + blockIntByOld);

            // Treat an active falling-off effect as "effect present" so we don't start a competing static fade.
            boolean hasDirEffect = hasDirEffectCue || blockDirByOld;
            boolean hasColEffect = hasColEffectCue || blockColByOld;
            boolean hasIntEffect = hasIntEffectCue || blockIntByOld;

            board.debug("ch: " + chId + ", hasDirEffect: " + hasDirEffect + ", hasColEffect: " + hasColEffect + ", hasIntEffect: " + hasIntEffect);

            Integer targetYaw   = hasDirEffect ? null : targetYawByCh.get(chId);
            Integer targetPitch = hasDirEffect ? null : targetPitchByCh.get(chId);
            Integer targetColor = hasColEffect ? null : targetColorByCh.get(chId);
            Integer targetInt   = hasIntEffect ? null : targetIntensityByCh.get(chId);

            for (Fixture fixture : channel.getAddresses()) {

                // ---- INTENSITY ----
                if (hasIntEffect && fixture != null) {
                    IntEff ie = intensityEffects.get(chId);
                    if (ie != null)
                        if (activeIntensityEffect(chId, ie.effect.getId()) == null) {
                            var inter = new IntensityEffectInterpolator(
                                    board, chId, fixture, ie.effect, ie.periodTicks, ie.offsetTicks, currentIntensityOfChannel(chId, simulatedCue), simulatedCue);
                            inter.beginExpand(ie.expandSeconds);
                            interpolators.add(inter);
                        } // else: already running this effect on this channel → do nothing
                } else if (!blockIntByOld && targetInt != null) {
                    interpolators.add(new IntensityInterpolator(
                            board, chId, currentIntensityOfChannel(chId, simulatedCue), targetInt, times.getIntensity(), fixture, simulatedCue));
                }

                // ---- COLOR ----
                if (fixture instanceof Hued hued) {
                    if (hasColEffect) {
                        ColEff ce = colorEffects.get(chId);
                        board.debug("ColEff: " + ce);
                        if (ce != null)
                            if (activeColorEffect(chId, ce.effect.getId()) == null) {
                                var inter = new ColorEffectInterpolator(
                                        board, chId, fixture, ce.effect, ce.periodTicks, ce.offsetTicks, currentColorOfChannel(chId, simulatedCue), simulatedCue);
                                inter.beginExpand(ce.expandSeconds);
                                interpolators.add(inter);
                            }
                    } else if (!blockColByOld && targetColor != null) {
                        interpolators.add(new ColorInterpolator(
                                board, chId, currentColorOfChannel(chId, simulatedCue), targetColor, times.getColor(), hued, simulatedCue));
                    }
                }

                // ---- DIRECTION ----
                if (fixture instanceof Mover mover) {
                    if (hasDirEffect) {
                        DirEff de = dirEffects.get(chId);
                        if (de != null)
                            if (activeDirEffect(chId, de.effect.getId()) == null) {
                                var inter = new DirectionEffectInterpolator(
                                        board, chId, fixture, de.effect, de.periodTicks, de.offsetTicks, currentYawOfChannel(chId, simulatedCue), currentPitchOfChannel(chId, simulatedCue), simulatedCue);
                                inter.beginExpand(de.expandSeconds);
                                interpolators.add(inter);
                            }
                    } else if (!blockDirByOld && (targetYaw != null || targetPitch != null)) {
                        int endYaw = (targetYaw == null ? currentYawOfChannel(chId, simulatedCue) : targetYaw);
                        int endPitch = (targetPitch == null ? currentPitchOfChannel(chId, simulatedCue) : targetPitch);
                        interpolators.add(new DirectionInterpolator(
                                board, chId, currentYawOfChannel(chId, simulatedCue), endYaw, currentPitchOfChannel(chId, simulatedCue), endPitch, times.getDirection(), mover, simulatedCue));
                    }
                }
            }
        }

        interpolators.forEach(Interpolator::start);
        this.interpolators.addAll(interpolators);

        if (times.getAutoFollow() != -1 && simulatedCue == null)
            autoFollowTask = new BukkitRunnable() {
                @Override
                public void run() {
                    board.go();
                }
            }.runTaskLater(plugin, Math.max(1, Math.round(times.getAutoFollow() * 20)));
    }

    public void stopConflicting(Cue nextCue, CueTimes times, SimulatedCue simulatedCue) {
        board.debug("Stopping conflicting");

        if (autoFollowTask != null)
            autoFollowTask.cancel();

        // --- Gather per-channel intentions from the next cue ---
        // Static targets (used as landing points when switching from effect -> static)
        Map<Integer, Integer> targetYaw   = new HashMap<>();
        Map<Integer, Integer> targetPitch = new HashMap<>();
        Map<Integer, Integer> targetColor = new HashMap<>();
        Map<Integer, Integer> targetInt   = new HashMap<>();

        // Channels that have static updates of each type in next cue
        Set<Integer> dirStaticCh = new HashSet<>();
        Set<Integer> colStaticCh = new HashSet<>();
        Set<Integer> intStaticCh = new HashSet<>();

        // Effects to KEEP running (same-id) per channel/type in next cue
        Map<Integer, Set<Integer>> keepDirIds = new HashMap<>();
        Map<Integer, Set<Integer>> keepColIds = new HashMap<>();
        Map<Integer, Set<Integer>> keepIntIds = new HashMap<>();

        // Helper
        BiConsumer<Integer, List<Permutation>> collectForChannel = (chId, perms) -> {
            for (Permutation p : perms) {
                if (p instanceof Permutation.Yaw y)   { dirStaticCh.add(chId); targetYaw.put(chId, y.getValue()); }
                else if (p instanceof Permutation.Pitch pt) { dirStaticCh.add(chId); targetPitch.put(chId, pt.getValue()); }
                else if (p instanceof Permutation.Color c)  { colStaticCh.add(chId); targetColor.put(chId, c.getValue()); }
                else if (p instanceof Permutation.Intensity i) { intStaticCh.add(chId); targetInt.put(chId, i.getValue()); }
                else if (p instanceof Permutation.Effect ep) {
                    var effect = board.getEffectRegistry().get(ep.getValue());
                    if (effect == null) continue;
                    switch (effect.getEffectType()) {
                        case DIRECTION -> keepDirIds.computeIfAbsent(chId, k -> new HashSet<>()).add(effect.getId());
                        case COLOR -> keepColIds.computeIfAbsent(chId, k -> new HashSet<>()).add(effect.getId());
                        case INTENSITY -> keepIntIds.computeIfAbsent(chId, k -> new HashSet<>()).add(effect.getId());
                    }
                }
            }
        };

        // Channel diffs
        if (nextCue.getChannelDiffs() != null) {
            board.debug("ChannelDiffs not null");

            for (var e : nextCue.getChannelDiffs().entrySet()) {
                board.debug("Diff for chan " + e.getKey() + ": " + e.getValue());
                collectForChannel.accept(e.getKey(), e.getValue());
            }
        }

        // Group diffs (apply to all channels in the group)
        if (nextCue.getGroupDiffs() != null) {
            for (var e : nextCue.getGroupDiffs().entrySet()) {
                var group = board.getGroups().stream().filter(g -> g.getId() == e.getKey()).findFirst().orElse(null);
                if (group == null || group.getChannels() == null) continue;
                var channels = group.getChannels();
                for (int chId : channels) {
                    collectForChannel.accept(chId, e.getValue());
                }
            }
        }

        // --- Resolve conflicts against currently running interpolators/effects ---
        for (var inter : interpolators) {
            int ch = inter.getChannel();
            if (inter.isDone()) {
                board.debug("Interpolator " + inter.getClass().getSimpleName() + " (" + inter.getId() + ") is done");
                continue;
            }

            board.debug("Interpolator " + inter.getClass().getSimpleName() + " (" + inter.getId() + ") is not done");

            // DIRECTION
            if (inter instanceof DirectionEffectInterpolator dei) {
                // If next cue reasserts SAME effect id on this channel -> let it keep running
                Set<Integer> keepIds = keepDirIds.getOrDefault(ch, Collections.emptySet());
                if (keepIds.contains(dei.getEffect().getId())) continue;

                // If next cue has a static direction OR a different direction effect -> fall off
                boolean nextHasDirChange = dirStaticCh.contains(ch) || !keepIds.isEmpty();
                if (nextHasDirChange) {
                    int landYaw   = targetYaw.getOrDefault(ch, currentYawOfChannel(ch, simulatedCue));
                    int landPitch = targetPitch.getOrDefault(ch, currentPitchOfChannel(ch, simulatedCue));
                    dei.beginFalloffTo(landYaw, landPitch, times.getDirection(), simulatedCue);
                }
                continue;
            }
            if (inter instanceof DirectionInterpolator) {
                // Stop static movement if next cue changes direction (static or effect)
                boolean nextHasDirChange = dirStaticCh.contains(ch) || keepDirIds.containsKey(ch);
                if (nextHasDirChange && simulatedCue == null) inter.stop();
                continue;
            }

            // COLOR
            if (inter instanceof ColorEffectInterpolator cei) {
                Set<Integer> keepIds = keepColIds.getOrDefault(ch, Collections.emptySet());
                board.debug("keepColIds: " + keepIds);
                board.debug("colStaticCh: " +  colStaticCh);
                if (keepIds.contains(cei.getEffect().getId())) continue;

                boolean nextHasColorChange = colStaticCh.contains(ch) || !keepIds.isEmpty();
                if (nextHasColorChange) {
                    board.debug("Beginning color falloff");
                    board.debug("Sim: " + (simulatedCue != null));

                    int landRgb = targetColor.getOrDefault(ch, currentColorOfChannel(ch, simulatedCue));
                    cei.beginFalloffTo(landRgb, times.getColor(), simulatedCue);
                }
                continue;
            }
            if (inter instanceof ColorInterpolator) {
                boolean nextHasColorChange = colStaticCh.contains(ch) || keepColIds.containsKey(ch);
                if (nextHasColorChange && simulatedCue == null) inter.stop();
                continue;
            }

            // INTENSITY
            if (inter instanceof IntensityEffectInterpolator iei) {
                Set<Integer> keepIds = keepIntIds.getOrDefault(ch, Collections.emptySet());
                if (keepIds.contains(iei.getEffect().getId())) continue;

                boolean nextHasIntChange = intStaticCh.contains(ch) || !keepIds.isEmpty();
                if (nextHasIntChange) {
                    int land = targetInt.getOrDefault(ch, currentIntensityOfChannel(ch, simulatedCue));
                    iei.beginFalloffTo(land, times.getIntensity(), simulatedCue);
                }
                continue;
            }
            if (inter instanceof IntensityInterpolator) {
                boolean nextHasIntChange = intStaticCh.contains(ch) || keepIntIds.containsKey(ch);
                if (nextHasIntChange && simulatedCue == null) inter.stop();
            }
        }
    }

    private int currentIntensityOfChannel(int channelId, SimulatedCue sim) {
        if (sim != null)
            return sim.getValue(channelId, Intensity.class);
        var ch = board.getChannels().get(channelId);
        if (ch == null) return 0;
        for (Fixture f : ch.getAddresses()) return f.getIntensity();
        return 0;
    }

    private int currentColorOfChannel(int channelId, SimulatedCue sim) {
        if (sim != null)
            return sim.getValue(channelId, Color.class);
        var ch = board.getChannels().get(channelId);
        if (ch == null) return 0;
        for (Fixture f : ch.getAddresses())
            if (f instanceof Hued h) return h.getColor();
        return 0;
    }

    private int currentYawOfChannel(int channelId, SimulatedCue sim) {
        if (sim != null)
            return sim.getValue(channelId, Yaw.class);
        var ch = board.getChannels().get(channelId);
        if (ch == null) return 0;
        for (Fixture f : ch.getAddresses()) if (f instanceof Mover m) return m.getYaw();
        return 0;
    }

    private int currentPitchOfChannel(int channelId, SimulatedCue sim) {
        if (sim != null)
            return sim.getValue(channelId, Pitch.class);
        var ch = board.getChannels().get(channelId);
        if (ch == null) return 0;
        for (Fixture f : ch.getAddresses()) if (f instanceof Mover m) return m.getPitch();
        return 0;
    }

    private boolean hasActiveDirectionEffect(int channelId) {
        if (simulatedCue != null)
            return false;
        return interpolators.stream()
                .anyMatch(i -> i instanceof DirectionEffectInterpolator && i.getChannel() == channelId && !i.isDone());
    }
    private boolean hasActiveColorEffect(int channelId) {
        if (simulatedCue != null)
            return false;
        return interpolators.stream()
                .anyMatch(i -> i instanceof ColorEffectInterpolator && i.getChannel() == channelId && !i.isDone());
    }
    private boolean hasActiveIntensityEffect(int channelId) {
        if (simulatedCue != null)
            return false;
        return interpolators.stream()
                .anyMatch(i -> i instanceof IntensityEffectInterpolator && i.getChannel() == channelId && !i.isDone());
    }

    private DirectionEffectInterpolator activeDirEffect(int channelId, int effectId) {
        if (simulatedCue != null)
            return null;
        for (var i : interpolators) {
            if (i instanceof DirectionEffectInterpolator dei &&
                    !dei.isDone() && dei.getChannel() == channelId && dei.getEffect().getId() == effectId) return dei;
        }
        return null;
    }
    private ColorEffectInterpolator activeColorEffect(int channelId, int effectId) {
        if (simulatedCue != null)
            return null;
        for (var i : interpolators) {
            if (i instanceof ColorEffectInterpolator cei &&
                    !cei.isDone() && cei.getChannel() == channelId && cei.getEffect().getId() == effectId) return cei;
        }
        return null;
    }
    private IntensityEffectInterpolator activeIntensityEffect(int channelId, int effectId) {
        if (simulatedCue != null)
            return null;
        for (var i : interpolators) {
            if (i instanceof IntensityEffectInterpolator iei &&
                    !iei.isDone() && iei.getChannel() == channelId && iei.getEffect().getId() == effectId) return iei;
        }
        return null;
    }

    // Simple bag of optional targets we might find in the same cue
    private static final class Targets {
        final Integer yaw, pitch, color, intensity;
        Targets(Integer yaw, Integer pitch, Integer color, Integer intensity) {
            this.yaw = yaw; this.pitch = pitch; this.color = color; this.intensity = intensity;
        }
    }

    // Look for explicit static targets for a channel in THIS cue
    private Targets findTargetsForChannelInCue(Cue cue, int channelId) {
        Integer yaw = null, pitch = null, color = null, intensity = null;
        var list = cue.getChannelDiffs() != null ? cue.getChannelDiffs().get(channelId) : null;
        if (list != null) {
            for (Permutation p : list) {
                if (p instanceof Permutation.Yaw y) yaw = y.getValue();
                else if (p instanceof Permutation.Pitch pt) pitch = pt.getValue();
                else if (p instanceof Permutation.Color c) color = c.getValue();
                else if (p instanceof Permutation.Intensity i) intensity = i.getValue();
            }
        }
        return new Targets(yaw, pitch, color, intensity);
    }

    // Look for explicit static group-wide targets (returns Optional<Targets> if any found)
    private Optional<Targets> findTargetsForGroupInCue(Cue cue, int groupId) {
        Integer yaw = null, pitch = null, color = null, intensity = null;
        if (cue.getGroupDiffs() != null) {
            var list = cue.getGroupDiffs().get(groupId);
            if (list != null) {
                for (Permutation p : list) {
                    if (p instanceof Permutation.Yaw y) yaw = y.getValue();
                    else if (p instanceof Permutation.Pitch pt) pitch = pt.getValue();
                    else if (p instanceof Permutation.Color c) color = c.getValue();
                    else if (p instanceof Permutation.Intensity i) intensity = i.getValue();
                }
            }
        }
        boolean any = yaw != null || pitch != null || color != null || intensity != null;
        return any ? Optional.of(new Targets(yaw, pitch, color, intensity)) : Optional.empty();
    }

}
