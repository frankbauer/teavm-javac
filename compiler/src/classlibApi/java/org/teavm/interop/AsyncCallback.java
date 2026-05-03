package org.teavm.interop;
public interface AsyncCallback<T> {
    void complete(T result);
    void error(Throwable error);
}
