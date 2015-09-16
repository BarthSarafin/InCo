package Grundlagen;

/**
 * @author Stefan R. Bachmann on  15/09/2015
 * @version v0.1 - Grundlagen
 */
public class Fibonacci_7 {

    public static long fib(int k){
        if(k == 0) return 1;
        if(k == 1) return 1;

        return fib(k-1) + fib(k-2);
    }
}
