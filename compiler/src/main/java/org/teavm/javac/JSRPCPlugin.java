package org.teavm.javac;

import org.teavm.model.*;
import org.teavm.model.emit.*;
import org.teavm.model.instructions.InvocationType;
import org.teavm.vm.spi.TeaVMHost;
import org.teavm.vm.spi.TeaVMPlugin;
import java.util.ArrayList;
import java.util.List;

public class JSRPCPlugin implements TeaVMPlugin, ClassHolderTransformer {
    private static final String REMOTE_OBJECT = "de.fau.tf.lgdv.runtime.RemoteObject";
    private static final String ASYNC = "org.teavm.interop.Async";
    private static final String JS_COMMAND = "de.fau.tf.lgdv.JSCommand";
    private static final String JS_EVENT = "de.fau.tf.lgdv.JSEvent";
    private static final String JS_QUERY = "de.fau.tf.lgdv.JSQuery";
    private static final String JSON_OBJECT = "de.fau.tf.lgdv.json.JsonObject";
    private static final String JSON_ARRAY = "de.fau.tf.lgdv.json.JsonArray";
    private static final String JSON_ELEMENT = "de.fau.tf.lgdv.json.JsonElement";
    private static final String JSON_OBJECTABLE = "de.fau.tf.lgdv.json.JsonObjectable";
    private static final String JSON_SERIALIZER = "de.fau.tf.lgdv.json.JsonSerializer";
    private static final String CODE_BLOCKS = "de.fau.tf.lgdv.CodeBlocks";
    private static final String CODE_BLOCKS_EVENT_FUNCTION = "de.fau.tf.lgdv.CodeBlocksEventFunction";
    private static final String CODE_BLOCKS_BASE_MESSAGE = "de.fau.tf.lgdv.CodeBlocksBaseMessage";
    private static final String MARKER = "__rpc_done";

    private static int wrapperCounter = 0;
    private final MutableClassHolderSource syntheticClasses;

    public JSRPCPlugin(MutableClassHolderSource syntheticClasses) {
        this.syntheticClasses = syntheticClasses;
    }

    @Override
    public void install(TeaVMHost host) {
        System.out.println("JSRPCPlugin: Installed");
        host.add(this);
    }

    @Override
    public void transformClass(ClassHolder cls, ClassHolderTransformerContext context) {
        if (cls.getAnnotations().get(MARKER) != null) return;
        
        List<MethodHolder> instanceEventMethods = new ArrayList<>();
        List<MethodHolder> staticEventMethods = new ArrayList<>();
        boolean isRemoteObject = context.getHierarchy().isSuperType(REMOTE_OBJECT, cls.getName(), false);
        boolean changed = false;

        for (MethodHolder method : cls.getMethods().toArray(new MethodHolder[0])) {
            AnnotationContainer annotations = method.getAnnotations();
            if (annotations.get(JS_COMMAND) != null) {
                transformCommand(cls, method, isRemoteObject, context);
                annotations.remove(JS_COMMAND);
                method.getModifiers().remove(ElementModifier.NATIVE);
                changed = true;
            } else if (annotations.get(JS_QUERY) != null) {
                transformQuery(cls, method, isRemoteObject, context);
                annotations.remove(JS_QUERY);
                annotations.remove(ASYNC);
                method.getModifiers().remove(ElementModifier.NATIVE);
                changed = true;
            } else if (annotations.get(JS_EVENT) != null) {
                if (method.getModifiers().contains(ElementModifier.STATIC)) {
                    staticEventMethods.add(method);
                } else if (isRemoteObject) {
                    instanceEventMethods.add(method);
                }
                changed = true;
            }
        }

        if (!instanceEventMethods.isEmpty() && isRemoteObject) {
            implementHandleEvent(cls, instanceEventMethods, context);
            for (MethodHolder method : instanceEventMethods) {
                method.getAnnotations().remove(JS_EVENT);
            }
            changed = true;
        }

        if (!staticEventMethods.isEmpty()) {
            implementStaticEventDispatch(cls, staticEventMethods, context);
            for (MethodHolder method : staticEventMethods) {
                method.getAnnotations().remove(JS_EVENT);
            }
            changed = true;
        }
        
        if (changed) {
            cls.getAnnotations().add(new AnnotationHolder(MARKER));
        }
    }

