package meldexun.fastentityrender.asm;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.annotation.Nullable;

import org.apache.commons.lang3.tuple.Pair;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;
import org.objectweb.asm.tree.TryCatchBlockNode;

import com.google.common.base.Predicates;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;

public class ControlFlowAnalysis {

	private static class ControlFlowGraph {
		private final Map<AbstractInsnNode, Node> nodes;
		private final Node root;
		private final Set<Node> returns;

		public ControlFlowGraph(Map<AbstractInsnNode, Node> nodes, Node root, Set<Node> returns) {
			this.nodes = nodes;
			this.root = root;
			this.returns = returns;
		}
	}

	private static class Node {
		private final AbstractInsnNode element;
		private int index;
		private final Set<Node> predecessors = new ObjectLinkedOpenHashSet<>(1);
		private final Set<Node> successors = new ObjectLinkedOpenHashSet<>(1);

		private final Dominator dominator = new Dominator();
		private final Dominator postDominator = new Dominator();

		public Node(AbstractInsnNode insn) {
			this.element = insn;
		}

		@Override
		public int hashCode() {
			return index;
		}

		public AbstractInsnNode element() {
			return element;
		}

		public Set<Node> predecessors() {
			return predecessors;
		}

		public Set<Node> successors() {
			return successors;
		}

		public Dominator dominator() {
			return dominator;
		}

		public Dominator postDominator() {
			return postDominator;
		}

		public class Dominator {
			private Node parent;
			private int preIndex;
			private int postIndex;

			private Dominator ancestor;
			private Dominator label;
			private Dominator semi;
			private Dominator idom;

			public Node node() {
				return Node.this;
			}

			public Dominator idom() {
				return idom;
			}
		}
	}

	private final ControlFlowGraph controlFlowGraph;
	private final Node dummyEnd = new Node(new InsnNode(Opcodes.NOP));

	public ControlFlowAnalysis(MethodNode methodNode) {
		if (methodNode.instructions.size() == 0) {
			controlFlowGraph = new ControlFlowGraph(Collections.emptyMap(), null, Collections.emptySet());
			return;
		}
		controlFlowGraph = computeCFG(methodNode);
		computeDominators(computeDFS(controlFlowGraph.root, methodNode.instructions, Node::successors, Node::dominator), Node::predecessors, Node::dominator);
		if (controlFlowGraph.returns.size() == 1) {
			computeDominators(computeDFS(Iterables.getOnlyElement(controlFlowGraph.returns), methodNode.instructions, Node::predecessors, Node::postDominator), Node::successors, Node::postDominator);
		} else {
			for (Node end : controlFlowGraph.returns) {
				end.successors.add(dummyEnd);
				dummyEnd.predecessors.add(end);
			}

			methodNode.instructions.add(dummyEnd.element());
			computeDominators(computeDFS(dummyEnd, methodNode.instructions, Node::predecessors, Node::postDominator), Node::successors, Node::postDominator);
			methodNode.instructions.remove(dummyEnd.element());
		}
	}

	/**
	 * @return The instruction that immediately dominates the given instruction.
	 */
	public AbstractInsnNode immediateDominator(AbstractInsnNode insn) {
		return immediateDominator(insn, Node::dominator);
	}

	/**
	 * @return The instruction that immediately dominates the given instruction.
	 */
	public AbstractInsnNode immediatePostDominator(AbstractInsnNode insn) {
		return immediateDominator(insn, Node::postDominator);
	}

	/**
	 * @return The instruction that immediately dominates the given instruction.
	 */
	private AbstractInsnNode immediateDominator(AbstractInsnNode insn, Function<Node, Node.Dominator> dominator) {
		return dominator.apply(controlFlowGraph.nodes.get(insn)).idom().node().element();
	}

	/**
	 * @return If one instruction dominates the other the dominating instruction is returned. Otherwise the instruction with the highest DFS pre-order index that dominates both instructions is returned.
	 */
	public AbstractInsnNode commonDominator(AbstractInsnNode insn1, AbstractInsnNode insn2) {
		return commonDominator(insn1, insn2, Node::dominator).element();
	}

	/**
	 * @return If one instruction dominates the other the dominating instruction is returned. Otherwise the instruction with the highest DFS pre-order index that dominates both instructions is returned.
	 */
	@Nullable
	public Collection<AbstractInsnNode> commonPostDominator(AbstractInsnNode insn1, AbstractInsnNode insn2) {
		Node commonPostDominator = commonDominator(insn1, insn2, Node::postDominator);
		if (commonPostDominator == dummyEnd) {
			return Arrays.asList(insn1, insn2);
		}
		return Collections.singleton(commonPostDominator.element());
	}

