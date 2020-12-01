package dev.sim0n.caesium.util;

import dev.sim0n.caesium.Caesium;
import lombok.experimental.UtilityClass;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

@UtilityClass
public class StringUtil {
    private final String ALPHABET = "abcdefghijklmnopqrstuvwxyz";

    public String getRandomString(int min, int max, boolean uppercase) {
        Random random = Caesium.getInstance().getRandom();

        StringBuilder sb = new StringBuilder();

        IntStream.range(0, ThreadLocalRandom.current().nextInt(min, max))
                .forEach(i -> {
                    char character = ALPHABET.charAt(random.nextInt(ALPHABET.length()));

                    if (uppercase && random.nextBoolean())
                        character = Character.toUpperCase(character);

                    sb.append(character);
                });

        return sb.toString();
    }

}
