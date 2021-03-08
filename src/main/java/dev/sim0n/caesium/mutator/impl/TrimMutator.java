package dev.sim0n.caesium.mutator.impl;

import dev.sim0n.caesium.mutator.ClassMutator;
import dev.sim0n.caesium.util.wrapper.impl.ClassWrapper;
import org.objectweb.asm.tree.*;

import java.util.stream.Stream;

public class TrimMutator extends ClassMutator {

    @Override
    public void handle(ClassWrapper wrapper) {
        wrapper.methods.stream()
                .map(m -> m.node)
                .forEach(method -> {
                    InsnList insns = method.instructions;

                    Stream.of(insns.toArray())
                            .filter(MethodInsnNode.class::isInstance)
                            .map(MethodInsnNode.class::cast)
                            .forEach(insn -> {
                                String owner = insn.owner;
                                String name = insn.name;
                                String desc = insn.desc;

                                if (owner.equals("java/lang/Math")) {
                                    switch (name) {
                                        case "abs":
                                            ++counter;
                                            mutateAbs(insns, insn, owner, name, desc);
                                            break;

                                        case "max":
                                            mutateMax(insns, insn, owner, name, desc);
                                            break;

                                        case "min":
                                            mutateMin(insns, insn, owner, name, desc);
                                            break;
                                    }

                                }

                            });
                });

    }

    private void mutateMax(InsnList insns, MethodInsnNode insn, String owner, String name, String desc) {
        mutate(insns, insn, desc, IF_ICMPGE, IFGE);
    }

    private void mutateMin(InsnList insns, MethodInsnNode insn, String owner, String name, String desc) {
        mutate(insns, insn, desc, IF_ICMPLE, IFLE);
    }

    private void mutate(InsnList insns, MethodInsnNode insn, String desc, int cmp, int cmp2) {
        ++counter;

        switch (desc.charAt(desc.length() - 1)) {
            case 'I': {
                LabelNode label = new LabelNode();
                InsnList toAdd = new InsnList();

                toAdd.add(new InsnNode(DUP2));
                toAdd.add(new JumpInsnNode(cmp, label));
                toAdd.add(new InsnNode(SWAP));
                toAdd.add(label);
                toAdd.add(new InsnNode(POP));

                insns.insert(insn, toAdd);
                insns.remove(insn);
                break;
            }

            case 'F': {
                LabelNode label = new LabelNode();
                InsnList toAdd = new InsnList();

                toAdd.add(new InsnNode(DUP2));
                toAdd.add(new InsnNode(FCMPL));
                toAdd.add(new JumpInsnNode(cmp2, label));
                toAdd.add(new InsnNode(SWAP));
                toAdd.add(label);
                toAdd.add(new InsnNode(POP));

                insns.insert(insn, toAdd);
                insns.remove(insn);
                break;
            }
        }
    }

    private void mutateAbs(InsnList insns, MethodInsnNode insn, String owner, String name, String desc) {
        switch (desc.charAt(desc.length() - 1)) {
            case 'I': {
                LabelNode label = new LabelNode();
                InsnList toAdd = new InsnList();

                toAdd.add(new InsnNode(DUP));
                toAdd.add(new JumpInsnNode(IFGE, label));
                toAdd.add(new InsnNode(INEG));
                toAdd.add(label);

                insns.insert(insn, toAdd);
                insns.remove(insn);
                break;
            }

            case 'D': {
                mutateAbsNumber(insns, insn, DUP2, DCONST_0, DCMPG, DNEG);
                break;
            }

            case 'F': {
                mutateAbsNumber(insns, insn, DUP, FCONST_0, FCMPG, FNEG);
                break;
            }
        }

    }

    private void mutateAbsNumber(InsnList insns, MethodInsnNode insn, int dup, int const0, int cmpg, int neg) {
        LabelNode label = new LabelNode();
        InsnList toAdd = new InsnList();

        toAdd.add(new InsnNode(dup));
        toAdd.add(new InsnNode(const0));
        toAdd.add(new InsnNode(cmpg));
        toAdd.add(new JumpInsnNode(IFGE, label));
        toAdd.add(new InsnNode(neg));
        toAdd.add(label);

        insns.insert(insn, toAdd);
        insns.remove(insn);
    }

    @Override
    public void handleFinish() {
        logger.info(String.format("Trimmed %d math functions", counter));
    }

}

