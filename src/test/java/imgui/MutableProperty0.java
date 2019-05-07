package imgui;

import kotlin.reflect.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;

public class MutableProperty0<T> implements KMutableProperty0<T> {

    T t = null;

    public MutableProperty0() {
    }

    public MutableProperty0(T t) {
        this.t = t;
    }

    @NotNull
    @Override
    public Setter<T> getSetter() {
        return null;
    }

    @Override
    public void set(T t) {
        this.t = t;
    }

    @NotNull
    @Override
    public KProperty0.Getter<T> getGetter() {
        return null;
    }

    @Override
    public T get() {
        return t;
    }

    @Nullable
    @Override
    public Object getDelegate() {
        return null;
    }

    @Override
    public T invoke() {
        return null;
    }

    @Override
    public boolean isConst() {
        return false;
    }

    @Override
    public boolean isLateinit() {
        return false;
    }

    @Override
    public boolean isAbstract() {
        return false;
    }

    @Override
    public boolean isFinal() {
        return false;
    }

    @Override
    public boolean isOpen() {
        return false;
    }

    @Override
    public boolean isSuspend() {
        return false;
    }

    @NotNull
    @Override
    public String getName() {
        return null;
    }

    @NotNull
    @Override
    public List<KParameter> getParameters() {
        return null;
    }

    @NotNull
    @Override
    public KType getReturnType() {
        return null;
    }

    @NotNull
    @Override
    public List<KTypeParameter> getTypeParameters() {
        return null;
    }

    @Nullable
    @Override
    public KVisibility getVisibility() {
        return null;
    }

    @Override
    public T call(@NotNull Object... objects) {
        return null;
    }

    @Override
    public T callBy(@NotNull Map<KParameter, ?> map) {
        return null;
    }

    @NotNull
    @Override
    public List<Annotation> getAnnotations() {
        return null;
    }
}
