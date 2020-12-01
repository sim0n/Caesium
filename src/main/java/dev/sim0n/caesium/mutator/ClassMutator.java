package dev.sim0n.caesium.mutator;

import dev.sim0n.caesium.Caesium;
import dev.sim0n.caesium.util.StringUtil;
import dev.sim0n.caesium.util.trait.Finishable;
import dev.sim0n.caesium.util.wrapper.impl.ClassWrapper;
import lombok.Getter;
import lombok.Setter;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.Opcodes;

import java.security.SecureRandom;
import java.util.stream.IntStream;

public abstract class ClassMutator implements Opcodes, Finishable {
    protected final Caesium caesium = Caesium.getInstance();
    protected final Logger logger = Caesium.getLogger();

    protected final SecureRandom random = caesium.getRandom();

    protected int counter;

    @Getter @Setter
    private boolean enabled = false;

    public abstract void handle(ClassWrapper wrapper);

    /**
     * Generates a random name using {@link SecureRandom#nextInt}
     * @return A random name
     */
    public String getRandomName() {
        switch (caesium.getDictionary()) {
            case ABC_LOWERCASE:
                return StringUtil.getRandomString(3, 6, false);

            case ABC:
                return StringUtil.getRandomString(3, 6, true);

            case III: {
                StringBuilder sb = new StringBuilder();

                IntStream.range(0, 20).forEach(i -> sb.append(random.nextBoolean() ? "I" : "l"));

                return sb.toString();
            }

            case NUMBERS:
                return String.valueOf(random.nextInt());

            case WACK: {
                StringBuilder sb = new StringBuilder();

                IntStream.range(0, 20).forEach(i -> {
                    if (random.nextBoolean())
                        sb.append("\n");

                    sb.append(random.nextBoolean() ? "I" : "l");
                });

                return sb.toString();
            }

            default:
                return "Unsupported";
        }

    }
}
