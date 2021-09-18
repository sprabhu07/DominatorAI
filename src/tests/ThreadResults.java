package tests;

public class ThreadResults {
	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
        Results1 results1 = new Results1();
        Results2 results2 = new Results2();
        
        results1.init();
        results2.init();
        
        Thread t1 = new Thread(results1);
        Thread t2 = new Thread(results2);

        t1.start();
        t2.start();
        
		System.out.println("Experiment running");

	}
}
