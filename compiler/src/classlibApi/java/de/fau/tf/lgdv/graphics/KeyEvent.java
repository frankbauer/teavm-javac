package de.fau.tf.lgdv.graphics;

public interface KeyEvent {
  void onKeyEvent(KeyEventType type, KeyInfo key, ModifiersInfo modifiers, MouseInfo mouse);
}