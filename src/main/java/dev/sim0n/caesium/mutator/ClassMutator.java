package dev.sim0n.caesium.mutator;

import dev.sim0n.caesium.Caesium;
import dev.sim0n.caesium.util.trait.Finishable;
import dev.sim0n.caesium.util.wrapper.impl.ClassWrapper;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.Opcodes;

import java.security.SecureRandom;

public abstract class ClassMutator implements Opcodes, Finishable {
    private final Caesium caesium = Caesium.getInstance();
    protected final Logger logger = Caesium.getLogger();

    protected final SecureRandom random = caesium.getRandom();

    protected int counter;

    public abstract void handle(ClassWrapper wrapper);

    /**
     * Generates a random name using {@link SecureRandom#nextInt}
     * @return A random name
     */
    public String getRandomName() {
        return String.valueOf(random.nextInt());
    }
}
