package ticketingsystem;

import java.util.ArrayList;
import java.util.Random;


public class Test {
	public static int routenum=10;      //��������
	public static int coachnum=8;      //�г��ĳ�����Ŀ
	public static int seatnum=100;       //ÿ�ڳ������λ��
	public static int stationnum=10;    //ÿ�����ξ�ͣվ������
	public static int execTimesAThread=10000;
	class TicketAgent implements Runnable{
		ArrayList<Ticket> selledList=new ArrayList<Ticket>();
		long queryTime;
		long buyTime;
		long refundTime;
		int queryCount;
		int buyCountSuccess;
		int buyCountFail;
		int refundCount;
		TicketingDS tds;
		
		public TicketAgent(TicketingDS tds) {
			// TODO Auto-generated constructor stub
			selledList=new ArrayList<Ticket>();
			queryTime=0L;
			buyTime=0L;
			refundTime=0L;
			queryCount=0;
			buyCountSuccess=0;
			buyCountFail=0;
			refundCount=0;
			this.tds=tds;
		}
		@Override
		public void run() {
			// TODO Auto-generated method stub
			Random random =new Random();
			for(int i=0;i<execTimesAThread;i++){
				double access=random.nextDouble();
				if (access<0.6) {  //��ѯ
					int route=random.nextInt(routenum)+1;
					int departure=random.nextInt(stationnum)+1;
					int arrival=random.nextInt(stationnum)+1;
					while(departure==arrival){
						arrival=random.nextInt(stationnum)+1;
					}
					if(departure>arrival){
						int tmp=departure;
						departure=arrival;
						arrival=tmp;
					}
					long startTime=System.nanoTime();
					int leftTicket=tds.inquiry(route, departure, arrival);
					long endTime=System.nanoTime();
					queryTime+=endTime-startTime;
					++queryCount;
					//System.out.println("��ѯ����Ʊ��Ϊ"+leftTicket);
				}else if(access<0.9){  //��Ʊ
					int route=random.nextInt(routenum)+1;
					int departure=random.nextInt(stationnum)+1;
					int arrival=random.nextInt(stationnum)+1;
					while(departure==arrival){
						arrival=random.nextInt(stationnum)+1;
					}
					if(departure>arrival){
						int tmp=departure;
						departure=arrival;
						arrival=tmp;
					}
					long startTime=System.nanoTime();
					Ticket ticket=tds.buyTicket(" ", route, departure, arrival);
					long endTime=System.nanoTime();
					buyTime+=endTime-startTime;
					if (ticket!=null) {
						selledList.add(ticket);
						//System.out.println("��Ʊ�ɹ�");
						++buyCountSuccess;
					}else{
						//System.out.println("��Ʊʧ��");
						++buyCountFail;
					}
					
				}else{   //��Ʊ
					if (selledList.isEmpty()){
						--i;  //��β���
						continue;
					}
					int tid=random.nextInt(selledList.size());
					long startTime=System.nanoTime();
					Boolean isSuccess=tds.refundTicket(selledList.get(tid));
					long endTime=System.nanoTime();
					refundTime+=endTime-startTime;
					if (isSuccess) {
						//System.out.println("��Ʊ�ɹ�");
						selledList.remove(tid);
					}else{
						//System.out.println("��Ʊʧ��");
					}
					++refundCount;
				}
			}
		}
	}

	
	public static void main(String args[]){
		Test test=new Test();
		int threadNum;
		if(args.length>0){
			threadNum=Integer.parseInt(args[0]);
		}else{
			threadNum=16;
		}
		TicketingDS tds = new TicketingDS(routenum,coachnum,seatnum,stationnum);
		Thread []thread=new Thread[threadNum];
		TicketAgent []agent=new TicketAgent[threadNum];
		for(int i=0;i<threadNum;i++){
			agent[i]=test.new TicketAgent(tds);
			thread[i]=new Thread(agent[i]);
		}
		System.out.println("-----------------------------------------------------");
		long startTime= System.currentTimeMillis();//��ʼʱ��
		try {
			for(int i=0;i<threadNum;i++){
				thread[i].start();
			}
			for(int i=0;i<threadNum;i++)
				thread[i].join();
		} catch (Exception e) {
			// TODO: handle exception
		}
		long endTime=System.currentTimeMillis();//����ʱ��
		System.out.println("-----------------------------------------------------");
		long TotalQueryTime=0L;
		long TotalBuyTime=0L;
		long TotalRefundTime=0L;
		long queryCallTimes=0L;
		long buySuccessCallTimes=0L;
		long buyFailCallTimes=0L;
		long refundCallTimes=0L;
		long selledNumber=0L;
		for(int i=0;i<threadNum;i++){
			TotalQueryTime+=agent[i].queryTime;
			TotalBuyTime+=agent[i].buyTime;
			TotalRefundTime+=agent[i].refundTime;
			queryCallTimes+=agent[i].queryCount;
			buySuccessCallTimes+=agent[i].buyCountSuccess;
			buyFailCallTimes+=agent[i].buyCountFail;
			refundCallTimes+=agent[i].refundCount;
			selledNumber+=agent[i].selledList.size();
		}
		System.out.println("Thread num:"+ threadNum);
		System.out.println("Total execution time(ms): "+(endTime-startTime));
		System.out.println("Throughput rate: "+threadNum*execTimesAThread*1000/(endTime-startTime));
		System.out.println("Query method average call time(ns): "+TotalQueryTime/queryCallTimes);
		System.out.println("Buy method average call time(ns): "+TotalBuyTime/(buySuccessCallTimes+buyFailCallTimes));
		System.out.println("Refund method average call time(ns): "+TotalRefundTime/refundCallTimes);
		System.out.println("Query method execution times: "+queryCallTimes);
		System.out.println("Buy method execution times: "+(buySuccessCallTimes+buyFailCallTimes));
		System.out.println("Refund method execution times: "+refundCallTimes);
		System.out.println("Total execution times: "+(queryCallTimes+buySuccessCallTimes+buyFailCallTimes+refundCallTimes));
		System.out.println("Buy success method execution times: "+buySuccessCallTimes);
		System.out.println("selled number:"+tds.selledTicket.size());
		System.out.println("selled number2:"+selledNumber);
		System.out.println("-----------------------------------------------------");
	}
}
