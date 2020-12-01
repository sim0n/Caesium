package dev.sim0n.caesium.manager;

import dev.sim0n.caesium.Caesium;
import dev.sim0n.caesium.mutator.ClassMutator;
import dev.sim0n.caesium.mutator.impl.*;
import dev.sim0n.caesium.mutator.impl.crasher.BadAnnotationMutator;
import dev.sim0n.caesium.mutator.impl.crasher.ImageCrashMutator;
import dev.sim0n.caesium.util.wrapper.impl.ClassWrapper;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class MutatorManager {
    private final Caesium caesium = Caesium.getInstance();

    private final List<ClassMutator> mutators = new ArrayList<>();

    public MutatorManager() {
        mutators.add(new ClassFolderMutator());

        mutators.add(new ImageCrashMutator());

        mutators.add(new LineNumberMutator());
        mutators.add(new LocalVariableMutator());
        mutators.add(new StringMutator());
        mutators.add(new ControlFlowMutator());
        mutators.add(new NumberMutator());
        mutators.add(new PolymorphMutator());
        mutators.add(new ReferenceMutator());
       // mutators.add(new AttributeMutator());
        mutators.add(new BadAnnotationMutator());
    }

    @SuppressWarnings("unchecked")
    public <T extends ClassMutator> T getMutator(Class<T> clazz) {
        return (T) mutators.stream()
                .filter(mutator -> mutator.getClass() == clazz)
                .findFirst()
                .orElse(null);
    }

    public void handleMutation(ClassWrapper clazz) {
        mutators.stream()
                .filter(ClassMutator::isEnabled)
                .forEach(mutator -> mutator.handle(clazz));
    }

    public void handleMutationFinish() {
        mutators.stream()
                .filter(ClassMutator::isEnabled)
                .forEach(mutator -> {
                    mutator.handleFinish();

                    caesium.separator();
                });
    }
}
