package Samples.Sample1;

import java.io.Serializable;

/**
 * Created by od on 11/19/17.
 */
public class SingleLinkedList implements Serializable {

    Node header;
    int size;

    static class Node implements Serializable {

        Node next;
        int elem;
    }

    public void add(int x) {
        Node n = new Node();
        n.elem = x;
        n.next = header;
        header = n;
        size++;
    }

    public boolean isEmpty() {
        return size == 0;
    }

}
