[TOC]



# 并发大作业报告

## 并发数据结构设计思路

### 思路1：粗粒度的读写锁

```java
public class TicketingDS implements TicketingSystem{
	private int routenum;      //车次总数
	private int coachnum;      //列车的车厢数目
	private int seatnum;       //每节车厢的座位数
	private int stationnum;    //每个车次经停站的数量
	private int sumSeat;       //每个车次列车的总座位数
	private ConcurrentHashMap<Long, Ticket> selledTicket;  //记录已经卖掉的票
	private boolean [][][]seatMap;    //座位表，第一维：车次 第二维：总座位数 第三维：车站数 
	private ReentrantReadWriteLock []lock;//读写锁，每一个车次一把
	private long tid;   //每次加一，生成唯一的tid
  	…………
}
```

​	当第一次分析完要求之后，首先想到的是用读写锁解决这个问题，买票和退票是写，查询票是读。当然要把读写锁变细，**每一个车次一把读写锁**，车次之间的读写互不影响。selledTicket用来记录已经卖掉的票，结构为ConcurrentHashMap<Long, Ticket>，key为tid，value为ticket引用。sumSeat记录了每一辆车次的总座位数，为coachnum（列车的车厢数目）*seatnum（每节车厢的座位数），可以根据根据总座位号来推断出在什么车厢第几座位，同理可以利用在什么车厢第几座位推断出总座位号。seatMap是一个三维的数组，第一维：车次，第二维：总座位数，第三维：车站数，seatMap=new boolean\[routenum]\[sumSeat][stationnum-1]，相当于每一个车次可以看作一个二维数组。假设一共有8个座位，10个车站，则seatMap如下图所示：![seatMap](C:\Users\79422\Desktop\seatMap.png)

​	一开始这个矩阵置为false，如果第一个人买了4-6站的，则置第一个座位的4-6站（3~4位）为true。

​	因此，程序的思路很简单，每次查询的时候加读锁，然后根据车次号以及起始站和终点站遍历矩阵，得到相应的余票数。

```java
public int inquiry(int route, int departure, int arrival) {
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
  } finally {
    rlock.unlock();
  }
}
```

​	对于买票来说，先加写锁，然后遍历矩阵，查询是否有空位，有空位则将该位置的部分置为true，然后出票。

```java
public Ticket buyTicket(String passenger, int route, int departure, int arrival) {
  Lock wlock=lock[route-1].writeLock();
  wlock.lock();
  try {
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
      return null;
    }
    for(j=departure;j<arrival;j++)
      seatMap[route-1][i][j-1]=true;
    ticket = new Ticket();
    ticket.tid=tid++;
    ticket.passenger=passenger;
    ticket.route=route;
    ticket.coach=i/seatnum+1;
    ticket.seat=i%seatnum+1;
    ticket.departure=departure;
    ticket.arrival=arrival;
    return ticket;
  } finally {
    if (ticket!=null) {
		selledTicket.put(ticket.tid, ticket);
	}
    wlock.unlock();
  }
}
```

​	对于退票来说，先检查是否有这样的票，若没有这样的票，则直接返回false，若有这样的票，则加写锁，改矩阵相应部分。

```java
public boolean refundTicket(Ticket ticket) {
  if (!selledTicket.containsKey(ticket.tid)||selledTicket.isEmpty()){    
    return false;
  }else{
    Lock wlock=lock[ticket.route-1].writeLock();
    wlock.lock();
    try {
      int seat=(ticket.coach-1)*seatnum + ticket.seat-1;
      for(int j=ticket.departure;j<ticket.arrival;j++)
        seatMap[ticket.route-1][seat][j-1]=false;
      return true;
    } finally {
      selledTicket.remove(ticket.tid);
      wlock.unlock();
    }	
  }
}
```

### 改进1：一种错误加速查询

​	根据要求，查询的比例占60%，而每一次查询时都需要遍历数组，将花费巨大的时间。因此考虑一种方法，能加速查询，建立一个二维数组everyStationTicket，记录对应的车次的每一个站已经卖了多少票，everyStationTicket=new int\[routenum]\[stationnum]，如果买了车次1的2~5战的票，则everyStationTicket\[2-1]\[stationnum-1]++，当退票的时候相应的位置减小，查询的时候求区间段中的最大值，利用总座位数-最大值即为余票数，代码如下：

