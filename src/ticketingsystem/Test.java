package ticketingsystem;

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;



public class Test {
	TicketingDS tds = new TicketingDS();
	
	private static int taskCount=1;
	class Task implements Runnable{
		private final int id=taskCount++;
		
		public Task() {
			// TODO Auto-generated constructor stub
		}
		@Override
		public void run() {
			// TODO Auto-generated method stub
//			Random random =new Random();
//			while(!Thread.interrupted()){
//				
//				tds.inquiry(random.nextInt(10)+1, random.nextInt(10)+1, random.nextInt(10)+1);
//				
//				Ticket ticket=tds.buyTicket("passenger", random.nextInt(10)+1, random.nextInt(10)+1, random.nextInt(10)+1);
//				
//				tds.refundTicket(ticket);
//			}
			while(!Thread.interrupted()){
				System.out.println("#"+id);
				Thread.yield();
			}
			
		}
	}
	
	public static void main(String args[]){
		Test test=new Test();
		ExecutorService exec = Executors.newCachedThreadPool();
		for(int i=0;i<5;i++)
			exec.execute(test.new Task());
		exec.shutdown();
	}
}