	/**
	 * @return If one instruction dominates the other the dominating instruction is returned. Otherwise the instruction with the highest DFS pre-order index that dominates both instructions is returned.
	 */
	private Node commonDominator(AbstractInsnNode insn1, AbstractInsnNode insn2, Function<Node, Node.Dominator> dominator) {
		Node.Dominator dominator1 = dominator.apply(controlFlowGraph.nodes.get(insn1));
		Node.Dominator dominator2 = dominator.apply(controlFlowGraph.nodes.get(insn2));
		while (dominator1 != dominator2) {
			while (dominator1.postIndex < dominator2.postIndex) dominator1 = dominator1.idom();
			while (dominator2.postIndex < dominator1.postIndex) dominator2 = dominator2.idom();
		}
		return dominator1.node();
	}

	/**
	 * @return Whether the target instruction is reachable from the start instruction through any path with length greater than or equal to two.
	 */
	public boolean isReachable(AbstractInsnNode start, AbstractInsnNode target) {
		return stream(start, true, false).anyMatch(target::equals);
	}

	/**
	 * @return Whether the target instruction is reachable from the start instruction through any path with length greater than or equal to two without hitting the end instruction.
	 */
	public boolean isReachable(AbstractInsnNode start, AbstractInsnNode end, AbstractInsnNode target) {
		return stream(start, true, end, false, false).anyMatch(target::equals);
	}

	/**
	 * @return A stream containing all instructions reachable from the first instruction in DFS pre-order.
	 */
	public Stream<AbstractInsnNode> stream() {
		return stream(null, true);
	}

	/**
	 * @return A stream containing all instructions reachable from start (inclusive) in DFS pre-order.
	 */
	public Stream<AbstractInsnNode> stream(@Nullable AbstractInsnNode start, boolean ignoreBackEdges) {
		return stream(start, false, ignoreBackEdges);
	}

	/**
	 * @return A stream containing all instructions reachable from start (inclusive if ignoreStart is false) in DFS pre-order.
	 */
	public Stream<AbstractInsnNode> stream(@Nullable AbstractInsnNode start, boolean ignoreStart, boolean ignoreBackEdges) {
		return stream(start, ignoreStart, Predicates.alwaysFalse(), false, ignoreBackEdges);
	}

	/**
	 * @return A stream containing all instructions on any path from start (inclusive) to end (inclusive) in DFS pre-order.
	 */
	public Stream<AbstractInsnNode> stream(@Nullable AbstractInsnNode start, AbstractInsnNode end, boolean ignoreBackEdges) {
		return stream(start, false, end, false, ignoreBackEdges);
	}

	/**
	 * @return A stream containing all instructions on any path from start (inclusive if ignoreStart is false) to end (inclusive if ignoreEnd is false) in DFS pre-order.
	 */
	public Stream<AbstractInsnNode> stream(@Nullable AbstractInsnNode start, boolean ignoreStart, AbstractInsnNode end, boolean ignoreEnd, boolean ignoreBackEdges) {
		return stream(start, ignoreStart, end::equals, ignoreEnd, ignoreBackEdges);
	}

	/**
	 * @return A stream containing all instructions on any path from start (inclusive) to the first instructions passing the filter (inclusive) in DFS pre-order.
	 */
	public Stream<AbstractInsnNode> stream(@Nullable AbstractInsnNode start, Predicate<AbstractInsnNode> filter, boolean ignoreBackEdges) {
		return stream(start, false, filter, false, ignoreBackEdges);
	}

	/**
	 * @return A stream containing all instructions on any path from start (inclusive if ignoreStart is false) to the first instructions passing the filter (inclusive if ignoreEnd is false) in DFS pre-order.
	 */
	public Stream<AbstractInsnNode> stream(@Nullable AbstractInsnNode start, boolean ignoreStart, Predicate<AbstractInsnNode> filter, boolean ignoreEnd, boolean ignoreBackEdges) {
		return StreamSupport.stream(spliterator(start, ignoreStart, filter, ignoreEnd, ignoreBackEdges), false);
	}

