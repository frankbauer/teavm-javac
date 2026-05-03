
/*
 *  Copyright 2026 frank bauer.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package de.fau.tf.lgdv.json;

public class JsonArray implements java.util.List<JsonElement>, JsonObjectable {
    private final java.util.List<JsonElement> list = new java.util.ArrayList<>(2);

    @Override
    public JsonElement toJsonElement() {
        return new JsonElement(this);
    }

    @Override
    public int size() {
        return list.size();
    }

    @Override
    public boolean isEmpty() {
        return list.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return list.contains(o);
    }

    @Override
    public java.util.Iterator<JsonElement> iterator() {
        return list.iterator();
    }

    @Override
    public Object[] toArray() {
        return list.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return list.toArray(a);
    }


    @Override
    public boolean add(JsonElement e){
        return list.add(e);
    }

    public boolean add(JsonObject o){
        return list.add(new JsonElement(o));
    }

    public boolean add(JsonObjectable o){
        return add(o.toJsonElement());
    }

    public boolean add(JsonArray o){
        return list.add(new JsonElement(o));
    }

    public boolean add(String o){
        return list.add(new JsonElement(o));
    }

    public boolean add(double o){
        return list.add(new JsonElement(o));
    }

    public boolean add(int o){
        return list.add(new JsonElement(o));
    }

    public boolean add(boolean o){
        return list.add(new JsonElement(o));
    }

    @Override
    public boolean remove(Object o) {
        return list.remove(o);
    }

    @Override
    public boolean containsAll(java.util.Collection<?> c) {
        return list.containsAll(c);
    }

    @Override
    public boolean addAll(java.util.Collection<? extends JsonElement> c) {
        return  list.addAll(c);
    }

    @Override
    public boolean addAll(int index, java.util.Collection<? extends JsonElement> c) {
        return list.addAll(index, c);
    }

    @Override
    public boolean removeAll(java.util.Collection<?> c) {
        return list.removeAll(c);
    }

    @Override
    public boolean retainAll(java.util.Collection<?> c) {
        return list.retainAll(c);
    }

    @Override
    public void clear(){
        list.clear();
    }

    @Override
    public JsonElement get(int index) {
        return list.get(index);
    }

    @Override
    public JsonElement set(int index, JsonElement element) {
        return list.set(index, element);
    }

    @Override
    public void add(int index, JsonElement element) {
        list.add(index, element);
    }

    @Override
    public JsonElement remove(int index) {
        return list.remove(index);
    }

    @Override
    public int indexOf(Object o) {
        return list.indexOf(o);
    }

    @Override
    public int lastIndexOf(Object o) {
        return list.lastIndexOf(o);
    }

    @Override
    public java.util.ListIterator<JsonElement> listIterator() {
        return list.listIterator();
    }

    @Override
    public java.util.ListIterator<JsonElement> listIterator(int index) {
        return list.listIterator(index);
    }

    @Override
    public java.util.List<JsonElement> subList(int fromIndex, int toIndex) {
        return list.subList(fromIndex, toIndex);
    }

    public String toJson(){
        return list
                .stream()
                .map(JsonElement::toJson)
                .collect(java.util.stream.Collectors.joining(",", "[", "]"));
    }
}
