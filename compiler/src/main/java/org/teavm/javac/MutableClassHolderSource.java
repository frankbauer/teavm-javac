package org.teavm.javac;

import org.teavm.model.ClassHolder;
import org.teavm.model.ClassHolderSource;
import java.util.HashMap;
import java.util.Map;

public class MutableClassHolderSource implements ClassHolderSource {
    private final Map<String, ClassHolder> classes = new HashMap<>();

    public void add(ClassHolder cls) {
        classes.put(cls.getName(), cls);
    }

    @Override
    public ClassHolder get(String name) {
        return classes.get(name);
    }
}
