package ticketingsystem;

import java.util.BitSet;
import java.util.concurrent.ConcurrentHashMap;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

public class TicketingDS implements TicketingSystem {

	private int coachNum;//车厢个数
	private int seatNum;//每个车厢座位个数
	private int routeNum;//车次个数
	private int stationNum;//站个数
	
	private BitSet[][][] bs;
	private AtomicLong genId;
	
	private ReentrantLock[][][] bs_lock;
	
	private ConcurrentHashMap<Long, Ticket> sales;//纪录已经卖出去的票，用于做退票验证

	public TicketingDS() {
		this(5, 8, 100, 10);
	}
	
	public TicketingDS(int routenum, int coachnum, int seatnum, int stationnum){
		this.coachNum = coachnum;
		this.routeNum = routenum;
		this.seatNum = seatnum;
		this.stationNum = stationnum;
		this.bs = new BitSet[routenum][stationnum][coachnum];//三维数组，车次个数＊车站个数＊车厢个数
		this.bs_lock = new ReentrantLock[routenum][stationnum][coachnum];
		this.genId = new AtomicLong(0);
		for(int i =0;i<routenum;i++){
			for(int j =0;j<stationnum;j++){
				for(int k = 0;k<coachnum;k++){
					this.bs[i][j][k]=new BitSet(this.seatNum);//初始化每一位是一个车厢内座位个数长度的二进制串
					this.bs[i][j][k].set(0,seatnum);//初始化为全1
					
					this.bs_lock[i][j][k] = new ReentrantLock();//每个元素上加一个锁
				}
			}
		}
		sales= new ConcurrentHashMap<Long, Ticket>();
	}
	
	public boolean checkTicket(int route,int departure,int arrival){
		if (route < 1 || route > routeNum)
			return false;
		if (departure > 0 && departure < arrival && arrival <= stationNum)
			return true;
		return false;
	}
	
	public Ticket buyTicket(String passenger, int route, int departure, int arrival) {
		// TODO Auto-generated method stub
		if (null == passenger || "".equals(passenger))
			return null;
		if (!checkTicket(route, departure, arrival))
			return null;
		int start = departure-1;
		int end = arrival-1;
		boolean isSuccess = false;
		int i;
		int pos = -1;
		//从第一个车厢开始只要找到从[start:end）全是1的便返回
		BitSet res = new BitSet(this.seatNum);
		for(i =0;i<this.coachNum;i++){
			res.set(0,this.seatNum);
			for(int p=start;p<end;p++){//对这个车站区间该车厢里的座位加锁
				bs_lock[route-1][p][i].lock();
			}
			try{
				for(int j = start; j < end; j++){
					res.and(bs[route-1][j][i]);
				}
				pos = res.nextSetBit(0);
				if(pos!=-1){
					//找到票,车厢是coachnum，座位号是pos
					for(int k = start;k<end;k++){
						bs[route-1][k][i].set(pos,false);//将指定的座位置为0
					}
					isSuccess = true;
					break;
				}
			}finally{
				for(int p=start;p<end;p++){
					bs_lock[route-1][p][i].unlock();
				}
			}
		}
		if(isSuccess == false)
			return null;
		Ticket ticket = new Ticket();
		ticket.tid = this.genId.incrementAndGet();
		ticket.passenger = passenger;
		ticket.departure = departure;
		ticket.arrival = arrival;
		ticket.coach = i+1;
		ticket.route = route;
		ticket.seat = pos+1;
		
		
		Ticket ticket2 = new Ticket();
		ticket2.tid = ticket.tid;
		ticket2.passenger = passenger;
		ticket2.departure = departure;
		ticket2.arrival = arrival;
		ticket2.coach = i+1;
		ticket2.route = route;
		ticket2.seat = pos+1;
		sales.put(ticket.tid, ticket2);
	
		return ticket;
	}

	public int inquiry(int route, int departure, int arrival) {
		// TODO Auto-generated method stub
		if (!checkTicket(route, departure, arrival))
			return 0;
		int start = departure-1;
		int end  = arrival-1;
		//只需要验证从[start:end）全是1的便是有座
		BitSet res = new BitSet(this.seatNum);
		int ticket_sum = 0;
		for(int i = 0; i<this.coachNum;i++){
			res.set(0,this.seatNum);
			for(int j=start;j<end;j++){
				res.and(bs[route-1][j][i]);
			}
			ticket_sum += res.cardinality();
		}
		return ticket_sum;
	}

	public boolean refundTicket(Ticket ticket) {
		// TODO Auto-generated method stub
		//确认这个票是之前卖过的
		//System.out.println(sales.get(1).coach);
		if(!sales.containsKey(ticket.tid)){
			return false;
		}
		Ticket temp = sales.get(ticket.tid);
		if(sales.containsKey(ticket.tid)&&temp.arrival==ticket.arrival&&temp.coach==ticket.coach&&temp.departure==ticket.departure&&temp.passenger==ticket.passenger&&temp.route==ticket.route&&temp.seat==ticket.seat){
			int start = ticket.departure-1;
			int end = ticket.arrival-1;
			int coach_refound = ticket.coach-1;
			int seat_refound = ticket.seat-1; 
			int route_refound = ticket.route-1;
			//把route-1 的coach-1的seat-1的start 到end 置为1
			
			for(int i =start;i<end;i++){//对这个车次这个区间的这个车厢的所有位置加锁
				bs_lock[route_refound][i][coach_refound].lock();
			}
			try{
				for(int i = start;i<end;i++){
					bs[route_refound][i][coach_refound].set(seat_refound);//把这个位置重新设置为1
				}
			}finally{
				for(int i =start;i<end;i++){
					bs_lock[route_refound][i][coach_refound].unlock();;
				}
			}
			sales.remove(ticket);
			return true;
		}
		return false;
	}
}