    private boolean implementsInterface(String className, String interfaceName, ClassHierarchy hierarchy) {
        if (className == null || className.equals("java.lang.Object")) return false;
        if (className.equals(interfaceName)) return true;
        ClassReader reader = hierarchy.getClassSource().get(className);
        if (reader == null) return false;
        for (String iface : reader.getInterfaces()) {
            if (iface.equals(interfaceName)) return true;
            if (implementsInterface(iface, interfaceName, hierarchy)) return true;
        }
        return implementsInterface(reader.getParent(), interfaceName, hierarchy);
    }

    private String[] readParamHints(AnnotationReader ann) {
        if (ann == null) return null;
        var paramsValue = ann.getValue("params");
        if (paramsValue == null) return null;
        var list = paramsValue.getList();
        if (list == null || list.isEmpty()) return null;
        var result = new String[list.size()];
        for (int i = 0; i < result.length; i++) result[i] = list.get(i).getString();
        return result;
    }

    private ValueEmitter emitPayload(ProgramEmitter pe, MethodHolder method, int offset, ClassHierarchy hierarchy,
            org.teavm.model.Program originalProgram, String[] paramHints) {
        ValueEmitter payload = pe.construct(JSON_OBJECT);
        for (int i = 0; i < method.parameterCount(); i++) {
            ValueType type = method.parameterType(i);
            String paramName = resolveParamName(method, originalProgram, i, paramHints, offset);
            ValueEmitter key = pe.constant(paramName);
            ValueEmitter val = pe.var(i + offset, type);
            if (type instanceof ValueType.Object) {
                String typeName = ((ValueType.Object) type).getClassName();
                if (implementsInterface(typeName, JSON_OBJECTABLE, hierarchy)) {
                    ValueEmitter element = val.invokeVirtual(new MethodReference(JSON_OBJECTABLE, "toJsonElement", ValueType.object(JSON_ELEMENT)));
                    payload = payload.invokeVirtual(new MethodReference(JSON_OBJECT, "put", ValueType.object("java.lang.String"), ValueType.object(JSON_ELEMENT), ValueType.object(JSON_OBJECT)), key, element);
                } else if (implementsInterface(typeName, JSON_SERIALIZER, hierarchy)) {
                    ValueEmitter json = val.invokeVirtual(new MethodReference(JSON_SERIALIZER, "toJson", ValueType.object("java.lang.String")));
                    payload = payload.invokeVirtual(new MethodReference(JSON_OBJECT, "put", ValueType.object("java.lang.String"), ValueType.object("java.lang.String"), ValueType.object(JSON_OBJECT)), key, json);
                } else if (typeName.equals("java.lang.String")) {
                    payload = payload.invokeVirtual(new MethodReference(JSON_OBJECT, "put", ValueType.object("java.lang.String"), ValueType.object("java.lang.String"), ValueType.object(JSON_OBJECT)), key, val);
                } else {
                    ValueEmitter str = val.invokeVirtual(new MethodReference("java.lang.Object", "toString", ValueType.object("java.lang.String")));
                    payload = payload.invokeVirtual(new MethodReference(JSON_OBJECT, "put", ValueType.object("java.lang.String"), ValueType.object("java.lang.String"), ValueType.object(JSON_OBJECT)), key, str);
                }
            } else {
                payload = payload.invokeVirtual(new MethodReference(JSON_OBJECT, "put", ValueType.object("java.lang.String"), type, ValueType.object(JSON_OBJECT)), key, val);
            }
        }
        return payload;
    }