```java
//--------初始化-----------
public int [][]everyStationTicket;
everyStationTicket=new int[routenum][stationnum];
		for(int i=0;i<routenum;i++)
			Arrays.fill(everyStationTicket[i], 0);
//--------查票-----------
public int inquiry(int route, int departure, int arrival) {
  int max=0;
  Lock rlock=lock[route-1].readLock();
  rlock.lock();
  try {
    for(int i=departure;i<arrival;i++){
      max= max>everyStationTicket[route-1][i-1]? max:everyStationTicket[route-1][i-1];
    }
    return sumSeat-max; 
  } finally {
    rlock.unlock();
}
//--------买票-----------
  for(int i=departure;i<arrival;i++){   //先检查是否有票
    max= max>everyStationTicket[route-1][i-1]? max:everyStationTicket[route-1][i-1];
  }
  if (max>=sumSeat) {
    return null;
  }	
  ……
  for(int k=departure;k<arrival;k++)     //有票且出票以后
	++everyStationTicket[route-1][k-1];
 //--------退票-----------
  for(int i=ticket.departure;i<ticket.arrival;i++)
    --everyStationTicket[ticket.route-1][i-1]; 
```

多线程测试程序设计思路

​	这可以大大加速查询的效率，然而却也有**巨大的问题**，看下面这一种情况，一个4个站，3张票。先买走1-2车站的一号座位，3-4车站的一号座位，以及2-3车站的3个座位，然后退票2-3站的1号座位，这时候买1-4站的车票，还有不有票呢？矩阵情况如图所示：

