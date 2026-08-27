package meldexun.fastentityrender.asm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.objectweb.asm.tree.AbstractInsnNode;

import com.google.common.base.Predicates;
import com.google.common.collect.Iterators;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

public class BatchAnalysis<T> {

	private final Multimap<T, T> correspondingStarts;
	private final Multimap<T, T> correspondingEnds;

	private BatchAnalysis(Multimap<T, T> correspondingStarts, Multimap<T, T> correspondingEnds) {
		this.correspondingStarts = correspondingStarts;
		this.correspondingEnds = correspondingEnds;
	}

	public static BatchAnalysis<AbstractInsnNode> of(ControlFlowAnalysis controlFlowAnalysis, Predicate<AbstractInsnNode> isBatchStart, Predicate<AbstractInsnNode> isBatchEnd, Predicate<AbstractInsnNode> isBatchable) {
		return of((start, filter) -> controlFlowAnalysis.iterator(start, false, filter, false, true), isBatchStart, isBatchEnd, isBatchable);
	}

	public static <T> BatchAnalysis<T> of(BiFunction<T, Predicate<T>, Iterator<T>> iteratorGenerator, Predicate<T> isBatchStart, Predicate<T> isBatchEnd, Predicate<T> isBatchable) {
		Multimap<T, T> correspondingStarts = Multimaps.newSetMultimap(new Object2ObjectOpenHashMap<>(), () -> new ObjectOpenHashSet<>(1));
		Multimap<T, T> correspondingEnds = Multimaps.newSetMultimap(new Object2ObjectOpenHashMap<>(), () -> new ObjectOpenHashSet<>(1));
		Iterators.filter(iteratorGenerator.apply(null, Predicates.alwaysFalse()), isBatchStart::test).forEachRemaining(start -> {
			List<T> nodes = new ArrayList<>();
			List<T> ends = new ArrayList<>(1);
			forEachUntil(iteratorGenerator, start, isBatchStart, isBatchEnd, node -> {
				if (isBatchEnd.test(node)) {
					ends.add(node);
				} else if (isBatchable.test(node)) {
					nodes.add(node);
				}
			});

			ends.forEach(end -> {
				correspondingStarts.put(end, start);
				correspondingEnds.put(start, end);
			});
			nodes.forEach(node -> {
				correspondingStarts.put(node, start);
				correspondingEnds.putAll(node, ends);
			});
		});
		return new BatchAnalysis<>(correspondingStarts, correspondingEnds);
	}

	/**
	 * Iterates starting from start until a batch end is reached and runs the provided action for every node (start excluded, end included).
	 * When a nested batch is reached all nodes of that nested batch (including start, end, and further nested batches) are skipped.
	 */
	private static <T> void forEachUntil(BiFunction<T, Predicate<T>, Iterator<T>> iteratorGenerator, T start, Predicate<T> isBatchStart, Predicate<T> isBatchEnd, Consumer<T> action) {
		Set<T> ignored = new HashSet<>();
		Iterator<T> iterator = iteratorGenerator.apply(start, ((Predicate<T>) ignored::contains).negate().and(isBatchEnd));
		iterator.next(); // skip start
		while (iterator.hasNext()) {
			T t = iterator.next();
			if (ignored.contains(t)) {
				continue;
			}
			if (isBatchStart.test(t)) {
				forEachAllUntil(iteratorGenerator, t, isBatchStart, isBatchEnd, ignored::add);
			} else {
				action.accept(t);
			}
		}
	}

	/**
	 * Iterates starting from start until a batch end is reached and runs the provided action for every node (start included, end included).
	 * When a nested batch is reached all nodes of that nested batch (including start, end, and further nested batches) are still processed (not counting the nested batch end as an end for this batch).
	 */
	private static <T> void forEachAllUntil(BiFunction<T, Predicate<T>, Iterator<T>> iteratorGenerator, T start, Predicate<T> isBatchStart, Predicate<T> isBatchEnd, Consumer<T> action) {
		action.accept(start);

		Set<T> ignored = new HashSet<>();
		Iterator<T> iterator = iteratorGenerator.apply(start, ((Predicate<T>) ignored::contains).negate().and(isBatchEnd));
		iterator.next(); // skip start
		while (iterator.hasNext()) {
			T t = iterator.next();
			action.accept(t);
			if (!ignored.contains(t) && isBatchStart.test(t)) {
				forEachAllUntil(iteratorGenerator, t, isBatchStart, isBatchEnd, ignored::add);
			}
		}
	}

	public Collection<T> getCorrespondingStarts(T t) {
		return correspondingStarts.get(t);
	}

	public Collection<T> getCorrespondingEnds(T t) {
		return correspondingEnds.get(t);
	}

	public Multimap<T, T> getCorrespondingStarts() {
		return correspondingStarts;
	}

	public Multimap<T, T> getCorrespondingEnds() {
		return correspondingEnds;
	}

}