    private void transformCommand(ClassHolder cls, MethodHolder method, boolean isRemoteObject, ClassHolderTransformerContext context) {
        AnnotationReader ann = method.getAnnotations().get(JS_COMMAND);
        String commandName = ann != null && ann.getValue("value") != null ? ann.getValue("value").getString() : method.getName();
        String[] paramHints = readParamHints(ann);
        int offset = method.getModifiers().contains(ElementModifier.STATIC) ? 0 : 1;
        org.teavm.model.Program originalProgram = method.getProgram();

        if (originalProgram != null && !method.getModifiers().contains(ElementModifier.NATIVE)) {
            String renamedName = "__rpc_orig_" + method.getName() + "_" + (wrapperCounter++);
            MethodHolder renamed = new MethodHolder(new MethodDescriptor(renamedName, method.getSignature()));
            renamed.setProgram(originalProgram);
            renamed.setLevel(AccessLevel.PRIVATE);
            if (method.getModifiers().contains(ElementModifier.STATIC)) renamed.getModifiers().add(ElementModifier.STATIC);
            cls.addMethod(renamed);

            ProgramEmitter pe = ProgramEmitter.create(method, context.getHierarchy());
            ValueEmitter[] args = new ValueEmitter[method.parameterCount()];
            for (int i = 0; i < args.length; i++) args[i] = pe.var(i + offset, method.parameterType(i));

            if (method.getModifiers().contains(ElementModifier.STATIC)) {
                pe.invoke(new MethodReference(cls.getName(), renamed.getName(), method.getSignature()), args);
            } else {
                pe.var(0, ValueType.object(cls.getName())).invokeVirtual(new MethodReference(cls.getName(), renamed.getName(), method.getSignature()), args);
            }
            emitCommandLogic(pe, cls, method, commandName, isRemoteObject, offset, context, originalProgram, paramHints);
            pe.exit();
        } else {
            ProgramEmitter pe = ProgramEmitter.create(method, context.getHierarchy());
            emitCommandLogic(pe, cls, method, commandName, isRemoteObject, offset, context, null, paramHints);
            pe.exit();
        }
    }

    private void emitCommandLogic(ProgramEmitter pe, ClassHolder cls, MethodHolder method, String commandName,
            boolean isRemoteObject, int offset, ClassHolderTransformerContext context,
            org.teavm.model.Program originalProgram, String[] paramHints) {
        ValueEmitter payload = emitPayload(pe, method, offset, context.getHierarchy(), originalProgram, paramHints);
        if (isRemoteObject) {
            MethodReference sendRef = new MethodReference(REMOTE_OBJECT, "sendCommand", ValueType.object("java.lang.String"), ValueType.object(JSON_SERIALIZER), ValueType.VOID);
            pe.var(0, ValueType.object(cls.getName())).invokeVirtual(sendRef, pe.constant(commandName), payload);
        } else {
            MethodReference postRef = new MethodReference(CODE_BLOCKS, "postMessage", ValueType.object("java.lang.String"), ValueType.object(JSON_SERIALIZER), ValueType.VOID);
            pe.invoke(postRef, pe.constant(commandName), payload);
        }
    }

    private void transformQuery(ClassHolder cls, MethodHolder method, boolean isRemoteObject, ClassHolderTransformerContext context) {
        AnnotationReader ann = method.getAnnotations().get(JS_QUERY);
        String commandName = ann != null && ann.getValue("value") != null ? ann.getValue("value").getString() : method.getName();
        String[] paramHints = readParamHints(ann);
        int offset = method.getModifiers().contains(ElementModifier.STATIC) ? 0 : 1;
        org.teavm.model.Program originalProgram = method.getProgram();

        ProgramEmitter pe = ProgramEmitter.create(method, context.getHierarchy());
        ValueEmitter payload = emitPayload(pe, method, offset, context.getHierarchy(), originalProgram, paramHints);
        ValueEmitter result;

        if (isRemoteObject) {
            MethodReference queryRef = new MethodReference(REMOTE_OBJECT, "sendQuery", ValueType.object("java.lang.String"), ValueType.object(JSON_SERIALIZER), ValueType.object(JSON_ELEMENT));
            result = pe.var(0, ValueType.object(cls.getName())).invokeVirtual(queryRef, pe.constant(commandName), payload);
        } else {
            MethodReference queryRef = new MethodReference(CODE_BLOCKS, "sendQuery", ValueType.object("java.lang.String"), ValueType.object(JSON_SERIALIZER), ValueType.object(JSON_ELEMENT));
            result = pe.invoke(queryRef, pe.constant(commandName), payload);
        }
        result.cast(method.getResultType()).returnValue();
    }

