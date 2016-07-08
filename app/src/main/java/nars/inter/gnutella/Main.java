//package nars.inter.gnutella;
//
//import java.io.IOException;
//import java.net.InetAddress;
//import java.net.NetworkInterface;
//import java.util.Enumeration;
//import java.util.Scanner;
//
//
//public class Main {
//
//	public static void main(String[] args) {
//
//		if (args.length == 4 || args.length == 0 || args.length == 1) {
//
//			try {
//				Scanner keyboard = new Scanner(System.in);
//				boolean working = true;
//				Peer peer;
///*****************************PARA SU CORRECTO FUNCIONAMIENTO SE DEBE EDITAR***************************************/
//				peer = new Peer("/home");
//				if (args.length == 4) {
//					try {
//						peer = new Peer(args[2]);
//						peer.connect(args[0], new Short(args[1]));
//						peer.ping();
//						peer.query(args[3]);
//					} catch (IOException e) {
//						// TODO Auto-generated catch block
//
//					}
//
//				}
//
//				int answer;
//				if (args.length == 4) {
//					answer = 6;
//					Thread.sleep(2000);
//
//				} else {
//					if (args.length == 1) {
//						peer = new Peer(args[0]);
//					}
//					Enumeration<NetworkInterface> e = NetworkInterface
//							.getNetworkInterfaces();
//					System.out.println("\tMY IPS: ");
//					while (e.hasMoreElements()) {
//						NetworkInterface n = (NetworkInterface) e.nextElement();
//						Enumeration<InetAddress> ee = n.getInetAddresses();
//						while (ee.hasMoreElements()) {
//							InetAddress i = (InetAddress) ee.nextElement();
//							System.out.println("\t\t" + i.getHostAddress());
//						}
//					}
//					System.out.println("\n\tLISTENING OVER PORT: "
//							+ peer.port());
//					answer = keyboard.nextInt();
//				}
//				while (working) {
//
//					switch (answer) {
//					case 0:
//						peer.stop();
//						working = false;
//						break;
//					case 1:
//						System.out.println("IP: (MUST BE IPv4)");
//						String ip = keyboard.next();
//						//String ip = "localhost";
//						System.out.println("PORT: ");
//						short p = Short.parseShort(keyboard.next());
//						peer.connect(ip, p);
//						break;
//
//					case 3:
//						peer.neighbors();
//						break;
//					case 4:
//						System.out.println("FILE SEARCHING:");
//						String searchCriteria = keyboard.next();
//						peer.query(searchCriteria);
//
//						break;
//					case 2:
//						peer.ping();
//						break;
//					case 5:
//						System.out.println("IP:");
//						String ipD = keyboard.next();
//
//						System.out.println("PORT:");
//						short portD = keyboard.nextShort();
//						System.out.println("FILE NAME:");
//						String nameD = keyboard.next();
//						System.out.println("SIZE: ");
//						int sizeD = keyboard.nextInt();
//						peer.makeDownload(ipD, portD, nameD, sizeD);
//					case 6:
//						if (!peer.getQueryHitMessage().isEmpty()) {
//
//							int marca[] = new int[peer.getQueryHitMessage()
//									.size()];
//							System.out
//									.println("CHOOSE A PAIR \"i j\" FOR DOWNLOAD\n(FOR EXIT TYPE -1 )");
//							int k = 0;
//							for (int i = 0; i < peer.getQueryHitMessage()
//									.size(); i++) {
//								QueryHitMessage tmp = peer.getQueryHitMessage()
//										.get(i);
//
//								for (int j = 0; j < tmp.getFileIndex().length; j++) {
//									System.out.println("(" + i + " " + j + ")"
//											+ ".-" + tmp.getFileName()[j]);
//								}
//								marca[i] = k;
//
//							}
//							int i = keyboard.nextInt();
//
//							if (i == -1) {
//
//							} else {
//								int j = keyboard.nextInt();
//
//								QueryHitMessage tmpQ = peer
//										.getQueryHitMessage().get(i);
//
//								peer.makeDownload(tmpQ.getMyIpAddress()
//										.getHostAddress(), tmpQ.getPort(), tmpQ
//										.getFileName()[j], 0);
//							}
//
//							peer.getQueryHitMessage().removeAll(
//									peer.getQueryHitMessage());
//
//						}
//						break;
//
//					default:
//						break;
//					}
//					System.out
//							.println("\tCURRENT DIRECTORY PATH "
//									+ "/"
//									+ "\n\t1.- CONNECT\n\t2.- MAKE A PING\n\t3.- NEIGHBORS \n\t4.- "
//									+ "MAKE A QUERY\n\t5.- DOWNLOAD \n\t6.- RESULTS OF QUERYS\n\t0.- LOGOUT");
//
//					answer = keyboard.nextInt();
//
//				}
//
//			} catch (IOException e) {
//				e.printStackTrace();
//				System.err.println("[ERROR] NO SUCH FILE OR DIRECTORY");
//				System.exit(0);
//			} catch (IndexOutOfBoundsException e) {
//				System.err.println("YOU HAVE AN ERROR IN ONE INDEX");
//			} catch (InterruptedException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//
//		} else {
//			System.err
//					.println("[ERROR] USE: java Main \n \tOR\t\n  java Main [directory path]\n\tOR\t \n java Main [ip] [port] [directory path] [file name] ");
//		}
//	}
//}
