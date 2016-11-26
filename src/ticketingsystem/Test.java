package ticketingsystem;

import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;



public class Test {
	//ConcurrentHashMap<Long, Ticket> map=new ConcurrentHashMap<Long, Ticket>();
	private static int taskCount=1;
	private int routenum=5;      //车次总数
	private int coachnum=8;      //列车的车厢数目
	private int seatnum=100;       //每节车厢的座位数
	private int stationnum=10;    //每个车次经停站的数量
	public static int ExecTimesAThread=10000;
	public static CountDownLatch countDownLatch;
	TicketingDS tds = new TicketingDS(routenum,coachnum,seatnum,stationnum);
	class Task implements Runnable{
		private final int id=taskCount++;
		
		public Task() {
			// TODO Auto-generated constructor stub
		}
		@Override
		public void run() {
			// TODO Auto-generated method stub
			Random random =new Random();
			int count=ExecTimesAThread/10;
			while(count>0){
				for(int i=0;i<6;i++){
					int route=random.nextInt(routenum)+1;
					int departure=random.nextInt(stationnum-1)+1;
					int arrival=departure+random.nextInt(stationnum-departure)+1;
					int res=tds.inquiry(route, departure, arrival);
//					System.out.print("代理"+id+"查询,线路为"+route+"始发站"+departure+"终点站"+arrival);
//					if(res==-1){
//						System.out.println("  查询失败,查询不合法");
//					}else{
//						System.out.println("  查询成功,余票为"+res);
//					}
				}
				
				for(int i=0;i<3;i++){
					int route=random.nextInt(routenum)+1;
					int departure=random.nextInt(stationnum-1)+1;
					int arrival=departure+random.nextInt(stationnum-departure)+1;
					Ticket ticket=tds.buyTicket("passenger",route,departure,arrival);
//					System.out.print("代理"+id+"买票,线路为"+route+"始发站"+departure+"终点站"+arrival);
//					if(ticket==null){
//						System.out.println("买票失败");
//					}else{
//						System.out.println("买票成功");
//						//map.put(ticket.tid, ticket);
//					}
				}
				
			//	System.out.print("代理"+id+"退票");
				boolean res=tds.refundTicket(tds.getSellTicket().elementAt(random.nextInt(tds.getSellTicket().size())));
//				if (res==false){
//					System.out.println("退票失败");
//				}else {
//					System.out.println("退票成功");
//				}
				synchronized (this) {
					--count;
				}
			}
			countDownLatch.countDown();
		}
	}
	
	public static void main(String args[]){
		Test test=new Test();
		ExecutorService exec = Executors.newFixedThreadPool(16);
		int taskNum=16;
		long startTime= System.currentTimeMillis();//开始时间
		countDownLatch = new CountDownLatch(taskNum);
		for(int i=0;i<taskNum;i++)
			exec.execute(test.new Task());
		
		try {  
            countDownLatch.await();  
        } catch (InterruptedException e) {  
            // TODO Auto-generated catch block  
            e.printStackTrace();  
        }     
		long endTime=System.currentTimeMillis();
		
        System.out.println("执行总时间:"+(endTime-startTime));
        System.out.println("吞吐率:"+taskNum*ExecTimesAThread*1000/(endTime-startTime)); 
		exec.shutdown();
	}
}
