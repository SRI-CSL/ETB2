# How to use ETB2

(1) After cloning the ETB2 directory, go into the directory and run 'make'

(2) Run the command 'java etb/etbCS/etcServer -h' to see the list of commands

(3) The first thing to do is initializing an ETB node in a given location. This is done by 'java etb/etbCS/etcServer -init'. There will be an interactive prompt to set the port number and git repo that the ETB node will use. 

(4) There are 2 ways of running ETB
	=> 'java etb/etbCS/etcServer -script <path-to-some-datalog-script>' : runs etc on the given script
	=> 'java etb/etbCS/etcServer -query <query>' : invokes a service/tool for a given query

(5) If a service required by a clause in a given datalog script or needed by a query is not yet available in ETB, the service needs to be added by running the command 'java etb/etbCS/etcServer -add-service'. This starts an interactive command prompt asking for the following 4 inputs:
	
	=> service name: a unique identifier for the service 

	=> service signature: this is a list of types for arguments of a given service. Currently we have four  	argument types in ETB. These are 'string', 'file', 'string_list' and 'file_list'. Service signature is a 	list of such types separated by space. For example the signature 'file file_list file' specifies that a 	given service has a file type for its first and third argument, and a list of files for its second 		argument. 

 	=> set of modes: a mode defines which of the service arguments are given as inputs to the service during 	its invocation and which are outputs that are expected to be produced by the service at the end of its run. 	For example, the mode '++-' for a given service with three arguments specifies that the first two arguments 	are inputs to the service (indicated by + sign) and the last argument is the output of the service. A 		service can be invoked in different modes at different times. An example mode set is '{++-, +++}'. 

	=> number of modes: number of possible modes a service can be invoked with. 

If a service is no more required by an ETB node, it can be removed interactively by running the command 'java etb/etbCS/etcServer -rm-service'

(6) Similarly, a remote server can be added interactively by running 'java etb/etbCS/etcServer -add-server'. The server is added using its IP address and port number. A server can also be removed by running the command 'java etb/etbCS/etcServer -rm-server'
