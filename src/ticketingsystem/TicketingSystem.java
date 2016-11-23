package ticketingsystem;

class Ticket{
	long tid;
	String passenger;
	int route;           //�г�����
	int coach;           //�����
	int seat;            //��λ��
	int departure;       //����վ���
	int arrival;         //����վ���
}

public interface TicketingSystem{
	Ticket buyTicket(String passenger,int route,int departure,int arrival);
	int inquiry(int route,int departure,int arrival); 
	boolean refundTicket(Ticket ticket);
}
