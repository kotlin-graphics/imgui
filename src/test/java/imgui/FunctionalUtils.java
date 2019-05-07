package imgui;

import kotlin.Unit;
import kotlin.jvm.functions.Function0;

public class FunctionalUtils {
    public static Function0<Unit> call(Runnable callable) {
        return () -> {
            callable.run();
            return Unit.INSTANCE;
        };
    }
}
