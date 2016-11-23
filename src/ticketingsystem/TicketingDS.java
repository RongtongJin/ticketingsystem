package ticketingsystem;

import java.nio.file.attribute.FileOwnerAttributeView;
import java.util.concurrent.locks.*;


public class TicketingDS implements TicketingSystem{
	
	private int routenum;      //��������
	private int coachnum;      //�г��ĳ�����Ŀ
	private int seatnum;       //ÿ�ڳ������λ��
	private int stationnum;    //ÿ�����ξ�ͣվ������
	private int sumSeat;       //ÿ�������г�������λ��
	private int [][]sellTiketNum;    //���ڸ�������ÿһ����ͣվ�ж���Ʊ������
	private boolean [][][]seatMap;
	private ReentrantReadWriteLock []lock;
	private long tid;
	
	
	public TicketingDS(){  //ȱʡֵ
		routenum=5;
		coachnum=8;
		seatnum=100;
		stationnum=10;
		sumSeat=coachnum*seatnum;
		sellTiketNum=new int [routenum][stationnum];
		lock=new ReentrantReadWriteLock[routenum];
		seatMap=new boolean[routenum][sumSeat][stationnum];
		tid=0;
	}
	
	public TicketingDS(int routenum,int coachnum,int seatnum,int stationnum){
		// TODO Auto-generated constructor stub
		this.routenum=routenum;
		this.coachnum=coachnum;
		this.seatnum=seatnum;
		this.stationnum=stationnum;
		sumSeat=coachnum*seatnum;
//		sellTiketNum=new int [routenum][stationnum];
		lock=new ReentrantReadWriteLock[routenum];
		seatMap=new boolean[routenum][sumSeat][stationnum];
		tid=0;
	}
	
	@Override
	public Ticket buyTicket(String passenger, int route, int departure, int arrival) {
		//����Ҫ���Ʊ��������������
		if(route<0||route>routenum||departure>=arrival||departure<0
				||departure>stationnum-1||arrival<2||arrival>stationnum)
			return null;
		int max=0;
		Lock wlock=lock[route-1].writeLock();
		wlock.lock();
		try {
			for(int i=departure;i<arrival;i++)  
				max = sellTiketNum[route-1][i-1]>max?  sellTiketNum[route-1][i-1]:max;
			if (max>=seatnum){
				return null;
			}else{
//				for(int i=departure;i<arrival;i++)
//					++sellTiketNum[route-1][i-1];
				Ticket ticket = new Ticket();
				ticket.tid=tid++;
				ticket.passenger=passenger;
				ticket.route=route;
				int i,j;
				for(i=0;i<sumSeat;i++){
					for(j=departure;j<arrival;j++){
						if (seatMap[route-1][i][j-1]==true) {
							break;
						}
					}
					if (j==arrival) {
						break;
					}
				}
				if(i==sumSeat)  //�Ҳ���λ���൱��û��Ʊ---��Ʊʧ��
					return null;
				for(j=departure;j<arrival;j++)
					seatMap[route-1][i][j-1]=true;
				ticket.coach=i/seatnum+1;
				ticket.seat=i%seatnum+1;
				ticket.departure=departure;
				ticket.arrival=arrival;
				return ticket;
			}	
		} finally {
			wlock.unlock();
		}
	}

	@Override
	public int inquiry(int route, int departure, int arrival) {
		//����ѯ�ʵ�Ʊ��������������
		if(route<0||route>routenum||departure>=arrival||departure<0
				||departure>stationnum-1||arrival<2||arrival>stationnum)
			return -1;
		int max=0;
		int count=0;
		Lock rlock=lock[route-1].readLock();
		rlock.lock();
		try {
			int i,j;
			for(i=0;i<sumSeat;i++){
				for(j=departure;j<arrival;j++){
					if (seatMap[route-1][i][j-1]==true) {
						break;
					}
				}
				if (j==arrival) {
					++count;
				}
			}
			return count;
//			for(int i=departure;i<arrival;i++)  //��ȡ�������ľ�ͣվ����ȥ��Ʊ��������ֵ
//				max = sellTiketNum[route-1][i-1]>max?  sellTiketNum[route-1][i-1]:max;
//			return sumSeat-max;
			
		} finally {
			rlock.unlock();
		}
	}

	@Override
	public boolean refundTicket(Ticket ticket) {
		if (ticket.tid>tid) 
			return false;
		Lock wlock=lock[ticket.route-1].writeLock();
		wlock.lock();
		try {
			int seat=(ticket.coach-1)*seatnum + ticket.seat-1;
			for(int j=ticket.departure;j<ticket.arrival;j++)
				seatMap[ticket.route-1][seat][j]=false;
			return true;
		} finally {
			wlock.unlock();
		}	
	}
}