    private void implementHandleEvent(ClassHolder cls, List<MethodHolder> eventMethods, ClassHolderTransformerContext context) {
        MethodDescriptor desc = new MethodDescriptor("handleEvent", ValueType.object("java.lang.String"), ValueType.object(JSON_ELEMENT), ValueType.VOID);
        MethodHolder handleEvent = cls.getMethod(desc);

        // If there is an existing body, preserve it so we can call it after dispatching.
        String preservedName = null;
        if (handleEvent != null && handleEvent.getProgram() != null) {
            preservedName = "__orig_handleEvent_" + (wrapperCounter++);
            MethodHolder preserved = new MethodHolder(new MethodDescriptor(preservedName,
                    ValueType.object("java.lang.String"), ValueType.object(JSON_ELEMENT), ValueType.VOID));
            preserved.setProgram(handleEvent.getProgram());
            preserved.setLevel(AccessLevel.PRIVATE);
            cls.addMethod(preserved);
            // handleEvent.getProgram() is replaced below by ProgramEmitter.create
        } else if (handleEvent == null) {
            handleEvent = new MethodHolder(desc);
            handleEvent.setLevel(AccessLevel.PUBLIC);
            cls.addMethod(handleEvent);
        }

        ProgramEmitter pe = ProgramEmitter.create(handleEvent, context.getHierarchy());
        ValueEmitter cmdVar = pe.var(1, ValueType.object("java.lang.String"));
        ValueEmitter jsonVar = pe.var(2, ValueType.object(JSON_ELEMENT));
        MethodReference equalsRef = new MethodReference("java.lang.String", "equals", ValueType.object("java.lang.Object"), ValueType.BOOLEAN);

        for (MethodHolder eventMethod : eventMethods) {
            AnnotationReader ann = eventMethod.getAnnotations().get(JS_EVENT);
            String eventName = ann != null && ann.getValue("value") != null ? ann.getValue("value").getString() : "";
            if (eventName.isEmpty()) eventName = eventMethod.getName();
            final String finalEventName = eventName;
            pe.when(() -> cmdVar.invokeVirtual(equalsRef, pe.constant(finalEventName)).isTrue())
              .thenDo(() -> {
                  if (eventMethod.parameterCount() > 0) {
                      ValueEmitter arg = jsonVar.cast(eventMethod.parameterType(0));
                      pe.var(0, ValueType.object(cls.getName())).invokeVirtual(eventMethod.getReference(), arg);
                  } else {
                      pe.var(0, ValueType.object(cls.getName())).invokeVirtual(eventMethod.getReference());
                  }
                  pe.exit();
              });
        }

        // Fall through: call preserved body if one existed, otherwise delegate to parent.
        if (preservedName != null) {
            MethodReference preservedRef = new MethodReference(cls.getName(), preservedName,
                    ValueType.object("java.lang.String"), ValueType.object(JSON_ELEMENT), ValueType.VOID);
            pe.var(0, ValueType.object(cls.getName())).invokeVirtual(preservedRef, cmdVar, jsonVar);
        } else {
            pe.var(0, ValueType.object(cls.getName()))
              .invoke(InvocationType.SPECIAL, new MethodReference(cls.getParent(), desc), cmdVar, jsonVar);
        }
        pe.exit();
    }