	/**
	 * @return A spliterator containing all instructions on any path from start (inclusive if ignoreStart is false) to the first instructions passing the filter (inclusive if ignoreEnd is false) in DFS pre-order.
	 */
	public Spliterator<AbstractInsnNode> spliterator(@Nullable AbstractInsnNode start, boolean ignoreStart, Predicate<AbstractInsnNode> filter, boolean ignoreEnd, boolean ignoreBackEdges) {
		return Spliterators.spliteratorUnknownSize(iterator(start, ignoreStart, filter, ignoreEnd, ignoreBackEdges), Spliterator.DISTINCT | Spliterator.ORDERED | Spliterator.NONNULL);
	}

	/**
	 * @return An iterator containing all instructions on any path from start (inclusive if ignoreStart is false) to the first instructions passing the filter (inclusive if ignoreEnd is false) in DFS pre-order.
	 */
	public Iterator<AbstractInsnNode> iterator(@Nullable AbstractInsnNode start, boolean ignoreStart, Predicate<AbstractInsnNode> filter, boolean ignoreEnd, boolean ignoreBackEdges) {
		return new AbstractIterator<AbstractInsnNode>() {
			private final BitSet visited = new BitSet(controlFlowGraph.nodes.size());
			private final Deque<Pair<Node, Iterator<Node>>> stack = new ArrayDeque<>();
			private boolean started;

			@Override
			protected AbstractInsnNode computeNext() {
				if (!started) {
					started = true;
					Node n = start != null ? controlFlowGraph.nodes.get(start) : controlFlowGraph.root;
					if (!filter.test(n.element())) {
						stack.push(Pair.of(n, n.successors().iterator()));
					} else if (ignoreEnd) {
						return endOfData();
					}
					if (!ignoreStart) {
						visited.set(n.index);
						return n.element();
					}
				}
				Pair<Node, Iterator<Node>> p;
				while ((p = stack.peek()) != null) {
					while (p.getRight().hasNext()) {
						Node s = p.getRight().next();
						if (ignoreBackEdges && s.dominator.preIndex < p.getLeft().dominator.preIndex && s.dominator.postIndex > p.getLeft().dominator.postIndex) continue;
						if (!visited.get(s.index)) {
							visited.set(s.index);
							if (!filter.test(s.element())) {
								stack.push(Pair.of(s, s.successors().iterator()));
							} else if (ignoreEnd) {
								continue;
							}
							return s.element();
						}
					}
					stack.pop();
				}
				return endOfData();
			}
		};
	}

	private static ControlFlowGraph computeCFG(MethodNode methodNode) {
		Node[] nodes = new Node[methodNode.instructions.size()];
		Set<Node> returns = new LinkedHashSet<>();

		for (AbstractInsnNode current = methodNode.instructions.getFirst(), next = next(current); current != null; current = next, next = next(next)) {
			Node currentNode = computeIfAbsent(methodNode.instructions, nodes, current);

			if (current.getOpcode() == Opcodes.RETURN
					|| current.getOpcode() == Opcodes.IRETURN
					|| current.getOpcode() == Opcodes.LRETURN
					|| current.getOpcode() == Opcodes.FRETURN
					|| current.getOpcode() == Opcodes.DRETURN
					|| current.getOpcode() == Opcodes.ARETURN
					|| current.getOpcode() == Opcodes.ATHROW) {
				returns.add(currentNode);
			} else if (current instanceof JumpInsnNode) {
				if (((JumpInsnNode) current).getOpcode() != Opcodes.GOTO) {
					addEdge(methodNode.instructions, nodes, currentNode, next);
				}
				addEdge(methodNode.instructions, nodes, currentNode, ((JumpInsnNode) current).label);
			} else if (current instanceof LookupSwitchInsnNode) {
				for (LabelNode label : ((LookupSwitchInsnNode) current).labels) {
					addEdge(methodNode.instructions, nodes, currentNode, label);
				}
				addEdge(methodNode.instructions, nodes, currentNode, ((LookupSwitchInsnNode) current).dflt);
			} else if (current instanceof TableSwitchInsnNode) {
				for (LabelNode label : ((TableSwitchInsnNode) current).labels) {
					addEdge(methodNode.instructions, nodes, currentNode, label);
				}
				addEdge(methodNode.instructions, nodes, currentNode, ((TableSwitchInsnNode) current).dflt);
			} else if (next != null) {
				addEdge(methodNode.instructions, nodes, currentNode, next);
			}
		}
		for (TryCatchBlockNode tryCatch : methodNode.tryCatchBlocks) {
			AbstractInsnNode insn = tryCatch.start;
			while (true) {
				addEdge(methodNode.instructions, nodes, computeIfAbsent(methodNode.instructions, nodes, insn), tryCatch.handler);
				if (insn == tryCatch.end) {
					break;
				}
				insn = next(insn);
			}
		}

		return new ControlFlowGraph(Arrays.stream(nodes).filter(Objects::nonNull).collect(Collectors.toMap(Node::element, Function.identity())), nodes[0], returns);
	}

