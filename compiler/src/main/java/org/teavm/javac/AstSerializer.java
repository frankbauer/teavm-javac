package org.teavm.javac;

import com.sun.tools.javac.tree.EndPosTable;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.*;

public class AstSerializer {
    private final StringBuilder out = new StringBuilder();
    private EndPosTable endPositions;

    public String serialize(com.sun.tools.javac.util.List<JCCompilationUnit> units) {
        out.append('[');
        boolean first = true;
        for (var unit : units) {
            if (!first) out.append(',');
            first = false;
            endPositions = unit.endPositions;
            writeTree(unit);
        }
        out.append(']');
        return out.toString();
    }

    private void writeTree(JCTree tree) {
        if (tree == null) { out.append("null"); return; }

        if (tree instanceof JCCompilationUnit t)    { writeCompilationUnit(t); }
        else if (tree instanceof JCImport t)        { writeImport(t); }
        else if (tree instanceof JCClassDecl t)     { writeClass(t); }
        else if (tree instanceof JCMethodDecl t)    { writeMethod(t); }
        else if (tree instanceof JCVariableDecl t)  { writeVariable(t); }
        else if (tree instanceof JCBlock t)         { writeBlock(t); }
        else if (tree instanceof JCIf t)            { writeIf(t); }
        else if (tree instanceof JCWhileLoop t)     { writeWhile(t); }
        else if (tree instanceof JCDoWhileLoop t)   { writeDoWhile(t); }
        else if (tree instanceof JCForLoop t)       { writeFor(t); }
        else if (tree instanceof JCEnhancedForLoop t) { writeForEach(t); }
        else if (tree instanceof JCReturn t)        { writeReturn(t); }
        else if (tree instanceof JCThrow t)         { writeThrow(t); }
        else if (tree instanceof JCTry t)           { writeTry(t); }
        else if (tree instanceof JCCatch t)         { writeCatch(t); }
        else if (tree instanceof JCSwitch t)        { writeSwitch(t); }
        else if (tree instanceof JCSwitchExpression t) { writeSwitchExpr(t); }
        else if (tree instanceof JCCase t)          { writeCase(t); }
        else if (tree instanceof JCBreak t)         { writeBreak(t); }
        else if (tree instanceof JCContinue t)      { writeContinue(t); }
        else if (tree instanceof JCExpressionStatement t) { writeExprStmt(t); }
        else if (tree instanceof JCSkip)            { begin("EMPTY_STATEMENT", tree); end(); }
        else if (tree instanceof JCMethodInvocation t)  { writeMethodInvocation(t); }
        else if (tree instanceof JCNewClass t)      { writeNewClass(t); }
        else if (tree instanceof JCNewArray t)      { writeNewArray(t); }
        else if (tree instanceof JCIdent t)         { writeIdent(t); }
        else if (tree instanceof JCFieldAccess t)   { writeFieldAccess(t); }
        else if (tree instanceof JCLiteral t)       { writeLiteral(t); }
        else if (tree instanceof JCBinary t)        { writeBinary(t); }
        else if (tree instanceof JCUnary t)         { writeUnary(t); }
        else if (tree instanceof JCAssign t)        { writeAssign(t); }
        else if (tree instanceof JCAssignOp t)      { writeAssignOp(t); }
        else if (tree instanceof JCConditional t)   { writeConditional(t); }
        else if (tree instanceof JCTypeCast t)      { writeTypeCast(t); }
        else if (tree instanceof JCInstanceOf t)    { writeInstanceOf(t); }
        else if (tree instanceof JCArrayAccess t)   { writeArrayAccess(t); }
        else if (tree instanceof JCParens t)        { writeParens(t); }
        else if (tree instanceof JCLambda t)        { writeLambda(t); }
        else if (tree instanceof JCMemberReference t) { writeMemberRef(t); }
        else if (tree instanceof JCPrimitiveTypeTree t) { writePrimType(t); }
        else if (tree instanceof JCArrayTypeTree t) { writeArrayType(t); }
        else if (tree instanceof JCTypeApply t)     { writeTypeApply(t); }
        else if (tree instanceof JCWildcard t)      { writeWildcard(t); }
        else if (tree instanceof JCTypeParameter t) { writeTypeParam(t); }
        else if (tree instanceof JCBindingPattern t) { writeBindingPattern(t); }
        else if (tree instanceof JCDefaultCaseLabel t) { begin("DEFAULT_CASE_LABEL", t); end(); }
        else if (tree instanceof JCConstantCaseLabel t) { begin("CONSTANT_CASE_LABEL", t); treeField("expr", t.expr); end(); }
        else if (tree instanceof JCPatternCaseLabel t)  { begin("PATTERN_CASE_LABEL", t); treeField("pattern", t.pat); end(); }
        else if (tree instanceof JCErroneous)       { begin("ERROR", tree); end(); }
        else {
            begin(tree.getClass().getSimpleName(), tree);
            end();
        }
    }

