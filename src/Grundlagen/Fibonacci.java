package Grundlagen;

/**
 * @author Stefan R. Bachmann on  15/09/2015
 * @version v0.1 - Grundlagen
 */
public class Fibonacci {

    public static void main(String[] args){
        int n = 48;

        if(n < 0) System.out.println("Fibonacci Number does not exist!");

        for( int k = 0; k <= n; k++) {
            System.out.println(k + ": " + Fibonacci_9.fib(k));
        }
    }
}