	private static AbstractInsnNode next(AbstractInsnNode insn) {
		if (insn == null) return null;
		do {
			insn = insn.getNext();
		} while (insn instanceof LineNumberNode || insn instanceof FrameNode);
		return insn;
	}

	private static void addEdge(InsnList instructions, Node[] nodes, Node currentNode, AbstractInsnNode next) {
		Node nextNode = computeIfAbsent(instructions, nodes, next);
		currentNode.successors.add(nextNode);
		nextNode.predecessors.add(currentNode);
	}

	private static Node computeIfAbsent(InsnList instructions, Node[] nodes, AbstractInsnNode insn) {
		int index = instructions.indexOf(insn);
		Node node = nodes[index];
		if (node == null) {
			node = new Node(insn);
			node.index = index;
			nodes[index] = node;
		}
		return node;
	}

	private static List<Node> computeDFS(Node root, InsnList instructions, Function<Node, ? extends Collection<Node>> successors, Function<Node, Node.Dominator> dominator) {
		List<Node> preorderDFS = new ArrayList<>(instructions.size());
		int postorderIndex = 0;

		BitSet visited = new BitSet(instructions.size());
		Deque<Pair<Node, Iterator<Node>>> stack = new ArrayDeque<>();
		Pair<Node, Iterator<Node>> p;

		visited.set(instructions.indexOf(root.element));
		dominator.apply(root).parent = null;
		dominator.apply(root).preIndex = 0;
		preorderDFS.add(root);
		stack.push(Pair.of(root, successors.apply(root).iterator()));
		while ((p = stack.peek()) != null) {
			if (p.getRight().hasNext()) {
				Node next = p.getRight().next();
				if (!visited.get(instructions.indexOf(next.element))) {
					visited.set(instructions.indexOf(next.element));
					dominator.apply(next).parent = p.getLeft();
					dominator.apply(next).preIndex = preorderDFS.size();
					preorderDFS.add(next);
					stack.push(Pair.of(next, successors.apply(next).iterator()));
				}
			} else {
				dominator.apply(p.getLeft()).postIndex = postorderIndex++;
				stack.pop();
			}
		}

		return preorderDFS;
	}

	private static void computeDominators(List<Node> D, Function<Node, ? extends Collection<Node>> predecessors, com.google.common.base.Function<Node, Node.Dominator> dominator) {
		for (Node.Dominator v : Iterables.transform(D, dominator)) {
			if (v.parent == null) {
				v.ancestor = null;
				v.label = v;
				v.semi = v;
				v.idom = v;
				continue;
			}
			v.ancestor = dominator.apply(v.parent);
			v.label = v;
			v.semi = v;
			v.idom = v.ancestor;
		}

		// compute semi dominators
		for (Node.Dominator w : Iterables.transform(Lists.reverse(D.subList(1, D.size())), dominator)) {
			for (Node.Dominator v : Iterables.transform(predecessors.apply(w.node()), dominator)) {
				if (v.label == null) {
					// not visited in DFS
					continue;
				}
				snca_compress(w, v, predecessors);
				if (v.label.preIndex < w.semi.preIndex) {
					w.semi = v.label;
				}
			}
			w.label = w.semi;
		}

		// compute immediate dominators
		for (Node.Dominator v : Iterables.transform(D.subList(1, D.size()), dominator)) {
			while (v.idom.preIndex > v.semi.preIndex) {
				v.idom = v.idom.idom;
			}
		}
	}

	private static void snca_compress(Node.Dominator w, Node.Dominator v, Function<Node, ? extends Collection<Node>> predecessors) {
		Node.Dominator u = v.ancestor;
		if (u == null) {
			return;
		}
		if (u.preIndex < w.preIndex) {
			return;
		}
		if (!predecessors.apply(u.node()).isEmpty()) {
			snca_compress(w, u, predecessors);
			if (u.label.preIndex < v.label.preIndex) {
				v.label = u.label;
			}
			v.ancestor = u.ancestor;
		}
	}

}