    // --- declarations ---

    private void writeCompilationUnit(JCCompilationUnit t) {
        begin("COMPILATION_UNIT", t);
        var pkgName = t.getPackageName();
        strField("package", pkgName != null ? pkgName.toString() : null);
        listField("imports", t.getImports());
        listField("typeDecls", t.getTypeDecls());
        end();
    }

    private void writeImport(JCImport t) {
        begin("IMPORT", t);
        boolField("static", t.staticImport);
        strField("qualid", t.qualid.toString());
        end();
    }

    private void writeClass(JCClassDecl t) {
        begin("CLASS", t);
        strField("name", t.name.toString());
        strField("classKind", classKind(t));
        modsField(t.mods);
        listField("typeParams", t.typarams);
        treeField("extending", t.extending);
        listField("implementing", t.implementing);
        listField("members", t.defs);
        end();
    }

    private void writeMethod(JCMethodDecl t) {
        begin("METHOD", t);
        strField("name", t.name.toString());
        modsField(t.mods);
        treeField("returnType", t.restype);
        listField("typeParams", t.typarams);
        listField("params", t.params);
        listField("thrown", t.thrown);
        treeField("body", t.body);
        end();
    }

    private void writeVariable(JCVariableDecl t) {
        begin("VARIABLE", t);
        strField("name", t.name.toString());
        modsField(t.mods);
        treeField("varType", t.vartype);
        treeField("init", t.init);
        end();
    }

    // --- statements ---

    private void writeBlock(JCBlock t) {
        begin("BLOCK", t);
        boolField("static", t.isStatic());
        listField("stats", t.stats);
        end();
    }

    private void writeIf(JCIf t) {
        begin("IF", t);
        treeField("cond", t.cond);
        treeField("thenpart", t.thenpart);
        treeField("elsepart", t.elsepart);
        end();
    }

    private void writeWhile(JCWhileLoop t) {
        begin("WHILE_LOOP", t);
        treeField("cond", t.cond);
        treeField("body", t.body);
        end();
    }

    private void writeDoWhile(JCDoWhileLoop t) {
        begin("DO_WHILE_LOOP", t);
        treeField("body", t.body);
        treeField("cond", t.cond);
        end();
    }

    private void writeFor(JCForLoop t) {
        begin("FOR_LOOP", t);
        listField("init", t.init);
        treeField("cond", t.cond);
        listField("step", t.step);
        treeField("body", t.body);
        end();
    }

    private void writeForEach(JCEnhancedForLoop t) {
        begin("ENHANCED_FOR_LOOP", t);
        treeField("var", t.var);
        treeField("expr", t.expr);
        treeField("body", t.body);
        end();
    }

    private void writeReturn(JCReturn t) {
        begin("RETURN", t);
        treeField("expr", t.expr);
        end();
    }

    private void writeThrow(JCThrow t) {
        begin("THROW", t);
        treeField("expr", t.expr);
        end();
    }

    private void writeTry(JCTry t) {
        begin("TRY", t);
        listField("resources", t.resources);
        treeField("body", t.body);
        listField("catchers", t.catchers);
        treeField("finalizer", t.finalizer);
        end();
    }

    private void writeCatch(JCCatch t) {
        begin("CATCH", t);
        treeField("param", t.param);
        treeField("body", t.body);
        end();
    }

