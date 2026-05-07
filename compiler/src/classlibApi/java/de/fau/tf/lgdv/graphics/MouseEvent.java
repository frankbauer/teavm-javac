package de.fau.tf.lgdv.graphics;

public interface MouseEvent {
  void onMouseEvent(MouseEventType type, MouseInfo mouse, ModifiersInfo modifiers);
}