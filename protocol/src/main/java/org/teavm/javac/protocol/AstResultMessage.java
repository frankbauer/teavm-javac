package org.teavm.javac.protocol;

import org.teavm.jso.JSProperty;

public interface AstResultMessage extends WorkerMessage {
    @JSProperty
    String getAst();

    @JSProperty
    void setAst(String ast);
}
