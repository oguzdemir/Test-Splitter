public class Number {
    public static int min (int x, int y, int z) {
        if (x < y) {
            if (x < z) {
                System.out.println("Path 1 is executed");
                return x;
            } else {
                System.out.println("Path 2 is executed");
                return z;
            }
        }
        if ( y < z ) {
            System.out.println("Path 3 is executed");
            return y;
        }

        System.out.println("Path 4 is executed");
        return z;
    }
}
