package TestSplitter.Samples.Sample1;

/**
 * Created by od on 11/19/17.
 */
public class SingleLinkedList {

    Node header;
    int size;

    static class Node {

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
