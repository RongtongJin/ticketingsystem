package ticketingsystem;

import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;



public class Test {
	private int routenum=5;      //��������
	private int coachnum=8;      //�г��ĳ�����Ŀ
	private int seatnum=100;       //ÿ�ڳ������λ��
	private int stationnum=10;    //ÿ�����ξ�ͣվ������
	public static int ExecTimesAThread=10000;
	public static Object object=new Object();
	TicketingDS tds = new TicketingDS(routenum,coachnum,seatnum,stationnum);
	class Task implements Runnable{
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
					tds.inquiry(route, departure, arrival);
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
					tds.buyTicket("passenger",route,departure,arrival);
//					System.out.print("����"+id+"��Ʊ,��·Ϊ"+route+"ʼ��վ"+departure+"�յ�վ"+arrival);
//					if(ticket==null){
//						System.out.println("��Ʊʧ��");
//					}else{
//						System.out.println("��Ʊ�ɹ�");
//						//map.put(ticket.tid, ticket);
//					}
				}
				
			//	System.out.print("����"+id+"��Ʊ");
				synchronized (object) {
					int num=tds.getSellTicket().size();
					tds.refundTicket(tds.getSellTicket().elementAt(random.nextInt(num)));
				}
//				if (res==false){
//					System.out.println("��Ʊʧ��");
//				}else {
//					System.out.println("��Ʊ�ɹ�");
//				}
				synchronized (this) {
					--count;
				}
			}
		}
	}
	
//	public static void main(String args[]){
//		Test test=new Test();
//		ExecutorService exec = Executors.newFixedThreadPool(16);
//		int threadNum=16;
//		long startTime= System.currentTimeMillis();//��ʼʱ��
//	//	countDownLatch = new CountDownLatch(threadNum);
//		Future<?> []future=(Future<?>[])new Future[threadNum];
//		try {
//			for(int i=0;i<threadNum;i++)
//				future[i]=exec.submit(test.new Task());
//			for(int i=0;i<threadNum;i++)
//				future[i].get();
//		} catch (Exception e) {
//			// TODO: handle exception
//		}
//		exec.shutdown(); 
//		long endTime=System.currentTimeMillis();
//		
//        System.out.println("ִ����ʱ��:"+(endTime-startTime));
//        System.out.println("������:"+threadNum*ExecTimesAThread*1000/(endTime-startTime)); 
//		
//	}
	
	public static void main(String args[]){
		Test test=new Test();
		//ExecutorService exec = Executors.newFixedThreadPool(16);
		int threadNum=64;
		long startTime= System.currentTimeMillis();//��ʼʱ��
		Thread []thread=new Thread[threadNum];
		try {
			for(int i=0;i<threadNum;i++){
				thread[i]=new Thread(test.new Task());
				thread[i].start();
			}
			for(int i=0;i<threadNum;i++)
				thread[i].join();
		} catch (Exception e) {
			// TODO: handle exception
		}
		long endTime=System.currentTimeMillis();//����ʱ��
		System.out.println("ִ����ʱ��:"+(endTime-startTime));
		System.out.println("������:"+threadNum*ExecTimesAThread*1000/(endTime-startTime)); 
	}
}
