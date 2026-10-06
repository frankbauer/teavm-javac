package de.fau.tf.lgdv.tilemap;

import de.fau.tf.lgdv.json.JsonObject;
import de.fau.tf.lgdv.runtime.CommandBuffer;
import de.fau.tf.lgdv.runtime.RemoteObject;

/**
 * Base of the tile map objects. They work in two modes, chosen when the object is created:
 * <ul>
 *   <li><b>RPC:</b> every method call is sent to the playground right away (requires message
 *   passing). Events (clicks, finished steps, ...) are delivered to the Java callbacks.</li>
 *   <li><b>Command queue:</b> while a {@link CommandBuffer} is active, all calls are recorded and
 *   replayed by the playground once {@link CommandBuffer#sendCommands()} is called. No events.</li>
 * </ul>
 * Both modes never wait for the playground, all state the methods return is kept in Java.
 */
abstract class MapObject extends RemoteObject {
    /** {@code true} if this object records into a {@link CommandBuffer} instead of sending messages. */
    protected final boolean queued;
    private boolean removed;

    protected MapObject(String type) {
        super(type);
        this.queued = CommandBuffer.getActive() != null;
    }

    /** Sends (or records) the creation of the object. Call at the end of the constructor. */
    protected void publish() {
        CommandBuffer buffer = CommandBuffer.getActive();
        if (buffer != null) {
            buffer.addNewObject(this);
        } else {
            sendNew();
        }
    }

    protected void send(String cmd, JsonObject data) {
        if (removed) {
            throw new IllegalStateException(TYPE + " #" + ID + " was removed");
        }
        sendCommand(cmd, data);
    }

    protected void send(String cmd) {
        send(cmd, new JsonObject());
    }

    protected void markRemoved() {
        removed = true;
    }

    /** {@code true} after {@code remove()} was called. */
    public boolean isRemoved() {
        return removed;
    }
}
