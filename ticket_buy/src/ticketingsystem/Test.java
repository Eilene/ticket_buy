package ticketingsystem;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class Test {

	public static int routeNum = 5;
	public static int coachNum = 8;
	public static int seatNum = 100;
	public static int stationNum = 10;

	public static void main(String[] args) throws InterruptedException {

		TicketingSystem tds = new TicketingDS(routeNum, coachNum, seatNum, stationNum);
		int num = 4;
		Passenger[] passengers = new Passenger[num];
		long sumTime;
		int count = 10000;
		long s = System.currentTimeMillis();
		for (int i = 0; i < num; i++) {
			passengers[i] = new Passenger("name"+i,tds,num);
			passengers[i].start();
		}
		for (int i = 0; i < num;i ++){
			passengers[i].join();
		}
		sumTime = System.currentTimeMillis() - s;
		System.out.println("在给定参数下，总运行时间：");
		System.out.println(sumTime);
		System.out.println("在给定参数下，平均每个任务执行时间：");
		System.out.println(sumTime*1.0/count);
	}
	
	public static class Passenger extends Thread {
		private String name;
		private TicketingSystem tds;
		private int count;
		private int thread_num;
		private List<Ticket> tickets;
		
		public Passenger(String name, TicketingSystem tds,int thread_num) {
			this.name = name;
			this.tds = tds;
			this.count = 0;
			this.thread_num =thread_num;
			this.tickets = new LinkedList<Ticket>();
		}

		@Override
		public void run() {
			Random r = new Random();
			while (count < (10000.0/thread_num)) {
				count++;
				int type = r.nextInt(10);
				if (type < 3) {//30%的买票
					int a = r.nextInt(routeNum)+1;
					int b = r.nextInt(stationNum-1)+1;
					int c = (r.nextInt(stationNum)+1)%(stationNum-b) + b+1;
					
					Ticket ticket = tds.buyTicket(name, a, b,c);
					
					tickets.add(ticket);
				
				}
				else if(type==3){//10%的退票
					if (!tickets.isEmpty()) {
						Ticket ticket = tickets.get(0);
						boolean flag = tds.refundTicket(ticket);
						
						if (flag) {
							tickets.remove(0);
						} else {
							throw new IllegalStateException("退票异常.正确的票退失败");
						}
					}
				}
				else{//60%的查询
					int a = r.nextInt(routeNum)+1;
					int b = r.nextInt(stationNum-1)+1;
					int c = (r.nextInt(stationNum)+1)%(stationNum-b) + b+1;
					tds.inquiry(a, b, c);
				}
			}
		}
	}
	
}