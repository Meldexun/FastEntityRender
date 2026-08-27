package meldexun.fastentityrender.asm;

import java.util.Collection;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.objectweb.asm.tree.AbstractInsnNode;

import com.google.common.collect.Iterables;

public class Batch {

	private final AbstractInsnNode dominator;
	private final AbstractInsnNode postDominator;

	private Batch(AbstractInsnNode insn) {
		this(insn, insn);
	}

	private Batch(AbstractInsnNode dominator, AbstractInsnNode postDominator) {
		this.dominator = dominator;
		this.postDominator = postDominator;
	}

	public static Batch of(AbstractInsnNode insn) {
		return new Batch(insn);
	}

	public Optional<Batch> add(ControlFlowAnalysis controlFlow, BiPredicate<AbstractInsnNode, Predicate<AbstractInsnNode>> onAdd, AbstractInsnNode insn) {
		return new Adder(controlFlow, onAdd, this).addAndBuild(insn);
	}

	public AbstractInsnNode dominator() {
		return dominator;
	}

	public AbstractInsnNode postDominator() {
		return postDominator;
	}

	private static class Adder {

		private final ControlFlowAnalysis controlFlow;
		private final Predicate<AbstractInsnNode> onAdd;
		private AbstractInsnNode dominator;
		private AbstractInsnNode postDominator;

		public Adder(ControlFlowAnalysis controlFlow, BiPredicate<AbstractInsnNode, Predicate<AbstractInsnNode>> onAdd, Batch original) {
			this.controlFlow = controlFlow;
			this.onAdd = insn -> onAdd.test(insn, this::add);
			this.dominator = original.dominator;
			this.postDominator = original.postDominator;
		}

		public Optional<Batch> addAndBuild(AbstractInsnNode insn) {
			if (!this.add(insn)) {
				return Optional.empty();
			}
			return Optional.of(new Batch(this.dominator, this.postDominator));
		}

		private boolean add(AbstractInsnNode insn) {
//			ASMUtil.LOGGER.info("Adding {}", ASMUtil.instructionToString(insn));

			AbstractInsnNode prevDominator = this.dominator;
			AbstractInsnNode prevPostDominator = this.postDominator;

			this.dominator = this.controlFlow.commonDominator(this.dominator, insn);

			Collection<AbstractInsnNode> newPostDominators = this.controlFlow.commonPostDominator(this.postDominator, insn);
			if (newPostDominators.size() != 1) {
				return false; // TODO add support for multiple post dominators
			}
			this.postDominator = Iterables.getOnlyElement(newPostDominators);

			while (this.controlFlow.isReachable(this.dominator, this.postDominator, this.dominator)) {
				this.dominator = this.controlFlow.immediateDominator(this.dominator);
			}
			while (this.controlFlow.isReachable(this.postDominator, this.dominator, this.postDominator)) {
				this.postDominator = this.controlFlow.immediatePostDominator(this.postDominator);
			}

			return this.controlFlow.stream(this.dominator, this.postDominator, true)
					.filter(((Predicate<AbstractInsnNode>) this.controlFlow.stream(prevDominator, prevPostDominator, true).collect(Collectors.toSet())::contains).negate())
					.allMatch(this.onAdd);
		}

	}

}
