package samples;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import java.util.Random;

public class JavaSampleClass {
    private final String word;
    private int number;

    public JavaSampleClass(int number, String word) {
        this.number = number;
        this.word = word;
    }

    public void doMagic() {
        while (number != 0) {
            System.out.print(word);
            number *= 0.01;
        }
    }
}


@SuppressWarnings("@SafeVarargs")
class JavaSampleClassWithAnnotation {
    private final int number = new Random().nextInt();

    @Deprecated
    @SafeVarargs
    public final void printNumber(List<Integer>... intLists) {
        System.out.println(number);
    }

    @MyAnnotation
    @Override
    public String toString() {
        return "Hello? The number is " + number;
    }

    @Retention(RetentionPolicy.RUNTIME)
    private @interface MyAnnotation {
    }

    @FunctionalInterface
    private interface SAM {
        void method();
    }
}