    private void implementStaticEventDispatch(ClassHolder cls, List<MethodHolder> eventMethods,
            ClassHolderTransformerContext context) {
        // Generate a dedicated $$StaticHandler inner class that implements CodeBlocksEventFunction.
        // This avoids requiring a default constructor on the outer class.
        String handlerName = cls.getName() + "$$StaticHandler";

        ClassHolder handler = new ClassHolder(handlerName);
        handler.setParent("java.lang.Object");
        handler.getInterfaces().add(CODE_BLOCKS_EVENT_FUNCTION);
        handler.setLevel(AccessLevel.PACKAGE_PRIVATE);

        // No-arg constructor: calls Object.<init>()
        MethodHolder ctor = new MethodHolder(new MethodDescriptor("<init>", ValueType.VOID));
        ctor.setLevel(AccessLevel.PUBLIC);
        ProgramEmitter ctorPe = ProgramEmitter.create(ctor, context.getHierarchy());
        ctorPe.var(0, ValueType.object(handlerName))
              .invoke(InvocationType.SPECIAL, new MethodReference("java.lang.Object", "<init>", ValueType.VOID));
        ctorPe.exit();
        handler.addMethod(ctor);

        // handleEvent(CodeBlocksBaseMessage): dispatch to static methods in outer class
        MethodDescriptor handleEventDesc = new MethodDescriptor(
                "handleEvent", ValueType.object(CODE_BLOCKS_BASE_MESSAGE), ValueType.VOID);
        MethodHolder handleEvent = new MethodHolder(handleEventDesc);
        handleEvent.setLevel(AccessLevel.PUBLIC);

        ProgramEmitter pe = ProgramEmitter.create(handleEvent, context.getHierarchy());
        ValueEmitter msgVar = pe.var(1, ValueType.object(CODE_BLOCKS_BASE_MESSAGE));
        ValueEmitter cmdVar = msgVar.invokeVirtual(
                new MethodReference(CODE_BLOCKS_BASE_MESSAGE, "getCommand", ValueType.object("java.lang.String")));
        MethodReference equalsRef = new MethodReference(
                "java.lang.String", "equals", ValueType.object("java.lang.Object"), ValueType.BOOLEAN);

        for (MethodHolder eventMethod : eventMethods) {
            AnnotationReader ann = eventMethod.getAnnotations().get(JS_EVENT);
            String eventName = ann != null && ann.getValue("value") != null
                    ? ann.getValue("value").getString() : eventMethod.getName();
            String[] paramHints = readParamHints(ann);
            final String finalEventName = eventName;
            pe.when(() -> cmdVar.invokeVirtual(equalsRef, pe.constant(finalEventName)).isTrue())
              .thenDo(() -> {
                  emitStaticEventCall(pe, eventMethod, msgVar, paramHints, context.getHierarchy());
                  pe.exit();
              });
        }
        pe.exit();
        handler.addMethod(handleEvent);

        syntheticClasses.add(handler);

        injectStaticEventRegistration(cls, handlerName, context);
    }

    private void emitStaticEventCall(ProgramEmitter pe, MethodHolder method, ValueEmitter msgVar,
            String[] paramHints, ClassHierarchy hierarchy) {
        int n = method.parameterCount();

        // 0 params: plain call
        if (n == 0) {
            pe.invoke(method.getReference());
            return;
        }

        // 1 param — dispatch on parameter type
        if (n == 1) {
            ValueType p0 = method.parameterType(0);
            if (p0 instanceof ValueType.Object) {
                String cn = ((ValueType.Object) p0).getClassName();
                // Single CodeBlocksBaseMessage subtype → cast
                if (implementsInterface(cn, CODE_BLOCKS_BASE_MESSAGE, hierarchy)) {
                    pe.invoke(method.getReference(), msgVar.cast(p0));
                    return;
                }
                // Single JsonElement → parse json field and pass
                if (cn.equals(JSON_ELEMENT)) {
                    MethodReference parseRef = new MethodReference(CODE_BLOCKS, "parseMessageJSON",
                            ValueType.object(CODE_BLOCKS_BASE_MESSAGE), ValueType.object(JSON_ELEMENT));
                    pe.invoke(method.getReference(), pe.invoke(parseRef, msgVar));
                    return;
                }
                // Single JsonObject → parse json and unwrap
                if (cn.equals(JSON_OBJECT)) {
                    MethodReference parseRef = new MethodReference(CODE_BLOCKS, "parseMessageJSON",
                            ValueType.object(CODE_BLOCKS_BASE_MESSAGE), ValueType.object(JSON_ELEMENT));
                    pe.invoke(method.getReference(), pe.invoke(parseRef, msgVar)
                            .invokeVirtual(new MethodReference(JSON_ELEMENT, "getObject",
                                    ValueType.object(JSON_OBJECT), ValueType.object(JSON_OBJECT)),
                                    pe.construct(JSON_OBJECT)));
                    return;
                }
                // Single JsonArray → parse json and unwrap
                if (cn.equals(JSON_ARRAY)) {
                    MethodReference parseRef = new MethodReference(CODE_BLOCKS, "parseMessageJSON",
                            ValueType.object(CODE_BLOCKS_BASE_MESSAGE), ValueType.object(JSON_ELEMENT));
                    pe.invoke(method.getReference(), pe.invoke(parseRef, msgVar)
                            .invokeVirtual(new MethodReference(JSON_ELEMENT, "getArray",
                                    ValueType.object(JSON_ARRAY))));
                    return;
                }
            }
        }

        // General case: unpack named values from the json payload
        MethodReference parseRef = new MethodReference(CODE_BLOCKS, "parseMessageJSON",
                ValueType.object(CODE_BLOCKS_BASE_MESSAGE), ValueType.object(JSON_ELEMENT));
        MethodReference getObjRef = new MethodReference(JSON_ELEMENT, "getObject", ValueType.object(JSON_OBJECT));
        ValueEmitter jsonObj = pe.invoke(parseRef, msgVar).invokeVirtual(getObjRef);

        ValueEmitter[] args = new ValueEmitter[n];
        int offset = method.getModifiers().contains(ElementModifier.STATIC) ? 0 : 1;
        for (int i = 0; i < n; i++) {
            args[i] = emitReadFromJson(pe, jsonObj,
                    resolveParamName(method, method.getProgram(), i, paramHints, offset), method.parameterType(i), hierarchy);
        }
        pe.invoke(method.getReference(), args);
    }