    private void writeSwitch(JCSwitch t) {
        begin("SWITCH", t);
        treeField("selector", t.selector);
        listField("cases", t.cases);
        end();
    }

    private void writeSwitchExpr(JCSwitchExpression t) {
        begin("SWITCH_EXPRESSION", t);
        treeField("selector", t.selector);
        listField("cases", t.cases);
        end();
    }

    private void writeCase(JCCase t) {
        begin("CASE", t);
        strField("caseKind", t.caseKind.name());
        listField("labels", t.labels);
        treeField("guard", t.guard);
        listField("stats", t.stats);
        treeField("body", t.body);
        end();
    }

    private void writeBreak(JCBreak t) {
        begin("BREAK", t);
        strField("label", t.label != null ? t.label.toString() : null);
        end();
    }

    private void writeContinue(JCContinue t) {
        begin("CONTINUE", t);
        strField("label", t.label != null ? t.label.toString() : null);
        end();
    }

    private void writeExprStmt(JCExpressionStatement t) {
        begin("EXPRESSION_STATEMENT", t);
        treeField("expr", t.expr);
        end();
    }

    // --- expressions ---

    private void writeMethodInvocation(JCMethodInvocation t) {
        begin("METHOD_INVOCATION", t);
        listField("typeArgs", t.typeargs);
        treeField("methodSelect", t.meth);
        listField("args", t.args);
        end();
    }

    private void writeNewClass(JCNewClass t) {
        begin("NEW_CLASS", t);
        treeField("encl", t.encl);
        listField("typeArgs", t.typeargs);
        treeField("clazz", t.clazz);
        listField("args", t.args);
        treeField("classBody", t.def);
        end();
    }

    private void writeNewArray(JCNewArray t) {
        begin("NEW_ARRAY", t);
        treeField("elemType", t.elemtype);
        listField("dims", t.dims);
        listField("elems", t.elems);
        end();
    }

    private void writeIdent(JCIdent t) {
        begin("IDENTIFIER", t);
        strField("name", t.name.toString());
        end();
    }

    private void writeFieldAccess(JCFieldAccess t) {
        begin("MEMBER_SELECT", t);
        treeField("expr", t.selected);
        strField("identifier", t.name.toString());
        end();
    }

    private void writeLiteral(JCLiteral t) {
        begin("LITERAL", t);
        strField("typeTag", t.typetag.name());
        out.append(",\"value\":");
        writeLiteralValue(t);
        end();
    }

    private void writeBinary(JCBinary t) {
        begin("BINARY", t);
        strField("operator", t.getTag().name());
        treeField("lhs", t.lhs);
        treeField("rhs", t.rhs);
        end();
    }

    private void writeUnary(JCUnary t) {
        begin("UNARY", t);
        strField("operator", t.getTag().name());
        treeField("arg", t.arg);
        end();
    }

    private void writeAssign(JCAssign t) {
        begin("ASSIGNMENT", t);
        treeField("lhs", t.lhs);
        treeField("rhs", t.rhs);
        end();
    }

    private void writeAssignOp(JCAssignOp t) {
        begin("COMPOUND_ASSIGNMENT", t);
        strField("operator", t.getTag().name());
        treeField("lhs", t.lhs);
        treeField("rhs", t.rhs);
        end();
    }

    private void writeConditional(JCConditional t) {
        begin("CONDITIONAL", t);
        treeField("cond", t.cond);
        treeField("truepart", t.truepart);
        treeField("falsepart", t.falsepart);
        end();
    }

    private void writeTypeCast(JCTypeCast t) {
        begin("TYPE_CAST", t);
        treeField("type", t.clazz);
        treeField("expr", t.expr);
        end();
    }

    private void writeInstanceOf(JCInstanceOf t) {
        begin("INSTANCE_OF", t);
        treeField("expr", t.expr);
        treeField("pattern", t.pattern);
        end();
    }

    private void writeArrayAccess(JCArrayAccess t) {
        begin("ARRAY_ACCESS", t);
        treeField("indexed", t.indexed);
        treeField("index", t.index);
        end();
    }

    private void writeParens(JCParens t) {
        begin("PARENTHESIZED", t);
        treeField("expr", t.expr);
        end();
    }