![question](https://www.processon.com/chart_image/58565750e4b04ce387831738.png)

​	这时候应该是没有票了，但是利用加速查询的方法，现在数组情况如下：

![question2](https://www.processon.com/chart_image/5856963ee4b04ce3878b74f6.png)

​	这时候最大值为2，余票应该为1，因此查询不准确。事实上这个加速方法会无法准确判断座位不同的情况，如果1-3有一张余票，3-5有一张余票，但两者座位不同，这时候加速的方法会错误判断有一张余票（事实上没有余票）。

### 改进2：位操作（失败）

​	遍历数组太浪费时间，能否利用位操作来加速遍历呢？我马上利用Java的Bitset改进了一下。

```java
//-------数据结构---------
private BitSet [][]seatMap;
seatMap=new BitSet[routenum][sumSeat];
for(int i=0;i<routenum;i++){
  for(int j=0;j<sumSeat;j++){
    seatMap[i][j]=new BitSet(stationnum);
  }
}
//-------买票---------
BitSet tmp=seatMap[route-1][i].get(departure-1,arrival-1);
tmp.or(tmpAllfalse);
if (tmp.isEmpty()) {
  seatMap[route-1][i].set(departure-1, arrival-1, true);
  ……
}
//-------退票---------
seatMap[ticket.route-1][seat].clear(ticket.departure-1, ticket.arrival-1);
```

​	然而，在性能测试中，发现位操作的速度要比普通的Boolean数组慢两倍之多，因此该改进被放弃。

### 改进3：细粒度的锁+先查询再加锁

​	加速并发除了可以缩小锁的范围（减少临界区的代码量，已经尽力优化），还可以减小的锁的粒度，因此，我们将锁细化，从对每个车次上一把读写锁，改成对每个车次的每个座位上一把可重入的锁，此外**在买票的时候，查询哪个位置有余票的时候先不加锁，当找到以后再加锁，然后重新验证该位置是否真的有余票（因为不是原子操作，所以可能找到位置以后，加锁之前，已经有人把该位置抢了，所以必须重新验证），若没有余票（已经被人抢了），则重新扫描，否则抢占该位置。**另外，对于改进1，则利用读写锁来保证其**可见性和原子性**具体代码如

```java
//---------数据结构--------
private AtomicLong tid;
private ReentrantLock [][]seatLock;
private ReentrantReadWriteLock []ESTLock;
seatLock=new ReentrantLock[routenum][sumSeat];
for(int i=0;i<routenum;i++){
  for(int j=0;j<sumSeat;j++){
    seatLock[i][j]=new ReentrantLock();
  }
}
seatLock=new ReentrantLock[routenum][sumSeat];
for(int i=0;i<routenum;i++){
  for(int j=0;j<sumSeat;j++){
    seatLock[i][j]=new ReentrantLock();
  }
}
//-----------买票----------
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
//-----------查票------------
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
//-----------退票-------------
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
```

<br>

## 多线程测试程序的设计思路

​	对于Test测试程序，最重要的是其中的一个售票代理内部类TicketAgent，该类实现Runnable接口。该类中有一个数据结构ArrayList\<Ticket>  selledList，记录了该售票代理卖出去的票（不包括退回的票）。由于不共享，栈封闭，所以不用考虑线程安全的问题。此外还记录了一些值用来进行系统正确性分析和性能分析。利用Random生成伪随机数来保证按照60%查询余票，30%购票和10%退票的比率以及随机生成车次，起始站，终点站等。利用System.nanoTime()计算每个方法调用之间的时间以计算平均方法调用时间和总时间。

​	对于main程序，第一个参数代表了线程的个数，具体代码如下：

```java
public class Test {
  public static int routenum=10;      //车次总数
  public static int coachnum=80;      //列车的车厢数目
  public static int seatnum=1000;       //每节车厢的座位数
  public static int stationnum=100;    //每个车次经停站的数量
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
        if (access<0.6) {  //查询
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
          //System.out.println("查询，余票数为"+leftTicket);
        }else if(access<0.9){  //买票
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
            //System.out.println("买票成功");
            ++buyCountSuccess;
          }else{
            //System.out.println("买票失败");
            ++buyCountFail;
          }

        }else{   //退票
          if (selledList.isEmpty()){
            --i;  //这次不算
            continue;
          }
          int tid=random.nextInt(selledList.size());
          long startTime=System.nanoTime();
          Boolean isSuccess=tds.refundTicket(selledList.get(tid));
          long endTime=System.nanoTime();
          refundTime+=endTime-startTime;
          if (isSuccess) {
            //System.out.println("退票成功");
            selledList.remove(tid);
          }else{
            //System.out.println("退票失败");
          }
          ++refundCount;
        }
      }
    }
  }


  public static void main(String args[]){
    Test test=new Test();
    if(args.length>0){
      threadNum=Integer.parseInt(args[0]);
    }else{
      threadNum=16;
    }	
    int totalExecTimes=160000;
    TicketingDS tds = new TicketingDS(routenum,coachnum,seatnum,stationnum);
    execTimesAThread=totalExecTimes/threadNum;
    Thread []thread=new Thread[threadNum];
    TicketAgent []agent=new TicketAgent[threadNum];
    for(int i=0;i<threadNum;i++){
      agent[i]=test.new TicketAgent(tds);
      thread[i]=new Thread(agent[i]);
    }
    System.out.println("-----------------------------------------------------");
    long startTime= System.currentTimeMillis();//开始时间
    try {
      for(int i=0;i<threadNum;i++){
        thread[i].start();
      }
      for(int i=0;i<threadNum;i++)
        thread[i].join();
    } catch (Exception e) {
      // TODO: handle exception
    }
    long endTime=System.currentTimeMillis();//结束时间
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
```

<br>

## 系统正确性分析

​	首先，当线程个数为1，即串行情况下，运行没有问题。

​	在执行次数上面经过多次测试符合比率且次数没有问题。

​	每个售票代理记录了各自卖的票，售票系统也进行了相关记录，两者在个数上没有问题，也符合买票退票的个数记录。如下图

​					![捕获2](C:\Users\79422\Desktop\捕获2.PNG)

​	tid是唯一的，在粗粒度锁中，tid是在锁内进行加减的，保证了原子性和可见性。而在细粒度锁中，tid虽然不是在锁内进行加减，但其数据结构为AtomicLong，也保证了原子性和可见性。

​	另外根据程序的设计思路，不会有超卖和有票不卖的情况发生。

<br>

## 系统性能分析

​	首先分析优化以后的粗粒度的读写锁，系统分析的时候要注意垃圾回收机制，因为在回收的过程中可能会影响效率，从而影响性能的分析。可以在调用JVM时指定 -verbose: gc来判断是否执行了垃圾回收操作。在服务器上的执行情况如下图所示。这里我们的测试基准是一共调用160000次方法，线程数分别为4，8，16，32，64。

![捕获](C:\Users\79422\Desktop\捕获.PNG)

​	这里测试的是**粗粒度的读写锁**。由于太长，这里不一一截图。我们将重要的测试数据以表格形式列出。其中AVE Time表示每个方法的平均调用时间	

| Thread num | Total execution time(ms) | Throughput rate | Query AVE time(ns) | Buy  AVE time(ns) | Refund AVE time(ns) |
| ---------- | ------------------------ | --------------- | ------------------ | ----------------- | ------------------- |
| 4          | 318                      | 503144          | 3926               | 10334             | 8597                |
| 8          | 332                      | 481927          | 9508               | 18255             | 14392               |
| 16         | 350                      | 457142          | 21803              | 32853             | 27774               |
| 32         | 353                      | 453257          | 48304              | 67531             | 58332               |
| 64         | 471                      | 339702          | 134271             | 198579            | 135197              |

​	从表中可看出对于粗粒度的读写锁来说，随着线程数的增加，执行时间略有上升，吞吐率略有下降，而每个方法的平均调用时间越来越大，这也是可以理解的，随着线程数增加，调度花的时间也变长，对于一个线程调用方法的调用返回时间也会变的更长。但可以看到，三种方法平均调用时间还是比较平均的，这可能是因为Java的读写锁采用了公平的机制。

​	我们再来看看**细粒度的锁**的性能测试。

| Thread num | Total execution time(ms) | Throughput rate | Query AVE time(ns) | Buy  AVE time(ns) | Refund AVE time(ns) |
| ---------- | ------------------------ | --------------- | ------------------ | ----------------- | ------------------- |
| 4          | 552                      | 289855          | 2180               | 19592             | 17360               |
| 8          | 335                      | 477611          | 2979               | 25785             | 21317               |
| 16         | 388                      | 412370          | 11267              | 63129             | 47957               |
| 32         | 485                      | 329896          | 30360              | 185430            | 52772               |
| 64         | 865                      | 184971          | 157731             | 581654            | 248930              |

​	令人失望的是，细粒度的锁的性能比粗粒度的读写锁性能还要差，在方法平均调用时间，买票花费的时间要高于其他两种方法。

​	但是当我们加大数据的时候，情况又发生了改变，这样的数据比较极端，比如说100个车次，800个车厢，每个车厢100000个座位，100个车站，执行100000次，这时候细粒度锁的性能要比粗粒度好。

<br>

## 其他分析

​	对于**粗粒度的读写锁**来说，情况如下表

|      | 是否可线性化 | deadlock-free | starvation-free | lock-free | wait-free |
| ---- | ------ | ------------- | --------------- | --------- | --------- |
| 查票方法 | 可以     | 是             | 是               | 否         | 否         |
| 买票方法 | 可以     | 是             | 是               | 否         | 否         |
| 退票方法 | 可以     | 是             | 是               | 否         | 否         |

​	粗粒度的读写锁，基本上基于Java的读写锁，因此可以被线性化，由于车次之间不相关，对于每个车次来说只有一把读写锁，所以一定不会死锁（无法构成顺序死锁的条件）。而Java的读写锁应该是公平的，因此每个线程最终都能获得锁，所以不会饿死。另外，无法保证方法在有限步内完成，所以不是lock-free更不是wait-free。

​	对于**细粒度的锁**来说，情况如下表

|      | 是否可线性化 | deadlock-free | starvation-free | lock-free | wait-free |
| ---- | ------ | ------------- | --------------- | --------- | --------- |
| 查票方法 | 是      | 是             | 否               | 否         | 否         |
| 买票方法 | 是      | 是             | 否               | 否         | 否         |
| 退票方法 | 是      | 是             | 否               | 否         | 否         |

​	细粒度的锁的方法利用的是可重入锁和读写锁，三个方法均可线性化，且不会死锁。但有可能会有饥饿问题，另外，无法保证方法在有限步内完成，所以不是lock-free更不是wait-free。