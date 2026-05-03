package de.fau.tf.lgdv.runtime;
import de.fau.tf.lgdv.json.*;
import org.teavm.jso.JSProperty;
public interface ObjectReplyMessage extends NewRemoteObjectMessage {
        @JSProperty("type")
        String _getType();

        default String getType() {
            String s = _getType();
            return s != null ? new String(s) : null;
        }

        @JSProperty("type")
        void setType(String value);

        @JSProperty("cmd")
        String _getCmd();

        default String getCmd() {
            String s = _getCmd();
            return s != null ? new String(s) : null;
        }

        @JSProperty("cmd")
        void setCmd(String value);
    }
