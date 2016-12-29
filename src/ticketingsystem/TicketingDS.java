package ticketingsystem;

import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.*;



public class TicketingDS implements TicketingSystem{
	
	private AtomicLong tid;
	private int routenum;      //��������
	private int coachnum;      //�г��ĳ�����Ŀ
	private int seatnum;       //ÿ�ڳ������λ��
	private int stationnum;    //ÿ�����ξ�ͣվ������
	private int sumSeat;       //ÿ�������г�������λ��
	public ConcurrentHashMap<Long, Ticket> selledTicket;
	private volatile boolean [][][]seatMap;  //Ϊ�˱�֤�߳�֮��Ŀɼ��ԣ������volatile
	private ReentrantLock [][]seatLock;
	
	public TicketingDS(){  //ȱʡֵ
		tid=new AtomicLong(0);
		routenum=5;
		coachnum=8;
		seatnum=100;
		stationnum=10;
		sumSeat=coachnum*seatnum;
		selledTicket=new ConcurrentHashMap<Long,Ticket>();
		seatLock=new ReentrantLock[routenum][sumSeat];
		for(int i=0;i<routenum;i++){
			for(int j=0;j<sumSeat;j++){
				seatLock[i][j]=new ReentrantLock();
			}
		}
		seatMap=new boolean[routenum][sumSeat][stationnum-1];
		for(int i=0;i<routenum;i++){
			for(int j=0;j<sumSeat;j++){
				Arrays.fill(seatMap[i][j], false);
			}
		}		
	}
	
	public TicketingDS(int routenum,int coachnum,int seatnum,int stationnum){
		tid=new AtomicLong(0);
		this.routenum=routenum;
		this.coachnum=coachnum;
		this.seatnum=seatnum;
		this.stationnum=stationnum;
		sumSeat=coachnum*seatnum;
		selledTicket=new ConcurrentHashMap<Long,Ticket>();
		seatLock=new ReentrantLock[routenum][sumSeat];
		for(int i=0;i<routenum;i++){
			for(int j=0;j<sumSeat;j++){
				seatLock[i][j]=new ReentrantLock();
			}
		}
		seatMap=new boolean[routenum][sumSeat][stationnum-1];
		for(int i=0;i<routenum;i++){
			for(int j=0;j<sumSeat;j++){
				Arrays.fill(seatMap[i][j], false);
			}
		}
	}	
	
	@Override
	public Ticket buyTicket(String passenger, int route, int departure, int arrival) {
		//�ȼ��һ���Ƿ������Ʊ
		int max=0;
		//ȷ���Ƿ���Ʊ������������һ�¿�λ��
		int i,j;
		Boolean flag=false;
		while(true){
			for(i=0;i<sumSeat;i++){
				for(j=departure;j<arrival;j++){
					if (seatMap[route-1][i][j-1]==true) {
						break;
					}
				}
				if (j==arrival) {
					seatLock[route-1][i].lock();
					try {
						//���ж�һ��,��Ϊ�п����Ѿ�����
						for(j=departure;j<arrival;j++){
							if (seatMap[route-1][i][j-1]==true) {
								break;
							}
						}
						if (j!=arrival) {  //����Ѿ����ı�������ɨ��
							break;
						}else{
							Arrays.fill(seatMap[route-1][i], departure-1,arrival-1,true);
							flag=true;
							break;
						}
					} finally {
						seatLock[route-1][i].unlock();
					}
				}
			}
			if (flag==true) {
				Ticket ticket=new Ticket();
				ticket.tid=tid.getAndIncrement();
				ticket.passenger=passenger;
				ticket.route=route;
				ticket.coach=i/seatnum+1;
				ticket.seat=i%seatnum+1;
				ticket.departure=departure;
				ticket.arrival=arrival;
				selledTicket.put(ticket.tid, ticket);
				//����ı�ÿ��վ�ĳ�Ʊ��
				return ticket;	
			}
			if (i==sumSeat) {  //�Ҳ���λ�ã��򲻵�Ʊ
				return null;
			}
		}
	}

	@Override
	public int inquiry(int route, int departure, int arrival) {  
		int i,j,count=0;
		for(i=0;i<sumSeat;i++){
			for(j=departure;j<arrival;j++){
				if (seatMap[route-1][i][j-1]==true) {
					break;
				}
			}
			if (j==arrival) {
				count++;
			}
		}
		return count;
			
	}

	@Override
	public boolean refundTicket(Ticket ticket) {
		if(!selledTicket.containsKey(ticket.tid)||selledTicket.isEmpty())
			return false;
		int seat=(ticket.coach-1)*seatnum + ticket.seat-1;
		seatLock[ticket.route-1][seat].lock();
		try {
			Arrays.fill(seatMap[ticket.route-1][seat],ticket.departure-1,ticket.arrival-1,false);
		} finally {
			seatLock[ticket.route-1][seat].unlock();
		}
		selledTicket.remove(ticket.tid);
		return true;
	}
}
