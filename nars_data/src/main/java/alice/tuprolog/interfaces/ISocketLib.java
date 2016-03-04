package alice.tuprolog.interfaces;

import alice.tuprolog.PTerm;
import alice.tuprolog.PrologError;
import alice.tuprolog.Struct;

public interface ISocketLib {
    boolean tcp_socket_client_open_2(Struct Address, PTerm Socket) throws PrologError;
    
    boolean tcp_socket_server_open_3(Struct Address, PTerm Socket, Struct Options) throws PrologError;
    
    boolean tcp_socket_server_accept_3(PTerm ServerSock, PTerm Client_Addr, PTerm Client_Slave_Socket) throws PrologError;
    
    boolean tcp_socket_server_close_1(PTerm serverSocket) throws PrologError;
    
    boolean read_from_socket_3(PTerm Socket, PTerm Msg, Struct Options) throws PrologError;
    
    boolean write_to_socket_2(PTerm Socket, PTerm Msg) throws PrologError;
    
    boolean aread_from_socket_2(PTerm Socket, Struct Options) throws PrologError;
    
    boolean udp_socket_open_2(Struct Address, PTerm Socket) throws PrologError;
    
    boolean udp_send_3(PTerm Socket, PTerm Data, Struct AddressTo) throws PrologError;
    
    boolean udp_receive(PTerm Socket, PTerm Data, Struct AddressFrom, Struct Options) throws PrologError;

    boolean udp_socket_close_1(PTerm socket) throws PrologError;
}
