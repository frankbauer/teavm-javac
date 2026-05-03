package de.fau.tf.lgdv.runtime;
import de.fau.tf.lgdv.CodeBlocksQueryMessage;
import de.fau.tf.lgdv.json.*;
import org.teavm.jso.JSProperty;
public interface NewRemoteObjectMessage extends CodeBlocksQueryMessage {
    @JSProperty("objid")
    int getObjId();

    @JSProperty("objid")
    void setObjId(int value);       
}
