package ticketingsystem;

import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;



public class Test {
	//ConcurrentHashMap<Long, Ticket> map=new ConcurrentHashMap<Long, Ticket>();
	private static int taskCount=1;
	private int routenum=5;      //��������
	private int coachnum=8;      //�г��ĳ�����Ŀ
	private int seatnum=100;       //ÿ�ڳ������λ��
	private int stationnum=10;    //ÿ�����ξ�ͣվ������
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
//					System.out.print("����"+id+"��ѯ,��·Ϊ"+route+"ʼ��վ"+departure+"�յ�վ"+arrival);
//					if(res==-1){
//						System.out.println("  ��ѯʧ��,��ѯ���Ϸ�");
//					}else{
//						System.out.println("  ��ѯ�ɹ�,��ƱΪ"+res);
//					}
				}
				
				for(int i=0;i<3;i++){
					int route=random.nextInt(routenum)+1;
					int departure=random.nextInt(stationnum-1)+1;
					int arrival=departure+random.nextInt(stationnum-departure)+1;
					Ticket ticket=tds.buyTicket("passenger",route,departure,arrival);
//					System.out.print("����"+id+"��Ʊ,��·Ϊ"+route+"ʼ��վ"+departure+"�յ�վ"+arrival);
//					if(ticket==null){
//						System.out.println("��Ʊʧ��");
//					}else{
//						System.out.println("��Ʊ�ɹ�");
//						//map.put(ticket.tid, ticket);
//					}
				}
				
			//	System.out.print("����"+id+"��Ʊ");
				boolean res=tds.refundTicket(tds.getSellTicket().elementAt(random.nextInt(tds.getSellTicket().size())));
//				if (res==false){
//					System.out.println("��Ʊʧ��");
//				}else {
//					System.out.println("��Ʊ�ɹ�");
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
		long startTime= System.currentTimeMillis();//��ʼʱ��
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
		
        System.out.println("ִ����ʱ��:"+(endTime-startTime));
        System.out.println("������:"+taskNum*ExecTimesAThread*1000/(endTime-startTime)); 
		exec.shutdown();
	}
}
