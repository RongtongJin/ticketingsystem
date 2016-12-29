package ticketingsystem;

import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.*;



public class TicketingDS implements TicketingSystem{
	
	private AtomicLong tid;
	private int routenum;      //车次总数
	private int coachnum;      //列车的车厢数目
	private int seatnum;       //每节车厢的座位数
	private int stationnum;    //每个车次经停站的数量
	private int sumSeat;       //每个车次列车的总座位数
	public ConcurrentHashMap<Long, Ticket> selledTicket;
	private volatile boolean [][][]seatMap;  //为了保证线程之间的可见性，必须加volatile
	private ReentrantLock [][]seatLock;
	
	public TicketingDS(){  //缺省值
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
		//先检测一下是否可能有票
		int max=0;
		//确认是否有票，首先试着找一下空位置
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
						//再判断一遍,因为有可能已经变了
						for(j=departure;j<arrival;j++){
							if (seatMap[route-1][i][j-1]==true) {
								break;
							}
						}
						if (j!=arrival) {  //如果已经被改变则重新扫描
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
				//下面改变每个站的车票数
				return ticket;	
			}
			if (i==sumSeat) {  //找不到位置，买不到票
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
