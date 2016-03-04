package alice.tuprolog.interfaces;

import alice.tuprolog.PrologError;
import alice.tuprolog.Struct;
import alice.tuprolog.Term;

public interface ISocketLib {
    boolean tcp_socket_client_open_2(Struct Address, Term Socket) throws PrologError;
    
    boolean tcp_socket_server_open_3(Struct Address, Term Socket, Struct Options) throws PrologError;
    
    boolean tcp_socket_server_accept_3(Term ServerSock, Term Client_Addr, Term Client_Slave_Socket) throws PrologError;
    
    boolean tcp_socket_server_close_1(Term serverSocket) throws PrologError;
    
    boolean read_from_socket_3(Term Socket, Term Msg, Struct Options) throws PrologError;
    
    boolean write_to_socket_2(Term Socket, Term Msg) throws PrologError;
    
    boolean aread_from_socket_2(Term Socket, Struct Options) throws PrologError;
    
    boolean udp_socket_open_2(Struct Address, Term Socket) throws PrologError;
    
    boolean udp_send_3(Term Socket, Term Data, Struct AddressTo) throws PrologError;
    
    boolean udp_receive(Term Socket, Term Data, Struct AddressFrom, Struct Options) throws PrologError;

    boolean udp_socket_close_1(Term socket) throws PrologError;
}
