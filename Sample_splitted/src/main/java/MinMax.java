public class MinMax {
    public static int min(int x, int y) {
        if (x < y) {
            System.out.println("min: path 1");
            return x;
        }
        System.out.println("min: path 2");
        return y;
    }

    public static int max(int x, int y) {
        if (x > y) {
            System.out.println("max: path 1");
            return x;
        }
        System.out.println("max: path 2");
        return y;
    }
}
