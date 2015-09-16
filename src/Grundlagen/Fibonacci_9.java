package Grundlagen;

/**
 * @author Stefan R. Bachmann on  15/09/2015
 * @version v0.1 - Grundlagen
 */
public class Fibonacci_9 {

    public static long fib(int n){
        if(n == 0) return 1;
        if(n == 1) return 1;

        long a = 1, b = 1, c = a + b, k = 2;
        while(k != n){
            a = b;
            b = c;
            c = a + b;
            k += 1;
        }
        return c;
    }
}
