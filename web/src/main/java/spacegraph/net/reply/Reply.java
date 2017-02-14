package spacegraph.net.reply;

/**
 * All standard Irc reply messages.
 */
public enum Reply {
	/**
	* Dummy reply number. Not used.
	*/
	RPL_NONE (300),
	/**
	* Reply format used by USERHOST to list replies to the query list.
	*/
	RPL_USERHOST (302),
	/**
	* Reply format used by ISON to list replies to the query list.
	* EX: ":[<nick> {<space><nick>}]"
	*/
	RPL_ISON (303),
	/**
	* Is sent to any client sending a PRIVMSG to a client which is away.
	* EX: "<nick> :<away message>"
	*/
	RPL_AWAY(301),
	/**
	* Sent when the client removes and sets an AWAY message.
	* EX: ":You are no longer marked as being away"
	*/
	RPL_UNAWAY(305),
	/**
	* Sent when the client removes and sets an AWAY message.
	* EX: ":You have been marked as being away"
	*/
	RPL_NOWAWAY (306),
	/**
	* Reply to a WHOIS command.
	* EX: "<nick> <user> <host> * :<real name>"
	*/
	RPL_WHOISUSER (311),
	/**
	* Reply to a WHOIS command.
	* EX: "<nick> <server> :<server info>"
	*/
	RPL_WHOISSERVER(312),
	/**
	* Reply to a WHOIS command.
	* EX: "<nick> :is an IRC operator"
	*/
	RPL_WHOISOPERATOR (313),
	/**
	* Reply to a WHOIS command.
	* EX: "<nick> <integer> :seconds idle"
	*/
	RPL_WHOISIDLE(317),
	/**
	* Reply that ends a WHOIS response.
	* EX: "<nick> :End of /WHOIS list"
	*/
	RPL_ENDOFWHOIS(318),
	/**
	* Reply to a WHOIS command.
	*  "<nick> :{[@|+]<channel><space>}"
	*/
	RPL_WHOISCHANNELS(319),
	/*
	* Reply to a WHOWAS command.
	* EX: "<nick> <user> <host> * :<real name>"
	*/
	RPL_WHOWASUSER(314),
	/**
	* Reply that ends a WHOWAS response.
	* EX: "<nick> :End of WHOWAS"
	*/
	RPL_ENDOFWHOWAS(369),
	/*
	* Starts a LIST response.
	* EX: "Channel :Users  Name"
	*/
	RPL_LISTSTART(321),
	/**
	* Replies to a LIST command.
	* EX: "<channel> <# visible> :<topic>"
	*/
	RPL_LIST (322),
	/**
	* Ends a LIST response.
	* EX: ":End of /LIST"
	*/
	RPL_LISTEND(323),
	/**
	* 
	* EX: "<channel> <mode> <mode params>"
	*/
	RPL_CHANNELMODEIS (324),
	/**
	* Reply when no topic is set
	* EX: "<channel> :No topic is set"
	*/
	RPL_NOTOPIC (331),
	/**
	* Informs the client of the channel topic if one is set. 
	* EX: "<channel> :<topic>"
	*/
	RPL_TOPIC (332),
	/**
	* Informs the client that issued an INVITE command that it was success.
	* EX: "<channel> <nick>"
	*/
	RPL_INVITING (341),
	/**
	* Informs the client that issued an SUMMON command that it was success.
	* EX: "<user> :Summoning user to IRC"
	*/
	RPL_SUMMONING (342),
	/**
	* Replies to a VERSION command.
	* EX: "<version>.<debuglevel> <server> :<comments>"
	*/
	RPL_VERSION(351),
	/**
	* Replies to the WHO command.
	* EX: "<channel> <user> <host> <server> <nick> \ <H|G>[*][@|+] :<hopcount> <real name>"
	*/
	RPL_WHOREPLY (352),
	/**
	* If a list is needed this must be sent after processing each list item.
	* EX: "<name> :End of /WHO list"
	*/
	RPL_ENDOFWHO (315),
	/**
	* Replies to a NAMES command.
	* EX: "<channel> :[[@|+]<nick> [[@|+]<nick> [...]]]"
	*/
	RPL_NAMREPLY (353),
	/**
	* Ends NAMES response. 
	* EX: "<channel> :End of /NAMES list"
	*/
	RPL_ENDOFNAMES (366),
	/**
	 * Replies to a LINKS command.
	 * EX: "<mask> <server> :<hopcount> <server info>"
	 */
	 RPL_LINKS (364),
	 /**
	  * Ends LINKS response.
	  * EX: "<mask> :End of /LINKS list"
	  */
	 RPL_ENDOFLINKS (365),
	 /**
	  * Send with each user on the ban list.
	  * EX: "<channel> <banid>"
	  */
	 RPL_BANLIST (367),
	 /**
	  * Ends a ban list.
	  * EX: "<channel> :End of channel ban list"
	  */
	 RPL_ENDOFBANLIST (368),
	 RPL_INFO (371),
	 RPL_ENDOFINFO (374),
	 RPL_MOTDSTART(375),
	 RPL_MOTD (372),
	 RPL_ENDOFMOTD (376),
	 RPL_YOUREOPER(381),
	 RPL_REHASHING (382),
	 RPL_TIME(391),
	 RPL_USERSSTART(392),
	 RPL_USERS (393),
	 RPL_ENDOFUSERS (394),
	 RPL_NOUSERS (395),
	 RPL_TRACELINK (200),
	 RPL_TRACECONNECTING (201),
	 RPL_TRACEHANDSHAKE (202),
	 RPL_TRACEUNKNOWN(203),
	 RPL_TRACEOPERATOR(204),
	 RPL_TRACEUSER(205),
	 RPL_TRACESERVER(206),
	 RPL_TRACENEWTYPE(208),
	 RPL_TRACELOG(261),
	 RPL_STATSLINKINFO(211),
	 RPL_STATSCOMMANDS(212),
	 RPL_STATSCLINE(213),
	 RPL_STATSNLINE(214),
	 RPL_STATSILINE(215),
	 RPL_STATSKLINE(216),
	 RPL_STATSYLINE(218),
	 RPL_ENDOFSTATS(219),
	 RPL_STATSLLINE(241),
	 RPL_STATSUPTIME(242),
	 RPL_STATSOLINE(243),
	 RPL_STATSHLINE(244),
	 RPL_UMODEIS(221),
	 RPL_LUSERCLIENT(251),
	 RPL_LUSEROP(252),
	 RPL_LUSERUNKNOWN(253),
	 RPL_LUSERCHANNELS(254),
	 RPL_LUSERME(255),
	 RPL_ADMINME(256),
	 RPL_ADMINLOC1(257),
	 RPL_ADMINLOC2(258),
	 RPL_ADMINMAIL(259);
	 
	 
	 
	
	final int code;
	Reply(int code){
		this.code = code;
	}
	
	public int getCode() {
		return code;
	}
	
}