    private void writeLambda(JCLambda t) {
        begin("LAMBDA", t);
        strField("bodyKind", t.getBodyKind().name());
        listField("params", t.params);
        treeField("body", t.body);
        end();
    }

    private void writeMemberRef(JCMemberReference t) {
        begin("MEMBER_REFERENCE", t);
        strField("mode", t.mode.name());
        treeField("expr", t.expr);
        strField("name", t.name.toString());
        listField("typeArgs", t.typeargs);
        end();
    }

    // --- types ---

    private void writePrimType(JCPrimitiveTypeTree t) {
        begin("PRIMITIVE_TYPE", t);
        strField("typeName", t.typetag.name().toLowerCase());
        end();
    }

    private void writeArrayType(JCArrayTypeTree t) {
        begin("ARRAY_TYPE", t);
        treeField("elemType", t.elemtype);
        end();
    }

    private void writeTypeApply(JCTypeApply t) {
        begin("PARAMETERIZED_TYPE", t);
        treeField("clazz", t.clazz);
        listField("args", t.arguments);
        end();
    }

    private void writeWildcard(JCWildcard t) {
        begin("WILDCARD", t);
        strField("boundKind", t.kind.kind.name());
        treeField("bound", t.inner);
        end();
    }

    private void writeTypeParam(JCTypeParameter t) {
        begin("TYPE_PARAMETER", t);
        strField("name", t.name.toString());
        listField("bounds", t.bounds);
        end();
    }

    private void writeBindingPattern(JCBindingPattern t) {
        begin("BINDING_PATTERN", t);
        treeField("var", t.var);
        end();
    }

    // --- helpers ---

    private void begin(String kind, JCTree tree) {
        out.append("{\"kind\":");
        writeStr(kind);
        out.append(",\"pos\":").append(tree.pos);
        int ep = endPositions != null ? tree.getEndPosition(endPositions) : -1;
        if (ep >= 0) out.append(",\"endPos\":").append(ep);
    }

    private void end() {
        out.append('}');
    }

    private void strField(String name, String value) {
        out.append(',');
        writeStr(name);
        out.append(':');
        if (value == null) out.append("null");
        else writeStr(value);
    }

    private void boolField(String name, boolean value) {
        out.append(',');
        writeStr(name);
        out.append(':').append(value);
    }

    private void treeField(String name, JCTree value) {
        out.append(',');
        writeStr(name);
        out.append(':');
        writeTree(value);
    }

    private void listField(String name, com.sun.tools.javac.util.List<? extends JCTree> items) {
        out.append(',');
        writeStr(name);
        out.append(":[");
        if (items != null) {
            boolean first = true;
            for (var item : items) {
                if (!first) out.append(',');
                first = false;
                writeTree(item);
            }
        }
        out.append(']');
    }

    private void modsField(JCModifiers mods) {
        out.append(",\"modifiers\":[");
        if (mods != null) {
            boolean first = true;
            for (var mod : mods.getFlags()) {
                if (!first) out.append(',');
                first = false;
                writeStr(mod.name().toLowerCase());
            }
        }
        out.append(']');
    }

    private void writeLiteralValue(JCLiteral t) {
        if (t.value == null) {
            out.append("null");
        } else if (t.value instanceof String s) {
            writeStr(s);
        } else if (t.value instanceof Character c) {
            writeStr(String.valueOf(c));
        } else {
            out.append(t.value);
        }
    }

    private void writeStr(String s) {
        out.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"'  -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default   -> {
                    if (c < 0x20) out.append(String.format("\\u%04x", (int) c));
                    else out.append(c);
                }
            }
        }
        out.append('"');
    }

    private String classKind(JCClassDecl t) {
        long flags = t.mods.flags;
        if ((flags & com.sun.tools.javac.code.Flags.ANNOTATION) != 0) return "ANNOTATION_TYPE";
        if ((flags & com.sun.tools.javac.code.Flags.ENUM) != 0) return "ENUM";
        if ((flags & com.sun.tools.javac.code.Flags.RECORD) != 0) return "RECORD";
        if ((flags & com.sun.tools.javac.code.Flags.INTERFACE) != 0) return "INTERFACE";
        return "CLASS";
    }
}
