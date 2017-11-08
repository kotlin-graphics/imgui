//package imgui;
//
//import kotlin.reflect.*;
//import org.jetbrains.annotations.NotNull;
//import org.jetbrains.annotations.Nullable;
//
//import java.lang.annotation.Annotation;
//import java.util.List;
//import java.util.Map;
//
//public class JavaProp <T> implements KMutableProperty0<T> {
//
//    private imgui.Getter<T> getter;
//    private imgui.Setter<T> setter;
//
//    public JavaProp(imgui.Getter<T> getter, imgui.Setter<T> setter) {
//        this.getter = getter;
//        this.setter = setter;
//    }
//
//    @Override
//    public void set(T t) {
//        setter.set(t);
//    }
//
//    @NotNull
//    @Override
//    public Setter<T> getSetter() {
//        return null;
//    }
//
//    @Override
//    public T get() {
//        return getter.get();
//    }
//
//    @Nullable
//    @Override
//    public Object getDelegate() {
//        return null;
//    }
//
//    @NotNull
//    @Override
//    public Getter<T> getGetter() {
//        return null;
//    }
//
//    @Override
//    public T invoke() {
//        return null;
//    }
//
//    @Override
//    public boolean isLateinit() {
//        return false;
//    }
//
//    @Override
//    public boolean isConst() {
//        return false;
//    }
//
//    @NotNull
//    @Override
//    public String getName() {
//        return null;
//    }
//
//    @NotNull
//    @Override
//    public List<KParameter> getParameters() {
//        return null;
//    }
//
//    @NotNull
//    @Override
//    public KType getReturnType() {
//        return null;
//    }
//
//    @NotNull
//    @Override
//    public List<KTypeParameter> getTypeParameters() {
//        return null;
//    }
//
//    @Override
//    public T call(Object... objects) {
//        return null;
//    }
//
//    @Override
//    public T callBy(Map<KParameter, ?> map) {
//        return null;
//    }
//
//    @Nullable
//    @Override
//    public KVisibility getVisibility() {
//        return null;
//    }
//
//    @Override
//    public boolean isFinal() {
//        return false;
//    }
//
//    @Override
//    public boolean isOpen() {
//        return false;
//    }
//
//    @Override
//    public boolean isAbstract() {
//        return false;
//    }
//
//    @NotNull
//    @Override
//    public List<Annotation> getAnnotations() {
//        return null;
//    }
//}
