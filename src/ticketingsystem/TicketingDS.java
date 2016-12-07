package ticketingsystem;

import java.util.Random;
import java.util.Vector;
import java.util.concurrent.locks.*;



public class TicketingDS implements TicketingSystem{
	
	private int routenum;      //车次总数
	private int coachnum;      //列车的车厢数目
	private int seatnum;       //每节车厢的座位数
	private int stationnum;    //每个车次经停站的数量
	private int sumSeat;       //每个车次列车的总座位数
	private MyVector<Ticket> sellTicket;
	private boolean [][][]seatMap;
	private ReentrantReadWriteLock []lock;//读写锁，没一个车次一把
	private long tid;
	
	@SuppressWarnings("serial")
	class MyVector<E> extends Vector<E>{
		synchronized E getRandomElement(Random random){
			return this.elementAt(random.nextInt(this.size()));
		}
	}
	
	public TicketingDS(){  //缺省值
		tid=0L;
		routenum=5;
		coachnum=8;
		seatnum=100;
		stationnum=10;
		sumSeat=coachnum*seatnum;
		lock=new ReentrantReadWriteLock[routenum];
		sellTicket=new MyVector<Ticket>();
		for(int i=0;i<routenum;i++){
			lock[i]=new ReentrantReadWriteLock();
		}
		seatMap=new boolean[routenum][sumSeat][stationnum];
		for(int i=0;i<routenum;i++){
			for(int j=0;j<sumSeat;j++){
				for(int k=0;k<stationnum;k++){
					seatMap[i][j][k]=false;
				}
			}
		}
		tid=0;
	}
	
	public TicketingDS(int routenum,int coachnum,int seatnum,int stationnum){
		// TODO Auto-generated constructor stub
		tid=0L;
		this.routenum=routenum;
		this.coachnum=coachnum;
		this.seatnum=seatnum;
		this.stationnum=stationnum;
		sumSeat=coachnum*seatnum;
		sellTicket=new MyVector<Ticket>();
		lock=new ReentrantReadWriteLock[routenum];
		for(int i=0;i<routenum;i++){
			lock[i]=new ReentrantReadWriteLock();
		}
		seatMap=new boolean[routenum][sumSeat][stationnum];
		for(int i=0;i<routenum;i++){
			for(int j=0;j<sumSeat;j++){
				for(int k=0;k<stationnum;k++){
					seatMap[i][j][k]=false;
				}
			}
		}
		tid=0;
	}
	
	public MyVector<Ticket> getSellTicket(){
		return sellTicket;
	}
	
	@Override
	public Ticket buyTicket(String passenger, int route, int departure, int arrival) {
		//所有要买的票本身有问题的情况
		if(route<0||route>routenum||departure>=arrival||departure<0
				||departure>stationnum-1||arrival<2||arrival>stationnum){
			//System.out.println("买票:车次-"+route+",起点站-"+departure+",终点站-"+arrival+",买票失败");
			return null;
		}
		//int max=0;
		Lock wlock=lock[route-1].writeLock();
		wlock.lock();
		try {
//			for(int i=departure;i<arrival;i++)  
//				max = sellTiketNum[route-1][i-1]>max?  sellTiketNum[route-1][i-1]:max;
//			if (max>=seatnum){
//				return null;
//			}else{
//				for(int i=departure;i<arrival;i++)
//					++sellTiketNum[route-1][i-1];
				
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
			if(i==sumSeat){  //找不到位置相当于没有票---买票失败
				//System.out.println("买票:车次-"+route+",起点站-"+departure+",终点站-"+arrival+",票已经卖完");
				return null;
			}
			for(j=departure;j<arrival;j++)
				seatMap[route-1][i][j-1]=true;
			Ticket ticket = new Ticket();
			ticket.tid=tid++;
			ticket.passenger=passenger;
			ticket.route=route;
			ticket.coach=i/seatnum+1;
			ticket.seat=i%seatnum+1;
			ticket.departure=departure;
			ticket.arrival=arrival;
			sellTicket.add(ticket);
			//System.out.println("买票:车次-"+route+",起点站-"+departure+",终点站-"+arrival+",买票成功");
			return ticket;
//			}	
		} finally {
			wlock.unlock();
		}
	}

	@Override
	public int inquiry(int route, int departure, int arrival) {
		//所有询问的票本身有问题的情况
		if(route<0||route>routenum||departure>=arrival||departure<0
				||departure>stationnum-1||arrival<2||arrival>stationnum){
			//System.out.println("查票:车次-"+route+",起点站-"+departure+",终点站-"+arrival+",查票失败");
			return -1;
		}
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
			//System.out.println("查票:车次-"+route+",起点站-"+departure+",终点站-"+arrival+",余票"+count);
			return count;
		} finally {
			rlock.unlock();
		}
	}

	@Override
	public boolean refundTicket(Ticket ticket) {
		if (!sellTicket.contains(ticket)){    //????有不有问题
			//System.out.println("退票:票号-"+ticket.tid+",没有这样的票");
			return false;
		}else{
			Lock wlock=lock[ticket.route-1].writeLock();
			wlock.lock();
			try {
				int seat=(ticket.coach-1)*seatnum + ticket.seat-1;
				for(int j=ticket.departure;j<ticket.arrival;j++)
					seatMap[ticket.route-1][seat][j-1]=false;
				sellTicket.remove(ticket);
				//System.out.println("退票:票号-"+ticket.tid+",退票成功");
				return true;
			} finally {
				wlock.unlock();
			}	
		}
	}
}