    private String resolveParamName(MethodHolder method, org.teavm.model.Program prog, int index, String[] hints, int offset) {
        if (hints != null && index < hints.length && hints[index] != null && !hints[index].isEmpty()) {
            return hints[index];
        }
        if (prog != null) {
            int effectiveOffset = offset;
            // Detect unexpected shift in static methods (e.g. variable 0 is empty but there's an extra variable)
            if (offset == 0 && prog.variableCount() > method.parameterCount()) {
                String firstVarName = prog.variableAt(0).getDebugName();
                if (firstVarName == null || firstVarName.isEmpty()) {
                    effectiveOffset = 1;
                }
            }
            if (index + effectiveOffset < prog.variableCount()) {
                String name = prog.variableAt(index + effectiveOffset).getDebugName();
                if (name != null && !name.isEmpty()) return name;
            }
        }
        return "p" + index;
    }

    private ValueEmitter emitReadFromJson(ProgramEmitter pe, ValueEmitter jsonObj, String name,
            ValueType type, ClassHierarchy hierarchy) {
        ValueEmitter key = pe.constant(name);
        if (type == ValueType.INTEGER || type == ValueType.SHORT || type == ValueType.BYTE) {
            return jsonObj.invokeVirtual(new MethodReference(JSON_OBJECT, "getInt",
                    ValueType.object("java.lang.String"), ValueType.INTEGER, ValueType.INTEGER),
                    key, pe.constant(0));
        }
        if (type == ValueType.FLOAT || type == ValueType.DOUBLE) {
            return jsonObj.invokeVirtual(new MethodReference(JSON_OBJECT, "getDouble",
                    ValueType.object("java.lang.String"), ValueType.DOUBLE, ValueType.DOUBLE),
                    key, pe.constant(0.0));
        }
        if (type == ValueType.BOOLEAN) {
            return jsonObj.invokeVirtual(new MethodReference(JSON_OBJECT, "getBoolean",
                    ValueType.object("java.lang.String"), ValueType.BOOLEAN, ValueType.BOOLEAN),
                    key, pe.constant(0));
        }
        if (type instanceof ValueType.Object) {
            String cn = ((ValueType.Object) type).getClassName();
            if (cn.equals("java.lang.String")) {
                return jsonObj.invokeVirtual(new MethodReference(JSON_OBJECT, "getString",
                        ValueType.object("java.lang.String"), ValueType.object("java.lang.String"),
                        ValueType.object("java.lang.String")),
                        key, pe.constant(""));
            }
            if (cn.equals(JSON_ELEMENT)) {
                return jsonObj.invokeVirtual(new MethodReference(JSON_OBJECT, "get",
                        ValueType.object("java.lang.String"), ValueType.object(JSON_ELEMENT)), key);
            }
            if (cn.equals(JSON_OBJECT)) {
                return jsonObj.invokeVirtual(new MethodReference(JSON_OBJECT, "get",
                        ValueType.object("java.lang.String"), ValueType.object(JSON_ELEMENT)), key)
                    .invokeVirtual(new MethodReference(JSON_ELEMENT, "getObject",
                        ValueType.object(JSON_OBJECT), ValueType.object(JSON_OBJECT)), pe.construct(JSON_OBJECT));
            }
            if (cn.equals(JSON_ARRAY)) {
                return jsonObj.invokeVirtual(new MethodReference(JSON_OBJECT, "get",
                        ValueType.object("java.lang.String"), ValueType.object(JSON_ELEMENT)), key)
                    .invokeVirtual(new MethodReference(JSON_ELEMENT, "getArray",
                        ValueType.object(JSON_ARRAY), ValueType.object(JSON_ARRAY)), pe.construct(JSON_ARRAY));
            }
            if (implementsInterface(cn, JSON_OBJECTABLE, hierarchy)) {
                ClassReader typeClass = hierarchy.getClassSource().get(cn);
                if (typeClass != null) {
                    // Prefer (JsonObject) constructor
                    if (typeClass.getMethod(new MethodDescriptor(
                            "<init>", ValueType.object(JSON_OBJECT), ValueType.VOID)) != null) {
                        ValueEmitter subObj = jsonObj.invokeVirtual(new MethodReference(JSON_OBJECT, "get",
                                ValueType.object("java.lang.String"), ValueType.object(JSON_ELEMENT)), key)
                            .invokeVirtual(new MethodReference(JSON_ELEMENT, "getObject",
                                ValueType.object(JSON_OBJECT), ValueType.object(JSON_OBJECT)), pe.construct(JSON_OBJECT));
                        return pe.construct(cn, subObj);
                    }
                    // Static fromJsonElement(JsonElement) factory — used e.g. for enums
                    if (typeClass.getMethod(new MethodDescriptor("fromJsonElement",
                            ValueType.object(JSON_ELEMENT), ValueType.object(cn))) != null) {
                        ValueEmitter jsonElem = jsonObj.invokeVirtual(new MethodReference(JSON_OBJECT, "get",
                                ValueType.object("java.lang.String"), ValueType.object(JSON_ELEMENT)), key);
                        return pe.invoke(new MethodReference(cn, "fromJsonElement",
                                ValueType.object(JSON_ELEMENT), ValueType.object(cn)), jsonElem);
                    }
                }
            }
        }
        // Unrecognised type — raw cast; will fail loudly at runtime
        return jsonObj.invokeVirtual(new MethodReference(JSON_OBJECT, "get",
                ValueType.object("java.lang.String"), ValueType.object(JSON_ELEMENT)), key).cast(type);
    }

