/*
 * This source file is part of BetterModel.
 * Copyright (c) 2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

package kr.toxicity.model.api.data.blueprint;

import it.unimi.dsi.fastutil.ints.Int2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectSortedMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import kr.toxicity.model.api.data.Float3;
import kr.toxicity.model.api.data.blueprint.BlueprintElement.Cube;
import kr.toxicity.model.api.util.MathUtil;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.*;
import java.util.stream.IntStream;
import java.util.stream.Stream;

final class CubeRotationGroups {

    private static final float ROTATION_EPSILON = 1E-5F;
    private static final List<Float3> ELEMENT_ROTATIONS = Stream.concat(
        Stream.of(Float3.ZERO),
        IntStream.of(1, -1, 2, -2)
            .mapToObj(step -> step * MathUtil.ROTATION_DEGREE)
            .flatMap(angle -> Stream.of(
                new Float3(angle, 0, 0),
                new Float3(0, angle, 0),
                new Float3(0, 0, angle)
            ))
    ).toList();

    private CubeRotationGroups() {
    }

    static List<Model> group(List<Cube> cubes) {
        if (cubes.isEmpty()) return List.of();
        if (cubes.stream().allMatch(cube -> MathUtil.checkValidDegree(cube.rotation()))) {
            return List.of(new Model(new Quaternionf(), cubes.stream()
                .map(cube -> new Element(cube, cube.rotation()))
                .toList()));
        }

        var byRotation = indexByRotation(cubes);
        if (byRotation.size() == 1) {
            return List.of(new Model(cubes.getFirst().rotation().toQuaternionZYX(), cubes.stream()
                .map(cube -> new Element(cube, Float3.ZERO))
                .toList()));
        }

        var cells = new Long2ObjectOpenHashMap<List<Seed>>();
        var seeds = createSeeds(byRotation, cells);
        var options = IntStream.range(0, cubes.size())
            .<List<Candidate>>mapToObj(_ -> new ArrayList<>())
            .toList();
        var coverages = new HashSet<IntList>();

        var complete = collectCandidates(seeds, cells, cubes, coverages, options);
        if (complete != null) return models(cubes, List.of(complete));

        addFallbackCandidates(byRotation, coverages, options);
        return models(cubes, selectCandidates(options));
    }

    private static Map<Vector3f, IntList> indexByRotation(List<Cube> cubes) {
        var byRotation = new LinkedHashMap<Vector3f, IntList>();
        for (var i = 0; i < cubes.size(); i++) {
            byRotation.computeIfAbsent(cubes.get(i).rotation().toVector(), _ -> new IntArrayList()).add(i);
        }
        return byRotation;
    }

    private static List<Seed> createSeeds(Map<Vector3f, IntList> byRotation, Long2ObjectOpenHashMap<List<Seed>> cells) {
        var seeds = new ArrayList<Seed>();
        seeds.add(new Seed(new Quaternionf(), IntList.of(), Float3.ZERO));
        // Every legal factorization is cube rotation = identifier * element rotation.
        // Try identity and existing cube rotations first to keep their JSON layout on ties.
        for (var relative : ELEMENT_ROTATIONS) {
            var inverse = relative.toQuaternionZYX().invert();
            byRotation.forEach((rotation, indices) -> {
                var identifier = MathUtil.toQuaternion(rotation).mul(inverse).normalize();
                var seed = new Seed(identifier, indices, relative);
                seeds.add(seed);
                cells.computeIfAbsent(cellKey(cell(identifier.x), cell(identifier.y), cell(identifier.z), cell(identifier.w)),
                    _ -> new ArrayList<>()).add(seed);
            });
        }
        return seeds;
    }

    private static Candidate collectCandidates(
        List<Seed> seeds, Long2ObjectOpenHashMap<List<Seed>> cells, List<Cube> cubes,
        Set<IntList> coverages, List<List<Candidate>> options
    ) {
        var seenIdentifiers = new HashSet<Quaternionf>();
        for (var seed : seeds) {
            if (!seenIdentifiers.add(seed.identifier)) continue;
            var placements = placements(seed.identifier, cells);
            if (placements.isEmpty()) continue;
            var candidate = new Candidate(seed.identifier, placements);
            if (placements.size() == cubes.size()) return candidate;
            addCandidate(candidate, coverages, options);
        }
        return null;
    }

    private static void addFallbackCandidates(
        Map<Vector3f, IntList> byRotation, Set<IntList> coverages, List<List<Candidate>> options
    ) {
        // Preserve a cube's original encoding if its geometry is already outside the element bounds.
        byRotation.forEach((rotation, indices) -> {
            var original = new Float3(rotation.x, rotation.y, rotation.z);
            var identifier = MathUtil.identifier(original);
            var placements = new Int2ObjectAVLTreeMap<Float3>();
            indices.intStream()
                .filter(index -> options.get(index).isEmpty())
                .forEach(index -> placements.put(index, original.minus(identifier)));
            if (!placements.isEmpty()) addCandidate(new Candidate(identifier.toQuaternionZYX(), placements), coverages, options);
        });
    }

    private static List<Candidate> selectCandidates(List<List<Candidate>> options) {
        var selected = new ArrayList<Candidate>();
        var unseen = new BitSet(options.size());
        var distinctOptions = new HashSet<List<Candidate>>();
        for (var i = 0; i < options.size(); i++) {
            if (distinctOptions.add(options.get(i))) unseen.set(i);
        }
        while (!unseen.isEmpty()) {
            var component = new BitSet();
            var pending = new IntArrayList();
            var candidates = new LinkedHashSet<Candidate>();
            var first = unseen.nextSetBit(0);
            unseen.clear(first);
            pending.add(first);
            for (var i = 0; i < pending.size(); i++) {
                var cube = pending.getInt(i);
                component.set(cube);
                for (var candidate : options.get(cube)) {
                    if (!candidates.add(candidate)) continue;
                    for (int index : candidate.placements.keySet()) {
                        if (unseen.get(index)) {
                            unseen.clear(index);
                            pending.add(index);
                        }
                    }
                }
            }
            selected.addAll(bestCover(options, candidates, component));
        }
        return selected;
    }

    private static Int2ObjectSortedMap<Float3> placements(
        Quaternionf identifier, Long2ObjectOpenHashMap<List<Seed>> cells
    ) {
        var placements = new Int2ObjectAVLTreeMap<Float3>();
        for (var x = firstCell(identifier.x); x <= lastCell(identifier.x); x++) {
            for (var y = firstCell(identifier.y); y <= lastCell(identifier.y); y++) {
                for (var z = firstCell(identifier.z); z <= lastCell(identifier.z); z++) {
                    for (var w = firstCell(identifier.w); w <= lastCell(identifier.w); w++) {
                        var matches = cells.get(cellKey(x, y, z, w));
                        if (matches == null) continue;
                        for (var match : matches) {
                            if (!sameRotation(identifier, match.identifier)) continue;
                            for (int index : match.cubes) {
                                placements.put(index, match.relative);
                            }
                        }
                    }
                }
            }
        }
        return placements;
    }

    private static int cell(float component) {
        return (int) (Math.abs(component) * 4096);
    }

    private static int firstCell(float component) {
        return cell(Math.max(0, Math.abs(component) - ROTATION_EPSILON));
    }

    private static int lastCell(float component) {
        return cell(Math.abs(component) + ROTATION_EPSILON);
    }

    private static long cellKey(int x, int y, int z, int w) {
        // Absolute components put q and -q in the same cell.
        return (long) x << 39 | (long) y << 26 | (long) z << 13 | w;
    }

    private static boolean sameRotation(Quaternionf a, Quaternionf b) {
        var sign = a.dot(b) < 0 ? -1F : 1F;
        var x = a.x - sign * b.x;
        var y = a.y - sign * b.y;
        var z = a.z - sign * b.z;
        var w = a.w - sign * b.w;
        return x * x + y * y + z * z + w * w <= ROTATION_EPSILON * ROTATION_EPSILON;
    }

    private static void addCandidate(Candidate candidate, Set<IntList> coverages, List<List<Candidate>> options) {
        if (coverages.add(new IntArrayList(candidate.placements.keySet()))) {
            candidate.placements.keySet().forEach(index -> options.get(index).add(candidate));
        }
    }

    private static List<Model> models(List<Cube> cubes, List<Candidate> selected) {
        var models = new Int2ObjectAVLTreeMap<Model>();
        var assigned = new BitSet(cubes.size());
        for (var candidate : selected) {
            var elements = new ArrayList<Element>();
            var first = -1;
            for (var placement : candidate.placements.int2ObjectEntrySet()) {
                var index = placement.getIntKey();
                if (assigned.get(index)) continue;
                if (first < 0) first = index;
                assigned.set(index);
                elements.add(new Element(cubes.get(index), placement.getValue()));
            }
            if (first >= 0) models.put(first, new Model(candidate.identifier, elements));
        }
        return List.copyOf(models.values());
    }

    record Model(@NotNull Quaternionf identifier, @NotNull List<Element> elements) {
    }

    record Element(@NotNull Cube cube, @NotNull Float3 rotation) {
    }

    private record Seed(@NotNull Quaternionf identifier, @NotNull IntList cubes, @NotNull Float3 relative) {
    }

    private record Candidate(Quaternionf identifier, Int2ObjectSortedMap<Float3> placements) {

        private int count(BitSet remaining) {
            var count = 0;
            for (int index : placements.keySet()) {
                if (remaining.get(index)) count++;
            }
            return count;
        }

        private void cover(BitSet remaining) {
            placements.keySet().forEach(remaining::clear);
        }
    }

    private static List<Candidate> bestCover(List<List<Candidate>> options, Set<Candidate> candidates, BitSet component) {
        var maxCoverage = candidates.stream().mapToInt(candidate -> candidate.count(component)).max().orElseThrow();
        var best = new ArrayList<Candidate>();
        var remaining = (BitSet) component.clone();
        while (!remaining.isEmpty()) {
            var largest = candidates.stream().max(Comparator.comparingInt(candidate -> candidate.count(remaining))).orElseThrow();
            largest.cover(remaining);
            best.add(largest);
        }

        var visited = new Object2IntOpenHashMap<BitSet>();
        visited.defaultReturnValue(Integer.MAX_VALUE);
        return searchCover(options, component, new ArrayList<>(), best, maxCoverage, visited);
    }

    private static List<Candidate> searchCover(
        List<List<Candidate>> options, BitSet remaining, ArrayList<Candidate> path,
        List<Candidate> best, int maxCoverage, Object2IntOpenHashMap<BitSet> visited
    ) {
        if (remaining.isEmpty()) {
            return path.size() < best.size() ? List.copyOf(path) : best;
        }
        var budget = best.size() - path.size();
        if (Math.ceilDiv(remaining.cardinality(), maxCoverage) >= budget) return best;
        if (visited.getInt(remaining) <= path.size()) return best;
        visited.put(remaining, path.size());
        if (needsAtLeast(options, remaining, budget)) return best;

        var pivot = remaining.nextSetBit(0);
        for (var index = remaining.nextSetBit(pivot + 1); index >= 0; index = remaining.nextSetBit(index + 1)) {
            if (options.get(index).size() < options.get(pivot).size()) pivot = index;
        }
        for (var candidate : options.get(pivot)) {
            var next = (BitSet) remaining.clone();
            candidate.cover(next);
            path.add(candidate);
            best = searchCover(options, next, path, best, maxCoverage, visited);
            path.removeLast();
        }
        return best;
    }

    private static boolean needsAtLeast(List<List<Candidate>> options, BitSet remaining, int budget) {
        // Every picked cube needs a different model from the ones picked before it.
        var separate = (BitSet) remaining.clone();
        for (var index = separate.nextSetBit(0); index >= 0; index = separate.nextSetBit(index + 1)) {
            if (--budget == 0) return true;
            for (var candidate : options.get(index)) candidate.cover(separate);
        }
        return false;
    }
}
