package de.fau.tf.lgdv;
import de.fau.tf.lgdv.json.*;
import org.teavm.jso.JSProperty;
public interface NewRemoteObjectMessage extends CodeBlocksBaseMessage {
    @JSProperty("json")
    String _getJSON();

    default JsonElement getJSON() {
        String s = _getJSON();
        return s != null ? JsonParser.parse(s) : null;
    }

    @JSProperty("json")
    void _setJSON(String value);    

    default void setJSON(JsonSerializer json) {
        String s = json != null ? json.toJson() : null;
        _setJSON(s);
    }    
} 