    private void injectStaticEventRegistration(ClassHolder cls, String handlerName,
            ClassHolderTransformerContext context) {
        MethodDescriptor clinitDesc = new MethodDescriptor("<clinit>", ValueType.VOID);
        MethodHolder clinit = cls.getMethod(clinitDesc);

        MethodReference startRef = new MethodReference(
                CODE_BLOCKS, "startReceivingEvents",
                ValueType.object(CODE_BLOCKS_EVENT_FUNCTION), ValueType.VOID);

        if (clinit != null && clinit.getProgram() != null) {
            String origName = "__orig_clinit_" + (wrapperCounter++);
            MethodHolder origClinit = new MethodHolder(new MethodDescriptor(origName, ValueType.VOID));
            origClinit.setProgram(clinit.getProgram());
            origClinit.setLevel(AccessLevel.PRIVATE);
            origClinit.getModifiers().add(ElementModifier.STATIC);
            cls.addMethod(origClinit);

            ProgramEmitter pe = ProgramEmitter.create(clinit, context.getHierarchy());
            pe.invoke(startRef, pe.construct(handlerName));
            pe.invoke(new MethodReference(cls.getName(), origName, ValueType.VOID));
            pe.exit();
        } else {
            if (clinit == null) {
                clinit = new MethodHolder(clinitDesc);
                clinit.getModifiers().add(ElementModifier.STATIC);
                clinit.setLevel(AccessLevel.PUBLIC);
                cls.addMethod(clinit);
            }
            ProgramEmitter pe = ProgramEmitter.create(clinit, context.getHierarchy());
            pe.invoke(startRef, pe.construct(handlerName));
            pe.exit();
        }
    }
}
